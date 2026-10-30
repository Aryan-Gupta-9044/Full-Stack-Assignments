package com.example.spb.task;

public record TaskStats(
		int total,
		int open,
		int done,
		int overdue,
		int dueToday,
		int highPriorityOpen,
		int completionRate) {
}
