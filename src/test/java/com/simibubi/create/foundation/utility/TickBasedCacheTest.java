package com.simibubi.create.foundation.utility;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.Test;

class TickBasedCacheTest {

	// The tick counters are static, so every test only advances them relative to the current value
	private static void serverTicks(int ticks) {
		for (int i = 0; i < ticks; i++)
			TickBasedCache.tick();
	}

	private static void clientTicks(int ticks) {
		for (int i = 0; i < ticks; i++)
			TickBasedCache.clientTick();
	}

	@Test
	void entryIsPresentUntilTimeout() {
		TickBasedCache<String, Integer> cache = new TickBasedCache<>(10, false);
		cache.put("a", 1);
		serverTicks(10);
		assertEquals(1, cache.getIfPresent("a"));
		serverTicks(1);
		assertNull(cache.getIfPresent("a"));
		assertEquals(0, cache.size());
	}

	@Test
	void missingKeyIsAbsent() {
		TickBasedCache<String, Integer> cache = new TickBasedCache<>(10, false);
		assertNull(cache.getIfPresent("missing"));
	}

	@Test
	void accessResetsTimerWhenEnabled() {
		TickBasedCache<String, Integer> cache = new TickBasedCache<>(10, true);
		cache.put("a", 1);
		for (int i = 0; i < 5; i++) {
			serverTicks(8);
			assertEquals(1, cache.getIfPresent("a"));
		}
		serverTicks(11);
		assertNull(cache.getIfPresent("a"));
	}

	@Test
	void accessDoesNotResetTimerWhenDisabled() {
		TickBasedCache<String, Integer> cache = new TickBasedCache<>(10, false);
		cache.put("a", 1);
		serverTicks(8);
		assertEquals(1, cache.getIfPresent("a"));
		serverTicks(8);
		assertNull(cache.getIfPresent("a"));
	}

	@Test
	void clientCacheIgnoresServerTicks() {
		TickBasedCache<String, Integer> cache = new TickBasedCache<>(5, false, true);
		cache.put("a", 1);
		serverTicks(100);
		assertEquals(1, cache.getIfPresent("a"));
		clientTicks(6);
		assertNull(cache.getIfPresent("a"));
	}

	@Test
	@SuppressWarnings("unchecked")
	void loaderIsCalledOnlyOnMiss() throws Exception {
		Callable<String> loader = mock(Callable.class);
		when(loader.call()).thenReturn("loaded");
		TickBasedCache<Integer, String> cache = new TickBasedCache<>(10, false);

		assertEquals("loaded", cache.get(1, loader));
		assertEquals("loaded", cache.get(1, loader));
		verify(loader, times(1)).call();

		serverTicks(11);
		assertEquals("loaded", cache.get(1, loader));
		verify(loader, times(2)).call();
	}

	@Test
	@SuppressWarnings("unchecked")
	void loaderIsNotCalledWhenValueIsCached() throws Exception {
		Callable<String> loader = mock(Callable.class);
		TickBasedCache<Integer, String> cache = new TickBasedCache<>(10, false);
		cache.put(1, "cached");
		assertEquals("cached", cache.get(1, loader));
		verify(loader, never()).call();
	}

	@Test
	@SuppressWarnings("unchecked")
	void loaderFailureIsWrapped() throws Exception {
		Callable<String> loader = mock(Callable.class);
		IOException failure = new IOException("disk on fire");
		when(loader.call()).thenThrow(failure);
		TickBasedCache<Integer, String> cache = new TickBasedCache<>(10, false);

		ExecutionException thrown = assertThrows(ExecutionException.class, () -> cache.get(1, loader));
		assertSame(failure, thrown.getCause());
		assertNull(cache.getIfPresent(1));
	}

	@Test
	void invalidateRemovesEntries() {
		TickBasedCache<String, Integer> cache = new TickBasedCache<>(10, false);
		cache.putAll(Map.of("a", 1, "b", 2, "c", 3));
		assertEquals(3, cache.size());

		cache.invalidate("a");
		assertNull(cache.getIfPresent("a"));
		cache.invalidateAll(List.of("b"));
		assertNull(cache.getIfPresent("b"));
		assertEquals(Map.of("c", 3), cache.asMap());

		cache.invalidateAll();
		assertEquals(0, cache.size());
	}

	@Test
	void cleanUpRemovesOnlyExpiredEntries() {
		TickBasedCache<String, Integer> cache = new TickBasedCache<>(10, false);
		cache.put("old", 1);
		serverTicks(6);
		cache.put("new", 2);
		serverTicks(6);
		cache.cleanUp();
		assertEquals(Map.of("new", 2), cache.asMap());
	}

	@Test
	void statsAreNotRecorded() {
		TickBasedCache<String, Integer> cache = new TickBasedCache<>(10, false);
		cache.put("a", 1);
		cache.getIfPresent("a");
		assertEquals(0, cache.stats().requestCount());
	}

	// The two tests below exposed bugs in the Cache contract implementation

	@Test
	void getAllPresentReturnsOnlyRequestedKeys() {
		TickBasedCache<String, Integer> cache = new TickBasedCache<>(10, false);
		cache.put("a", 1);
		cache.put("b", 2);
		assertEquals(Map.of("a", 1), cache.getAllPresent(List.of("a", "missing")));
	}

	@Test
	void sizeDoesNotCountAsAccess() {
		TickBasedCache<String, Integer> cache = new TickBasedCache<>(10, true);
		cache.put("a", 1);
		for (int i = 0; i < 3; i++) {
			serverTicks(8);
			assertTrue(cache.size() <= 1);
		}
		assertNull(cache.getIfPresent("a"));
	}
}
