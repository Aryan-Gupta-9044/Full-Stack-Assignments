package com.example.spb.quote;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class QuoteServiceTest {

	/** Clock we can move forward by hand. */
	static class MutableClock extends Clock {
		Instant now = Instant.parse("2026-10-20T10:00:00Z");
		@Override public java.time.ZoneId getZone() { return ZoneOffset.UTC; }
		@Override public Clock withZone(java.time.ZoneId zone) { return this; }
		@Override public Instant instant() { return now; }
		void advance(Duration d) { now = now.plus(d); }
	}

	@Test
	void cachesSuccessfulResponses() {
		MutableClock clock = new MutableClock();
		AtomicInteger calls = new AtomicInteger();
		QuoteService s = new QuoteService(() -> new Quote("q" + calls.incrementAndGet(), "A", "dummyjson.com"), clock);

		assertThat(s.get(false).text()).isEqualTo("q1");
		assertThat(s.get(false).text()).isEqualTo("q1");   // served from cache
		assertThat(calls.get()).isEqualTo(1);

		assertThat(s.get(true).text()).isEqualTo("q2");    // fresh=true bypasses cache
		clock.advance(QuoteService.CACHE_TTL.plusSeconds(1));
		assertThat(s.get(false).text()).isEqualTo("q3");   // cache expired
	}

	@Test
	void fallsBackToOfflineQuoteAndBacksOffAfterFailure() {
		MutableClock clock = new MutableClock();
		AtomicInteger calls = new AtomicInteger();
		QuoteService s = new QuoteService(() -> {
			calls.incrementAndGet();
			throw new IllegalStateException("network down");
		}, clock);

		Quote q = s.get(false);
		assertThat(q.source()).isEqualTo("offline");
		assertThat(q.text()).isNotBlank();
		s.get(true);
		s.get(false);
		assertThat(calls.get()).isEqualTo(1);              // no hammering a dead API

		clock.advance(QuoteService.FAILURE_BACKOFF.plusSeconds(1));
		s.get(false);
		assertThat(calls.get()).isEqualTo(2);              // retried after back-off
	}

	@Test
	void keepsServingLastGoodQuoteWhenApiFailsLater() {
		MutableClock clock = new MutableClock();
		AtomicInteger calls = new AtomicInteger();
		QuoteService s = new QuoteService(() -> {
			if (calls.incrementAndGet() == 1) return new Quote("good", "A", "dummyjson.com");
			throw new IllegalStateException("boom");
		}, clock);

		assertThat(s.get(false).text()).isEqualTo("good");
		clock.advance(QuoteService.CACHE_TTL.plusSeconds(1));
		assertThat(s.get(false).text()).isEqualTo("good");  // stale-but-real beats offline
	}

	@Test
	void treatsBlankRemoteQuoteAsFailure() {
		QuoteService s = new QuoteService(() -> new Quote(" ", "A", "dummyjson.com"), new MutableClock());
		assertThat(s.get(false).source()).isEqualTo("offline");
	}
}
