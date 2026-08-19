package kmlib.starsector.ui.map.probes;

import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;

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

    /**
     * This map as a component of the screen it stands on, for a caller whose subject is the widget
     * itself rather than a box - where it is placed whatever it is drawn at, or what it is drawn at
     * to begin with.
     *
     * <p>The same sifting {@link #resolveDrawnBox} opens with, answered on its own because the two
     * questions come apart: a box is what the player can point at, and this is the widget that is
     * there whether or not anything of it shows. Stated here rather than as an {@code instanceof} at
     * each holder, so what counts as a component is one answer across the reads of a map.
     *
     * <p>Handing the component back is still a read, and this package only reads. What a caller
     * does with it afterwards is that caller's, and a write into a widget belongs with the packages
     * that write.
     *
     * @return the map as a UI component, or null when it is not one - which is every map that is
     *         not a placed widget at all, and nothing about where it is or how it is drawn
     */
    public UIComponentAPI resolveComponent() {
        return widget instanceof UIComponentAPI component ? component : null;
    }
}
