package de.bsommerfeld.updater;

/**
 * A listener interface for monitoring the progress of a task.
 *
 * <p>The {@code ProgressListener} interface should be implemented by any class whose instances are
 * intended to be registered to receive progress updates of a long-running task, such as file download or data
 * processing.
 *
 * <p>Implementing classes should define the behavior to be executed when progress updates are
 * received.
 */
public interface ProgressListener {

    /**
     * Called to receive progress updates of a task.
     *
     * @param bytesRead  the number of bytes that have been read so far
     * @param totalBytes the total number of bytes to be read
     */
    void onProgress(long bytesRead, long totalBytes);
}
