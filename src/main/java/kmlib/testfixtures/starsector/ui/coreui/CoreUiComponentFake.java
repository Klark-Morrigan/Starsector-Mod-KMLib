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
 */
public final class CoreUiComponentFake {
    private final List<Object> children;

    public CoreUiComponentFake(Object... children) {
        this.children = List.of(children);
    }

    public List<Object> getChildrenCopy() {
        return children;
    }
}
