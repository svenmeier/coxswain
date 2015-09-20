package svenmeier.coxswain.gym;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by sven on 07.07.15.
 */
public enum Difficulty {
    EASY, MEDIUM, HARD;

    public Difficulty increase() {
        Difficulty[] values = values();

        return values[(ordinal() + 1) % values.length];
    }
}