package de.bsommerfeld.updater;

public class Version implements Comparable<Version> {

  private final int major;
  private final int minor;
  private final int patch;

  public Version(String version) {
    String[] parts = version.split("\\.");
    if (parts.length != 3) {
      throw new IllegalArgumentException("Invalid version format: " + version);
    }
    try {
      this.major = Integer.parseInt(parts[0]);
      this.minor = Integer.parseInt(parts[1]);
      this.patch = Integer.parseInt(parts[2]);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Invalid version format: " + version, e);
    }
  }

  @Override
  public int compareTo(Version other) {
    if (this.major != other.major) {
      return Integer.compare(this.major, other.major);
    }
    if (this.minor != other.minor) {
      return Integer.compare(this.minor, other.minor);
    }
    return Integer.compare(this.patch, other.patch);
  }
}
