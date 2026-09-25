package kmlib.starsector.time;

import com.fs.starfarer.api.campaign.CampaignClockAPI;

/**
 * A countdown in campaign days: started at one clock timestamp, run for a fixed duration, and read against the
 * campaign clock whenever a caller asks how much of it is left.
 *
 * <p>Carries a completion slack beside the start and the duration. The campaign advances in uneven frame steps, so a
 * countdown compared exactly against its duration can sit a fraction of a day short for a frame or two after the
 * player expects it done; the slack is how early "complete" is allowed to answer. It belongs to the countdown rather
 * than to each read, so every read of one countdown agrees on when it ends.
 *
 * <p>The clock is handed to each read rather than held. A countdown is a value made of numbers a save can keep, and
 * the clock belongs to whichever sector is running when it is read.
 *
 * @param startTimestamp the campaign timestamp the countdown started at
 * @param durationDays   the countdown's length in campaign days. A negative length is accepted and reads as already
 *                       over, since a duration often comes from a setting a caller does not police.
 * @param slackDays      how many days short of the duration still count as complete; zero for an exact countdown
 */
public record CampaignCountdown(
    long startTimestamp,
    float durationDays,
    float slackDays) {

    private static final float NO_DAYS_LEFT = 0f;

    /**
     * @throws IllegalArgumentException when the slack is negative, which would leave a countdown that never
     *                                  completes: the days left bottom out at zero and could never fall below it
     */
    public CampaignCountdown {

        if (slackDays < NO_DAYS_LEFT) {
            throw new IllegalArgumentException("slackDays must not be negative: " + slackDays);
        }
    }

    /**
     * Returns the duration minus the days elapsed since the start, clamped at zero: a clock that ticked past the end
     * reads as "0 days left" rather than as a negative remainder.
     *
     * @param clock the running sector's campaign clock
     * @return the days left, never negative
     */
    public float computeRemainingDays(CampaignClockAPI clock) {

        var elapsedDays = clock.getElapsedDaysSince(startTimestamp);

        return Math.max(NO_DAYS_LEFT, durationDays - elapsedDays);
    }

    /**
     * @param clock the running sector's campaign clock
     * @return whether the days left have fallen to the slack or below
     */
    public boolean isComplete(CampaignClockAPI clock) {

        return computeRemainingDays(clock) <= slackDays;
    }
}
