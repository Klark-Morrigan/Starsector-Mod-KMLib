package kmlib.starsector.ui.widgets.tabs;

import kmlib.starsector.ui.controls.specs.ControlSpec;
import kmlib.starsector.ui.controls.specs.TabsSpec;
import kmlib.starsector.ui.text.ImageSpan;
import kmlib.starsector.ui.widgets.tabs.style.TabStyle;

/**
 * The button a tab panel flies after its last tab, as a caller asks for one: what it says and does, and
 * the look it wears. It is the panel's own chrome rather than any tab's, so pressing it fires its own
 * action and never moves the selection.
 *
 * <p>The two travel as one value because a spec and the style it is measured and painted at are only
 * meaningful together - a button sized to one face and lettered in another reads as a label that has
 * outgrown its box - and because both are absent together: a panel flying no button has neither.
 *
 * <p><strong>Its own style, though usually the panel's own with one thing changed.</strong> The style
 * travels with the spec because what the button is sized to is not what the tabs are: a row of tabs may
 * stand in a fixed box wide enough for the longest name it will ever carry, while the button is as wide
 * as the one thing it shows. Only the band height is not its own: the button stands in the panel's band,
 * so the layout lays it at the band the panel was given and whatever height this style names is not read.
 *
 * <p>A one-cell {@link TabsSpec} rather than a control kind of its own, so it is measured, split,
 * drawn and hit through the very geometry a tab is - the button differs from its neighbours in what it
 * shows and what it does, not in how it is laid. It must state {@link ControlSpec#NO_SELECTION}: a tabs
 * control's lit cell is inert, so a button that was ever the lit one would stop answering presses.
 *
 * <p>An {@code icon} is drawn into the button's box in place of a word. It is the panel's own control
 * rather than one of its tabs, so a mark serves where a label would only repeat what pressing it does;
 * a button showing one carries an empty label and states its width through the style's box, since an
 * image is sized by the room it is given rather than measured like text. Filling the box, it is also
 * what answers the pointer: the mark is lit at the button's fade by the light its own fill would have
 * taken (see {@link BandButtonPlacement#resolveIconTint}), the chrome beneath being covered by it.
 *
 * @param spec  the one-cell tabs control the button is expressed as - its word, its action, and no
 *              selection
 * @param style the chrome, box, palette and face the button is measured and painted at; its band height
 *              is unread, the panel's own band being the one it stands in
 * @param icon  the image drawn into the button's box, sized to it, or null for a button that shows its
 *              label instead
 */
public record BandButtonSpec(
    TabsSpec spec,
    TabStyle style,
    ImageSpan icon) {
}
