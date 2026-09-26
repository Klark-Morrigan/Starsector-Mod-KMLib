package kmlib.mods.nexerelin;

import kmlib.testfixtures.starsector.settings.ModStateScopes;
import kmlib.testfixtures.starsector.settings.StarsectorSettingsFake;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static kmlib.testfixtures.starsector.settings.StubbedModIds.NEXERELIN;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the soft-dependency gate on the side an install without Nexerelin takes: the gate answers
 * before anything naming {@code exerelin.*} is resolved, so a classloader that has no such class to
 * find is never asked to find one.
 *
 * <p>Worth holding because the answer is somebody's input: a gate that threw, or that invented a
 * partnership, would decide what an install without the mod acts on.
 *
 * <p>The other side cannot be driven here at all, and not for want of the jar: Nexerelin's alliance
 * manager reads its own configuration off {@code Global.getSettings()} in a static initialiser, so
 * naming that class outside a running game fails to initialise it whatever is on the classpath.
 * What the far side does with alliances once it has them is {@link NexerelinAllianceReaderTest}'s,
 * over the flattening that needs no manager, and what a failed read does is
 * {@link GuardedAllianceSourceTest}'s. What is left here of that is which report it files.
 */
final class NexerelinAllianceSourceTest {

    @Nested
    class ReadAllianceRecords {

        @Test
        void readsNoAlliancesWhereNexerelinIsAbsent() {

            ModStateScopes.runWithModEnabled(NEXERELIN, false, () ->
                assertThat(NexerelinAllianceSource.readAllianceRecords())
                    .isEmpty());
        }

        @Test
        void readsNoAlliancesBeforeTheGameSettingsAreUp() {
            // Wiring runs before the game is fully up, and the gate declines there rather than
            // throwing - which is what keeps a caller reading alliances at start-up from taking
            // the launch down.
            ModStateScopes.runWithoutGameSettings(() ->
                assertThat(NexerelinAllianceSource.readAllianceRecords())
                    .isEmpty());
        }
    }

    @Nested
    class DescribeAllianceIntegration {

        @AfterEach
        void clearSettings() {

            StarsectorSettingsFake.clearSettings();
        }

        @Test
        void namesNexerelinAndTheAlliancesFeatureTheReadCosts() {
            // Its own feature rather than the routines', so a failed read and a failed routine are
            // two reports with two sentences. A transposition composes as plausibly as the right
            // pairing and reaches a player naming the wrong loss.
            StarsectorSettingsFake.installSettings((category, key) -> "the sentence for " + key);

            var integration = NexerelinAllianceSource.describeAllianceIntegration();

            assertThat(integration.subjectModId())
                .isEqualTo(NEXERELIN);
            assertThat(integration.subjectModName())
                .isEqualTo("Nexerelin");
            assertThat(integration.consumer().consumerKey())
                .isEqualTo("kmlib:nexerelin-alliances");
            assertThat(integration.consumer().lostFeature())
                .isEqualTo("the sentence for compatibility_lost_nexerelin_alliances");
            assertThat(integration.consumer().unaffectedFeature())
                .isEqualTo("the sentence for compatibility_unaffected_nexerelin_alliances");
        }
    }

    @Nested
    class IsModEnabled {

        @Test
        void reportsNotEnabledWhereNexerelinIsAbsent() {

            ModStateScopes.runWithModEnabled(NEXERELIN, false, () ->
                assertThat(NexerelinAllianceSource.isModEnabled())
                    .isFalse());
        }

        @Test
        void reportsEnabledWhileTheModManagerSaysSo() {

            ModStateScopes.runWithModEnabled(NEXERELIN, true, () ->
                assertThat(NexerelinAllianceSource.isModEnabled())
                    .isTrue());
        }
    }
}
