package kmlib.starsector.ui.widgets;

/**
 * The text a {@link VanillaTabStrip} tab shows: its label and an optional shortcut key name that
 * paints in the accent gold. The shortcut is the key's display name only (e.g. {@code "P"}); the
 * strip wraps it in brackets when it draws it, so callers store the bare name.
 *
 * @param label    the tab's main label
 * @param shortcut the shortcut key's display name, or null/blank when the tab has none
 */
public record VanillaTabContent(String label, String shortcut) {
}
