package com.example.spb.config;

import com.example.spb.task.Priority;
import com.example.spb.task.Task;
import com.example.spb.task.TaskRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/** Adds a few sample tasks on the very first run (empty database). Disable with app.seed=false. */
@Component
public class DataSeeder implements CommandLineRunner {

	private final TaskRepository repository;
	private final boolean enabled;

	public DataSeeder(TaskRepository repository, @Value("${app.seed:true}") boolean enabled) {
		this.repository = repository;
		this.enabled = enabled;
	}

	@Override
	public void run(String... args) {
		if (!enabled || repository.count() > 0) {
			return;
		}
		LocalDate today = LocalDate.now();
		repository.save(new Task("Finish Spring Boot assignment", "Controller, service, repository, tests", Priority.HIGH, today.plusDays(1)));
		repository.save(new Task("Revise REST API concepts", "Status codes, validation, error handling", Priority.MEDIUM, today.plusDays(3)));
		repository.save(new Task("Pay hostel fees", null, Priority.HIGH, today.minusDays(1)));
		repository.save(new Task("Plan weekend trip", "Check train timings", Priority.LOW, null));
	}
}
