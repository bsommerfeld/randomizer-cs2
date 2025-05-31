package de.bsommerfeld.model.config.keybind;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class KeyBindNameTypeMapper {

  private final Map<String, NameType> descriptorToNameMap = new HashMap<>();

  public KeyBindNameTypeMapper() {
    initializeDescriptorMappings();
  }

  /**
   * Checks if the given descriptor exists in the descriptor-to-name map.
   *
   * @param descriptor the key to be checked in the map
   *
   * @return true if the map contains the key; false otherwise
   */
  public boolean hasKey(String descriptor) {
    return descriptorToNameMap.containsKey(descriptor);
  }

  /**
   * Retrieves the key name associated with the given descriptor. If the descriptor does not have a predefined key
   * name, the descriptor itself is returned.
   *
   * @param descriptor The descriptor whose key name is to be retrieved.
   *
   * @return The key name associated with the given descriptor, or the descriptor itself if no key name is found.
   */
  public String getKeyName(String descriptor) {
    if (!hasKey(descriptor)) {
      return descriptor;
    }
    NameType nameType = descriptorToNameMap.get(descriptor);
    return nameType.name();
  }

  /**
   * Retrieves the {@link KeyBindType} associated with the given descriptor.
   *
   * @param descriptor the key used to retrieve the associated Type
   *
   * @return the Type associated with the given descriptor
   *
   * @throws IllegalStateException if the given descriptor does not have an associated Type
   */
  public KeyBindType getType(String descriptor) {
    if (!hasKey(descriptor)) {
      return KeyBindType.MISCELLANEOUS;
    }
    NameType nameType = descriptorToNameMap.get(descriptor);
    return nameType.type();
  }

  /**
   * Retrieves the KeyBindType by its name.
   *
   * @param name the name of the KeyBindType to be retrieved
   *
   * @return the KeyBindType corresponding to the provided name, or KeyBindType.MISCELLANEOUS if no matching type is
   * found
   */
  public KeyBindType getTypeByName(String name) {
    for (NameType nameType : descriptorToNameMap.values()) {
      if (nameType.name().equals(name)) {
        return nameType.type();
      }
    }
    return KeyBindType.MISCELLANEOUS;
  }

  /**
   * Retrieves an immutable copy of the descriptor-to-name map.
   *
   * @return an unmodifiable map where keys are descriptors and values are name types.
   */
  public Map<String, NameType> getDescriptorToNameMap() {
    return Map.copyOf(descriptorToNameMap);
  }

  private void initializeDescriptorMappings() {
    descriptorToNameMap.put("+jump", new NameType("Jump", KeyBindType.MOVEMENT));
    descriptorToNameMap.put("+left", new NameType("Move Left", KeyBindType.MOVEMENT));
    descriptorToNameMap.put("+right", new NameType("Move Right", KeyBindType.MOVEMENT));
    descriptorToNameMap.put("+back", new NameType("Move Backward", KeyBindType.MOVEMENT));
    descriptorToNameMap.put("+forward", new NameType("Move Forward", KeyBindType.MOVEMENT));
    descriptorToNameMap.put("+duck", new NameType("Crouch", KeyBindType.MOVEMENT));
    descriptorToNameMap.put("+sprint", new NameType("Sprint", KeyBindType.MOVEMENT));

    descriptorToNameMap.put("slot1", new NameType("Select Slot 1", KeyBindType.INVENTORY));
    descriptorToNameMap.put("slot2", new NameType("Select Slot 2", KeyBindType.INVENTORY));
    descriptorToNameMap.put("slot3", new NameType("Select Slot 3", KeyBindType.INVENTORY));
    descriptorToNameMap.put("slot4", new NameType("Select Slot 4", KeyBindType.INVENTORY));

    descriptorToNameMap.put("drop", new NameType("Drop Item", KeyBindType.WEAPON));
    descriptorToNameMap.put("+reload", new NameType("Reload", KeyBindType.WEAPON));
    descriptorToNameMap.put("+attack", new NameType("Primary Attack", KeyBindType.WEAPON));
    descriptorToNameMap.put("+attack2", new NameType("Secondary Attack", KeyBindType.WEAPON));

    descriptorToNameMap.put("player_ping", new NameType("Player Ping", KeyBindType.MISCELLANEOUS));
    descriptorToNameMap.put("+showscores", new NameType("Scoreboard", KeyBindType.MISCELLANEOUS));

    log.info("{} Deskriptoren initialisiert.", descriptorToNameMap.size());
  }

  public record NameType(String name, KeyBindType type) {
  }
}
