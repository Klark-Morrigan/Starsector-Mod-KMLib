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

    private static final int NO_VALUE = 0;

    private SalvageDrops() {
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

    private static DropData buildDrop(String group, int chances, int value, float valueMult) {

        var drop = new DropData();

        drop.group = group;
        drop.chances = chances;
        drop.value = value;
        drop.valueMult = valueMult;

        return drop;
    }
}
