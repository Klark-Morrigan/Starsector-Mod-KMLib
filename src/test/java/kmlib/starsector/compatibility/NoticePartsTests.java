package kmlib.starsector.compatibility;

import kmlib.starsector.compatibility.CompatibilityNoticeLine.Emphasis;
import kmlib.starsector.compatibility.CompatibilityNoticeLine.EmphasisedRun;
import kmlib.testfixtures.starsector.compatibility.CompatibilityFailureFixture;
import kmlib.testfixtures.starsector.compatibility.CompatibilitySlotTemplates;
import kmlib.testfixtures.starsector.settings.ModStateScopes;
import kmlib.testfixtures.starsector.settings.StarsectorSettingsFake;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * Pins the parts every kind of notice words alike: how the mod is named, the rows saying what is lost
 * and what is not, and the plain-text layout.
 *
 * <p>The mod is named three ways depending on what the game holds for it, and only one of them shows
 * where no settings are installed, so each is posed here on its own.
 */
final class NoticePartsTests {

    private static final CompatibilityConsumer CONSUMER = CompatibilityFailureFixture.MAP_OVERLAY_CONSUMER;

    private static final String MOD_ID = CompatibilityFailureFixture.MAP_OVERLAY_MOD_ID;

    private static final String MOD_NAME = "Map Overlays";

    @AfterEach
    void clearSettings() {

        StarsectorSettingsFake.clearSettings();
    }

    @Nested
    class BuildConsequenceRows {

        @Test
        void warnsOnWhatIsLostAndSetsAtEaseOnWhatIsNot() {

            CompatibilitySlotTemplates.installSlotTemplates();

            assertThat(NoticeParts.buildConsequenceRows(CONSUMER))
                .extracting(row -> row.emphasisedRuns().get(0))
                .extracting(EmphasisedRun::runText, EmphasisedRun::emphasis)
                .containsExactly(
                    tuple(CompatibilityFailureFixture.LOST_FEATURE, Emphasis.WARNING),
                    tuple(CompatibilityFailureFixture.UNAFFECTED_FEATURE, Emphasis.REASSURANCE));
        }

        @Test
        void leavesOutWhatIsNotLostWhereTheConsumerSaidNothing() {
            // A row reading "No effect: -" claims less than no row at all.
            CompatibilitySlotTemplates.installSlotTemplates();

            var consumer = new CompatibilityConsumer(
                MOD_ID,
                CompatibilityFailureFixture.MAP_OVERLAY_FEATURE_KEY,
                CompatibilityFailureFixture.LOST_FEATURE);

            assertThat(NoticeParts.buildConsequenceRows(consumer))
                .extracting(CompatibilityNoticeLine::lineText)
                .containsExactly("effect{" + CompatibilityFailureFixture.LOST_FEATURE + "}");
        }
    }

    @Nested
    class ComposePlainNotice {

        @Test
        void leavesOutTheDiagnosisParagraphWhereThereIsNone() {
            // A binding with no target version has nothing to advise, and an empty paragraph would
            // leave a double gap between the heading and the rows.
            CompatibilitySlotTemplates.installSlotTemplates();

            var failure = CompatibilityFailureFixture.createFailureBetweenVersions(null, "v0.9.1");

            assertThat(NoticeParts.composePlainNotice(failure))
                .startsWith("title{failed|" + MOD_ID + "|Fast Rendering}\n\nmod{");
        }
    }

    @Nested
    class DescribeClosing {

        @Test
        void bringsTheLogsOwnFileNameForward() {

            CompatibilitySlotTemplates.installSlotTemplates();

            assertThat(NoticeParts.describeClosing().emphasisedRuns())
                .extracting(EmphasisedRun::runText, EmphasisedRun::emphasis)
                .containsExactly(tuple("starsector.log", Emphasis.HIGHLIGHT));
        }
    }

    @Nested
    class DescribeMod {

        @Test
        void namesTheModByItsIdAloneWhereTheGameListsNoSuchMod() {

            ModStateScopes.runWithModNamed("another-mod", MOD_NAME, () ->
                assertThat(NoticeParts.describeMod(CONSUMER))
                    .isEqualTo(MOD_ID));
        }

        @Test
        void namesTheModBesideItsIdWhereTheGameHoldsANameButNoVersion() {

            ModStateScopes.runWithModNamed(MOD_ID, MOD_NAME, () ->
                assertThat(NoticeParts.describeMod(CONSUMER))
                    .isEqualTo("Map Overlays (map-mod)"));
        }

        @Test
        void addsTheVersionWhereTheGameHoldsOne() {

            ModStateScopes.runWithModVersioned(MOD_ID, MOD_NAME, "1.2.0", () ->
                assertThat(NoticeParts.describeMod(CONSUMER))
                    .isEqualTo("Map Overlays (map-mod) 1.2.0"));
        }
    }

    @Nested
    class DescribeModName {

        @Test
        void namesTheModByItsOwnNameWhereTheGameHoldsOne() {

            ModStateScopes.runWithModNamed(MOD_ID, MOD_NAME, () ->
                assertThat(NoticeParts.describeModName(CONSUMER))
                    .isEqualTo(MOD_NAME));
        }

        @Test
        void namesTheModByItsIdWhereTheGameListsNoSuchMod() {

            ModStateScopes.runWithModNamed("another-mod", MOD_NAME, () ->
                assertThat(NoticeParts.describeModName(CONSUMER))
                    .isEqualTo(MOD_ID));
        }
    }
}
