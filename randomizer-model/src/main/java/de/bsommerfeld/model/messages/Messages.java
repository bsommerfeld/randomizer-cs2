package de.bsommerfeld.model.messages;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@UtilityClass
@Slf4j
public final class Messages {

    private static final Map<String, String> MESSAGES_MAP = new HashMap<>();

    public static void cache() {
        try (InputStream inputStream =
                     Messages.class.getClassLoader().getResourceAsStream("messages.properties")) {

            if (inputStream == null)
                throw new IllegalStateException("Corrupted jar - messages.properties is missing");

            BufferedReader bufferedReader =
                    new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            bufferedReader
                    .lines()
                    .forEach(
                            line -> {
                                String[] lineSplit = line.split("=", 2);
                                MESSAGES_MAP.put(lineSplit[0], lineSplit[1]);
                            });
        } catch (IOException e) {
            log.error("Failed to cache messages", e);
        }
    }

    public static String getMessage(String key) {
        return MESSAGES_MAP.get(key);
    }
}
