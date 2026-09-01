package kmlib.testfixtures.starsector.ui.map.controls;

import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.PositionAPI;

import kmlib.math.geometry.Rectangle;
import kmlib.testfixtures.starsector.ui.layout.PositionFake;

import java.util.List;

/**
 * One toggle on the game's map filter row: a checked state, the listener its clicks are reported to,
 * and the box the row laid it out at. Shipped from KMLib so both KMLib's and consuming mods' tests
 * build the same shape of row.
 *
 * <p>A {@link ButtonAPI}, because the game's own filter buttons are: their checked state is part of
 * the published modding interface even though the class carrying it is obfuscated, so anything
 * driving one drives it through that interface and a stand-in that were not one could not be driven
 * at all. Everything on it but the checked state and the position throws - a row's button is asked
 * what it is showing and where it sits, and answering the rest silently would let a subject that
 * strayed into painting or shortcut handling pass while proving nothing.
 *
 * <p>Its own type rather than a bare object, because the row's two helpers are told apart by the
 * types in their signatures rather than by their names - the game gives them the same name - so a
 * button that were an {@code Object} would leave the appender indistinguishable from anything else
 * taking one.
 *
 * <p>The listener is replaceable, which is the property the whole arrangement exists to model:
 * setting one detaches the button from the row that built it, so the row stops rewriting the game's
 * filter settings on every click of it.
 *
 * <p>Unplaced until the row lays it out, and answering no position until then. That is the state the
 * game's own button is in between being built and being added, and it is a state anything measuring
 * a row has to survive.
 */
public final class MapFilterButtonFake implements ButtonAPI {

    private static final String NOT_A_ROW_BUTTON =
        "A fixture for a toggle on the map's filter row models what one is asked, not what one "
            + "draws.";

    private boolean isChecked;

    private MapFilterActionListenerFake listener;

    private PositionAPI position;

    /**
     * @param listener what its clicks are reported to, which is the row that built it
     */
    public MapFilterButtonFake(MapFilterActionListenerFake listener) {
        this.listener = listener;
    }

    /**
     * @return what its clicks are currently reported to
     */
    public MapFilterActionListenerFake readListener() {
        return listener;
    }

    @Override
    public boolean isChecked() {
        return isChecked;
    }

    @Override
    public PositionAPI getPosition() {
        return position;
    }

    /** Reports a click the way the game's own button does, so a diverted listener can be observed. */
    public void click() {
        isChecked = !isChecked;
        if (listener != null) {
            listener.actionPerformed(null, this);
        }
    }

    /**
     * Places the button where a row has laid it out.
     *
     * @param box where on the row it stands, in the UI units a layout is measured in
     */
    public void layOutAt(Rectangle box) {
        position = new PositionFake(box);
    }

    @Override
    public void setChecked(boolean isChecked) {
        this.isChecked = isChecked;
    }

    public void setListener(MapFilterActionListenerFake listener) {
        this.listener = listener;
    }

    @Override
    public void advance(float amount) {
        throw new UnsupportedOperationException(NOT_A_ROW_BUTTON);
    }

    @Override
    public void flash() {
        throw new UnsupportedOperationException(NOT_A_ROW_BUTTON);
    }

    @Override
    public void flash(boolean isBright) {
        throw new UnsupportedOperationException(NOT_A_ROW_BUTTON);
    }

    @Override
    public void flash(boolean isBright, float inDuration, float outDuration) {
        throw new UnsupportedOperationException(NOT_A_ROW_BUTTON);
    }

    @Override
    public Object getCustomData() {
        throw new UnsupportedOperationException(NOT_A_ROW_BUTTON);
    }

    @Override
    public float getGlowBrightness() {
        throw new UnsupportedOperationException(NOT_A_ROW_BUTTON);
    }

    @Override
    public float getHighlightBrightness() {
        throw new UnsupportedOperationException(NOT_A_ROW_BUTTON);
    }

    @Override
    public float getOpacity() {
        throw new UnsupportedOperationException(NOT_A_ROW_BUTTON);
    }

