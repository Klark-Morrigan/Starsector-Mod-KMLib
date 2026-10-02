package kmlib.testfixtures.localisation;

import kmlib.testfixtures.localisation.ChangelogOutline.BlockOutline;
import kmlib.testfixtures.localisation.ChangelogOutline.SectionOutline;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;

/**
 * Pins how a Keep a Changelog file is read as its shape, and how two shapes are told apart: by version
 * heading as written, by heading level in order, by list items at each depth and by code spans, never by
 * prose.
 */
final class ChangelogOutlineTests {

    private static final String ENGLISH_CHANGELOG = """
        # Changelog

        ## Index

        - [Unreleased](#unreleased)
        - [0.1.0](#010---2026-09-14)

        ## [Unreleased]

        ### Added

        - **Layers**:
          - Political.
          - Diplomatic.
        - Sidebar, `SidebarPanel` and `SidebarStyles`.

        ### Fixed

        - Tooltip, `SystemTooltip`.

        ## [0.1.0] - 2026-09-14

        - First release.
        """;

    // The same shape in other words, as a translation is meant to read. The code spans are the same, one point
    // naming them in another order.
    private static final String TRANSLATED_CHANGELOG = """
        # Journal

        ## Sommaire

        - [Non publie](#unreleased)
        - [0.1.0](#010---2026-09-14)

        ## [Unreleased]

        ### Ajouts

        - **Calques** :
          - Politique.
          - Diplomatique.
        - Barre laterale, `SidebarStyles` et `SidebarPanel`.

        ### Corrections

        - Infobulle, `SystemTooltip`.

        ## [0.1.0] - 2026-09-14

        - Premiere version.
        """;

    // "Changelog" (U+66F4 U+65B0 U+65E5 U+5FD7): a title outside Latin-1, as a translated changelog
    // carries it.
    private static final String TRANSLATED_TITLE = "更新日志";

    private static ChangelogOutline parseText(String text) {
        return ChangelogOutline.parseOutline(text.lines().toList());
    }

    // The agreeing translation with one run of it replaced, so a case states exactly the break it makes.
    // A run the text does not hold fails the case, which would otherwise pass over an agreeing file.
    private static ChangelogOutline parseTranslationReplacing(String agreeingRun, String brokenRun) {

        if (!TRANSLATED_CHANGELOG.contains(agreeingRun)) {
            throw new IllegalArgumentException("The agreeing translation holds no \"" + agreeingRun + "\"");
        }
        return parseText(TRANSLATED_CHANGELOG.replace(agreeingRun, brokenRun));
    }

    private static List<String> describeTranslationReplacing(String agreeingRun, String brokenRun) {

        return parseTranslationReplacing(agreeingRun, brokenRun)
            .describeDifferencesFrom(parseText(ENGLISH_CHANGELOG), "en");
    }

    @Nested
    class DescribeDifferencesFrom {

        @Test
        void theSameShapeInOtherWordsAgrees() {

            assertThat(parseText(TRANSLATED_CHANGELOG).describeDifferencesFrom(parseText(ENGLISH_CHANGELOG), "en"))
                .isEmpty();
        }

        @Test
        void aMissingAndAnAddedVersionAreDescribed() {

            assertThat(describeTranslationReplacing("## [0.1.0] - 2026-09-14", "## [0.1.1] - 2026-09-15"))
                .containsExactly(
                    "lacks version ## [0.1.0] - 2026-09-14, which en declares",
                    "declares version ## [0.1.1] - 2026-09-15, which en does not");
        }

        @Test
        void aVersionHeadingTranslatedIsDescribedAsMissing() {

            assertThat(describeTranslationReplacing("## [Unreleased]", "## [Non publie]"))
                .containsExactly(
                    "lacks version ## [Unreleased], which en declares",
                    "declares version ## [Non publie], which en does not");
        }

        @Test
        void theFirstVersionOutOfPlaceIsDescribed() {

            var swapped = parseText("""
                ## [0.1.0] - 2026-09-14

                ## [Unreleased]
                """);
            var reference = parseText("""
                ## [Unreleased]

                ## [0.1.0] - 2026-09-14
                """);

            assertThat(swapped.describeDifferencesFrom(reference, "en"))
                .containsExactly("places version ## [0.1.0] - 2026-09-14 at position 1, where en places "
                    + "## [Unreleased]");
        }

