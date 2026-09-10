package com.bleedthrough.meatscape.debug;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class SoakTelemetryTest {
    @Test
    void sampleHasStableCsvColumnCount() {
        var sample = new SoakTelemetry.Sample("2026-09-10T00:00:00Z", 42, 12.5F,
                100L, 200L, 300L, 2, 4, 6, 8L, 1);

        assertEquals(11, SoakTelemetry.HEADER.split(",").length);
        assertEquals(11, sample.csv().split(",").length);
        assertTrue(sample.csv().startsWith("2026-09-10T00:00:00Z,42,12.5,"));
    }

    @Test
    void directorySizeSumsOnlyFiles() throws Exception {
        var root = Path.of("build", "test-soak-telemetry");
        try {
            Files.createDirectories(root);
            Files.writeString(root.resolve("a"), "abc");
            Files.createDirectory(root.resolve("nested"));
            Files.writeString(root.resolve("nested").resolve("b"), "hello");
            assertEquals(8L, SoakTelemetry.directorySize(root));
        } finally {
            try (var paths = Files.walk(root)) {
                paths.sorted(java.util.Comparator.reverseOrder()).forEach(path -> path.toFile().delete());
            }
        }
    }
}
