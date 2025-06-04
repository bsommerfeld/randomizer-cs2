package de.bsommerfeld.model.action.spi;

/**
 * Interface for executing action sequences.
 * This interface defines the contract for classes that execute action sequences.
 */
public interface ActionSequenceExecutor extends Runnable {

    /**
     * Sets the minimum wait time between action sequence executions.
     *
     * @param minWaitTime the minimum wait time in seconds
     */
    void setMinWaitTime(int minWaitTime);

    /**
     * Sets the maximum wait time between action sequence executions.
     *
     * @param maxWaitTime the maximum wait time in seconds
     */
    void setMaxWaitTime(int maxWaitTime);

    /**
     * Starts the executor in a new thread.
     *
     * @return the thread in which the executor is running
     */
    Thread start();

    /**
     * Stops the executor.
     */
    void stop();
}