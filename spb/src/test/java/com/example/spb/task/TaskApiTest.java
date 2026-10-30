package com.example.spb.task;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end test: starts the real app on a random port with an in-memory database
 * and talks to it over HTTP (no Spring test helpers, so it works on any Boot version).
 */
@SpringBootTest(
		webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = {"app.seed=false", "spring.datasource.url=jdbc:h2:mem:apitest;DB_CLOSE_DELAY=-1"})
class TaskApiTest {

	@Value("${local.server.port}")
	int port;

	@Autowired
	TaskRepository repository;

	private final HttpClient http = HttpClient.newHttpClient();

	@BeforeEach
	void clean() {
		repository.deleteAll();
	}

	private HttpResponse<String> call(String method, String path, String json) throws Exception {
		HttpRequest.Builder b = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
				.header("Content-Type", "application/json");
		b.method(method, json == null ? BodyPublishers.noBody() : BodyPublishers.ofString(json));
		return http.send(b.build(), HttpResponse.BodyHandlers.ofString());
	}

	private long create(String title, String priority, String due) throws Exception {
		String json = "{\"title\":\"" + title + "\",\"priority\":\"" + priority + "\""
				+ (due == null ? "" : ",\"dueDate\":\"" + due + "\"") + "}";
		HttpResponse<String> r = call("POST", "/api/tasks", json);
		assertThat(r.statusCode()).isEqualTo(201);
		Matcher m = Pattern.compile("\"id\":(\\d+)").matcher(r.body());
		assertThat(m.find()).isTrue();
		return Long.parseLong(m.group(1));
	}

	@Test
	void createReturns201WithLocationAndDefaults() throws Exception {
		HttpResponse<String> r = call("POST", "/api/tasks", "{\"title\":\"  Buy milk  \"}");
		assertThat(r.statusCode()).isEqualTo(201);
		assertThat(r.headers().firstValue("Location")).isPresent();
		assertThat(r.body()).contains("\"title\":\"Buy milk\"", "\"priority\":\"MEDIUM\"", "\"done\":false", "\"overdue\":false");
	}

	@Test
	void validationErrorsAreReportedPerField() throws Exception {
		HttpResponse<String> r = call("POST", "/api/tasks", "{\"title\":\"   \"}");
		assertThat(r.statusCode()).isEqualTo(400);
		assertThat(r.body()).contains("\"fieldErrors\"", "Title is required");
		assertThat(call("POST", "/api/tasks", "{\"title\":\"" + "x".repeat(121) + "\"}").statusCode()).isEqualTo(400);
	}

	@Test
	void malformedBodyAndBadEnumAre400() throws Exception {
		assertThat(call("POST", "/api/tasks", "{not json").statusCode()).isEqualTo(400);
		assertThat(call("POST", "/api/tasks", "{\"title\":\"a\",\"priority\":\"URGENT\"}").statusCode()).isEqualTo(400);
		assertThat(call("POST", "/api/tasks", "{\"title\":\"a\",\"dueDate\":\"tomorrow\"}").statusCode()).isEqualTo(400);
	}

	@Test
	void unknownIdIs404AndBadIdIs400() throws Exception {
		HttpResponse<String> r = call("GET", "/api/tasks/9999", null);
		assertThat(r.statusCode()).isEqualTo(404);
		assertThat(r.body()).contains("Task 9999 not found");
		assertThat(call("DELETE", "/api/tasks/9999", null).statusCode()).isEqualTo(404);
		assertThat(call("PATCH", "/api/tasks/9999/toggle", null).statusCode()).isEqualTo(404);
		assertThat(call("GET", "/api/tasks/abc", null).statusCode()).isEqualTo(400);
	}

	@Test
	void toggleUpdateAndDeleteWork() throws Exception {
		long id = create("Write tests", "HIGH", null);

		HttpResponse<String> toggled = call("PATCH", "/api/tasks/" + id + "/toggle", null);
		assertThat(toggled.body()).contains("\"done\":true");
		assertThat(toggled.body()).doesNotContain("\"completedAt\":null");

		HttpResponse<String> updated = call("PUT", "/api/tasks/" + id,
				"{\"title\":\"Write more tests\",\"priority\":\"LOW\",\"dueDate\":\"2099-01-01\",\"done\":false}");
		assertThat(updated.statusCode()).isEqualTo(200);
		assertThat(updated.body()).contains("Write more tests", "\"priority\":\"LOW\"", "2099-01-01", "\"done\":false");

		assertThat(call("DELETE", "/api/tasks/" + id, null).statusCode()).isEqualTo(204);
		assertThat(call("GET", "/api/tasks/" + id, null).statusCode()).isEqualTo(404);
	}

