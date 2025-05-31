package de.bsommerfeld.model.persistence;

import com.google.gson.Gson;
import com.google.inject.Inject;
import de.bsommerfeld.model.action.Action;
import de.bsommerfeld.model.action.sequence.ActionSequence;

public class JsonUtil {

    private final Gson gson;

    @Inject
    public JsonUtil(Gson gson) {
        this.gson = gson;
    }

    /**
     * Serializes the given ActionSequence object into its JSON representation.
     *
     * @param actionSequence the ActionSequence object to be serialized
     *
     * @return the JSON representation of the given ActionSequence object
     */
    public String serialize(Object actionSequence) {
        return gson.toJson(actionSequence);
    }

    /**
     * Deserializes a JSON string into an {@link ActionSequence} object.
     *
     * @param json The JSON string to be deserialized.
     *
     * @return The {@link ActionSequence} object represented by the JSON string.
     */
    public ActionSequence deserializeActionSequence(String json) {
        return gson.fromJson(json, ActionSequence.class);
    }

    /**
     * Deserializes a JSON string into an {@link Action} object.
     *
     * @param json The JSON string to be deserialized.
     *
     * @return The {@link Action} object represented by the JSON string.
     */
    public Action deserializeAction(String json) {
        return gson.fromJson(json, Action.class);
    }
}
