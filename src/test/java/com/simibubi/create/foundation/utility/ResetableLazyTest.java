package com.simibubi.create.foundation.utility;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ResetableLazyTest {

	private Supplier<String> supplier;

	@BeforeEach
	@SuppressWarnings("unchecked")
	void setUp() {
		supplier = mock(Supplier.class);
		when(supplier.get()).thenReturn("first", "second");
	}

	@Test
	void doesNotComputeUntilRequested() {
		ResetableLazy.of(supplier);
		verify(supplier, never()).get();
	}

	@Test
	void computesValueOnlyOnce() {
		ResetableLazy<String> lazy = ResetableLazy.of(supplier);
		assertEquals("first", lazy.get());
		assertEquals("first", lazy.get());
		assertEquals("first", lazy.get());
		verify(supplier, times(1)).get();
	}

	@Test
	void resetRecomputesValue() {
		ResetableLazy<String> lazy = new ResetableLazy<>(supplier);
		assertEquals("first", lazy.get());
		lazy.reset();
		assertEquals("second", lazy.get());
		verify(supplier, times(2)).get();
	}

	@Test
	void resetWithoutGetDoesNotCompute() {
		ResetableLazy<String> lazy = ResetableLazy.of(supplier);
		lazy.reset();
		verify(supplier, never()).get();
	}

	@Test
	void nullValueIsNotCached() {
		@SuppressWarnings("unchecked")
		Supplier<String> nullSupplier = mock(Supplier.class);
		ResetableLazy<String> lazy = ResetableLazy.of(nullSupplier);
		assertNull(lazy.get());
		assertNull(lazy.get());
		verify(nullSupplier, times(2)).get();
	}
}
