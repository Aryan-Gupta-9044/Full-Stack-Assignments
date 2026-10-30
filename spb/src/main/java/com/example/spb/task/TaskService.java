package com.example.spb.task;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class TaskService {

	private final TaskRepository repository;

	public TaskService(TaskRepository repository) {
		this.repository = repository;
	}

	@Transactional(readOnly = true)
	public List<Task> list(String status, String priority, String q, String sort) {
		List<Task> filtered = TaskQuery.filter(repository.findAll(),
				TaskQuery.Status.parse(status), TaskQuery.parsePriority(priority), q);
		return TaskQuery.sort(filtered, TaskQuery.Sort.parse(sort));
	}

	@Transactional(readOnly = true)
	public Task get(Long id) {
		return repository.findById(id).orElseThrow(() -> new TaskNotFoundException(id));
	}

	public Task create(TaskRequest req) {
		Task task = new Task(req.title().trim(), clean(req.description()), req.priority(), req.dueDate());
		task.markDone(Boolean.TRUE.equals(req.done()));
		return repository.save(task);
	}

	public Task update(Long id, TaskRequest req) {
		Task task = get(id);
		task.setTitle(req.title().trim());
		task.setDescription(clean(req.description()));
		task.setPriority(req.priority());
		task.setDueDate(req.dueDate());
		if (req.done() != null) {
			task.markDone(req.done());
		}
		return repository.save(task);
	}

	public Task toggle(Long id) {
		Task task = get(id);
		task.markDone(!task.isDone());
		return repository.save(task);
	}

	public void delete(Long id) {
		if (!repository.existsById(id)) {
			throw new TaskNotFoundException(id);
		}
		repository.deleteById(id);
	}

	public int deleteCompleted() {
		List<Task> completed = repository.findAll().stream().filter(Task::isDone).toList();
		repository.deleteAll(completed);
		return completed.size();
	}

	@Transactional(readOnly = true)
	public TaskStats stats() {
		return TaskQuery.stats(repository.findAll(), LocalDate.now());
	}

	@Transactional(readOnly = true)
	public String exportCsv(String status, String priority, String q, String sort) {
		return TaskQuery.toCsv(list(status, priority, q, sort));
	}

	private static String clean(String s) {
		return s == null || s.isBlank() ? null : s.trim();
	}
}
