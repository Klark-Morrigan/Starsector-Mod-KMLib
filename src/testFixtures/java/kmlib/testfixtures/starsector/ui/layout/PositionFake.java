package kmlib.testfixtures.starsector.ui.layout;

import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;

import kmlib.math.geometry.Rectangle;

/**
 * A widget position already laid out at a known box, standing in for the engine's own wherever a
 * rule reads where something is rather than placing it.
 *
 * <p>{@link PositionAPI} is thirty-odd methods of which every such rule reads at most six, and a
 * test stubbing those six by hand states the same four numbers three ways: as the box it means, as
 * the stubs it writes, and as the expectation it asserts. Taking a {@link Rectangle} says it once,
 * and says it in the type the reads answer in - so a test can hand the same box to a fixture and to
 * an assertion and be describing one thing.
 *
 * <p>Published as a fixture variant, which is also what makes it worth having over a mock: a
 * consuming mod's tests can lay a widget out without depending on a mocking framework to describe a
 * rectangle, and every repo's idea of "a widget at this box" is then one class.
 *
 * <p>The centre is derived rather than taken, so it cannot disagree with the box around it. The
 * engine derives it too, and a fixture that let a test set the two independently would admit a
 * position no laid-out widget is ever in.
 *
 * <p>Every laying-out method throws. Nothing that reads a position calls them, so answering them
 * silently - or worse, letting one quietly move this box - would let a test that strayed into a
 * layout path pass while proving nothing.
 */
public final class PositionFake implements PositionAPI {

    private static final String NOT_A_LAYOUT =
        "A fixture for reading a laid-out position does not lay one out.";

    private final Rectangle box;

    /**
     * @param box where the widget was laid out, in the UI units a layout is measured in
     */
    public PositionFake(Rectangle box) {
        this.box = box;
    }

    @Override
    public float getX() {
        return box.x();
    }

    @Override
    public float getY() {
        return box.y();
    }

    @Override
    public float getWidth() {
        return box.width();
    }

    @Override
    public float getHeight() {
        return box.height();
    }

    @Override
    public float getCenterX() {
        return box.computeCenterX();
    }

    @Override
    public float getCenterY() {
        return box.computeCenterY();
    }

    @Override
    public boolean containsEvent(InputEventAPI event) {
        throw new UnsupportedOperationException(NOT_A_LAYOUT);
    }

    @Override
    public boolean isSuspendRecompute() {
        throw new UnsupportedOperationException(NOT_A_LAYOUT);
    }

    @Override
    public PositionAPI aboveLeft(UIComponentAPI other, float pad) {
        throw new UnsupportedOperationException(NOT_A_LAYOUT);
    }

    @Override
    public PositionAPI aboveMid(UIComponentAPI other, float pad) {
        throw new UnsupportedOperationException(NOT_A_LAYOUT);
    }

    @Override
    public PositionAPI aboveRight(UIComponentAPI other, float pad) {
        throw new UnsupportedOperationException(NOT_A_LAYOUT);
    }

    @Override
    public PositionAPI belowLeft(UIComponentAPI other, float pad) {
        throw new UnsupportedOperationException(NOT_A_LAYOUT);
    }

    @Override
    public PositionAPI belowMid(UIComponentAPI other, float pad) {
        throw new UnsupportedOperationException(NOT_A_LAYOUT);
    }

    @Override
    public PositionAPI belowRight(UIComponentAPI other, float pad) {
        throw new UnsupportedOperationException(NOT_A_LAYOUT);
    }

    @Override
    public PositionAPI inBL(float padX, float padY) {
        throw new UnsupportedOperationException(NOT_A_LAYOUT);
    }

    @Override
    public PositionAPI inBMid(float pad) {
        throw new UnsupportedOperationException(NOT_A_LAYOUT);
    }

    @Override
    public PositionAPI inBR(float padX, float padY) {
        throw new UnsupportedOperationException(NOT_A_LAYOUT);
    }

    @Override
    public PositionAPI inLMid(float pad) {
        throw new UnsupportedOperationException(NOT_A_LAYOUT);
    }

    @Override
    public PositionAPI inMid() {
        throw new UnsupportedOperationException(NOT_A_LAYOUT);
    }

    @Override
    public PositionAPI inRMid(float pad) {
        throw new UnsupportedOperationException(NOT_A_LAYOUT);
    }

    @Override
    public PositionAPI inTL(float padX, float padY) {
        throw new UnsupportedOperationException(NOT_A_LAYOUT);
    }

    @Override
    public PositionAPI inTMid(float pad) {
        throw new UnsupportedOperationException(NOT_A_LAYOUT);
    }

    @Override
    public PositionAPI inTR(float padX, float padY) {
        throw new UnsupportedOperationException(NOT_A_LAYOUT);
    }

    @Override
    public PositionAPI leftOfBottom(UIComponentAPI other, float pad) {
        throw new UnsupportedOperationException(NOT_A_LAYOUT);
    }

    @Override
    public PositionAPI leftOfMid(UIComponentAPI other, float pad) {
        throw new UnsupportedOperationException(NOT_A_LAYOUT);
    }

    @Override
    public PositionAPI leftOfTop(UIComponentAPI other, float pad) {
        throw new UnsupportedOperationException(NOT_A_LAYOUT);
    }

    @Override
    public PositionAPI rightOfBottom(UIComponentAPI other, float pad) {
        throw new UnsupportedOperationException(NOT_A_LAYOUT);
    }

    @Override
    public PositionAPI rightOfMid(UIComponentAPI other, float pad) {
        throw new UnsupportedOperationException(NOT_A_LAYOUT);
    }

    @Override
    public PositionAPI rightOfTop(UIComponentAPI other, float pad) {
        throw new UnsupportedOperationException(NOT_A_LAYOUT);
    }

    @Override
    public PositionAPI setLocation(float x, float y) {
        throw new UnsupportedOperationException(NOT_A_LAYOUT);
    }

    @Override
    public PositionAPI setSize(float width, float height) {
        throw new UnsupportedOperationException(NOT_A_LAYOUT);
    }

    @Override
    public PositionAPI setXAlignOffset(float offset) {
        throw new UnsupportedOperationException(NOT_A_LAYOUT);
    }

    @Override
    public PositionAPI setYAlignOffset(float offset) {
        throw new UnsupportedOperationException(NOT_A_LAYOUT);
    }

    @Override
    public void setSuspendRecompute(boolean isSuspended) {
        throw new UnsupportedOperationException(NOT_A_LAYOUT);
    }
}
