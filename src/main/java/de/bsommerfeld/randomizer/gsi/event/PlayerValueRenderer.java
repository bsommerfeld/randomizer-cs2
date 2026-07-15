package de.bsommerfeld.randomizer.gsi.event;

import com.cs2gsi.nodes.Player;

/**
 * Renders a {@link Player} as a compact identity. The full player state is noise on "X changed"
 * events (it even repeats the affected weapon); the actual change is carried by the event's own
 * dedicated fields.
 */
public final class PlayerValueRenderer implements ValueRenderer {

    @Override
    public boolean supports(Object value) {
        return value instanceof Player;
    }

    @Override
    public String render(Object value) {
        Player player = (Player) value;
        StringBuilder sb = new StringBuilder("[Name: ").append(player.name);
        if (player.steamId != null && !player.steamId.isBlank()) {
            sb.append(", SteamID: ").append(player.steamId);
        }
        return sb.append(", Team: ").append(player.team).append(']').toString();
    }
}
