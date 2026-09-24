package de.bsommerfeld.randomizer.ui.overview;

import com.cs2gsi.nodes.Player;
import de.bsommerfeld.randomizer.action.ActionRunner;
import de.bsommerfeld.randomizer.action.FireGate;
import de.bsommerfeld.randomizer.config.AppPreferences;
import de.bsommerfeld.randomizer.gsi.GsiService;
import de.bsommerfeld.randomizer.input.JnaGameInput;
import de.bsommerfeld.randomizer.input.RawInputWatcher;
import de.bsommerfeld.randomizer.steam.SteamLocator;
import de.bsommerfeld.randomizer.ui.ControllerFactory;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Loads the FXML views for real. The compiler does not check that every {@code fx:id} has its
 * {@code @FXML} field or that {@code initialize()} runs through.
 */
class OverviewViewTest {

    private static final String OVERVIEW_VIEW = "/de/bsommerfeld/randomizer/ui/overview-view.fxml";
    private static final String MAIN_VIEW = "/de/bsommerfeld/randomizer/ui/main-view.fxml";

    @TempDir
    Path tempDir;

    @BeforeAll
    static void initToolkit() {
        try {
            CountDownLatch ready = new CountDownLatch(1);
            Platform.startup(ready::countDown);
            assertTrue(ready.await(10, TimeUnit.SECONDS), "JavaFX toolkit should start");
        } catch (IllegalStateException alreadyRunning) {
            // another test started the toolkit already
        } catch (Throwable headless) {
            Assumptions.abort("JavaFX toolkit unavailable in this environment: " + headless);
        }
    }

    @Test
    void overviewViewLoads() throws Exception {
        loadOverview(new GsiService()); // does not bind the port until start()
    }

