package kmlib.starsector.salvage;

import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.impl.campaign.procgen.SalvageEntityGenDataSpec.DropData;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.SalvageEntity;

import java.util.List;
import java.util.Random;

/**
 * Drop lists for the game's salvage roller, and the roll itself. {@link DropData} is a mutable field bag with a
 * no-arg constructor, and a roll takes four bare float scalars, so a caller writing either by hand repeats the same
 * setter runs and unit literals. A random drop is drawn by weight; a value drop spends a credit budget on its group.
 */
public final class SalvageDrops {

    private static final int NO_CHANCES = 0;
    private static final int NO_VALUE = 0;

    // The roller's value, random and fuel multipliers, and a drop's own value multiplier, at the setting that leaves
    // the roll as the data describes it.
    private static final float NEUTRAL_MULT = 1f;

    private SalvageDrops() {
    }

    /**
     * A random drop at its full value.
     *
     * @param group   the drop group
     * @param chances the drop's weight against the other random drops
     * @return the drop
     */
    public static DropData buildRandomDrop(String group, int chances) {

        return buildRandomDrop(group, chances, NEUTRAL_MULT);
    }

    /**
     * A random drop with its value scaled.
     *
     * @param group     the drop group
     * @param chances   the drop's weight against the other random drops
     * @param valueMult the multiplier on what the drop is worth
     * @return the drop
     */
    public static DropData buildRandomDrop(String group, int chances, float valueMult) {

        return buildDrop(group, chances, NO_VALUE, valueMult);
    }

    /**
     * A value drop: a credit budget the roller spends on {@code group}.
     *
     * @param group the drop group
     * @param value the credit budget
     * @return the drop
     */
    public static DropData buildValueDrop(String group, int value) {

        return buildDrop(group, NO_CHANCES, value, NEUTRAL_MULT);
    }

    private static DropData buildDrop(String group, int chances, int value, float valueMult) {

        var drop = new DropData();

        drop.group = group;
        drop.chances = chances;
        drop.value = value;
        drop.valueMult = valueMult;

        return drop;
    }
}
