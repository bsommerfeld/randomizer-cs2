package de.bsommerfeld.randomizer.gsi;

import org.junit.jupiter.api.Test;

import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GsiServiceTest {

    private static int freePort() throws Exception {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private static HttpResponse<String> post(int port, String payload) throws Exception {
        try (HttpClient client = HttpClient.newHttpClient()) {
            return client.send(
                    HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/"))
                            .POST(HttpRequest.BodyPublishers.ofString(payload))
                            .header("Content-Type", "application/json")
                            .build(),
                    HttpResponse.BodyHandlers.ofString());
        }
    }

    @Test
    void deliversGameEventsWithSummaryAndDetails() throws Exception {
        int port = freePort();
        CountDownLatch received = new CountDownLatch(1);
        AtomicReference<GsiService.GsiEvent> firstEvent = new AtomicReference<>();

        try (GsiService service = new GsiService(port)) {
            service.onGameEvent(event -> {
                firstEvent.compareAndSet(null, event);
                received.countDown();
            });
            assertTrue(service.start());

            post(port, "{\"round\":{\"phase\":\"live\"}}");
            post(port, "{\"round\":{\"phase\":\"over\"}}");

            assertTrue(received.await(5, TimeUnit.SECONDS), "event callback should fire");
            GsiService.GsiEvent event = firstEvent.get();
            assertNotNull(event);
            assertTrue(event.summary().matches("\\d{2}:\\d{2}:\\d{2}  .+"), "summary should start with a timestamp");
            assertTrue(event.details().contains("Event: "), "details should name the event type");
        }
    }

    @Test
    void canBeRestartedAfterStop() throws Exception {
        int port = freePort();
        try (GsiService service = new GsiService(port)) {
            assertTrue(service.start());
            service.stop();
            assertTrue(service.start(), "listener should start again after stop()");
            assertTrue(service.isRunning());
        }
    }

    @Test
    void deliversPostedGameStateAsPrettyJson() throws Exception {
        int port = freePort();
        CountDownLatch received = new CountDownLatch(1);
        AtomicReference<String> json = new AtomicReference<>();

        try (GsiService service = new GsiService(port)) {
            service.onGameStateJson(value -> {
                json.set(value);
                received.countDown();
            });
            assertTrue(service.start(), "GSI listener should start");
            assertTrue(service.isRunning());

            String payload = """
                    {"provider":{"name":"Counter-Strike: Global Offensive","appid":730},
                     "round":{"phase":"live"}}
                    """;
            HttpResponse<String> response;
            try (HttpClient client = HttpClient.newHttpClient()) {
                response = client.send(
                        HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/"))
                                .POST(HttpRequest.BodyPublishers.ofString(payload))
                                .header("Content-Type", "application/json")
                                .build(),
                        HttpResponse.BodyHandlers.ofString());
            }
            assertTrue(response.statusCode() < 300, "GSI endpoint should accept the POST");

            assertTrue(received.await(5, TimeUnit.SECONDS), "game-state callback should fire");
            assertNotNull(json.get());
            assertTrue(json.get().contains("\"phase\": \"live\""), "pretty JSON should contain the round phase");
        }
    }
}
