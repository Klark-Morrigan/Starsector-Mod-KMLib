package kmlib.starsector.colonies;

import java.util.Set;

/**
 * The rule a colony set is shown under: the reveal that lifts the fog outright, and the gates
 * that hold back the shapes of colony the fog alone would leak.
 *
 * <p>The fog itself - the player has found the market's entity - is not a knob and is not
 * carried here. What is carried is everything that <em>changes</em> that answer: a reveal that
 * admits what has not been found, and, for the shapes a bare fog shows before the player could
 * plausibly have heard of them, a requirement that somebody have seen them where they stand.
 *
 * <p>Passed as one value rather than as loose flags because both are read together wherever the
 * rule is applied. A surface handed one gate and not the other would show a derelict it had been
 * told to hold back, and a signature taking three booleans in a row would say nothing about it
 * either way.
 *
 * <p>The gates are a set of names rather than a flag apiece, so which gate is which cannot be
 * got wrong at a call site and a gate added later needs no existing rule rewritten.
 * {@link RevelationGate} says what each covers.
 *
 * <p>A gate narrows and never widens. Leaving one out drops that shape back to the fog alone
 * rather than admitting anything the fog refuses, so no rule stated here can put a colony on the
 * map that the player has not found.
 *
 * @param shouldIncludeUndiscoveredMarkets whether an undiscovered colony still counts - the
 *                                         "show all factions" reveal, which admits everything
 *                                         and overrides every gate below
 * @param revelationGates                  the shapes that must have been revealed as well as
 *                                         found; a shape whose gate is absent is held to the fog
 *                                         alone
 */
public record ColonyVisibility(
    boolean shouldIncludeUndiscoveredMarkets,
    Set<RevelationGate> revelationGates) {

    /**
     * The fog alone: nothing admitted that has not been found, and nothing held back beyond it.
     * What a caller stating no rule of its own is read as, since a gate nobody asked for must
     * not appear out of an unstated argument.
     */
    public static final ColonyVisibility BASE_FOG = new ColonyVisibility(false, Set.of());

    /**
     * Takes an immutable copy of the gates, and reads an absent set as no gates at all, so a
     * rule handed around a render pass cannot change under its readers and an unstated set
     * cannot hold anything back.
     */
    public ColonyVisibility {
        revelationGates = revelationGates == null ? Set.of() : Set.copyOf(revelationGates);
    }
}
