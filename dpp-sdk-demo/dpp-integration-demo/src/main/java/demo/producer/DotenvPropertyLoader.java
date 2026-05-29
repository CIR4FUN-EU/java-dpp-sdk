package demo.producer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

final class DotenvPropertyLoader {

    private DotenvPropertyLoader() {
    }

    static void loadIfPresent(Path path) {
        if (!Files.exists(path)) {
            return;
        }
        List<String> lines;
        try {
            lines = Files.readAllLines(path);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read optional dotenv file at " + path, exception);
        }
        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            int separator = line.indexOf('=');
            if (separator <= 0) {
                continue;
            }
            String key = line.substring(0, separator).trim();
            String value = line.substring(separator + 1).trim();
            if (System.getProperty(key) == null) {
                System.setProperty(key, value);
            }
        }
    }
}
