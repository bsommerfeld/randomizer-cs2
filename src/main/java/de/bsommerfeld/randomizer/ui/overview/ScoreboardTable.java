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

/** Sets up one team's scoreboard table with its columns and a row style for dead players and the local player. */
final class ScoreboardTable {

    private ScoreboardTable() {
    }

    /** @param localSteamId asked on every row update, because the local player can change with every game state */
    static void configure(TableView<Player> table, Supplier<String> localSteamId) {
        table.getColumns().setAll(columns());
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.setPlaceholder(new Label("No players."));
        table.setRowFactory(tv -> new StyledRow(localSteamId));
    }

    private static List<TableColumn<Player, String>> columns() {
        return List.of(
                column("Name", 150, p -> p.name.isBlank() ? GameTexts.ABSENT : p.name),
                stat("HP", 52, p -> GameTexts.num(p.state.health)),
                stat("Armor", 64, p -> GameTexts.num(p.state.armor)),
                stat("$", 70, p -> GameTexts.money(p.state.money)),
                stat("K", 44, p -> GameTexts.num(p.matchStats.kills)),
                stat("A", 44, p -> GameTexts.num(p.matchStats.assists)),
                stat("D", 44, p -> GameTexts.num(p.matchStats.deaths)),
                stat("MVP", 56, p -> GameTexts.num(p.matchStats.mvps)),
                stat("Score", 62, p -> GameTexts.num(p.matchStats.score)),
                column("Weapon", 130, p -> GameTexts.weaponName(p.getActiveWeapon())));
    }

    private static final class StyledRow extends TableRow<Player> {

        private final Supplier<String> localSteamId;

        StyledRow(Supplier<String> localSteamId) {
            this.localSteamId = localSteamId;
        }

        @Override
        protected void updateItem(Player player, boolean empty) {
            super.updateItem(player, empty);
            getStyleClass().removeAll("dead", "self");
            if (empty || player == null) {
                return;
            }
            if (isDead(player)) {
                getStyleClass().add("dead");
            }
            if (isSelf(player)) {
                getStyleClass().add("self");
            }
        }

        private boolean isSelf(Player player) {
            String selfId = localSteamId.get();
            return selfId != null && selfId.equals(player.steamId);
        }
    }

    private static boolean isDead(Player player) {
        return player.state.isValid() && player.state.health == 0;
    }

    /** All players of {@code team}, best score first. */
    static List<Player> playersOf(GameState state, PlayerTeam team) {
        return state.allPlayers.values().stream()
                .filter(p -> p.team == team)
                .sorted(Comparator.comparingInt((Player p) -> p.matchStats.score).reversed()
                        .thenComparing(p -> p.name))
                .toList();
    }

    /** A number column, capped in width so the spare room goes to the name and weapon columns. */
    private static TableColumn<Player, String> stat(String title, double width, Function<Player, String> value) {
        TableColumn<Player, String> col = column(title, width, value);
        col.setMaxWidth(width);
        return col;
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
