package kmlib.profiling;

import java.util.ArrayList;
import java.util.List;

/**
 * A section whose calls run a loop, declared together with the steps one turn of
 * that loop is split into.
 *
 * <p>A per-item loop cannot afford a scope per item: opening one for every cell
 * in the sector times itself about as much as the work it is timing. So the loop
 * is one scope that counts its turns, and the steps inside a turn are slots a
 * turn adds into - a clock read and an add apiece. What the row then reports is
 * what one item cost in each step, which is the number a loop is judged by.
 *
 * <p>Its own type rather than a flag on {@link ProfileSection}, so what a caller
 * may do is what its type allows: a step can only be marked on a scope opened
 * over a loop, and such a scope can only be opened on a section that declared
 * the steps.
 *
 * <p>One instance per name, like the rest of profiling's registered values, and
 * one row: the section a caller opens the loop under is the same one a plain
 * open of that name lands on. The phases are the ones the name was first
 * registered with - a section declares them once, beside the constant holding
 * it.
 */
public final class PhasedSection {

    private static final NameRegistry<PhasedSection> PHASED_SECTIONS_BY_NAME = new NameRegistry<>();

    private final ProfileSection section;
    private final List<ProfilePhase> phases;

    private PhasedSection(String name, SectionTerms terms, String[] phaseNames) {
        this.section = ProfileSection.registerSection(name, terms);
        this.phases = createPhases(phaseNames);
    }

    /**
     * Resolves the phased section {@code name} identifies, declaring it with
     * {@code phaseNames} on {@link SectionTerms#DEFAULT} the first time the
     * name is seen.
     *
     * @param name       what the section is called in a report
     * @param phaseNames what one turn of its loop is split into, in the order a
     *                   turn pays them
     * @return the one phased section carrying that name
     */
    public static PhasedSection registerPhasedSection(String name, String... phaseNames) {
        return registerPhasedSection(name, SectionTerms.DEFAULT, phaseNames);
    }

    /**
     * Resolves the phased section {@code name} identifies, declaring it on
     * {@code terms} with {@code phaseNames} the first time the name is seen.
     *
     * <p>The terms are the loop's as one call: a line it states a threshold for
     * is written once per pass however many items the loop ran over, and what
     * one turn cost is what the row's own steps are read for.
     *
     * @param name       what the section is called in a report
     * @param terms      what the section states about itself beyond its name
     * @param phaseNames what one turn of its loop is split into, in the order a
     *                   turn pays them
     * @return the one phased section carrying that name
     */
    public static PhasedSection registerPhasedSection(
            String name,
            SectionTerms terms,
            String... phaseNames) {

        return PHASED_SECTIONS_BY_NAME.resolveByName(
            name, resolvedName -> new PhasedSection(resolvedName, terms, phaseNames));
    }

    /**
     * @return the row this section's calls are recorded on, which is the row a
     *         plain open of the same name lands on
     */
    public ProfileSection getSection() {
        return section;
    }

    /**
     * @return the steps one turn of the loop is split into, in declaration order
     *         - which is the order their slots are in
     */
    public List<ProfilePhase> getPhases() {
        return phases;
    }

    /**
     * Resolves one declared phase, for a caller holding it in a constant beside
     * the section itself.
     *
     * @param phaseName the step, as this section declared it
     * @return the phase to mark that step with
     * @throws IllegalArgumentException where the section declared no such step,
     *                                  which is a misspelling in a constant
     *                                  rather than a state to measure through
     */
    public ProfilePhase resolvePhase(String phaseName) {

        for (var phase : phases) {
            if (phase.getName().equals(phaseName)) {
                return phase;
            }
        }
        throw new IllegalArgumentException(
            "Section '" + section.getName() + "' declared no phase '" + phaseName + "'");
    }

    @Override
    public String toString() {
        return section.getName();
    }

    // Each phase is handed its slot here, once, so that marking one during a
    // turn is an index rather than a search for the name among the others.
    private List<ProfilePhase> createPhases(String[] phaseNames) {

        var declared = new ArrayList<ProfilePhase>(phaseNames.length);

        for (var index = 0; index < phaseNames.length; index++) {
            declared.add(new ProfilePhase(this, phaseNames[index], index));
        }
        return List.copyOf(declared);
    }
}
