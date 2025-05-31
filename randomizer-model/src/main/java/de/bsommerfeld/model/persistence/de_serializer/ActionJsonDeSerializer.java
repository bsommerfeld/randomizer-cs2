package de.bsommerfeld.model.persistence.de_serializer;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.inject.Inject;
import de.bsommerfeld.model.action.Action;
import de.bsommerfeld.model.action.repository.ActionRepository;
import de.bsommerfeld.model.action.value.Interval;
import java.lang.reflect.Type;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ActionJsonDeSerializer implements JsonSerializer<Action>, JsonDeserializer<Action> {

  private static final String NAME_KEY = "name";
  private static final String INTERVAL_KEY = "interval";

  private final ActionRepository actionRepository;

  @Inject
  public ActionJsonDeSerializer(ActionRepository actionRepository) {
    this.actionRepository = actionRepository;
  }

  private static void validateAction(String actionName, Action action) {
    if (action == null) {
      throw new JsonParseException("No Action found with the name: " + actionName);
    }
  }

  @Override
  public JsonElement serialize(Action action, Type type, JsonSerializationContext context) {
    JsonObject jsonObject = new JsonObject();
    jsonObject.addProperty(NAME_KEY, action.getName());
    jsonObject.add(INTERVAL_KEY, context.serialize(action.getInterval()));
    return jsonObject;
  }

  @Override
  public Action deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext context)
      throws JsonParseException {
    JsonObject jsonObject = jsonElement.getAsJsonObject();
    String actionName = jsonObject.get(NAME_KEY).getAsString();

    try {
      return assembleAction(jsonObject, actionName, context);
    } catch (Exception exception) {
      log.error(
          "Failed to deserialize Action: {}. Diese spezielle Aktion wird übersprungen.",
          actionName,
          exception);
      // Rückgabe von null, damit der fehlerhafte Eintrag übersprungen werden kann
      return null;
    }
  }

  private Action assembleAction(
      JsonObject jsonObject, String actionName, JsonDeserializationContext context) {
    Action action = actionRepository.getByName(actionName);
    validateAction(actionName, action);
    Interval interval = context.deserialize(jsonObject.get(INTERVAL_KEY), Interval.class);
    action.setInterval(interval);
    return action;
  }
}
