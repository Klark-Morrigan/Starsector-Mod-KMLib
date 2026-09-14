package kmlib.testfixtures.starsector.ui.map.controls;

import kmlib.testfixtures.starsector.ui.label.ButtonLabelFake;

/**
 * What draws one of the row's buttons, in the shape anything reaching that button's words has to get
 * through: a renderer that draws none of its own, holding the piece that does.
 *
 * <p>That indirection is the fixture's whole reason for existing. The game furnishes a map's filter
 * row with buttons whose renderer holds its words one level down, which is why the published text
 * accessors on the button answer nothing for them and why the game's own key announcement passes
 * them by. A fixture whose button held its words directly would let a subject that reached for them
 * the easy way pass, and that subject would find nothing in a running game.
 *
 * <p>The hop down is deliberately named as meaninglessly as the game names it, and the title
 * accessor deliberately is not - the game leaves that one alone, and matching it by name is what
 * makes the reach possible at all.
 */
public final class MapFilterButtonRendererFake {

    private final MapFilterButtonTitleFake title;

    MapFilterButtonRendererFake(ButtonLabelFake label) {
        this.title = new MapFilterButtonTitleFake(label);
    }

    /** The hop the game gives a regenerated name, so nothing can reach past it by knowing one. */
    public MapFilterButtonTitleFake o00000() {
        return title;
    }
}