        @Test
        void aVersionDeclaredTwiceIsDescribed() {

            var doubled = parseText("""
                ## [Unreleased]

                ## [Unreleased]
                """);

            assertThat(doubled.describeDifferencesFrom(parseText("## [Unreleased]"), "en"))
                .containsExactly("declares 2 versions, where en declares 1");
        }

        @Test
        void aMissingSectionIsDescribedByHeadingLevels() {

            assertThat(describeTranslationReplacing("### Corrections\n\n- Infobulle, `SystemTooltip`.\n", ""))
                .containsExactly("## [Unreleased] holds headings [###], where en holds [###, ###]");
        }

        @Test
        void aSectionAtAnotherLevelIsDescribedByHeadingLevels() {

            assertThat(describeTranslationReplacing("### Corrections", "#### Corrections"))
                .containsExactly("## [Unreleased] holds headings [###, ####], where en holds [###, ###]");
        }

        @Test
        void aDroppedNestedPointIsDescribedUnderBothHeadings() {

            assertThat(describeTranslationReplacing("  - Diplomatique.\n", ""))
                .containsExactly("## [Unreleased] under ### Ajouts holds [2, 1] list items by depth, where en "
                    + "under ### Added holds [2, 2]");
        }

        @Test
        void aCodeSpanChangedInOneFileIsDescribedBothWays() {

            assertThat(describeTranslationReplacing("`SystemTooltip`", "`SystemHoverBox`"))
                .containsExactly(
                    "## [Unreleased] under ### Corrections holds code spans [SystemHoverBox] that en under "
                        + "### Fixed does not",
                    "## [Unreleased] under ### Corrections lacks code spans [SystemTooltip] that en under "
                        + "### Fixed holds");
        }

        @Test
        void aCodeSpanWrittenTwiceWhereTheReferenceWritesItOnceIsDescribed() {

            assertThat(describeTranslationReplacing("et `SidebarPanel`", "et `SidebarPanel`, `SidebarPanel`"))
                .containsExactly("## [Unreleased] under ### Ajouts holds code spans [SidebarPanel] that en under "
                    + "### Added does not");
        }

        @Test
        void aPointWrittenAsAParagraphIsDescribed() {

            assertThat(describeTranslationReplacing("- Premiere version.", "Premiere version."))
                .containsExactly("## [0.1.0] - 2026-09-14 before its first heading holds [] list items by depth, "
                    + "where en before its first heading holds [1]");
        }

        @Test
        void aPreambleMissingAnIndexEntryIsDescribed() {

            assertThat(describeTranslationReplacing("- [0.1.0](#010---2026-09-14)\n", ""))
                .containsExactly("the preamble under ## Sommaire holds [1] list items by depth, where en under "
                    + "## Index holds [2]");
        }

        @Test
        void aVersionOnlyOneSideHoldsIsDescribedOnceAsMissing() {

            var shortened = parseText(TRANSLATED_CHANGELOG.substring(
                0,
                TRANSLATED_CHANGELOG.indexOf("## [0.1.0]")));

            assertThat(shortened.describeDifferencesFrom(parseText(ENGLISH_CHANGELOG), "en"))
                .containsExactly("lacks version ## [0.1.0] - 2026-09-14, which en declares");
        }
    }

    @Nested
    class ParseOutline {

        @Test
        void versionsAreHeldByTheirHeadingLinesInOrder() {

            assertThat(parseText(ENGLISH_CHANGELOG).versions())
                .extracting(BlockOutline::blockName)
                .containsExactly("## [Unreleased]", "## [0.1.0] - 2026-09-14");
        }

        @Test
        void aSecondLevelHeadingWithoutAVersionIsASectionOfThePreamble() {

            assertThat(parseText(ENGLISH_CHANGELOG).preamble().sectionOutlines())
                .extracting(SectionOutline::headingLevel, SectionOutline::headingText)
                .containsExactly(
                    tuple(0, ""),
                    tuple(1, "Changelog"),
                    tuple(2, "Index"));
        }

        @Test
        void itemsAreCountedAtEachDepthPerSection() {

            assertThat(parseText(ENGLISH_CHANGELOG).versions().get(0).sectionOutlines())
                .extracting(SectionOutline::itemCountsByDepth)
                .containsExactly(List.of(), List.of(2, 2), List.of(1));
        }

