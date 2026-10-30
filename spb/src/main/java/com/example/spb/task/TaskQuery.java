package com.example.spb.task;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Pure, framework-free logic: filtering, sorting, statistics and CSV export.
 * Keeping it free of Spring/JPA makes it trivial to unit test.
 */
public final class TaskQuery {

	private TaskQuery() {
	}

	public enum Status {
		ALL, OPEN, DONE;

		public static Status parse(String raw) {
			if (raw == null || raw.isBlank()) return ALL;
			try {
				return valueOf(raw.trim().toUpperCase(Locale.ROOT));
			} catch (IllegalArgumentException e) {
				throw new IllegalArgumentException("Unknown status '" + raw + "' (use all, open or done)");
			}
		}
	}

	public enum Sort {
		CREATED, DUE, PRIORITY;

		public static Sort parse(String raw) {
			if (raw == null || raw.isBlank()) return CREATED;
			try {
				return valueOf(raw.trim().toUpperCase(Locale.ROOT));
			} catch (IllegalArgumentException e) {
				throw new IllegalArgumentException("Unknown sort '" + raw + "' (use created, due or priority)");
			}
		}
	}

	public static Priority parsePriority(String raw) {
		if (raw == null || raw.isBlank()) return null;
		try {
			return Priority.valueOf(raw.trim().toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("Unknown priority '" + raw + "' (use low, medium or high)");
		}
	}

	public static boolean isOverdue(Task t, LocalDate today) {
		return !t.isDone() && t.getDueDate() != null && t.getDueDate().isBefore(today);
	}

	public static List<Task> filter(List<Task> tasks, Status status, Priority priority, String q) {
		String needle = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);
		return tasks.stream()
				.filter(t -> status == Status.ALL || (status == Status.DONE) == t.isDone())
				.filter(t -> priority == null || t.getPriority() == priority)
				.filter(t -> needle.isEmpty()
						|| t.getTitle().toLowerCase(Locale.ROOT).contains(needle)
						|| (t.getDescription() != null && t.getDescription().toLowerCase(Locale.ROOT).contains(needle)))
				.toList();
	}

	public static List<Task> sort(List<Task> tasks, Sort sort) {
		Comparator<Task> newestFirst = Comparator.comparing(Task::getCreatedAt).reversed()
				.thenComparing(Comparator.comparing(Task::getId, Comparator.nullsLast(Comparator.<Long>naturalOrder())).reversed());
		Comparator<Task> dueSoonest = Comparator.comparing(Task::getDueDate, Comparator.nullsLast(Comparator.<LocalDate>naturalOrder()));
		Comparator<Task> highestFirst = Comparator.comparing(Task::getPriority).reversed();

		Comparator<Task> cmp = switch (sort) {
			case DUE -> dueSoonest.thenComparing(highestFirst).thenComparing(newestFirst);
			case PRIORITY -> highestFirst.thenComparing(dueSoonest).thenComparing(newestFirst);
			case CREATED -> newestFirst;
		};
		return tasks.stream().sorted(cmp).toList();
	}

	public static TaskStats stats(List<Task> tasks, LocalDate today) {
		int total = tasks.size();
		int done = 0, overdue = 0, dueToday = 0, highOpen = 0;
		for (Task t : tasks) {
			if (t.isDone()) {
				done++;
				continue;
			}
			if (isOverdue(t, today)) overdue++;
			if (today.equals(t.getDueDate())) dueToday++;
			if (t.getPriority() == Priority.HIGH) highOpen++;
		}
		int rate = total == 0 ? 0 : Math.round(done * 100f / total);
		return new TaskStats(total, total - done, done, overdue, dueToday, highOpen, rate);
	}

	public static String toCsv(List<Task> tasks) {
		StringBuilder sb = new StringBuilder("id,title,description,priority,due_date,done,created_at,completed_at\r\n");
		for (Task t : tasks) {
			sb.append(t.getId()).append(',')
					.append(csv(t.getTitle())).append(',')
					.append(csv(t.getDescription())).append(',')
					.append(t.getPriority()).append(',')
					.append(t.getDueDate() == null ? "" : t.getDueDate()).append(',')
					.append(t.isDone()).append(',')
					.append(t.getCreatedAt()).append(',')
					.append(t.getCompletedAt() == null ? "" : t.getCompletedAt())
					.append("\r\n");
		}
		return sb.toString();
	}

	/** Quotes a CSV field and neutralises spreadsheet formula injection (=, +, -, @). */
	static String csv(String value) {
		if (value == null) return "";
		String v = value;
		if (!v.isEmpty() && "=+-@".indexOf(v.charAt(0)) >= 0) {
			v = "'" + v;
		}
		return "\"" + v.replace("\"", "\"\"") + "\"";
	}
}
