package com.example.spb.task;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "tasks")
public class Task {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 120)
	private String title;

	@Column(length = 500)
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private Priority priority = Priority.MEDIUM;

	private LocalDate dueDate;

	@Column(nullable = false)
	private boolean done = false;

	@Column(nullable = false)
	private LocalDateTime createdAt = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);

	private LocalDateTime completedAt;

	protected Task() {
		// required by JPA
	}

	public Task(String title, String description, Priority priority, LocalDate dueDate) {
		this.title = title;
		this.description = description;
		this.priority = priority == null ? Priority.MEDIUM : priority;
		this.dueDate = dueDate;
	}

	/** Keeps completedAt in sync with the done flag. */
	public void markDone(boolean value) {
		if (value && !done) {
			completedAt = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
		} else if (!value) {
			completedAt = null;
		}
		this.done = value;
	}

	public Long getId() { return id; }
	public String getTitle() { return title; }
	public void setTitle(String title) { this.title = title; }
	public String getDescription() { return description; }
	public void setDescription(String description) { this.description = description; }
	public Priority getPriority() { return priority; }
	public void setPriority(Priority priority) { this.priority = priority == null ? Priority.MEDIUM : priority; }
	public LocalDate getDueDate() { return dueDate; }
	public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
	public boolean isDone() { return done; }
	public LocalDateTime getCreatedAt() { return createdAt; }
	public LocalDateTime getCompletedAt() { return completedAt; }
}
