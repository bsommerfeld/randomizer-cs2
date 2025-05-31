package de.bsommerfeld.model.tracker;

public class TimeTracker {

  private final long startTime;

  public TimeTracker() {
    this.startTime = System.currentTimeMillis();
  }

  public long getElapsedTime() {
    return System.currentTimeMillis() - startTime;
  }
}
