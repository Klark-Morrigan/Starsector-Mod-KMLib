package kmlib.testfixtures.starsector.ui.coreui;

import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import kmlib.math.geometry.Rectangle;
import kmlib.testfixtures.starsector.ui.layout.PositionFake;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A panel that takes children, standing in for the core UI wherever a rule adds a panel of its own to
 * the live tree rather than reading it. Shipped from KMLib so both KMLib's and consuming mods' tests
 * describe "somewhere to attach to" as one class.
 *
 * <p>Records what was added, what was removed and what was raised, because those are what such a rule
 * does and none of them is observable from its return value. Held in order and by identity, a panel's
 * children being a list of distinct widgets rather than a set of equal ones.
 *
 * <p>Hands back a laid-out position for every child, so a caller that positions what it added has
 * something to position. The box is the same for every child and means nothing: what a fixture cannot
 * honestly model is the layout, and a caller reading these numbers back is reading its own input.
 *
 * <p>Every drawing and input method throws. Nothing that attaches a panel calls them, so answering
 * them silently would let a test that strayed into a render path pass while proving nothing.
 */
public final class CoreUiPanelFake implements UIPanelAPI {

    private static final String NOT_A_SCREEN =
        "A fixture for attaching to a panel does not draw one or route input to it.";

    // Any box at all, so a caller with something to position has a position to move. See above.
    private static final Rectangle PLACEHOLDER_BOX = new Rectangle(0f, 0f, 1f, 1f);

    // In order of addition, so a test can say which child was added first.
    private final List<UIComponentAPI> addedComponents = new ArrayList<>();

    private final List<UIComponentAPI> removedComponents = new ArrayList<>();

    private final List<UIComponentAPI> raisedComponents = new ArrayList<>();

    // Keyed by identity so two children that compare equal still get positions of their own, which is
    // what the engine's own child list does.
    private final Map<UIComponentAPI, PositionAPI> componentPlacements = new LinkedHashMap<>();

    /**
     * @return the children added to this panel, in the order they were added and including any since
     *         removed - what was attached being a different question from what is still attached
     */
    public List<UIComponentAPI> getAddedComponents() {
        return List.copyOf(addedComponents);
    }

    /**
     * @return the children taken off this panel, in the order they were removed
     */
    public List<UIComponentAPI> getRemovedComponents() {
        return List.copyOf(removedComponents);
    }

    /**
     * @return the children raised above their siblings, in the order they were raised
     */
    public List<UIComponentAPI> getRaisedComponents() {
        return List.copyOf(raisedComponents);
    }

    /**
     * @return the children still standing on this panel - what was added and not since removed
     */
    public List<UIComponentAPI> getStandingComponents() {

        var standing = new ArrayList<>(addedComponents);
        standing.removeAll(removedComponents);

        return List.copyOf(standing);
    }

    @Override
    public PositionAPI addComponent(UIComponentAPI component) {

        addedComponents.add(component);

        return componentPlacements.computeIfAbsent(
            component,
            placed -> new PositionFake(PLACEHOLDER_BOX));
    }

    @Override
    public void removeComponent(UIComponentAPI component) {
        removedComponents.add(component);
    }

    @Override
    public void bringComponentToTop(UIComponentAPI component) {
        raisedComponents.add(component);
    }

    @Override
    public void sendToBottom(UIComponentAPI component) {
        throw new UnsupportedOperationException(NOT_A_SCREEN);
    }

    @Override
    public PositionAPI getPosition() {
        return new PositionFake(PLACEHOLDER_BOX);
    }

    @Override
    public void render(float alphaMult) {
        throw new UnsupportedOperationException(NOT_A_SCREEN);
    }

    @Override
    public void processInput(List<InputEventAPI> events) {
        throw new UnsupportedOperationException(NOT_A_SCREEN);
    }

    @Override
    public void advance(float amount) {
        throw new UnsupportedOperationException(NOT_A_SCREEN);
    }

    @Override
    public void setOpacity(float opacity) {
        throw new UnsupportedOperationException(NOT_A_SCREEN);
    }

    @Override
    public float getOpacity() {
        throw new UnsupportedOperationException(NOT_A_SCREEN);
    }
}
