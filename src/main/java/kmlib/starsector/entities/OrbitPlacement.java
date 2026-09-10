package kmlib.starsector.entities;

/**
 * Where a body is put around its focus: how far out it sits, how fast it goes round, and where on
 * the circle it starts.
 *
 * <p>One value rather than three parameters because the three travel together through every
 * signature that places a body, and they are all {@code float}. Passed separately they can be
 * reordered at a call site without the compiler noticing - a speed handed to the angle slot pins
 * the body silently instead of orbiting it - and each new placing method repeats the triple. Held
 * together, one argument crosses each of those signatures and a mis-ordering has one place left to
 * happen rather than four.
 *
 * <p>A body sits still when its speed is non-positive, which {@link #createPinned} says outright
 * so a caller wanting that does not have to pass a literal nought and hope the reader knows what
 * it means. {@link #createOrbiting} still accepts one - a rate computed from a radius or widened
 * by a jitter can come out at nought, and refusing it would make every such caller test for a
 * case this already handles.
 *
 * @param orbitDistance     the orbit radius from the focus, in game units
 * @param speedDegPerDay    the orbital angular speed; non-positive means the body is pinned
 * @param startAngleDegrees the angle from the focus at which the body starts
 */
public record OrbitPlacement(
    float orbitDistance,
    float speedDegPerDay,
    float startAngleDegrees) {

    // The speed at or below which a body does not travel its circle at all. Nought rather than a
    // negative floor: a rate of nought is the natural "does not move", and anything below it is
    // the same standstill arrived at by arithmetic.
    private static final float PINNED_SPEED = 0f;

    /**
     * A body travelling its circle at {@code speedDegPerDay}.
     *
     * @param orbitDistance     the orbit radius from the focus
     * @param speedDegPerDay    the orbital angular speed; non-positive reads as pinned
     * @param startAngleDegrees the angle from the focus at which the body starts
     * @return the placement
     */
    public static OrbitPlacement createOrbiting(
            float orbitDistance,
            float speedDegPerDay,
            float startAngleDegrees) {

        return new OrbitPlacement(orbitDistance, speedDegPerDay, startAngleDegrees);
    }

    /**
     * A body held still at a point on the circle - the same standstill a non-positive speed
     * produces, said in the signature rather than in a nought a reader has to recognise.
     *
     * @param orbitDistance     the distance from the focus the body is held at
     * @param startAngleDegrees the angle from the focus the body is held at
     * @return the placement
     */
    public static OrbitPlacement createPinned(float orbitDistance, float startAngleDegrees) {
        return new OrbitPlacement(orbitDistance, PINNED_SPEED, startAngleDegrees);
    }

    /**
     * Whether this places the body still rather than on a circuit.
     *
     * @return true when the speed is non-positive
     */
    public boolean isPinned() {
        return speedDegPerDay <= PINNED_SPEED;
    }
}
