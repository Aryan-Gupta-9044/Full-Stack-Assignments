package com.example.spb.task;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

	private final TaskService service;

	public TaskController(TaskService service) {
		this.service = service;
	}

	/** GET /api/tasks?status=open|done|all&priority=high&q=text&sort=created|due|priority */
	@GetMapping
	public List<TaskResponse> list(
			@RequestParam(name = "status", required = false) String status,
			@RequestParam(name = "priority", required = false) String priority,
			@RequestParam(name = "q", required = false) String q,
			@RequestParam(name = "sort", required = false) String sort) {
		LocalDate today = LocalDate.now();
		return service.list(status, priority, q, sort).stream().map(t -> TaskResponse.from(t, today)).toList();
	}

	@GetMapping("/stats")
	public TaskStats stats() {
		return service.stats();
	}

	/** Same filters as the list endpoint, returned as a downloadable CSV file. */
	@GetMapping("/export.csv")
	public ResponseEntity<String> export(
			@RequestParam(name = "status", required = false) String status,
			@RequestParam(name = "priority", required = false) String priority,
			@RequestParam(name = "q", required = false) String q,
			@RequestParam(name = "sort", required = false) String sort) {
		String csv = service.exportCsv(status, priority, q, sort);
		return ResponseEntity.ok()
				.contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
				.header(HttpHeaders.CONTENT_DISPOSITION,
						ContentDisposition.attachment().filename("tasks.csv").build().toString())
				.body(csv);
	}

	@GetMapping("/{id}")
	public TaskResponse get(@PathVariable("id") Long id) {
		return TaskResponse.from(service.get(id), LocalDate.now());
	}

	@PostMapping
	public ResponseEntity<TaskResponse> create(@Valid @RequestBody TaskRequest request) {
		Task saved = service.create(request);
		return ResponseEntity.created(URI.create("/api/tasks/" + saved.getId()))
				.body(TaskResponse.from(saved, LocalDate.now()));
	}

	@PutMapping("/{id}")
	public TaskResponse update(@PathVariable("id") Long id, @Valid @RequestBody TaskRequest request) {
		return TaskResponse.from(service.update(id, request), LocalDate.now());
	}

	@PatchMapping("/{id}/toggle")
	public TaskResponse toggle(@PathVariable("id") Long id) {
		return TaskResponse.from(service.toggle(id), LocalDate.now());
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
		service.delete(id);
		return ResponseEntity.noContent().build();
	}

	/** Bulk "clear completed". */
	@DeleteMapping("/completed")
	public Map<String, Integer> deleteCompleted() {
		return Map.of("deleted", service.deleteCompleted());
	}
}
