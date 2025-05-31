package de.bsommerfeld.model.exception;

import de.bsommerfeld.model.ApplicationContext;
import lombok.extern.slf4j.Slf4j;

import java.io.File;

@Slf4j
public class UncaughtExceptionLogger implements Thread.UncaughtExceptionHandler {

    public static final UncaughtExceptionLogger DEFAULT_UNCAUGHT_EXCEPTION_LOGGER =
            new UncaughtExceptionLogger();

    private static final File LOG_FOLDER =
            new File(ApplicationContext.getAppdataFolder() + File.separator + "logs");

    static {
        LOG_FOLDER.mkdirs();
    }

    @Override
    public void uncaughtException(Thread thread, Throwable throwable) {
        String exceptionMessage = buildExceptionMessage(thread, throwable);
        log.error("Ein Fehler ist aufgetreten: {}", exceptionMessage);
    }

    private String buildExceptionMessage(Thread thread, Throwable throwable) {
        StringBuilder exceptionMessageBuilder =
                new StringBuilder()
                        .append("Uncaught exception in thread \"")
                        .append(thread.getName())
                        .append("\": ")
                        .append(throwable.getClass().getSimpleName())
                        .append(" - ")
                        .append(throwable.getMessage())
                        .append("\n");

        for (StackTraceElement element : throwable.getStackTrace()) {
            exceptionMessageBuilder.append("\tat ").append(element.toString()).append("\n");
        }

        if (throwable.getCause() != null) {
            exceptionMessageBuilder.append("Caused by: ").append(throwable.getCause()).append("\n");
        }
        return exceptionMessageBuilder.toString();
    }
}
