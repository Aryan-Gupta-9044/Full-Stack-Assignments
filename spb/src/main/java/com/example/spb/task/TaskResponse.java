package com.example.spb.task;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** What the API returns for a task (adds the computed "overdue" flag). */
public record TaskResponse(
		Long id,
		String title,
		String description,
		Priority priority,
		LocalDate dueDate,
		boolean done,
		boolean overdue,
		LocalDateTime createdAt,
		LocalDateTime completedAt) {

	public static TaskResponse from(Task t, LocalDate today) {
		return new TaskResponse(t.getId(), t.getTitle(), t.getDescription(), t.getPriority(),
				t.getDueDate(), t.isDone(), TaskQuery.isOverdue(t, today), t.getCreatedAt(), t.getCompletedAt());
	}
}
