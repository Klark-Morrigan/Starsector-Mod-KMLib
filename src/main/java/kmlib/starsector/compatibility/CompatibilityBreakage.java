package kmlib.starsector.compatibility;

import kmlib.text.KmlibStrings;

/**
 * How a binding to third-party code broke, as the two readings a diagnosis is made from: which guard
 * caught it, and what no longer holds.
 *
 * <p>The two travel as one value because neither diagnoses anything alone. A member that moved and
 * an entry point a release declares and then refuses both arrive as a broken binding naming the same
 * member, and only where each was caught tells them apart; the site alone says a guard fired and not
 * what it fired on. A caller that had one and not the other would file half a report.
 *
 * <p>Held apart from {@link CompatibilityFailure} rather than as two more slots on it, so that the
 * failure's own components are four distinct types. Three strings in a row on one record are three a
 * caller can transpose with nothing to catch it - a report naming the member where the site belongs
 * reads as plausibly as the right one, and is wrong in the direction nobody checks.
 *
 * <p>Both are for the log alone. Neither reaches the player, who is told what stopped working rather
 * than which of somebody else's members it stopped working through.
 *
 * @param failureSite  where the binding stopped holding, as a phrase completing "failed while" -
 *                     "resolving the binding", "calling it from the game thread". Whoever guards a
 *                     binding spells its own, because only the guard that caught it knows which
 *                     one it was
 * @param brokenDetail the member or detail that stopped holding, as a short phrase rather than a
 *                     sentence, since it is a row of a block. Where a probe found several, all of
 *                     them: the one the JVM gave up on is rarely the whole of what moved
 */
public record CompatibilityBreakage(
    String failureSite,
    String brokenDetail) {

    public CompatibilityBreakage {

        KmlibStrings.requireText(
            failureSite,
            "A breakage with no site says a binding broke and not which guard caught it.");
        KmlibStrings.requireText(
            brokenDetail,
            "A breakage with no detail leaves the log with nothing to diagnose from.");
    }
}
