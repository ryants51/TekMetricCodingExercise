package com.interview;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public final class TestResourceLoader {
    private TestResourceLoader() {
    }

    public static String json(String path) {
        String resourcePath = "/json/" + path;

        try (InputStream inputStream = TestResourceLoader.class.getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new IllegalArgumentException("Test resource was not found: " + resourcePath);
            }

            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read test resource: " + resourcePath, exception);
        }
    }
}
