package kmlib.starsector.ui.widgets.lists;

import java.util.List;

/**
 * Whether an index names one of the options a control was laid out from - the one bounds rule the
 * picker and its two selectors read a reported index through before taking the option it stands for.
 *
 * <p>Each of the three is built from a list and driven back by an index resolved from geometry, so
 * each stands one step away from reading past the list it was laid from. Stated once, they cannot
 * drift into three ideas of which indices are real; restated at each, one of them eventually guards
 * the far end and not the near one.
 */
final class ListOptions {

    private ListOptions() {
    }

    /**
     * Whether {@code index} names one of {@code options}, asked before taking the option it stands
     * for. A control's laid-out parts and the list they were laid from line up only while that
     * layout holds, so an index outside the list is possible in principle - and reads as an option
     * like any other to anything keying by it.
     *
     * <p>What a caller does about an index naming none of them is its own. A click leaves the state
     * it would have changed alone; a hover reports that the pointer is on nothing.
     *
     * @param options the options the control was laid out from, in the order they were laid
     * @param index   the index a click or a hover reported
     * @return whether that index names one of the options
     */
    static boolean isOptionAt(List<?> options, int index) {
        return index >= 0 && index < options.size();
    }
}
