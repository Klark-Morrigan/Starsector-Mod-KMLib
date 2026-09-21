package kmlib.starsector.compatibility;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Covers the two readings a diagnosis is made from, and that neither may be left out.
 *
 * <p>A breakage missing either half is half a report: the site alone says a guard fired and not what
 * it fired on, and the detail alone names a member without saying which of the guards over that
 * binding met it - which is the reading that tells a member that moved from an entry point a release
 * declares and then refuses.
 */
final class CompatibilityBreakageTest {

    private static final String FAILURE_SITE = "calling the bridge from the game thread";

    private static final String BROKEN_DETAIL = "GLCommand.run (ClassNotFoundException)";

    @Nested
    class Constructor {

        @Test
        void refusesABreakageWithNoSite() {

            assertThatIllegalArgumentException()
                .isThrownBy(() -> new CompatibilityBreakage(" ", BROKEN_DETAIL));
        }

        @Test
        void refusesABreakageWithNoDetail() {

            assertThatIllegalArgumentException()
                .isThrownBy(() -> new CompatibilityBreakage(FAILURE_SITE, " "));
        }

        @Test
        void keepsBothReadingsApart() {

            var breakage = new CompatibilityBreakage(FAILURE_SITE, BROKEN_DETAIL);

            assertThat(breakage.failureSite())
                .isEqualTo(FAILURE_SITE);
            assertThat(breakage.brokenDetail())
                .isEqualTo(BROKEN_DETAIL);
        }
    }
}