    @Override
    public String getText() {
        throw new UnsupportedOperationException(NOT_A_ROW_BUTTON);
    }

    @Override
    public void highlight() {
        throw new UnsupportedOperationException(NOT_A_ROW_BUTTON);
    }

    @Override
    public boolean isEnabled() {
        throw new UnsupportedOperationException(NOT_A_ROW_BUTTON);
    }

    @Override
    public boolean isHighlighted() {
        throw new UnsupportedOperationException(NOT_A_ROW_BUTTON);
    }

    @Override
    public boolean isPerformActionWhenDisabled() {
        throw new UnsupportedOperationException(NOT_A_ROW_BUTTON);
    }

    @Override
    public boolean isSkipPlayingPressedSoundOnce() {
        throw new UnsupportedOperationException(NOT_A_ROW_BUTTON);
    }

    @Override
    public void processInput(List<InputEventAPI> events) {
        throw new UnsupportedOperationException(NOT_A_ROW_BUTTON);
    }

    @Override
    public void render(float alphaMult) {
        throw new UnsupportedOperationException(NOT_A_ROW_BUTTON);
    }

    @Override
    public void setButtonDisabledPressedSound(String soundId) {
        throw new UnsupportedOperationException(NOT_A_ROW_BUTTON);
    }

    @Override
    public void setButtonPressedSound(String soundId) {
        throw new UnsupportedOperationException(NOT_A_ROW_BUTTON);
    }

    @Override
    public void setClickable(boolean isClickable) {
        throw new UnsupportedOperationException(NOT_A_ROW_BUTTON);
    }

    @Override
    public void setCustomData(Object customData) {
        throw new UnsupportedOperationException(NOT_A_ROW_BUTTON);
    }

    @Override
    public void setEnabled(boolean isEnabled) {
        throw new UnsupportedOperationException(NOT_A_ROW_BUTTON);
    }

    @Override
    public void setFlashBrightness(float brightness) {
        throw new UnsupportedOperationException(NOT_A_ROW_BUTTON);
    }

    @Override
    public void setGlowBrightness(float brightness) {
        throw new UnsupportedOperationException(NOT_A_ROW_BUTTON);
    }

    @Override
    public void setHighlightBounceDown(boolean isBouncingDown) {
        throw new UnsupportedOperationException(NOT_A_ROW_BUTTON);
    }

    @Override
    public void setHighlightBrightness(float brightness) {
        throw new UnsupportedOperationException(NOT_A_ROW_BUTTON);
    }

    @Override
    public void setMouseOverSound(String soundId) {
        throw new UnsupportedOperationException(NOT_A_ROW_BUTTON);
    }

    @Override
    public void setOpacity(float opacity) {
        throw new UnsupportedOperationException(NOT_A_ROW_BUTTON);
    }

    @Override
    public void setPerformActionWhenDisabled(boolean isPerformed) {
        throw new UnsupportedOperationException(NOT_A_ROW_BUTTON);
    }

    @Override
    public void setQuickMode(boolean isQuick) {
        throw new UnsupportedOperationException(NOT_A_ROW_BUTTON);
    }

    @Override
    public void setRightClicksOkWhenDisabled(boolean areRightClicksOk) {
        throw new UnsupportedOperationException(NOT_A_ROW_BUTTON);
    }

    @Override
    public void setShortcut(int keyCode, boolean isShownOnButton) {
        throw new UnsupportedOperationException(NOT_A_ROW_BUTTON);
    }

    @Override
    public void setShowTooltipWhileInactive(boolean isShown) {
        throw new UnsupportedOperationException(NOT_A_ROW_BUTTON);
    }

    @Override
    public void setSkipPlayingPressedSoundOnce(boolean isSkipped) {
        throw new UnsupportedOperationException(NOT_A_ROW_BUTTON);
    }

    @Override
    public void setText(String text) {
        throw new UnsupportedOperationException(NOT_A_ROW_BUTTON);
    }

    @Override
    public void unhighlight() {
        throw new UnsupportedOperationException(NOT_A_ROW_BUTTON);
    }
}
