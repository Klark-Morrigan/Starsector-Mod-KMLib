package kmlib.testfixtures.starsector.ui.coreui;

import java.util.List;

/**
 * A component that answers the core UI's {@code getChildrenCopy} contract, so a by-name walk over
 * the live widget tree can be driven without a running game. Shipped from KMLib so both KMLib's and
 * consuming mods' tests build the same shape of tree.
 *
 * <p>Models only the parent side of that contract. A component exposing no such method is what the
 * tree calls a leaf, and any object at all already stands for one - giving this fixture a "no
 * children" mode would model it as a parent holding nothing, which is the opposite state.
 *
 * <p>Counts the times it was asked for its children, which is how a walk is observed from outside:
 * anything that answers without walking never reaches the root's children, and a memo is only a memo
 * if the second ask costs nothing. Recorded rather than exposed as a flag, so a test can tell "not
 * walked" from "walked once" from "walked every time".
 */
public final class CoreUiComponentFake {
    private final List<Object> children;

    private int childrenReadCount;

    public CoreUiComponentFake(Object... children) {
        this.children = List.of(children);
    }

    /**
     * @return how many times this component's children have been read
     */
    public int countChildrenReads() {
        return childrenReadCount;
    }

    public List<Object> getChildrenCopy() {
        childrenReadCount++;
        return children;
    }
}
