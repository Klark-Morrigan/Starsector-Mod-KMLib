package kmlib.starsector.ui.map.probes;

import com.fs.starfarer.api.ui.SectorMapAPI;

import kmlib.math.geometry.Rectangle;

import java.util.List;

/**
 * A sector map standing somewhere other than the screen the game is showing one on, together with
 * the chain of widgets it hangs under.
 *
 * <p>The ancestry travels with the map because the map alone cannot say whose it is. A mod builds
 * its panel out of the engine's own widget classes, so the map is indistinguishable from any other
 * while it is held on its own - what surrounds it is the only thing that can name an owner, and it
 * is in hand exactly once, during the walk that found the map.
 *
 * <p>Outermost first, so the last entry is what the map hangs directly under and the first is the
 * root the walk started from.
 *
 * <p>Typed as the map interface, which is what the widget was recognised by. Whether it is also a
 * placed, drawn component - and so something the player can point at - is a further question, and
 * one whoever holds this answers for itself rather than having it decided by what the walk let
 * through.
 *
 * @param widget    the map itself
 * @param ancestors the widgets it hangs under, outermost first
 */
public record EmbeddedMap(
    SectorMapAPI widget,
    List<Object> ancestors) {

    /**
     * Copies the ancestry, which a walk carries down as a single list it pushes onto and pops off
     * again - so a chain kept as handed in would be emptied by the walk that is still running.
     */
    public EmbeddedMap {
        ancestors = List.copyOf(ancestors);
    }

    /**
     * Where this map is drawn on screen, in UI units.
     *
     * <p>The further question the note above leaves to whoever holds one, answered here rather than
     * at each holder so the sifting - a map that is not a component, one faded out, one the layout
     * never positioned - is stated once and the same way the tree walks state it.
     *
     * <p>Read afresh at every ask and never kept, because an embedded map is not furniture. A panel
     * a mod slides on and off screen is created somewhere the player cannot see it and walks to its
     * resting place over many frames, so a box held even for the length of that walk describes where
     * the map has been rather than where it is - and a rule comparing the cursor against the stale
     * one answers about a surface that is no longer there.
     *
     * @return its box in UI units, or null when the map is not something the player can currently
     *         point at - not a placed component, or drawn to nothing
     */
    public Rectangle resolveDrawnBox() {
        return DrawnWidgets.resolveDrawnBoxOf(widget);
    }
}
