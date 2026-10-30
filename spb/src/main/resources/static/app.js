'use strict';

const $ = (sel) => document.querySelector(sel);
const state = { status: 'all', priority: '', q: '', sort: 'created' };
let editingId = null;
let toastTimer = null;

/* ---------- API helper ---------- */
async function api(path, options = {}) {
  const res = await fetch(path, {
    headers: { 'Content-Type': 'application/json' },
    ...options,
  });
  if (res.status === 204) return null;
  const isJson = (res.headers.get('content-type') || '').includes('json');
  const body = isJson ? await res.json() : await res.text();
  if (!res.ok) throw new Error(errorMessage(body, res.status));
  return body;
}

function errorMessage(body, status) {
  if (body && typeof body === 'object') {
    const fields = body.fieldErrors ? Object.values(body.fieldErrors) : [];
    if (fields.length) return fields.join('. ');
    if (body.message) return body.message;
  }
  return `Request failed (${status})`;
}

function toast(message, ok = false) {
  const el = $('#toast');
  el.textContent = message;
  el.className = 'toast' + (ok ? ' ok' : '');
  el.hidden = false;
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => { el.hidden = true; }, 3500);
}

async function guarded(action) {
  try {
    await action();
  } catch (e) {
    toast(e && e.name === 'TypeError' ? 'Cannot reach the server. Is it running?' : e.message);
  }
}

/* ---------- dates ---------- */
function parseDate(iso) {            // "2026-10-20" -> local Date, no timezone shift
  const [y, m, d] = iso.split('-').map(Number);
  return new Date(y, m - 1, d);
}

function dueLabel(task) {
  if (!task.dueDate) return null;
  const today = new Date(); today.setHours(0, 0, 0, 0);
  const days = Math.round((parseDate(task.dueDate) - today) / 86400000);
  const pretty = parseDate(task.dueDate).toLocaleDateString(undefined, { day: 'numeric', month: 'short' });
  if (task.done) return { text: pretty, cls: '' };
  if (days < 0) return { text: `Overdue ${-days}d (${pretty})`, cls: 'late' };
  if (days === 0) return { text: 'Due today', cls: 'soon' };
  if (days === 1) return { text: 'Due tomorrow', cls: 'soon' };
  return { text: `Due ${pretty}`, cls: '' };
}

/* ---------- rendering ---------- */
function queryString() {
  const p = new URLSearchParams();
  if (state.status !== 'all') p.set('status', state.status);
  if (state.priority) p.set('priority', state.priority);
  if (state.q) p.set('q', state.q);
  p.set('sort', state.sort);
  return p.toString();
}

async function refresh() {
  const [tasks, stats] = await Promise.all([api('/api/tasks?' + queryString()), api('/api/tasks/stats')]);
  renderList(tasks);
  renderStats(stats);
  $('#exportLink').href = '/api/tasks/export.csv?' + queryString();
}

function renderStats(s) {
  $('#stTotal').textContent = s.total;
  $('#stOpen').textContent = s.open;
  $('#stOverdue').textContent = s.overdue;
  $('#stToday').textContent = s.dueToday;
  $('#stRate').textContent = s.completionRate + '%';
  document.querySelector('.stats .warn').classList.toggle('hot', s.overdue > 0);
}

function el(tag, className, text) {
  const node = document.createElement(tag);
  if (className) node.className = className;
  if (text !== undefined) node.textContent = text;   // textContent only: user text can never inject HTML
  return node;
}

function renderList(tasks) {
  const list = $('#list');
  list.replaceChildren();
  $('#empty').hidden = tasks.length > 0;

  for (const t of tasks) {
    const li = el('li', `item p-${t.priority}${t.done ? ' done' : ''}${t.overdue ? ' overdue' : ''}`);
    li.dataset.id = t.id;

    const box = el('input');
    box.type = 'checkbox';
    box.checked = t.done;
    box.setAttribute('aria-label', 'Mark done: ' + t.title);
    box.addEventListener('change', () => guarded(async () => { await api(`/api/tasks/${t.id}/toggle`, { method: 'PATCH' }); await refresh(); }));

    const body = el('div');
    body.append(el('div', 'title', t.title));
    if (t.description) body.append(el('div', 'details', t.description));
    const meta = el('div', 'meta');
    meta.append(el('span', 'badge', t.priority.toLowerCase()));
    const due = dueLabel(t);
    if (due) meta.append(el('span', 'badge ' + due.cls, due.text));
    body.append(meta);

    const actions = el('div', 'actions');
    const edit = el('button', 'ghost', 'Edit');
    edit.type = 'button';
    edit.addEventListener('click', () => openEdit(t));
    const del = el('button', 'ghost danger', 'Delete');
    del.type = 'button';
    del.addEventListener('click', () => guarded(async () => { await api(`/api/tasks/${t.id}`, { method: 'DELETE' }); await refresh(); }));
    actions.append(edit, del);

    li.append(box, body, actions);
    list.append(li);
  }
}

