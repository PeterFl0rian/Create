package com.simibubi.create.foundation.utility;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.google.gson.JsonObject;

class FilesHelperTest {

	@TempDir
	Path folder;

	@Test
	void slugReplacesNonWordCharacters() {
		assertEquals("my_cool_schematic", FilesHelper.slug("My Cool Schematic"));
		assertEquals("a_b_c", FilesHelper.slug("a-b.c"));
	}

	@Test
	void firstValidFilenameInEmptyFolder() {
		assertEquals("bridge.nbt", FilesHelper.findFirstValidFilename("Bridge", folder, "nbt"));
	}

	@Test
	void firstValidFilenameSkipsExistingFiles() throws IOException {
		Files.createFile(folder.resolve("bridge.nbt"));
		Files.createFile(folder.resolve("bridge_1.nbt"));
		assertEquals("bridge_2.nbt", FilesHelper.findFirstValidFilename("Bridge", folder, "nbt"));
	}

	@Test
	void createFolderIfMissingCreatesNestedFolders() {
		Path nested = folder.resolve("schematics").resolve("uploaded");
		FilesHelper.createFolderIfMissing(nested);
		assertTrue(Files.isDirectory(nested));
	}

	@Test
	void loadJsonResourceParsesLenientJson() {
		JsonObject json = FilesHelper.loadJsonResource("create_test/sample.json").getAsJsonObject();
		assertEquals("bridge", json.get("name").getAsString());
		assertEquals(3, json.get("size").getAsInt());
	}

	@Test
	void loadJsonResourceOfMissingFileFails() {
		assertThrows(NullPointerException.class, () -> FilesHelper.loadJsonResource("create_test/missing.json"));
	}
}
