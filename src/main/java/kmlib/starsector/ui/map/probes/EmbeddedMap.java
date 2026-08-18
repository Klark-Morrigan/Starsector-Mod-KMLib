package kmlib.starsector.ui.map.probes;

import com.fs.starfarer.api.ui.SectorMapAPI;

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
}