    @Test
    void mainViewLoadsWithAllIncludedTabs() throws Exception {
        AtomicReference<Throwable> error = new AtomicReference<>();
        CountDownLatch done = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                GsiService gsiService = new GsiService();
                FireGate fireGate = new FireGate(gsiService, () -> false);
                FXMLLoader loader = new FXMLLoader(OverviewViewTest.class.getResource(MAIN_VIEW));
                // The same registry RandomizerApp uses, so every fx:include resolves its controller. Settings
                // in a temp folder and a registry without Steam: nothing on this machine changes the result.
                loader.setControllerFactory(ControllerFactory.create(
                        new AppPreferences(tempDir),
                        new SteamLocator((hive, keyPath, valueName) -> Optional.empty()),
                        gsiService, () -> true,
                        new ActionRunner(new JnaGameInput(), fireGate::isOpen, Player::new,
                                new RawInputWatcher(() -> false)), // never started here
                        fireGate));
                loader.load();
                // Whichever sub-tab comes first: the merged view must be filled once loading is done.
                // Its status line has text in every case, with or without CS2 on this machine.
                Node mergedView = (Node) loader.getNamespace().get("mergedConfigView");
                String mergedStatus = ((Label) mergedView.lookup("#statusLabel")).getText();
                assertFalse(mergedStatus == null || mergedStatus.isEmpty(),
                        "merged config view should be filled after the main view loaded");
            } catch (Throwable t) {
                error.set(t);
            } finally {
                done.countDown();
            }
        });

        assertTrue(done.await(10, TimeUnit.SECONDS), "FXML load should complete");
        if (error.get() != null) {
            fail("main-view.fxml (with included tabs) failed to load", error.get());
        }
    }

    @Test
    @Tag("integration")
    void scoreboardFillsFromPostedAllPlayers() throws Exception {
        int port = freePort();
        GsiService service = new GsiService(port);
        Map<String, Object> view = loadOverview(service);
        TableView<?> ctTable = (TableView<?>) view.get("ctTable");
        TableView<?> tTable = (TableView<?>) view.get("tTable");
        Label mapLabel = (Label) view.get("mapLabel");
        Label timeLabel = (Label) view.get("timeLabel");

        try {
            assertTrue(service.start(), "GSI listener should start");
            post(port, """
                    {"map":{"name":"de_dust2","mode":"competitive","team_ct":{"score":7},"team_t":{"score":5}},
                     "round":{"phase":"live"},
                     "allplayers":{
                       "111":{"name":"Alice","team":"CT","match_stats":{"kills":10,"score":25},
                              "state":{"health":100,"armor":100,"money":1200},
                              "weapons":{"weapon_0":{"name":"weapon_ak47","type":"Rifle","ammo_clip":30,"ammo_reserve":90,"state":"active"}}},
                       "222":{"name":"Bob","team":"T","match_stats":{"kills":8,"score":18},
                              "state":{"health":0,"money":800}}
                     }}
                    """);

            // render() runs later on the FX thread, so poll until the tables fill
            boolean filled = awaitFx(
                    () -> ctTable.getItems().size() == 1 && tTable.getItems().size() == 1, 5000);
            assertTrue(filled, "scoreboard should populate one CT and one T player from the posted allplayers");

            AtomicReference<String> header = new AtomicReference<>();
            runFx(() -> header.set(mapLabel.getText()));
            assertTrue(header.get().contains("de_dust2"), "header should show the map name, was: " + header.get());

            AtomicReference<String> time = new AtomicReference<>();
            AtomicBoolean timeVisible = new AtomicBoolean();
            runFx(() -> {
                time.set(timeLabel.getText());
                timeVisible.set(timeLabel.isVisible());
            });
            assertTrue(timeVisible.get(), "round timer should be visible once the round is live");
            assertTrue(time.get() != null && time.get().matches("1:5\\d"),
                    "round timer should start at ~1:55, was: " + time.get());

            // A bomb plant switches to the 40 s bomb timer while the round is still live. CS2 sends the
            // map with every state, and the bomb time comes from its mode.
            post(port, "{\"map\":{\"mode\":\"competitive\"},\"round\":{\"phase\":\"live\",\"bomb\":\"planted\"}}");
            boolean switched = awaitFx(() -> timeLabel.getText().startsWith("Bomb"), 5000);
            assertTrue(switched, "bomb plant should switch to the 'Bomb' detonation timer");
            AtomicReference<String> bombTime = new AtomicReference<>();
            runFx(() -> bombTime.set(timeLabel.getText()));
            assertTrue(bombTime.get().matches("Bomb\\s+0:[34]\\d"),
                    "bomb timer should start at ~0:40, was: " + bombTime.get());

            post(port, "{\"round\":{\"phase\":\"over\"}}");
            boolean hidden = awaitFx(() -> !timeLabel.isVisible(), 5000);
            assertTrue(hidden, "timer should stop once the round is no longer live");
        } finally {
            service.close();
        }
    }

    @Test
    @Tag("integration")
    void wingmanRoundStartsAtNinetySeconds() throws Exception {
        int port = freePort();
        GsiService service = new GsiService(port);
        Label timeLabel = (Label) loadOverview(service).get("timeLabel");

        try {
            assertTrue(service.start(), "GSI listener should start");
            // "scrimcomp2v2" is Wingman
            post(port, "{\"map\":{\"mode\":\"scrimcomp2v2\"},\"round\":{\"phase\":\"live\"}}");

            boolean visible = awaitFx(() -> timeLabel.isVisible(), 5000);
            assertTrue(visible, "timer should show once a Wingman round goes live");
            AtomicReference<String> time = new AtomicReference<>();
            runFx(() -> time.set(timeLabel.getText()));
            assertTrue(time.get().matches("1:3\\d"),
                    "Wingman round timer should start at ~1:30, was: " + time.get());
        } finally {
            service.close();
        }
    }

    // ---- helpers ---------------------------------------------------------------------------

    /** Loads the overview FXML with a controller on {@code service}; returns its fx:id namespace. */
    private static Map<String, Object> loadOverview(GsiService service) throws Exception {
        AtomicReference<Map<String, Object>> namespace = new AtomicReference<>();
        AtomicReference<Throwable> error = new AtomicReference<>();
        runFx(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(OverviewViewTest.class.getResource(OVERVIEW_VIEW));
                loader.setControllerFactory(type -> new OverviewController(service));
                loader.load();
                namespace.set(loader.getNamespace());
            } catch (Throwable t) {
                error.set(t);
            }
        });
        if (error.get() != null) {
            fail("overview-view.fxml failed to load", error.get());
        }
        return namespace.get();
    }

    private static int freePort() throws Exception {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private static void post(int port, String payload) throws Exception {
        try (HttpClient client = HttpClient.newHttpClient()) {
            client.send(
                    HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/"))
                            .POST(HttpRequest.BodyPublishers.ofString(payload))
                            .header("Content-Type", "application/json")
                            .build(),
                    HttpResponse.BodyHandlers.ofString());
        }
    }

    /** Runs {@code action} on the FX thread and waits for it to finish. */
    private static void runFx(Runnable action) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                action.run();
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(10, TimeUnit.SECONDS), "FX action should complete");
    }

    /** Polls {@code condition} (evaluated on the FX thread) until true or the timeout elapses. */
    private static boolean awaitFx(BooleanSupplier condition, long timeoutMillis) throws Exception {
        long deadline = System.currentTimeMillis() + timeoutMillis;
        while (System.currentTimeMillis() < deadline) {
            AtomicBoolean satisfied = new AtomicBoolean();
            runFx(() -> satisfied.set(condition.getAsBoolean()));
            if (satisfied.get()) {
                return true;
            }
            Thread.sleep(100);
        }
        return false;
    }
}
