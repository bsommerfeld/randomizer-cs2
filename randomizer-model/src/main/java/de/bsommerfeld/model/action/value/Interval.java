package de.bsommerfeld.model.action.value;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/** The Interval class represents a range between a minimum and maximum value. */
@Getter
@Setter
@AllArgsConstructor(staticName = "of")
@ToString
public class Interval {

    private int min;
    private int max;

    /**
     * Checks if the interval is empty, i.e., both minimum and maximum values are zero.
     *
     * @return true if the interval is empty, false otherwise
     */
    public boolean isEmpty() {
        return min == 0 && max == 0;
    }
}
