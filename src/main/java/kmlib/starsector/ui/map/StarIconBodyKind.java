package kmlib.starsector.ui.map;

/**
 * Which kind of body a sector-map icon stands for, for the purpose of sizing it - the three cases
 * the map sizes differently. Naming the kind rather than passing separate {@code isStar} /
 * {@code isNebulaCentre} flags keeps a caller from expressing the contradiction of both at once,
 * and gives {@link StarIconHitTest#computeIconWorldRadius} one thing to branch on.
 */
public enum StarIconBodyKind {

    /** A true star, drawn a shade smaller than another body of the same radius. */
    STAR,

    /** A nebula centre, sized off a fixed base rather than its diffuse radius. */
    NEBULA_CENTRE,

    /** A black hole - sized like a star, but named apart since callers dial its icon separately. */
    BLACK_HOLE,

    /** Any other body, sized straight from its radius. */
    OTHER
}
