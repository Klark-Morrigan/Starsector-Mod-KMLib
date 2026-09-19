package kmlib.testfixtures.starsector.ui.widgets.lists;

import kmlib.starsector.ui.controls.specs.ControlSpec;
import kmlib.starsector.ui.controls.specs.HorizontalRadioSpec;
import kmlib.starsector.ui.controls.specs.ScrollingSectionSpec;
import kmlib.starsector.ui.controls.specs.SideBySideSpec;
import kmlib.starsector.ui.controls.specs.VerticalTableSpec;

import java.util.List;

/**
 * Reaches into a built picker block for the control a case is about. The block is a fixed run - a rule,
 * the columns selector, a row pairing the sort selector beside the caller's trailing controls, then the
 * scrolling section holding the item list - and every suite that exercises a picker has to know both
 * where each control sits and what type it arrives as.
 *
 * <p>Held once rather than per suite because it is the block's composition rather than any one case's
 * setup. A control inserted into that run moves every index after it, and a suite holding its own copy
 * of them fails as a cast against a control it never named - so the position is stated in one place and
 * a rebuilt block breaks one file.
 *
 * <p>Positions are not published. A case pinning where a control sits asserts against its own literal,
 * which is what makes it a pin rather than a restatement of what this reads.
 */
public final class ListPickerBlockReads {

    // Where each control sits in the block, top to bottom. The item list is not among them: it is
    // always the last control, so it is found from the end and survives anything inserted above it.
    private static final int COLUMNS_SELECTOR_INDEX = 1;
    private static final int SORT_ROW_INDEX = 2;

    private ListPickerBlockReads() {
    }

    /**
     * @param blockControls the picker block, top to bottom
     * @return the columns selector, the block's second control
     */
    public static HorizontalRadioSpec readColumnsSelector(List<ControlSpec> blockControls) {
        return (HorizontalRadioSpec) blockControls.get(COLUMNS_SELECTOR_INDEX);
    }

    /**
     * The item list: the block's last control is the scrolling section, and the list is the one thing
     * inside it. Read through the section rather than by index, since what scrolls is a group and the
     * list's position inside the block moves with anything added above it.
     *
     * @param blockControls the picker block, top to bottom
     * @return the ranked item list
     */
    public static VerticalTableSpec readItemList(List<ControlSpec> blockControls) {
        var section = (ScrollingSectionSpec) blockControls.get(blockControls.size() - 1);
        return (VerticalTableSpec) section.controls().get(0);
    }

    /**
     * The paired sort row, read whole so a case can reach its left column (the sort selector) and its
     * right column (the caller's trailing controls) separately.
     *
     * @param blockControls the picker block, top to bottom
     * @return the row the sort selector shares with the caller's trailing controls
     */
    public static SideBySideSpec readSortRow(List<ControlSpec> blockControls) {
        return (SideBySideSpec) blockControls.get(SORT_ROW_INDEX);
    }

    /**
     * @param blockControls the picker block, top to bottom
     * @return the sort selector, which fills the left half of the sort row
     */
    public static VerticalTableSpec readSortSelector(List<ControlSpec> blockControls) {
        return (VerticalTableSpec) readSortRow(blockControls)
            .leftColumn()
            .get(0);
    }
}
