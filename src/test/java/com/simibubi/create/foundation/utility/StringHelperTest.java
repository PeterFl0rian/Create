package com.simibubi.create.foundation.utility;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class StringHelperTest {

	@ParameterizedTest
	@CsvSource({
		"train_station, TrainStation",
		"id, Id",
		"a, A",
		"remaining_cargo_count, RemainingCargoCount",
		"alreadyCamel, AlreadyCamel"
	})
	void snakeCaseToCamelCase(String input, String expected) {
		assertEquals(expected, StringHelper.snakeCaseToCamelCase(input));
	}

	@ParameterizedTest
	@CsvSource({
		"TrainStation, train_station",
		"trainStation, train_station",
		"Id, id",
		"already_snake, already_snake",
		"ABC, a_b_c"
	})
	void camelCaseToSnakeCase(String input, String expected) {
		assertEquals(expected, StringHelper.camelCaseToSnakeCase(input));
	}

	@Test
	void camelCaseToSnakeCaseOfEmptyString() {
		assertEquals("", StringHelper.camelCaseToSnakeCase(""));
	}

	@ParameterizedTest
	@ValueSource(strings = {"train_station", "remaining_cargo_count", "id"})
	void conversionsAreInverse(String snake) {
		assertEquals(snake, StringHelper.camelCaseToSnakeCase(StringHelper.snakeCaseToCamelCase(snake)));
	}

	// The two tests below exposed bugs: both inputs used to throw StringIndexOutOfBoundsException

	@Test
	void snakeCaseToCamelCaseOfEmptyString() {
		assertEquals("", StringHelper.snakeCaseToCamelCase(""));
	}

	@Test
	void snakeCaseToCamelCaseWithTrailingUnderscore() {
		assertEquals("Train", StringHelper.snakeCaseToCamelCase("train_"));
	}
}
