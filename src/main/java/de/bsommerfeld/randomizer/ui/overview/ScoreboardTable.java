package de.bsommerfeld.randomizer.ui.overview;

import com.cs2gsi.GameState;
import com.cs2gsi.nodes.Player;
import com.cs2gsi.nodes.PlayerTeam;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;

import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Configures one team's scoreboard table: the stat columns and the row styling (dead players,
 * the local player's own row).
 */
final class ScoreboardTable {

    private ScoreboardTable() {
    }

    /**
     * @param localSteamId supplies the local player's Steam ID at render time, so their row can be
     *                     highlighted even though it changes with every game state
     */
    static void configure(TableView<Player> table, Supplier<String> localSteamId) {
        table.getColumns().setAll(List.of(
                column("Name", 150, p -> p.name.isBlank() ? "-" : p.name),
                column("HP", 44, p -> GameTexts.num(p.state.health)),
                column("Armor", 54, p -> GameTexts.num(p.state.armor)),
                column("$", 62, p -> GameTexts.money(p.state.money)),
                column("K", 34, p -> GameTexts.num(p.matchStats.kills)),
                column("A", 34, p -> GameTexts.num(p.matchStats.assists)),
                column("D", 34, p -> GameTexts.num(p.matchStats.deaths)),
                column("MVP", 44, p -> GameTexts.num(p.matchStats.mvps)),
                column("Score", 52, p -> GameTexts.num(p.matchStats.score)),
                column("Waffe", 130, p -> GameTexts.weaponName(p.getActiveWeapon()))));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.setPlaceholder(new Label("Keine Spieler."));
        table.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(Player player, boolean empty) {
                super.updateItem(player, empty);
                getStyleClass().removeAll("dead", "self");
                if (!empty && player != null) {
                    if (player.state.isValid() && player.state.health == 0) {
                        getStyleClass().add("dead");
                    }
                    String selfId = localSteamId.get();
                    if (selfId != null && selfId.equals(player.steamId)) {
                        getStyleClass().add("self");
                    }
                }
            }
        });
    }

    /** All players of {@code team}, best score first. */
    static List<Player> playersOf(GameState state, PlayerTeam team) {
        return state.allPlayers.values().stream()
                .filter(p -> p.team == team)
                .sorted(Comparator.comparingInt((Player p) -> p.matchStats.score).reversed()
                        .thenComparing(p -> p.name))
                .toList();
    }

    private static TableColumn<Player, String> column(String title, double width, Function<Player, String> value) {
        TableColumn<Player, String> col = new TableColumn<>(title);
        col.setPrefWidth(width);
        col.setSortable(false);
        col.setReorderable(false);
        col.setCellValueFactory(cd -> new SimpleStringProperty(value.apply(cd.getValue())));
        return col;
    }
}
