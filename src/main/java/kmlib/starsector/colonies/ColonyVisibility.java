package kmlib.starsector.colonies;

import java.util.Set;

/**
 * The rule a colony set is shown under: the reveals that lift a half of the fog outright, and the
 * gates that hold back the shapes of colony the fog alone would leak.
 *
 * <p>The fog itself - the player has found the market's entity, or has surveyed enough of a dead
 * world to see it is dead - is not a knob and is not carried here. What is carried is everything
 * that <em>changes</em> that answer: the reveals that admit what has not been found, and, for the
 * shapes a bare fog shows before the player could plausibly have heard of them, a requirement that
 * somebody have seen them where they stand.
 *
 * <p>Passed as one value rather than as loose flags because all of it is read together wherever the
 * rule is applied. A surface handed one gate and not the other would show a derelict it had been
 * told to hold back, and a signature taking four booleans in a row would say nothing about it
 * either way.
 *
 * <p>Each side is a set of names rather than a flag apiece, so which reveal or gate is which cannot
 * be got wrong at a call site and one added later needs no existing rule rewritten.
 * {@link VisibilityReveal} and {@link RevelationGate} each say what theirs covers.
 *
 * <p>Reveals and gates never reach each other. A reveal drops the fog arm it names and no more, so
 * a colony a gate is holding back stays back however wide the fog is opened; a gate narrows and
 * never widens, so leaving one out drops that shape to the fog alone rather than admitting anything
 * the fog refuses. Which is what lets a player turn on exactly the thing they meant and nothing
 * beside it.
 *
 * @param reveals         the halves of the fog this rule drops; an absent set is the fog entire
 * @param revelationGates the shapes that must have been revealed as well as found; a shape whose
 *                        gate is absent is held to the fog alone
 */
public record ColonyVisibility(
    Set<VisibilityReveal> reveals,
    Set<RevelationGate> revelationGates) {

    /**
     * The fog alone: nothing admitted that has not been found, and nothing held back beyond it.
     * What a caller stating no rule of its own is read as, since a gate nobody asked for must
     * not appear out of an unstated argument.
     */
    public static final ColonyVisibility BASE_FOG = new ColonyVisibility(Set.of(), Set.of());

    /**
     * Takes immutable copies of both sets, and reads an absent one as empty, so a rule handed
     * around a render pass cannot change under its readers, an unstated set cannot hold anything
     * back, and an unstated set cannot reveal anything either.
     */
    public ColonyVisibility {
        reveals = reveals == null ? Set.of() : Set.copyOf(reveals);
        revelationGates = revelationGates == null ? Set.of() : Set.copyOf(revelationGates);
    }

    /**
     * Whether a colony on an entity the player has not found still counts.
     *
     * @return true when the discovery arm of the fog is dropped
     */
    public boolean shouldIncludeUndiscoveredMarkets() {
        return reveals.contains(VisibilityReveal.UNDISCOVERED_MARKETS);
    }

    /**
     * Whether a dead world the player has not surveyed closely enough still counts.
     *
     * @return true when the survey arm of the fog is dropped
     */
    public boolean shouldIncludeUnsurveyedDeadWorlds() {
        return reveals.contains(VisibilityReveal.UNSURVEYED_DEAD_WORLDS);
    }
}
