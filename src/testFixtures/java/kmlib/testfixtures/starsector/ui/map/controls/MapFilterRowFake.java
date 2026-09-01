package kmlib.testfixtures.starsector.ui.map.controls;

import java.util.ArrayList;
import java.util.List;

/**
 * The row of toggles the game furnishes a map screen from, in the shape a control appended to it has
 * to be matched against: a panel holding its buttons, a private factory that makes one, and a
 * private appender that puts one at the end of the row. Shipped from KMLib so both KMLib's and
 * consuming mods' tests build the same shape of row.
 *
 * <p>Both helpers carry the same meaningless name here because both carry the same meaningless name
 * in the game, where they are obfuscated members that are renamed with each build. That is the whole
 * reason this fixture is worth having in this shape rather than a tidier one: anything matching them
 * has to tell them apart by their signatures, and a fixture that named them helpfully would let a
 * match by name pass while the game's own row defeated it.
 *
 * <p>The row is also what its own buttons report their clicks to, which is how the game keeps its
 * filter settings in step with them - and so is what an appended control has to divert one button
 * away from.
 */
public final class MapFilterRowFake implements MapFilterActionListenerFake {

    // What the game's own rows are built with. Stated so a caller asking for a row gets one that
    // stands for a real screen rather than an empty panel, which is not a shape either screen has.
    private static final float BUTTON_HEIGHT = 25f;
    private static final float BUTTON_WIDTH = 120f;

    private final List<Object> buttons = new ArrayList<>();

    private int clickCount;

    /**
     * @param buttonLabels the words on each button, in the order the row lays them out
     */
    public MapFilterRowFake(String... buttonLabels) {
        for (var buttonLabel : buttonLabels) {
            o00000(o00000(buttonLabel, null), BUTTON_WIDTH, BUTTON_HEIGHT);
        }
    }

    /**
     * @return how many clicks the row itself has been told about, which is none of the clicks on a
     *         button whose listener has been diverted elsewhere
     */
    public int countClicksHeard() {
        return clickCount;
    }

    public List<Object> getChildrenCopy() {
        return List.copyOf(buttons);
    }

    @Override
    public void actionPerformed(Object action, Object component) {
        clickCount++;
    }

    // The row's appender, which the game names the same as its factory below.
    private void o00000(MapFilterButtonFake button, float width, float height) {
        buttons.add(button);
    }

    // The row's button factory. Its first argument is the words on the button and its second the
    // keyboard shortcut, which the game passes as one of its own enum constants and as null for the
    // button that has none. Neither is carried onto the button: what this fixture stands for is the
    // signature, which is what tells the factory from the appender above.
    private MapFilterButtonFake o00000(String buttonLabel, Object shortcut) {
        return new MapFilterButtonFake(this);
    }
}