	@Test
	void listSupportsFilterSearchAndSort() throws Exception {
		long a = create("Alpha report", "LOW", "2099-05-01");
		create("Beta homework", "HIGH", "2099-01-01");
		create("Gamma chores", "MEDIUM", null);
		call("PATCH", "/api/tasks/" + a + "/toggle", null);

		assertThat(call("GET", "/api/tasks", null).body()).contains("Alpha", "Beta", "Gamma");

		String open = call("GET", "/api/tasks?status=open", null).body();
		assertThat(open).contains("Beta", "Gamma").doesNotContain("Alpha");
		assertThat(call("GET", "/api/tasks?status=done", null).body()).contains("Alpha").doesNotContain("Beta");
		assertThat(call("GET", "/api/tasks?priority=high", null).body()).contains("Beta").doesNotContain("Gamma");
		assertThat(call("GET", "/api/tasks?q=HOMEWORK", null).body()).contains("Beta").doesNotContain("Alpha");

		String byDue = call("GET", "/api/tasks?sort=due", null).body();
		assertThat(byDue.indexOf("Beta")).isLessThan(byDue.indexOf("Alpha"));
		assertThat(byDue.indexOf("Alpha")).isLessThan(byDue.indexOf("Gamma"));

		assertThat(call("GET", "/api/tasks?status=weird", null).statusCode()).isEqualTo(400);
	}

	@Test
	void overdueFlagAndStats() throws Exception {
		create("Late", "HIGH", LocalDate.now().minusDays(2).toString());
		create("Today", "HIGH", LocalDate.now().toString());
		long done = create("Done", "LOW", null);
		call("PATCH", "/api/tasks/" + done + "/toggle", null);

		assertThat(call("GET", "/api/tasks?q=Late", null).body()).contains("\"overdue\":true");
		String stats = call("GET", "/api/tasks/stats", null).body();
		assertThat(stats).contains("\"total\":3", "\"open\":2", "\"done\":1", "\"overdue\":1",
				"\"dueToday\":1", "\"highPriorityOpen\":2", "\"completionRate\":33");
	}

	@Test
	void clearCompletedRemovesOnlyDoneTasks() throws Exception {
		long a = create("Keep me", "LOW", null);
		long b = create("Remove me", "LOW", null);
		call("PATCH", "/api/tasks/" + b + "/toggle", null);

		HttpResponse<String> r = call("DELETE", "/api/tasks/completed", null);
		assertThat(r.statusCode()).isEqualTo(200);
		assertThat(r.body()).contains("\"deleted\":1");
		assertThat(call("GET", "/api/tasks/" + a, null).statusCode()).isEqualTo(200);
		assertThat(call("GET", "/api/tasks/" + b, null).statusCode()).isEqualTo(404);
	}

	@Test
	void csvExportHasHeaderRowsAndAttachmentHeader() throws Exception {
		create("Export me", "HIGH", "2099-03-03");
		HttpResponse<String> r = call("GET", "/api/tasks/export.csv", null);
		assertThat(r.statusCode()).isEqualTo(200);
		assertThat(r.headers().firstValue("Content-Type").orElse("")).startsWith("text/csv");
		assertThat(r.headers().firstValue("Content-Disposition").orElse("")).contains("tasks.csv");
		assertThat(r.body()).startsWith("id,title,description,priority,due_date,done").contains("\"Export me\"", "HIGH", "2099-03-03");
	}

	@Test
	void quoteEndpointAlwaysAnswers() throws Exception {
		HttpResponse<String> r = call("GET", "/api/quote", null);
		assertThat(r.statusCode()).isEqualTo(200);   // live quote or offline fallback
		assertThat(r.body()).contains("\"text\"", "\"author\"", "\"source\"");
	}

	@Test
	void frontendIsServedAtRoot() throws Exception {
		HttpResponse<String> r = call("GET", "/", null);
		assertThat(r.statusCode()).isEqualTo(200);
		assertThat(r.body()).contains("Task Manager");
	}
}
