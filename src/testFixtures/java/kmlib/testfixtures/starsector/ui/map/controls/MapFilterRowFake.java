package kmlib.testfixtures.starsector.ui.map.controls;

import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;

import kmlib.math.geometry.Rectangle;
import kmlib.testfixtures.starsector.ui.layout.PositionFake;

import java.util.ArrayList;
import java.util.List;

/**
 * The row of toggles the game furnishes a map screen from, in the shape a control appended to it has
 * to be matched against and measured off: a panel holding its buttons, a private factory that makes
 * one, and a private appender that puts one at the end of the row. Published as a fixture variant so both
 * KMLib's and consuming mods' tests build the same shape of row.
 *
 * <p>Both helpers carry the same meaningless name here because both carry the same meaningless name
 * in the game, where they are obfuscated members that are renamed with each build. That is the whole
 * reason this fixture is worth having in this shape rather than a tidier one: anything matching them
 * has to tell them apart by their signatures, and a fixture that named them helpfully would let a
 * match by name pass while the game's own row defeated it.
 *
 * <p>It lays its buttons out as the game's row does - left to right from the row's own left edge,
 * each one gap past the last - and it is a laid-out component itself. That is what makes it a row
 * something can be measured against rather than a list of buttons: a control appended to a real row
 * takes its height from the row and its width from the button already at the end, and neither
 * question has an answer on a row that was never placed.
 *
 * <p>The two rows the game actually builds are named rather than described, because their metrics
 * are facts about the game rather than about any one subject: a caller wanting the {@code M}
 * screen's strip or the intel visor's band asks for it by name and cannot state it wrongly. What is
 * left to state is the case neither of those covers - a row somebody else has already filled - and
 * it is stated as a box and one width, so the row's height is the height of the buttons on it by
 * construction. The game sizes a row to its buttons, and a fixture taking the two separately would
 * admit a row no screen has.
 *
 * <p>The row is also what its own buttons report their clicks to, which is how the game keeps its
 * filter settings in step with them - and so is what an appended control has to divert one button
 * away from.
 */
public final class MapFilterRowFake implements MapFilterActionListenerFake, UIComponentAPI {

    private static final String NOT_A_ROW =
        "A fixture for the map's filter row models what one is asked, not what one draws.";

    // What the game lays the M screen's strip at: six full-size toggles across the bottom of the
    // map.
    private static final float MAP_SCREEN_BUTTON_HEIGHT = 25f;
    private static final float MAP_SCREEN_BUTTON_WIDTH = 120f;

    // What it lays the intel screen's map visor at: two toggles, wider and shorter, in a smaller
    // face.
    private static final float INTEL_BAND_BUTTON_HEIGHT = 19f;
    private static final float INTEL_BAND_BUTTON_WIDTH = 125f;

    // What the game leaves between two buttons on either of its rows.
    private static final float BUTTON_GAP = 3f;

    // How much spare room a row named after one of the game's own has, counted in buttons it could
    // still take. Two rather than one so such a row is plainly roomy rather than only just wide
    // enough, which is the state the game leaves both of its own in - its M-screen strip spends
    // barely half the panel it is laid on.
    private static final int SPARE_BUTTON_SLOTS = 2;

    private final List<Object> buttons = new ArrayList<>();

    private final PositionAPI position;

    private int clickCount;

    // Where the next button laid out goes, which the game's row tracks the same way - by holding the
    // last button it placed and putting the next one to the right of it.
    private float nextButtonX;

    private MapFilterRowFake(Rectangle rowBox, float buttonWidth, String... buttonLabels) {
        position = new PositionFake(rowBox);
        nextButtonX = rowBox.x();
        for (var buttonLabel : buttonLabels) {
            o00000(o00000(buttonLabel, null), buttonWidth, rowBox.height());
        }
    }

    /**
     * The intel screen's map visor band, with room to spare, as the game leaves it.
     *
     * @param buttonLabels the words on each button, in the order the row lays them out
     * @return a row at the intel band's own metrics
     */
    public static MapFilterRowFake createIntelVisorBand(String... buttonLabels) {

        return createSpaciousRow(
            INTEL_BAND_BUTTON_WIDTH, INTEL_BAND_BUTTON_HEIGHT, buttonLabels);
    }

    /**
     * The {@code M} screen's filter strip, with room to spare, as the game leaves it.
     *
     * @param buttonLabels the words on each button, in the order the row lays them out
     * @return a row at the map screen's own metrics
     */
    public static MapFilterRowFake createMapScreenStrip(String... buttonLabels) {

        return createSpaciousRow(
            MAP_SCREEN_BUTTON_WIDTH, MAP_SCREEN_BUTTON_HEIGHT, buttonLabels);
    }

    /**
     * A row of a stated size, for the cases the game's own two do not cover - a row already filled
     * by somebody else, a row standing away from the origin, or one the layout gave no size at all.
     *
     * @param rowBox       where the row was laid out and how big it is; its height is the height of
     *                     the buttons on it, the game sizing a row to what it holds
     * @param buttonWidth  how wide each button on it is laid out
     * @param buttonLabels the words on each button, in the order the row lays them out
     * @return a row at that size
     */
    public static MapFilterRowFake createRowOfSize(
        Rectangle rowBox,
        float buttonWidth,
        String... buttonLabels) {

        return new MapFilterRowFake(rowBox, buttonWidth, buttonLabels);
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
    public PositionAPI getPosition() {
        return position;
    }

    @Override
    public void actionPerformed(Object action, Object component) {
        clickCount++;
    }

    @Override
    public void advance(float amount) {
        throw new UnsupportedOperationException(NOT_A_ROW);
    }

    @Override
    public float getOpacity() {
        throw new UnsupportedOperationException(NOT_A_ROW);
    }

    @Override
    public void processInput(List<InputEventAPI> events) {
        throw new UnsupportedOperationException(NOT_A_ROW);
    }

    @Override
    public void render(float alphaMult) {
        throw new UnsupportedOperationException(NOT_A_ROW);
    }

    @Override
    public void setOpacity(float opacity) {
        throw new UnsupportedOperationException(NOT_A_ROW);
    }

    // A row at the given metrics, laid at the origin and wide enough to take more buttons than it
    // holds - which is what both of the game's own rows are.
    private static MapFilterRowFake createSpaciousRow(
        float buttonWidth,
        float buttonHeight,
        String... buttonLabels) {

        var rowWidth = (buttonLabels.length + SPARE_BUTTON_SLOTS) * (buttonWidth + BUTTON_GAP);

        return new MapFilterRowFake(
            new Rectangle(0f, 0f, rowWidth, buttonHeight), buttonWidth, buttonLabels);
    }

    // The row's appender, which the game names the same as its factory below. It places the button
    // as well as holding it, the game's row doing both in the same member.
    private void o00000(MapFilterButtonFake button, float width, float height) {
        button.layOutAt(new Rectangle(nextButtonX, position.getY(), width, height));
        nextButtonX += width + BUTTON_GAP;
        buttons.add(button);
    }

    // The row's button factory. Its first argument is the words on the button and its second the
    // keyboard shortcut, which the game passes as one of its own enum constants and as null for the
    // button that has none. What this fixture stands for is the signature, which is what tells the
    // factory from the appender above - but the words are carried onto the button, a button's words
    // being where a bound key is announced and so something a subject can be caught not saying.
    private MapFilterButtonFake o00000(String buttonLabel, Object shortcut) {
        return new MapFilterButtonFake(this, buttonLabel);
    }
}
