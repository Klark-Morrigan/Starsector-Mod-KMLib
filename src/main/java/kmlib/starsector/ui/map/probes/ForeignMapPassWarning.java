package kmlib.starsector.ui.map.probes;

import kmlib.logging.SessionWarning;

import org.apache.log4j.Logger;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * Says once a session that a surface drawn through the sector map's render hook was drawn by something
 * that is not a map.
 *
 * <p>{@code renderOnMap} is the sector map's hook by contract, and a map overlay rests on that: a cursor
 * read inverts whichever transform the running pass has bound, and divides by the {@code factor} that
 * pass supplied. Both are only meaningful if the pass really is a map's. Nothing enforces it, though -
 * the hook is a plain method on a terrain plugin, and any mod that walks the sector's terrain can call it
 * with a transform and a zoom of its own.
 *
 * <p>What that costs is not a crash but a quiet wrongness: a cursor pixel inverted against a foreign
 * transform still yields a world point, and in hyperspace - where campaign coordinates are the sector
 * map's own coordinates - that point lands somewhere real. An overlay then answers a pointer that is
 * nowhere near, with no map open at all. Nothing about that reads as a transform problem to whoever
 * hears it, which is why it is worth a line naming the pass rather than leaving each report to be
 * diagnosed from the symptom.
 *
 * <p>What the line says about the overlay is the caller's, since only the caller knows what was drawn
 * and what a foreign transform costs it. What this adds is what the caller cannot know: whatever the
 * live tree holds around any embedded map, and the stack of the pass. The stack is the whole value of
 * the line, since the caller is exactly what cannot be worked out from inside the pass. It rides in on
 * a throwable that was never thrown and never will be - purely a carrier for the frames, not a failure
 * being reported.
 *
 * <p>Once per session, at WARN. Once because this sits in a per-frame path and a repeat says nothing the
 * first did not; at WARN because a line the default log level swallows is not a warning. The presence
 * read is asked only while the warning is still owed, that read being a walk of the live widget tree.
 *
 * <p>Never throws. It is a report about the frame and not part of drawing one, so a read that cannot be
 * made costs the report and nothing else - the overlay keeps painting. A failed read spends the warning
 * too, so it is said once rather than retried every frame for the session.
 */
public final class ForeignMapPassWarning {

    // What the line says where the live tree could not be walked, so a null read does not pass for a
    // walk that found nobody.
    private static final String UNWALKED_TREE = "the tree could not be walked";

    // What parts the caller's lead from the host trace that follows it.
    private static final String LEAD_TO_HOSTS_SEPARATOR = "; ";

    // What the carried stack is labelled as in the log.
    private static final String PASS_STACK_LABEL = "Stack of the pass that drew with no map on screen";

    // What the live tree holds around any embedded map, or null when it could not be read. Supplied
    // rather than called for here, for the reason the presence read is: both walk a widget tree
    // that only exists in a running game, so a class that reached for either directly would work
    // only inside one.
    private final Supplier<String> describeEmbeddedMapHosts;

    // Whether a map is on screen at all, as a supplied read rather than one composed here: the
    // hosts a map can be showing on are not this class's knowledge, and a supplied read is the
    // caller's to answer without this class holding a widget tree to walk.
    private final BooleanSupplier isAnyMapShowing;

    // The caller's own words for the pass, before the host trace is appended.
    private final String noMapWarningLead;

    // The caller's own words for a presence read that failed, which also says the warning is spent.
    private final String unreadablePresenceWarning;

    private final SessionWarning warning;

    /**
     * @param isAnyMapShowing           whether any host is showing a map
     * @param describeEmbeddedMapHosts  what the live tree holds around any map embedded outside those
     *                                  hosts, as {@link EmbeddedMapHostTrace#describeEmbeddedMapHosts}
     *                                  answers it - asked only when a line is owed, being a full tree walk
     * @param logger                    the calling surface's own logger, so the line is attributed to the
     *                                  surface that was drawn rather than to this reporter
     * @param noMapWarningLead          what the line says about the surface drawn with no map on screen;
     *                                  the host trace, or that the tree could not be walked, follows it
     * @param unreadablePresenceWarning what the line says when whether a map is showing could not be read
     */
    public ForeignMapPassWarning(
            BooleanSupplier isAnyMapShowing,
            Supplier<String> describeEmbeddedMapHosts,
            Logger logger,
            String noMapWarningLead,
            String unreadablePresenceWarning) {

        this.isAnyMapShowing = isAnyMapShowing;
        this.describeEmbeddedMapHosts = describeEmbeddedMapHosts;
        this.noMapWarningLead = noMapWarningLead;
        this.unreadablePresenceWarning = unreadablePresenceWarning;
        this.warning = new SessionWarning(logger);
    }

    /**
     * Reports this pass, if no map is on screen and nothing has been reported yet this session.
     *
     * <p>Call at the top of a render pass, before anything is resolved or drawn, so the stack is the one
     * that actually reached the surface.
     */
    public void warnOnceIfNoMapIsShowing() {

        // Asked before the read, not after, because the read is the expensive half: with the line
        // already said there is nothing a widget walk could tell us that would be printed.
        if (warning.hasWarnedThisSession()) {
            return;
        }

        try {
            if (isAnyMapShowing.getAsBoolean()) {
                return;
            }

            // The tree read, not the stack, is what can name an owner: a mod builds its widget once
            // and the engine renders it forever after, so the frames below are all the engine's.
            // Null when the tree could not be walked, which the line says rather than hides.
            var embeddedMapHosts = describeEmbeddedMapHosts.get();

            warning.warnOnce(
                noMapWarningLead
                    + LEAD_TO_HOSTS_SEPARATOR
                    + (embeddedMapHosts == null ? UNWALKED_TREE : embeddedMapHosts),
                new Throwable(PASS_STACK_LABEL));

        } catch (Throwable exception) {
            // The read walks the live widget tree and reaches core classes, neither of which every
            // game build is obliged to present. Letting that out would take the overlay away over a
            // line of diagnostics, which inverts what the two are worth.
            warning.warnOnce(unreadablePresenceWarning, exception);
        }
    }
}
