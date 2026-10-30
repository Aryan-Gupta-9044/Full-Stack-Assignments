package com.example.spb.quote;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Supplier;

/**
 * Calls an external API (dummyjson.com) for a motivational quote.
 * Practical extras: short timeouts, 10-minute cache, and a 30-second back-off plus
 * built-in fallback quotes, so the app keeps working when the API is slow or offline.
 */
@Service
public class QuoteService {

	static final Duration CACHE_TTL = Duration.ofMinutes(10);
	static final Duration FAILURE_BACKOFF = Duration.ofSeconds(30);
	private static final String URL = "https://dummyjson.com/quotes/random";

	private static final List<Quote> FALLBACK = List.of(
			new Quote("The secret of getting ahead is getting started.", "Mark Twain", "offline"),
			new Quote("Well begun is half done.", "Aristotle", "offline"),
			new Quote("Do the hard jobs first. The easy jobs will take care of themselves.", "Dale Carnegie", "offline"),
			new Quote("It always seems impossible until it's done.", "Nelson Mandela", "offline"),
			new Quote("You don't have to see the whole staircase, just take the first step.", "Martin Luther King Jr.", "offline"));

	private final Supplier<Quote> remote;
	private final Clock clock;
	private final Random random = new Random();

	private Quote cached;
	private Instant cachedAt = Instant.EPOCH;
	private Instant retryAfter = Instant.EPOCH;

	public QuoteService() {
		this(QuoteService::fetchFromDummyJson, Clock.systemUTC());
	}

	/** For tests: plug in a fake remote and clock. */
	QuoteService(Supplier<Quote> remote, Clock clock) {
		this.remote = remote;
		this.clock = clock;
	}

	public synchronized Quote get(boolean fresh) {
		Instant now = clock.instant();
		if (!fresh && cached != null && now.isBefore(cachedAt.plus(CACHE_TTL))) {
			return cached;
		}
		if (now.isBefore(retryAfter)) {
			return cached != null ? cached : fallback();
		}
		try {
			Quote q = remote.get();
			if (q == null || q.text() == null || q.text().isBlank()) {
				throw new IllegalStateException("Empty quote from remote API");
			}
			cached = q;
			cachedAt = now;
			return q;
		} catch (RuntimeException e) {
			retryAfter = now.plus(FAILURE_BACKOFF);
			return cached != null ? cached : fallback();
		}
	}

	private Quote fallback() {
		return FALLBACK.get(random.nextInt(FALLBACK.size()));
	}

	private static Quote fetchFromDummyJson() {
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout(Duration.ofSeconds(3));
		factory.setReadTimeout(Duration.ofSeconds(3));
		RestClient client = RestClient.builder().requestFactory(factory).build();

		Map<String, Object> body = client.get().uri(URL).retrieve()
				.body(new ParameterizedTypeReference<Map<String, Object>>() { });
		if (body == null || body.get("quote") == null) {
			throw new IllegalStateException("Unexpected response from " + URL);
		}
		Object author = body.get("author");
		return new Quote(String.valueOf(body.get("quote")), author == null ? "Unknown" : String.valueOf(author), "dummyjson.com");
	}
}
