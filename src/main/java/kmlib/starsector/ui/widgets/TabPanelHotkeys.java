package kmlib.starsector.ui.widgets;

import java.util.List;

/**
 * The pure key-to-tab mapping a {@link TabPanel} consumer opts into to drive its tabs from the
 * keyboard: given the keycode pressed and each tab's bound keycode, it reports which tab the press
 * selects. It owns only the mapping - the consumer owns where the per-tab keycodes come from and
 * what selecting a tab means - so the binder stays free of any settings or registry coupling and a
 * panel built with no keycodes wires no input at all.
 *
 * <p>An unbound tab stores a non-positive keycode (0 is LWJGL's {@code KEY_NONE}, left by clearing a
 * binding); such a tab never claims a press, so a stray zero-valued key event resolves to no tab
 * rather than to the first unbound one.
 */
public final class TabPanelHotkeys {
    private TabPanelHotkeys() {
    }

    /**
     * The index of the tab whose bound keycode matches {@code pressedKeycode}, or
     * {@link TabStrip#NO_TAB} when no bound tab matches. Tabs with a non-positive keycode are
     * unbound and skipped, so an unbound tab never captures a press.
     *
     * @param pressedKeycode  the keycode of the key just pressed
     * @param perTabKeycodes  each tab's bound keycode, in tab order; non-positive means unbound
     * @return the selected tab's index, or {@link TabStrip#NO_TAB}
     */
    public static int findTabForKey(int pressedKeycode, List<Integer> perTabKeycodes) {
        for (var index = 0; index < perTabKeycodes.size(); index++) {
            var keycode = perTabKeycodes.get(index);
            // A non-positive keycode is an unbound tab; skip it so it never matches, even a 0 press.
            if (keycode <= 0) {
                continue;
            }
            if (keycode == pressedKeycode) {
                return index;
            }
        }
        return TabStrip.NO_TAB;
    }
}
