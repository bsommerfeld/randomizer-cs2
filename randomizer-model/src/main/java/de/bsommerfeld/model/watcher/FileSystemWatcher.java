package de.bsommerfeld.model.watcher;

import de.bsommerfeld.model.persistence.dao.ActionSequenceDao;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

@Slf4j
public class FileSystemWatcher implements Runnable {

    private static final long DEBOUNCE_TIME_MS = 50;

    private final List<Consumer<Path>> fileChangeListeners = new CopyOnWriteArrayList<>();
    private final Map<Path, Long> lastModifiedTimes = new ConcurrentHashMap<>();

    public void addFileChangeListener(Consumer<Path> fileChangeListener) {
        fileChangeListeners.add(fileChangeListener);
    }

    @Override
    public void run() {
        try (WatchService watcher = FileSystems.getDefault().newWatchService()) {

            Path clusterDirectory = ActionSequenceDao.ACTION_SEQUENCE_FOLDER.toPath();
            clusterDirectory.register(
                    watcher,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_DELETE,
                    StandardWatchEventKinds.ENTRY_MODIFY);

            log.debug("Started watching the directory: {}", clusterDirectory);

            while (!Thread.currentThread().isInterrupted()) {
                WatchKey key = watcher.take();
                for (WatchEvent<?> event : key.pollEvents()) {

                    WatchEvent.Kind<?> kind = event.kind();
                    if (kind == StandardWatchEventKinds.OVERFLOW) {
                        log.warn("Event overflow occurred. Some file system events might have been missed.");
                        continue;
                    }

                    WatchEvent<Path> ev = (WatchEvent<Path>) event;
                    Path filename = ev.context();

                    if (filename.toString().endsWith(".sequence")) {
                        processEvent(filename, kind);
                    }
                }

                boolean valid = key.reset();
                if (!valid) {
                    log.warn("WatchKey no longer valid. Stopping watcher.");
                    break;
                }
            }

        } catch (IOException e) {
            log.error("IOException occurred while watching directory: ", e);
        } catch (InterruptedException e) {
            log.warn("FileSystemWatcher was interrupted: ", e);
        } finally {
            Thread.currentThread().interrupt();
        }
    }

    private void processEvent(Path filename, WatchEvent.Kind<?> kind) {
        synchronized (lastModifiedTimes) {
            Long lastModifiedTime = lastModifiedTimes.getOrDefault(filename, 0L);
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastModifiedTime > DEBOUNCE_TIME_MS) {
                lastModifiedTimes.put(filename, currentTime);
                log.debug("Detected {} on file: {}", kind.name(), filename);
                for (Consumer<Path> listener : fileChangeListeners) {
                    try {
                        listener.accept(filename);
                    } catch (Exception e) {
                        log.error("Error executing file change listener: ", e);
                    }
                }
            }
        }
    }
}