package de.bsommerfeld.randomizer.ui.crosshair;

import de.bsommerfeld.randomizer.config.AppPreferences;
import de.bsommerfeld.randomizer.config.ConfigRepository;
import de.bsommerfeld.randomizer.config.crosshair.Crosshair;
import de.bsommerfeld.randomizer.config.crosshair.CrosshairConfigParser;
import de.bsommerfeld.randomizer.config.crosshair.CrosshairSource;
import de.bsommerfeld.randomizer.steam.JnaWindowsRegistry;
import de.bsommerfeld.randomizer.steam.SteamLocator;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Smoke test that the crosshair FXML loads: every {@code fx:id} resolves, {@code initialize()} runs
 * (background image loads from resources, canvas draws), and the stylesheet is found. Auto-detection
 * of {@code cs2_user_convars.vcfg} degrading to empty must not throw.
 */
class CrosshairViewTest {

    private static final String VIEW = "/de/bsommerfeld/randomizer/ui/crosshair-view.fxml";

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
    void crosshairViewLoads() throws Exception {
        AtomicReference<Throwable> error = new AtomicReference<>();
        CountDownLatch done = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                ConfigRepository<Crosshair> repository = new ConfigRepository<>(
                        CrosshairSource.INSTANCE, new AppPreferences(),
                        new SteamLocator(new JnaWindowsRegistry()), new CrosshairConfigParser());
                FXMLLoader loader = new FXMLLoader(CrosshairViewTest.class.getResource(VIEW));
                loader.setControllerFactory(type -> new CrosshairController(repository));
                loader.load();
            } catch (Throwable t) {
                error.set(t);
            } finally {
                done.countDown();
            }
        });

        assertTrue(done.await(10, TimeUnit.SECONDS), "FXML load should complete");
        if (error.get() != null) {
            fail("crosshair-view.fxml failed to load", error.get());
        }
    }
}
