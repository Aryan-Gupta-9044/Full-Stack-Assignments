package com.example.spb.task;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Pure unit tests: no Spring context, runs in milliseconds. */
class TaskQueryTest {

	private static final LocalDate TODAY = LocalDate.of(2026, 10, 20);

	private static Task task(String title, String desc, Priority p, LocalDate due, boolean done) {
		Task t = new Task(title, desc, p, due);
		t.markDone(done);
		return t;
	}

	private final Task overdueHigh = task("Pay fees", null, Priority.HIGH, TODAY.minusDays(1), false);
	private final Task todayMedium = task("Submit report", "weekly status", Priority.MEDIUM, TODAY, false);
	private final Task laterLow = task("Plan trip", "check trains", Priority.LOW, TODAY.plusDays(5), false);
	private final Task noDueHigh = task("Read docs", null, Priority.HIGH, null, false);
	private final Task finished = task("Old task", null, Priority.HIGH, TODAY.minusDays(9), true);
	private final List<Task> all = List.of(overdueHigh, todayMedium, laterLow, noDueHigh, finished);

	@Test
	void overdueIgnoresDoneTasksAndMissingDates() {
		assertThat(TaskQuery.isOverdue(overdueHigh, TODAY)).isTrue();
		assertThat(TaskQuery.isOverdue(todayMedium, TODAY)).isFalse();
		assertThat(TaskQuery.isOverdue(noDueHigh, TODAY)).isFalse();
		assertThat(TaskQuery.isOverdue(finished, TODAY)).isFalse();
	}

	@Test
	void filtersByStatus() {
		assertThat(TaskQuery.filter(all, TaskQuery.Status.ALL, null, null)).hasSize(5);
		assertThat(TaskQuery.filter(all, TaskQuery.Status.OPEN, null, null)).hasSize(4).doesNotContain(finished);
		assertThat(TaskQuery.filter(all, TaskQuery.Status.DONE, null, null)).containsExactly(finished);
	}

	@Test
	void filtersByPriorityAndSearchesTitleAndDescriptionCaseInsensitively() {
		assertThat(TaskQuery.filter(all, TaskQuery.Status.OPEN, Priority.HIGH, null))
				.containsExactlyInAnyOrder(overdueHigh, noDueHigh);
		assertThat(TaskQuery.filter(all, TaskQuery.Status.ALL, null, "TRAINS")).containsExactly(laterLow);
		assertThat(TaskQuery.filter(all, TaskQuery.Status.ALL, null, "  report ")).containsExactly(todayMedium);
		assertThat(TaskQuery.filter(all, TaskQuery.Status.ALL, null, "zzz")).isEmpty();
	}

	@Test
	void sortsByDueDateWithMissingDatesLast() {
		assertThat(TaskQuery.sort(all, TaskQuery.Sort.DUE))
				.containsExactly(finished, overdueHigh, todayMedium, laterLow, noDueHigh);
	}

	@Test
	void sortsByPriorityThenDueDate() {
		List<Task> sorted = TaskQuery.sort(all, TaskQuery.Sort.PRIORITY);
		assertThat(sorted.subList(0, 3)).containsExactly(finished, overdueHigh, noDueHigh);
		assertThat(sorted.subList(3, 5)).containsExactly(todayMedium, laterLow);
	}

	@Test
	void statsAreComputedCorrectly() {
		TaskStats s = TaskQuery.stats(all, TODAY);
		assertThat(s.total()).isEqualTo(5);
		assertThat(s.done()).isEqualTo(1);
		assertThat(s.open()).isEqualTo(4);
		assertThat(s.overdue()).isEqualTo(1);
		assertThat(s.dueToday()).isEqualTo(1);
		assertThat(s.highPriorityOpen()).isEqualTo(2);
		assertThat(s.completionRate()).isEqualTo(20);
	}

	@Test
	void statsOfEmptyListAreZero() {
		TaskStats s = TaskQuery.stats(List.of(), TODAY);
		assertThat(s).isEqualTo(new TaskStats(0, 0, 0, 0, 0, 0, 0));
	}

	@Test
	void markDoneTracksCompletionTime() {
		Task t = new Task("x", null, null, null);
		assertThat(t.getPriority()).isEqualTo(Priority.MEDIUM);
		assertThat(t.getCompletedAt()).isNull();
		t.markDone(true);
		assertThat(t.isDone()).isTrue();
		assertThat(t.getCompletedAt()).isNotNull();
		t.markDone(false);
		assertThat(t.getCompletedAt()).isNull();
	}

	@Test
	void parsersAcceptAnyCaseAndRejectGarbage() {
		assertThat(TaskQuery.Status.parse(null)).isEqualTo(TaskQuery.Status.ALL);
		assertThat(TaskQuery.Status.parse("Open")).isEqualTo(TaskQuery.Status.OPEN);
		assertThat(TaskQuery.Sort.parse("")).isEqualTo(TaskQuery.Sort.CREATED);
		assertThat(TaskQuery.parsePriority("high")).isEqualTo(Priority.HIGH);
		assertThat(TaskQuery.parsePriority(" ")).isNull();
		assertThatThrownBy(() -> TaskQuery.Status.parse("nope")).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> TaskQuery.Sort.parse("nope")).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> TaskQuery.parsePriority("urgent")).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void csvEscapesQuotesCommasAndFormulas() {
		Task t = task("=HYPERLINK(\"x\")", "a, \"b\"", Priority.LOW, TODAY, false);
		String csv = TaskQuery.toCsv(List.of(t));
		String[] lines = csv.split("\r\n");
		assertThat(lines[0]).startsWith("id,title,description,priority,due_date,done");
		assertThat(lines[1]).contains("\"'=HYPERLINK(\"\"x\"\")\"");
		assertThat(lines[1]).contains("\"a, \"\"b\"\"\"");
		assertThat(lines[1]).contains(",LOW,2026-10-20,false,");
	}
}