/* ---------- add / edit ---------- */
$('#addForm').addEventListener('submit', (ev) => {
  ev.preventDefault();
  guarded(async () => {
    const payload = {
      title: $('#title').value,
      description: $('#desc').value,
      priority: $('#priority').value,
      dueDate: $('#due').value || null,
    };
    await api('/api/tasks', { method: 'POST', body: JSON.stringify(payload) });
    $('#addForm').reset();
    $('#priority').value = 'MEDIUM';
    await refresh();
    $('#title').focus();
  });
});

function openEdit(t) {
  editingId = t.id;
  $('#eTitle').value = t.title;
  $('#eDesc').value = t.description || '';
  $('#ePriority').value = t.priority;
  $('#eDue').value = t.dueDate || '';
  const dlg = $('#editDialog');
  if (dlg.showModal) dlg.showModal(); else dlg.setAttribute('open', '');
}

function closeEdit() {
  const dlg = $('#editDialog');
  if (dlg.close) dlg.close(); else dlg.removeAttribute('open');
}

$('#eCancel').addEventListener('click', closeEdit);
$('#editForm').addEventListener('submit', (ev) => {
  ev.preventDefault();
  guarded(async () => {
    const payload = {
      title: $('#eTitle').value,
      description: $('#eDesc').value,
      priority: $('#ePriority').value,
      dueDate: $('#eDue').value || null,
    };
    await api(`/api/tasks/${editingId}`, { method: 'PUT', body: JSON.stringify(payload) });
    closeEdit();
    await refresh();
    toast('Task updated', true);
  });
});

/* ---------- toolbar ---------- */
let searchTimer = null;
$('#search').addEventListener('input', (ev) => {
  clearTimeout(searchTimer);
  searchTimer = setTimeout(() => { state.q = ev.target.value.trim(); guarded(refresh); }, 250);
});

document.querySelectorAll('.tab').forEach((btn) => btn.addEventListener('click', () => {
  document.querySelectorAll('.tab').forEach((b) => b.classList.toggle('active', b === btn));
  state.status = btn.dataset.status;
  guarded(refresh);
}));

$('#fPriority').addEventListener('change', (ev) => { state.priority = ev.target.value; guarded(refresh); });
$('#sort').addEventListener('change', (ev) => { state.sort = ev.target.value; guarded(refresh); });

$('#clearDone').addEventListener('click', () => guarded(async () => {
  if (!window.confirm('Delete all completed tasks?')) return;
  const r = await api('/api/tasks/completed', { method: 'DELETE' });
  await refresh();
  toast(`Removed ${r.deleted} completed task(s)`, true);
}));

/* ---------- quote (external API via backend) ---------- */
async function loadQuote(fresh = false) {
  try {
    const q = await api('/api/quote' + (fresh ? '?fresh=true' : ''));
    $('#quoteText').textContent = '\u201C' + q.text + '\u201D';
    $('#quoteAuthor').textContent = '\u2014 ' + q.author + (q.source === 'offline' ? ' (offline quote)' : '');
  } catch (e) {
    $('#quoteText').textContent = 'Stay focused and keep going.';
    $('#quoteAuthor').textContent = '';
  }
}
$('#quoteBtn').addEventListener('click', () => loadQuote(true));

/* ---------- theme ---------- */
function applyTheme(theme) {
  if (theme) document.documentElement.dataset.theme = theme;
}
try { applyTheme(localStorage.getItem('theme')); } catch (e) { /* storage unavailable */ }
$('#themeBtn').addEventListener('click', () => {
  const dark = document.documentElement.dataset.theme === 'dark'
    || (!document.documentElement.dataset.theme && window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches);
  const next = dark ? 'light' : 'dark';
  applyTheme(next);
  try { localStorage.setItem('theme', next); } catch (e) { /* ignore */ }
});

/* ---------- start ---------- */
guarded(refresh);
loadQuote();
