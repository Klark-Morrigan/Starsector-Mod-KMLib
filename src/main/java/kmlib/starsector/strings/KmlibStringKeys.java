package kmlib.starsector.strings;

/**
 * The string IDs KMLib registers in {@code data/strings/strings.json}, paired
 * with the category they live under. Call sites fetch through
 * {@link StarsectorStrings#get(String, String)} (or
 * {@link StarsectorStrings#format(String, String, Object...)}) using these
 * constants, so the keys live in exactly one place and never reappear as loose
 * literals that can drift from the JSON.
 *
 * <p>{@link StarsectorStrings} is intentionally mod-agnostic and takes
 * {@code (category, key)} on every call; this holder is the KMLib-specific
 * other half it documents - the typed key names bound to KMLib's category.
 */
public final class KmlibStringKeys {

    /** strings.json category namespacing every KMLib-owned string. */
    public static final String CATEGORY = "kmlib";

    /** Display word for a jump point, e.g. "&lt;focus&gt; Jump-point &lt;radius&gt;". */
    public static final String JUMP_POINT_LABEL = "jump_point_label";

    private KmlibStringKeys() {
    }
}
