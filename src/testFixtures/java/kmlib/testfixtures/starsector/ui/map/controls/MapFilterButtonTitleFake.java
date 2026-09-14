package kmlib.testfixtures.starsector.ui.map.controls;

import kmlib.testfixtures.starsector.ui.label.ButtonLabelFake;

/**
 * The piece of a button's renderer that carries its words, reached the way the game leaves it
 * reachable: by an accessor it does not rename between builds.
 *
 * <p>Its own type rather than the words themselves, because the hop above it is chosen by what a
 * return type promises - a reach looking for words follows the member whose type says it has some,
 * so a fixture in which that member handed the words back directly would not exercise the choice.
 */
public final class MapFilterButtonTitleFake {

    private final ButtonLabelFake label;

    MapFilterButtonTitleFake(ButtonLabelFake label) {
        this.label = label;
    }

    /** The name the game leaves alone, and so the one thing here worth matching by name. */
    public ButtonLabelFake getTitle() {
        return label;
    }
}