        @Test
        void codeSpansAreHeldPerSectionSorted() {

            assertThat(parseText(ENGLISH_CHANGELOG).versions().get(0).sectionOutlines())
                .extracting(SectionOutline::codeSpans)
                .containsExactly(List.of(), List.of("SidebarPanel", "SidebarStyles"), List.of("SystemTooltip"));
        }

        @Test
        void aCodeSpanOpenedByTwoBackticksHoldsASingleOneAndLosesItsPadding() {

            var outline = parseText("""
                ## [Unreleased]

                - The `` `ID` `` key and ``a`b``.
                """);

            assertThat(outline.versions().get(0).sectionOutlines().get(0).codeSpans())
                .containsExactly("`ID`", "a`b");
        }

        @Test
        void nestingByFourSpacesReadsAsNestingByTwo() {

            var outline = parseText("""
                ## [Unreleased]

                - Outer.
                    - Inner.
                        - Innermost.
                    - Inner again.
                - Outer again.
                """);

            assertThat(outline.versions().get(0).sectionOutlines().get(0).itemCountsByDepth())
                .containsExactly(2, 2, 1);
        }

        @Test
        void aTabIndentsToTheNextTabStop() {

            // One tab is four columns, deeper than the outer item's two-space indent but level with the
            // item indented by four spaces.
            var outline = parseText("""
                ## [Unreleased]

                  - Outer.
                \t- By a tab.
                    - By four spaces.
                """);

            assertThat(outline.versions().get(0).sectionOutlines().get(0).itemCountsByDepth())
                .containsExactly(1, 2);
        }

        @Test
        void numberedItemsAreCounted() {

            var outline = parseText("""
                ## [Unreleased]

                1. First.
                2. Second.
                   - Under the second.
                """);

            assertThat(outline.versions().get(0).sectionOutlines().get(0).itemCountsByDepth())
                .containsExactly(2, 1);
        }

        @Test
        void anIndentedParagraphKeepsTheListOpen() {

            var outline = parseText("""
                ## [Unreleased]

                - Outer.

                  A paragraph continuing the outer point.

                  - Inner.
                """);

            assertThat(outline.versions().get(0).sectionOutlines().get(0).itemCountsByDepth())
                .containsExactly(1, 1);
        }

        @Test
        void anUnindentedParagraphEndsTheList() {

            var outline = parseText("""
                ## [Unreleased]

                - Outer.

                A paragraph of its own.

                  - Indented, but under no item.
                """);

            assertThat(outline.versions().get(0).sectionOutlines().get(0).itemCountsByDepth())
                .containsExactly(2);
        }

        @Test
        void fencedLinesAreNeitherHeadingsNorItemsNorCodeSpans() {

            var outline = parseText("""
                ## [Unreleased]

                ```markdown
                ## [0.0.1]
                ### Added
                - Not a point, `not a span`.
                ```

                - A point.
                """);

            assertThat(outline.versions())
                .singleElement()
                .satisfies(version -> assertThat(version.sectionOutlines())
                    .singleElement()
                    .satisfies(section -> {
                        assertThat(section.itemCountsByDepth()).isEqualTo(List.of(1));
                        assertThat(section.codeSpans()).isEmpty();
                    }));
        }
    }

    @Nested
    class ReadOutline {

        @Test
        void theFileIsReadAsUtf8(@TempDir Path directory) throws IOException {

            var changelogFile = directory.resolve("CHANGELOG.md");

            Files.writeString(
                changelogFile,
                "# " + TRANSLATED_TITLE + "\r\n\r\n## [Unreleased]\r\n",
                StandardCharsets.UTF_8);

            assertThat(ChangelogOutline.readOutline(changelogFile).preamble().sectionOutlines())
                .extracting(SectionOutline::headingText)
                .containsExactly("", TRANSLATED_TITLE);
        }

        @Test
        void anAbsentFileNamesItself(@TempDir Path directory) {

            var changelogFile = directory.resolve("CHANGELOG.md");

            assertThatThrownBy(() -> ChangelogOutline.readOutline(changelogFile))
                .isInstanceOf(UncheckedIOException.class)
                .hasMessageContaining("CHANGELOG.md");
        }
    }
}
