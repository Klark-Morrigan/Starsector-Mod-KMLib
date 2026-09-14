package kmlib.starsector.ui.render.gl.tooltip;

/**
 * How heavily a tooltip's leader rules draw: how thick each one is, and how far it is let down from the
 * opacity the box itself fades by. The two knobs that decide whether the rule reads as the greyed-out
 * text it runs between or as a mark competing with it - held together because they answer one question
 * between them, a rule being made lighter by either thinning it or fading it.
 *
 * <p>On the host's style rather than fixed in the paint because the balance cannot be settled from the
 * code: how heavy a solid run looks beside a line of glyphs depends on the face, the size it draws at,
 * and how the atlas was rasterised, none of which the rule can read. What the paint does keep is the
 * <em>colour</em>, which is not a matter of taste in the same way - a leader is the quietest mark on the
 * line by definition, so it takes the engine's grey whatever the box states for its own text.
 *
 * <p>The alpha is a multiplier rather than an opacity of its own, so a rule cannot outlive the box: a
 * tooltip fading out takes its rules with it, where an absolute alpha would leave them hanging at full
 * strength over a box that had already gone.
 *
 * @param thickness how thick each rule draws, in UI units; floored at nothing, which draws none at all
 * @param alphaMult how much of the box's own opacity each rule draws at, 0..1; floored at nothing, which
 *                  again draws none
 */
public record TooltipLeaderLineStyle(
    float thickness,
    float alphaMult) {

    // What a rule drawn at neither thickness nor alpha comes to, and the floor both are held to: a
    // negative of either would have a paint fill a quad running backwards or blend toward a colour it
    // was never given.
    private static final float NOTHING = 0f;

    // The weights that put a rule at the same apparent strength as the grey text it runs between, on the
    // faces a KM tooltip is ordinarily set in.
    //
    // A whole UI unit, because a run thinner than a pixel no longer covers a pixel row outright: how
    // solid it landed would depend on where the row fell against the pixel grid, and the box follows the
    // cursor, so the rule would strengthen and fade as the player tracked across the map. The weight is
    // spent on the alpha instead, which composites the same wherever the run lands.
    //
    // And let down to roughly two thirds, because a solid run covers every pixel it crosses outright
    // where a glyph stroke of the same shade spends much of its own footprint at partial alpha - so at
    // equal alpha the line comes out the heavier mark, and a leader reading more strongly than the words
    // it joins has become a divider parting them.
    private static final float HAIRLINE_THICKNESS = 1f;
    private static final float TEXT_WEIGHT_ALPHA_MULT = 0.65f;

    /**
     * The weights a rule reads as greyed-out text at - what a box takes unless its host has a reason to
     * say otherwise, and the starting point a tuned one is moved from.
     *
     * <p>Offered as a value rather than left for each host to spell, because the pair is a judgement
     * about how a solid run reads beside glyphs rather than a preference: a host restating it would be
     * restating a finding, and two hosts restating it would eventually disagree about it.
     */
    public static final TooltipLeaderLineStyle TEXT_WEIGHTED =
        new TooltipLeaderLineStyle(HAIRLINE_THICKNESS, TEXT_WEIGHT_ALPHA_MULT);

    /**
     * Floors both weights at nothing, since either read as a negative would have the paint fill a quad
     * running backwards or blend toward a colour it was never given. A zero is left standing on both:
     * it is how a host - or a player at a slider's near end - turns the rules off outright.
     */
    public TooltipLeaderLineStyle {
        thickness = Math.max(NOTHING, thickness);
        alphaMult = Math.max(NOTHING, alphaMult);
    }
}
