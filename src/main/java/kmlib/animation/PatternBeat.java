package kmlib.animation;

/**
 * Where a phase stands within one element of a {@link PhasePattern}: whether that element is one the pattern
 * sounds through or one it stays silent through, and how far into it the phase currently is.
 *
 * <p>The two travel together because reading either alone gets a consumer the wrong picture. A progress
 * without the state cannot tell the opening instant of a sound from a silence, and a state without the
 * progress cannot shape the sound it just reported - a consumer would have to ask twice and hope the two
 * readings landed in the same element.
 *
 * <p>Silences are reported as elements in their own right rather than as an absence, so a consumer that
 * wants to do something with the gaps - shape them, count them, hold something across one - has the same
 * reading for both.
 *
 * @param isSounding whether the pattern is sounding through the element the phase stands in
 * @param beatPhase  how far through that element the phase is, 0 at its start and approaching 1 at its end;
 *                   a phase to hand to an envelope so the element takes a shape rather than a flat level
 */
public record PatternBeat(
    boolean isSounding,
    float beatPhase) {
}
