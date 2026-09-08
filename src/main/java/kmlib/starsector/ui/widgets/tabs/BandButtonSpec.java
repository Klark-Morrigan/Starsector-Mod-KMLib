package kmlib.starsector.ui.widgets.tabs;

import kmlib.starsector.ui.controls.ControlSpec;
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
 * <p><strong>Its own style, not the panel's.</strong> A button is not a tab, so a row of tabs is the wrong
 * thing for it to look like: it takes its own {@link kmlib.starsector.ui.widgets.tabs.style.TabChrome} -
 * a raised button, whatever the tabs beside it wear - and its own {@link
 * kmlib.starsector.ui.widgets.tabs.style.TabBox}, which is what lets it snap to its own word while the
 * tabs beside it stand at a fixed width. Only the band height is not its own: the button stands in the
 * panel's band, so the layout lays it at the band the panel was given and whatever height this style
 * names is not read.
 *
 * <p>A one-cell {@link ControlSpec.Tabs} rather than a control kind of its own, so it is measured, split,
 * drawn and hit through the very geometry a tab is - the button differs from its neighbours in what it
 * wears and what it does, not in how it is laid. It must state {@link ControlSpec#NO_SELECTION}: a tabs
 * control's lit cell is inert, so a button that was ever the lit one would stop answering presses.
 *
 * @param spec  the one-cell tabs control the button is expressed as - its word, its action, and no
 *              selection
 * @param style the chrome, box, palette and face the button is measured and painted at; its band height
 *              is unread, the panel's own band being the one it stands in
 */
public record BandButtonSpec(
    ControlSpec.Tabs spec,
    TabStyle style) {
}
