package kmlib.math.angles;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Turning angles into a form that can be compared, and the constants that define one.
 *
 * <p>An angle carries no record of which turn it came from, so two of them computed
 * separately are not comparable until something says which turn to read them in. That is
 * what is here: put an angle in a known range, measure how far apart two are, and place one
 * in the same turn as another so a comparison means something. Without it every caller
 * re-inlines its own {@code while (a > PI) a -= 2 * PI}, and two that disagree about the
 * open end of the range disagree about whether a point is inside an interval.
 *
 * <p>Spans of angle too, as {@code {start, width}} pairs. The width rather than an end,
 * which is the opposite of {@link kmlib.math.geometry.Spans} and deliberately so: along a
 * line an end is unambiguous and a width is redundant, while on a circle an end is
 * ambiguous - 0.1 is both before and after 6.2 - and only a width says which way round the
 * span was meant. The two are not the same operation and must not be made to look like it.
 *
 * <p>Nothing shape-like belongs here. A span is not an arc until something pairs it with a
 * circle, and that pairing belongs to whatever owns the circle.
 */
public final class Angles {

    /** A whole turn, in radians. */
    public static final double FULL_TURN = 2 * Math.PI;

    /** Half a turn, in radians - the furthest apart two directions can be. */
    public static final double HALF_TURN = Math.PI;

    /** A quarter turn, in radians - the furthest apart two undirected lines can be. */
    public static final double QUARTER_TURN = Math.PI / 2;

    private Angles() {
    }

    /**
     * The same direction expressed in the first turn.
     *
     * @param angle any angle
     * @return the same direction, from zero up to but not including a full turn
     */
    public static double normalise(double angle) {

        var turned = angle % FULL_TURN;
        return turned < 0 ? turned + FULL_TURN : turned;
    }

    /**
     * How far apart two directions are, whichever way round is shorter.
     *
     * @param from one direction
     * @param to   the other
     * @return the angle between them, never more than a half turn
     */
    public static double measureGap(double from, double to) {

        var turned = normalise(to - from);
        return turned > HALF_TURN ? FULL_TURN - turned : turned;
    }

    /**
     * The same direction, moved into the turn that begins at {@code origin}.
     *
     * <p>What makes an angle comparable to an interval. An interval is carried as a pair of
     * angles that may run past a full turn, so an angle taken fresh from {@code atan2} is in
     * the wrong turn as often as not, and comparing the two directly puts a direction outside
     * an interval that in fact contains it.
     *
     * @param angle  the direction to move
     * @param origin where the turn begins
     * @return the same direction, at or after {@code origin} and less than a turn past it
     */
    public static double placeAfter(double angle, double origin) {
        return origin + normalise(angle - origin);
    }

    /**
     * A turn as the shorter way round, signed: positive anticlockwise, negative clockwise.
     *
     * <p>What a sweep between two directions wants. The difference of two angles taken raw
     * can be most of two turns, and taking it at face value sweeps the long way round the
     * circle - which for anything drawn along that sweep is the arc on the wrong side.
     *
     * @param angle the turn to shorten
     * @return the same turn, more than a half turn back and at most a half turn on
     */
    public static double measureSignedTurn(double angle) {

        var turned = normalise(angle);
        return turned > HALF_TURN ? turned - FULL_TURN : turned;
    }
}
