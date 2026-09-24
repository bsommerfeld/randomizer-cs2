package de.bsommerfeld.randomizer.gsi;

import com.cs2gsi.GameState;
import com.cs2gsi.nodes.PlayerTeam;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs the real listener on a free local port and posts real payloads at it. Tagged so the socket
 * tests can be left out: {@code mvn test -DexcludedGroups=integration}.
 */
@Tag("integration")
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
        AtomicReference<GsiEvent> firstEvent = new AtomicReference<>();

        try (GsiService service = new GsiService(port)) {
            service.onGameEvent(event -> {
                firstEvent.compareAndSet(null, event);
                received.countDown();
            });
            assertTrue(service.start());

            post(port, "{\"round\":{\"phase\":\"live\"}}");
            post(port, "{\"round\":{\"phase\":\"over\"}}");

            assertTrue(received.await(5, TimeUnit.SECONDS), "event callback should fire");
            GsiEvent event = firstEvent.get();
            assertNotNull(event);
            assertTrue(event.summary().matches("\\d{2}:\\d{2}:\\d{2}  .+"), "summary should start with a timestamp");
            assertTrue(event.details().contains("Event: "), "details should name the event type");
        }
    }

    @Test
    void leavesOutTheProviderHeartbeat() throws Exception {
        int port = freePort();
        CountDownLatch roundEvent = new CountDownLatch(1);
        List<String> summaries = new CopyOnWriteArrayList<>();

        try (GsiService service = new GsiService(port)) {
            service.onGameEvent(event -> {
                summaries.add(event.summary());
                if (event.summary().contains("Round")) {
                    roundEvent.countDown();
                }
            });
            assertTrue(service.start());

            post(port, "{\"provider\":{\"name\":\"cs2\",\"timestamp\":1},\"round\":{\"phase\":\"live\"}}");
            post(port, "{\"provider\":{\"name\":\"cs2\",\"timestamp\":2},\"round\":{\"phase\":\"over\"}}");

            // The library broadcasts provider events before round events, so they would be here by now
            assertTrue(roundEvent.await(5, TimeUnit.SECONDS), "round event should fire");
            assertTrue(summaries.stream().noneMatch(s -> s.contains("ProviderUpdated") || s.contains("ProviderTimestampChanged")),
                    "heartbeat events should be filtered: " + summaries);
        }
    }

    @Test
    void silenceIsTheTimeSinceTheLastGameStateARepeatedOneIncluded() throws Exception {
        int port = freePort();
        AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-09-21T12:00:00Z"));

        try (GsiService service = new GsiService(port, now::get)) {
            assertTrue(service.start());
            assertEquals(Optional.empty(), service.silence(), "nothing came in yet");

            post(port, "{\"round\":{\"phase\":\"live\"}}");
            now.set(now.get().plusSeconds(12));
            assertEquals(Optional.of(Duration.ofSeconds(12)), service.silence());

            post(port, "{\"round\":{\"phase\":\"live\"}}"); // the same state again, which is what a heartbeat sends
            assertEquals(Optional.of(Duration.ZERO), service.silence());
        }
    }

    @Test
    void exposesStructuredGameStateWithAllPlayers() throws Exception {
        int port = freePort();
        CountDownLatch received = new CountDownLatch(1);
        AtomicReference<GameState> firstState = new AtomicReference<>();

        try (GsiService service = new GsiService(port)) {
            service.onGameState(state -> {
                if (!state.allPlayers.isEmpty() && firstState.compareAndSet(null, state)) {
                    received.countDown();
                }
            });
            assertTrue(service.start());

            String payload = """
                    {"map":{"name":"de_dust2","mode":"competitive","team_ct":{"score":7},"team_t":{"score":5}},
                     "allplayers":{
                       "76561190000000001":{"name":"Alice","team":"CT","match_stats":{"kills":10,"score":25},"state":{"health":100,"money":1200}},
                       "76561190000000002":{"name":"Bob","team":"T","match_stats":{"kills":8,"score":18},"state":{"health":50,"money":800}}
                     }}
                    """;
            post(port, payload);

            assertTrue(received.await(5, TimeUnit.SECONDS), "structured game-state callback should fire");
            GameState state = firstState.get();
            assertNotNull(state);
            assertEquals(2, state.allPlayers.size(), "both players should be parsed");
            assertEquals(PlayerTeam.CT, state.allPlayers.get("76561190000000001").team);
            assertEquals(PlayerTeam.T, state.allPlayers.get("76561190000000002").team);
            assertEquals(7, state.map.ctStatistics.score, "CT team score");
            assertEquals(5, state.map.tStatistics.score, "T team score");
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
