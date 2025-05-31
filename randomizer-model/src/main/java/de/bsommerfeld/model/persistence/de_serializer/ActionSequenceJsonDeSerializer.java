package de.bsommerfeld.model.persistence.de_serializer;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;
import de.bsommerfeld.model.action.Action;
import de.bsommerfeld.model.action.sequence.ActionSequence;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

@Slf4j
public class ActionSequenceJsonDeSerializer
        implements JsonSerializer<ActionSequence>, JsonDeserializer<ActionSequence> {

    private static final String NAME_KEY = "name";
    private static final String DESCRIPTION_KEY = "description";
    private static final String ACTIVE_KEY = "active";
    private static final String ACTIONS_KEY = "actions";

    @Override
    public JsonElement serialize(
            ActionSequence actionSequence, Type type, JsonSerializationContext context) {
        JsonObject jsonObject = new JsonObject();

        jsonObject.addProperty(NAME_KEY, actionSequence.getName());
        jsonObject.addProperty(DESCRIPTION_KEY, actionSequence.getDescription());
        jsonObject.addProperty(ACTIVE_KEY, actionSequence.isActive());
        jsonObject.add(ACTIONS_KEY, context.serialize(actionSequence.getActions()));

        return jsonObject;
    }

    @Override
    public ActionSequence deserialize(
            JsonElement jsonElement, Type type, JsonDeserializationContext context)
            throws JsonParseException {

        JsonObject jsonObject = jsonElement.getAsJsonObject();
        warnOnUnknownKeys(jsonObject);

        String name = extractStringValue(jsonObject, NAME_KEY, "<INVALID>");
        String description = extractStringValue(jsonObject, DESCRIPTION_KEY, "");
        List<Action> actions = extractActions(jsonObject, context);
        boolean active = extractBooleanValue(jsonObject, ACTIVE_KEY);

        ActionSequence actionSequence = new ActionSequence(name);
        actionSequence.setDescription(description);
        actionSequence.setActive(active);
        actionSequence.setActions(actions);
        return actionSequence;
    }

    private void warnOnUnknownKeys(JsonObject jsonObject) {
        for (String key : jsonObject.keySet()) {
            if (!isValidKey(key)) {
                log.warn("Unbekannter Key gefunden: {}", key);
            }
        }
    }

    private boolean isValidKey(String key) {
        return NAME_KEY.equals(key)
                || DESCRIPTION_KEY.equals(key)
                || ACTIONS_KEY.equals(key)
                || ACTIVE_KEY.equals(key);
    }

    private String extractStringValue(JsonObject jsonObject, String key, String defaultValue) {
        return jsonObject.has(key) && !jsonObject.get(key).isJsonNull()
                ? jsonObject.get(key).getAsString()
                : defaultValue;
    }

    private List<Action> extractActions(JsonObject jsonObject, JsonDeserializationContext context) {
        return jsonObject.has(ACTIONS_KEY) && !jsonObject.get(ACTIONS_KEY).isJsonNull()
                ? context.deserialize(
                jsonObject.get(ACTIONS_KEY), new TypeToken<List<Action>>() {
                }.getType())
                : Collections.emptyList();
    }

    private boolean extractBooleanValue(JsonObject jsonObject, String key) {
        return jsonObject.has(key)
                && !jsonObject.get(key).isJsonNull()
                && jsonObject.get(key).getAsBoolean();
    }
}
