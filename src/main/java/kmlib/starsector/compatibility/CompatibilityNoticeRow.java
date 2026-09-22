package kmlib.starsector.compatibility;

import kmlib.text.KmlibStrings;

/**
 * One labelled row of the notice a player reads, as the template that words it and the one value
 * that fills it.
 *
 * <p>Held apart rather than rendered where it is composed, because the notice has two surfaces and
 * they want the row differently. A dialog takes a single string and wants the row already filled; a
 * panel of the game's own widgets takes the template and the value separately, which is what lets
 * it tint the value and leave the label alone. Composed once either way, so a row added to the
 * notice arrives on both surfaces rather than on whichever was edited.
 *
 * @param rowTemplate the row's wording, out of KMLib's strings, carrying exactly one slot
 * @param rowValue    what fills that slot, already worded by whoever owns it - a version, a mod's
 *                    name, or a consuming mod's own sentence
 */
public record CompatibilityNoticeRow(
    String rowTemplate,
    String rowValue) {

    // The slot a row's template carries. Both components are strings, so nothing but their shape
    // tells them apart, and a template is the one with a hole in it.
    private static final String VALUE_SLOT = "%s";

    public CompatibilityNoticeRow {

        KmlibStrings.requireText(
            rowTemplate,
            "A row with no template would put a value in front of a player under no label.");

        KmlibStrings.requireText(
            rowValue,
            "A row with no value would show a player a label and leave them looking for the answer.");

        // The transposition the two slots cannot catch between themselves. A value where the
        // template belongs renders as itself with the template lost, which reads as a row that was
        // merely worded oddly rather than as one built wrong.
        if (!rowTemplate.contains(VALUE_SLOT)) {

            throw new IllegalArgumentException(
                "A row's template must carry a slot for its value: " + rowTemplate);
        }
    }

    /**
     * @return the row as one line, the template with its slot filled - what a surface that takes
     *         whole strings rather than widgets shows
     */
    public String fillRow() {

        return String.format(rowTemplate, rowValue);
    }
}
