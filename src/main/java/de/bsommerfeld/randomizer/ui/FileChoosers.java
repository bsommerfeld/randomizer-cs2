package de.bsommerfeld.randomizer.ui;

import javafx.stage.FileChooser;

/** Builds the {@link FileChooser}s used to pick CS2 config files manually. */
public final class FileChoosers {

    private FileChoosers() {
    }

    /** A chooser whose filter only offers files named exactly {@code fileName}. */
    public static FileChooser exactName(String fileName) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(fileName + " auswählen");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(fileName, fileName));
        return chooser;
    }
}
