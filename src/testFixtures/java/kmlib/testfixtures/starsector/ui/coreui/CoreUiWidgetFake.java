package kmlib.testfixtures.starsector.ui.coreui;

import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import java.util.List;

/**
 * A component that is both a placed, drawn widget and a parent in the core UI's tree, so a walk that
 * measures what a widget occupies and then descends into it can be driven without a running game.
 * Published as a fixture variant so both KMLib's and consuming mods' tests build the same shape of tree.
 *
 * <p>{@link CoreUiComponentFake} stands for a parent that is nothing else; this is the case a rule
 * about layout needs, where the same object has to answer for its own box and for what it holds.
 * Holding nothing reads to a walk exactly as a leaf does - both come back with no children - so no
 * second fixture is needed for the childless case.
 *
 * <p>The position is taken rather than built, because {@link PositionAPI} is a wide interface of
 * which a layout rule reads four numbers; a test supplies whichever double it already uses for the
 * rest of its widgets rather than this fixture standing up a second one.
 *
 * <p>The drawing and input halves of {@link UIComponentAPI} throw. Nothing that walks the tree for
 * layout calls them, so answering them silently would let a test that strayed into a render path
 * pass while proving nothing.
 */
public final class CoreUiWidgetFake implements UIComponentAPI {
    private final List<Object> children;
    private float opacity;
    private PositionAPI position;

    public CoreUiWidgetFake(PositionAPI position, float opacity, Object... children) {
        this.position = position;
        this.opacity = opacity;
        this.children = List.of(children);
    }

    public List<Object> getChildrenCopy() {
        return children;
    }

    @Override
    public float getOpacity() {
        return opacity;
    }

    @Override
    public PositionAPI getPosition() {
        return position;
    }

    @Override
    public float getX() {
        return readPlacedPosition().getX();
    }

    @Override
    public float getY() {
        return readPlacedPosition().getY();
    }

    @Override
    public float getWidth() {
        return readPlacedPosition().getWidth();
    }

    @Override
    public float getHeight() {
        return readPlacedPosition().getHeight();
    }

    /**
     * Puts the widget somewhere else, for a case about a widget that moves - a panel sliding on or
     * off screen, which is a state some rules exist entirely to read.
     *
     * <p>Here rather than on the position, which stays immutable on purpose: a laid-out box that
     * could be moved through {@link PositionAPI}'s own layout methods would let a test stray into a
     * layout path and pass. Moving the widget is the engine's own doing, and is stated as such.
     *
     * @param position where it stands now
     */
    public void moveWidgetTo(PositionAPI position) {
        this.position = position;
    }

    @Override
    public void setOpacity(float opacity) {
        this.opacity = opacity;
    }

    @Override
    public void advance(float amount) {
        throw new UnsupportedOperationException("A fixture for tree walks does not advance.");
    }

    @Override
    public void processInput(List<InputEventAPI> events) {
        throw new UnsupportedOperationException("A fixture for tree walks takes no input.");
    }

    @Override
    public void render(float alphaMult) {
        throw new UnsupportedOperationException("A fixture for tree walks does not render.");
    }

    @Override
    public void focusLost() {
        throw new UnsupportedOperationException("A fixture for tree walks takes no focus.");
    }

    @Override
    public UIPanelAPI getParent() {
        throw new UnsupportedOperationException("A fixture for tree walks is walked down into, never up.");
    }

    @Override
    public void setParent2(UIPanelAPI parent) {
        throw new UnsupportedOperationException("A fixture for tree walks is never reparented.");
    }

    @Override
    public void setPosition2(PositionAPI position) {
        throw new UnsupportedOperationException("A fixture for tree walks moves through moveWidgetTo.");
    }

    // A widget built with no position has no box to measure, which is the state a never-placed map is
    // in; asking it for one is the subject's mistake, so it fails by name rather than as a null read.
    private PositionAPI readPlacedPosition() {

        if (position == null) {
            throw new IllegalStateException("A fixture widget that was never placed has no box to measure.");
        }
        return position;
    }
}
