package de.bsommerfeld.randomizer.ui.overview;

import de.bsommerfeld.randomizer.config.AppPreferences;
import de.bsommerfeld.randomizer.config.ConfigRepository;
import de.bsommerfeld.randomizer.config.ConfigSaver;
import de.bsommerfeld.randomizer.config.ConfigVerifier;
import de.bsommerfeld.randomizer.config.crosshair.CrosshairBackup;
import de.bsommerfeld.randomizer.config.crosshair.CrosshairConfigParser;
import de.bsommerfeld.randomizer.config.crosshair.CrosshairRandomizer;
import de.bsommerfeld.randomizer.config.crosshair.CrosshairSource;
import de.bsommerfeld.randomizer.config.crosshair.CrosshairStandard;
import de.bsommerfeld.randomizer.config.keybinds.ConfigKind;
import de.bsommerfeld.randomizer.config.keybinds.KeybindConfigParser;
import de.bsommerfeld.randomizer.exec.Cs2Window;
import de.bsommerfeld.randomizer.exec.ExecApplier;
import de.bsommerfeld.randomizer.exec.ExecConfig;
import de.bsommerfeld.randomizer.gsi.GsiService;
import de.bsommerfeld.randomizer.steam.JnaWindowsRegistry;
import de.bsommerfeld.randomizer.steam.SteamLocator;
import de.bsommerfeld.randomizer.ui.ControllerFactory;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.ServerSocket;
import java.nio.file.Path;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Smoke test that the overview FXML actually loads: it verifies every {@code fx:id} resolves to a
 * matching {@code @FXML} field, {@code initialize()} runs (columns, selection wiring, subscription),
 * and the stylesheet is found - none of which the compiler checks.
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
            // Toolkit already initialised by another test - fine.
        } catch (Throwable headless) {
            Assumptions.abort("JavaFX toolkit unavailable in this environment: " + headless);
        }
    }

    @Test
    void overviewViewLoads() throws Exception {
        AtomicReference<Throwable> error = new AtomicReference<>();
        CountDownLatch done = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                GsiService service = new GsiService(); // does not bind the port until start()
                FXMLLoader loader = new FXMLLoader(OverviewViewTest.class.getResource(OVERVIEW_VIEW));
                loader.setControllerFactory(type -> new OverviewController(service));
                loader.load();
            } catch (Throwable t) {
                error.set(t);
            } finally {
                done.countDown();
            }
        });

        assertTrue(done.await(10, TimeUnit.SECONDS), "FXML load should complete");
        if (error.get() != null) {
            fail("overview-view.fxml failed to load", error.get());
        }
    }

    @Test
    void mainViewLoadsWithAllIncludedTabs() throws Exception {
        AtomicReference<Throwable> error = new AtomicReference<>();
        CountDownLatch done = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                AppPreferences preferences = new AppPreferences();
                SteamLocator steamLocator = new SteamLocator(new JnaWindowsRegistry());
                KeybindConfigParser keybindParser = new KeybindConfigParser();
                GsiService gsiService = new GsiService();
                FXMLLoader loader = new FXMLLoader(OverviewViewTest.class.getResource(MAIN_VIEW));
                // The same registry RandomizerApp uses, so every fx:include resolves its controller.
                CrosshairStandard crosshairStandard = new CrosshairStandard(
                        new CrosshairBackup(tempDir.resolve("backup"), new CrosshairConfigParser()));
                loader.setControllerFactory(ControllerFactory.create(
                        new ConfigRepository<>(ConfigKind.DEFAULT, preferences, steamLocator, keybindParser),
                        new ConfigRepository<>(ConfigKind.USER, preferences, steamLocator, keybindParser),
                        new ConfigRepository<>(CrosshairSource.INSTANCE, preferences, steamLocator,
                                new CrosshairConfigParser()),
                        crosshairStandard,
                        new ExecApplier(new ExecConfig(java.util.Optional::empty),
                                vk -> Cs2Window.PressResult.WINDOW_NOT_FOUND, () -> "l"),
                        new AppPreferences(tempDir),
                        new CrosshairRandomizer(),
                        new ConfigSaver(),
                        new ConfigVerifier(),
                        gsiService));
                loader.load();
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
    void scoreboardFillsFromPostedAllPlayers() throws Exception {
        int port = freePort();
        GsiService service = new GsiService(port);
        AtomicReference<TableView<?>> ctTable = new AtomicReference<>();
        AtomicReference<TableView<?>> tTable = new AtomicReference<>();
        AtomicReference<Label> mapLabel = new AtomicReference<>();
        AtomicReference<Label> timeLabel = new AtomicReference<>();
        AtomicReference<Throwable> error = new AtomicReference<>();
        CountDownLatch loaded = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(OverviewViewTest.class.getResource(OVERVIEW_VIEW));
                loader.setControllerFactory(type -> new OverviewController(service));
                loader.load();
                ctTable.set((TableView<?>) loader.getNamespace().get("ctTable"));
                tTable.set((TableView<?>) loader.getNamespace().get("tTable"));
                mapLabel.set((Label) loader.getNamespace().get("mapLabel"));
                timeLabel.set((Label) loader.getNamespace().get("timeLabel"));
            } catch (Throwable t) {
                error.set(t);
            } finally {
                loaded.countDown();
            }
        });
        assertTrue(loaded.await(10, TimeUnit.SECONDS), "FXML load should complete");
        if (error.get() != null) {
            fail("overview-view.fxml failed to load", error.get());
        }

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

            // render() runs on the FX thread via Platform.runLater; poll until the tables fill.
            boolean filled = awaitFx(
                    () -> ctTable.get().getItems().size() == 1 && tTable.get().getItems().size() == 1, 5000);
            assertTrue(filled, "scoreboard should populate one CT and one T player from the posted allplayers");

            AtomicReference<String> header = new AtomicReference<>();
            runFx(() -> header.set(mapLabel.get().getText()));
            assertTrue(header.get().contains("de_dust2"), "header should show the map name, was: " + header.get());

            AtomicReference<String> time = new AtomicReference<>();
            AtomicBoolean timeVisible = new AtomicBoolean();
            runFx(() -> {
                time.set(timeLabel.get().getText());
                timeVisible.set(timeLabel.get().isVisible());
            });
            assertTrue(timeVisible.get(), "round timer should be visible once the round is live");
            assertTrue(time.get() != null && time.get().matches("1:5\\d"),
                    "round timer should start at ~1:55, was: " + time.get());

            // A bomb plant switches to the 40s detonation timer (round is still live).
            post(port, "{\"round\":{\"phase\":\"live\",\"bomb\":\"planted\"}}");
            boolean switched = awaitFx(() -> timeLabel.get().getText().startsWith("Bombe"), 5000);
            assertTrue(switched, "bomb plant should switch to the 'Bombe' detonation timer");
            AtomicReference<String> bombTime = new AtomicReference<>();
            runFx(() -> bombTime.set(timeLabel.get().getText()));
            assertTrue(bombTime.get().matches("Bombe\\s+0:[34]\\d"),
                    "bomb timer should start at ~0:40, was: " + bombTime.get());

            // Round ends → timer stops (hidden).
            post(port, "{\"round\":{\"phase\":\"over\"}}");
            boolean hidden = awaitFx(() -> !timeLabel.get().isVisible(), 5000);
            assertTrue(hidden, "timer should stop once the round is no longer live");
        } finally {
            service.close();
        }
    }

    @Test
    void wingmanRoundStartsAtNinetySeconds() throws Exception {
        int port = freePort();
        GsiService service = new GsiService(port);
        AtomicReference<Label> timeLabel = new AtomicReference<>();
        AtomicReference<Throwable> error = new AtomicReference<>();
        CountDownLatch loaded = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(OverviewViewTest.class.getResource(OVERVIEW_VIEW));
                loader.setControllerFactory(type -> new OverviewController(service));
                loader.load();
                timeLabel.set((Label) loader.getNamespace().get("timeLabel"));
            } catch (Throwable t) {
                error.set(t);
            } finally {
                loaded.countDown();
            }
        });
        assertTrue(loaded.await(10, TimeUnit.SECONDS), "FXML load should complete");
        if (error.get() != null) {
            fail("overview-view.fxml failed to load", error.get());
        }

        try {
            assertTrue(service.start(), "GSI listener should start");
            // mode "scrimcomp2v2" is Wingman.
            post(port, "{\"map\":{\"mode\":\"scrimcomp2v2\"},\"round\":{\"phase\":\"live\"}}");

            boolean visible = awaitFx(() -> timeLabel.get().isVisible(), 5000);
            assertTrue(visible, "timer should show once a Wingman round goes live");
            AtomicReference<String> time = new AtomicReference<>();
            runFx(() -> time.set(timeLabel.get().getText()));
            assertTrue(time.get().matches("1:3\\d"),
                    "Wingman round timer should start at ~1:30, was: " + time.get());
        } finally {
            service.close();
        }
    }

    // ---- helpers ---------------------------------------------------------------------------

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
        assertTrue(latch.await(5, TimeUnit.SECONDS), "FX action should complete");
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
