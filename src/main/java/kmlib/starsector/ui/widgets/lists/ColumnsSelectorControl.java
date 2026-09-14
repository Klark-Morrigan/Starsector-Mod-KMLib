package kmlib.starsector.ui.widgets.lists;

import kmlib.starsector.ui.controls.ControlSpec;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * A picker's columns selector: a two-segment horizontal radio ("1" / "2") with a trailing caption,
 * the lit segment the count the picker list is currently laid across. Picking a segment reports that
 * count back to the caller, which persists it however it keeps such state; the caller's next
 * per-frame body build then re-wraps its list under it, so the selector and the list always agree on
 * how many columns the rows spread across.
 *
 * <p>The caption arrives as drawn text rather than as a string ID, for the same reason
 * {@link ListSortMode} hands its labels over drawn: a string ID only means something against the mod
 * category that registered it. The segment labels are the counts themselves and need no such
 * hand-over ({@link ListColumns#resolveLabelText()}).
 *
 * <p>The segments are drawn in {@link ListColumns}'s own order, so the segment index a click
 * reports maps straight back to a choice by position. It is an ordinary option radio - re-picking
 * the lit segment is inert, and the count only changes by picking the other segment - rather than
 * a deselectable list or the re-firing {@link SortSelectorControl}.
 */
public final class ColumnsSelectorControl {

    // The segments in the order they are laid out left to right, so a click's index maps back to a
    // choice by position. Held once rather than read from values() per build: a picker's body is
    // rebuilt twice a frame (paint and hit-test) and values() hands back a fresh array each call.
    private static final List<ListColumns> CHOICES = List.of(ListColumns.values());

    private ColumnsSelectorControl() {
    }

    /**
     * Builds the columns selector for the active layout: a two-segment radio lit on that choice's
     * segment, each segment labelled with its count and the control trailed by the caller's caption.
     *
     * @param activeColumns   the column count the list is currently laid across, which this lights
     * @param captionText     the caption drawn after the segments, resolved by the caller
     * @param onColumnsPicked told the count a click lands on, for the caller to persist
     * @return the horizontal, two-segment columns-selector radio
     */
    public static ControlSpec.HorizontalRadio buildSelector(
            ListColumns activeColumns,
            String captionText,
            Consumer<ListColumns> onColumnsPicked) {

        var labels = new ArrayList<String>(CHOICES.size());
        for (var choice : CHOICES) {
            labels.add(choice.resolveLabelText());
        }
        return ControlSpec.HorizontalRadio
            .of(
                labels,
                CHOICES.indexOf(activeColumns),
                cellIndex -> applySelection(onColumnsPicked, cellIndex))
            .showsCaption(captionText);
    }

    // Reports the column count for the clicked segment. Any index outside the two segments is
    // ignored, so a stray hit changes nothing. Re-picking the lit segment never reaches here (the
    // horizontal radio swallows a re-pick as inert), so this only ever runs for a real change.
    private static void applySelection(Consumer<ListColumns> onColumnsPicked, int cellIndex) {
        if (!ListOptions.isOptionAt(CHOICES, cellIndex)) {
            return;
        }
        onColumnsPicked.accept(CHOICES.get(cellIndex));
    }
}
