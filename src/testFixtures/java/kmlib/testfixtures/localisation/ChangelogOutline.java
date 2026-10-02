package kmlib.testfixtures.localisation;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * The shape of a Keep a Changelog file: its versions, each version's sections, how many list items each
 * section holds at each nesting depth, and the code spans each section holds.
 *
 * <p>The prose is not read. A translation therefore has the same outline as the file it translates, until
 * one of them gains a point the other lacks. Two things are read as written, because a translation keeps
 * them verbatim. Version headings: a release finds a version's notes by its heading, and the index links to
 * the heading's anchor. Code spans: identifiers are never translated, so a point rewritten in one file and
 * not the other usually changes them.
 *
 * @param preamble everything before the first version: the title, the introduction and the index
 * @param versions every version, in file order
 */
public record ChangelogOutline(
    BlockOutline preamble,
    List<BlockOutline> versions) {

    // A run of backticks, then the shortest text up to a run of the same length. The lookarounds keep a
    // longer run from closing a shorter one, as CommonMark has it.
    private static final Pattern CODE_SPAN = Pattern.compile("(`+)(?!`)(.+?)(?<!`)\\1(?!`)");

    // Lines inside a code fence are neither headings nor list items.
    private static final Pattern FENCE = Pattern.compile("[ \\t]*(```|~~~).*");

    private static final Pattern HEADING = Pattern.compile("(#{1,6})\\s+(.*)");

    private static final Pattern LIST_ITEM = Pattern.compile("([ \\t]*)(?:[-*+]|\\d+[.)])\\s+.*");

    // CommonMark's tab stop.
    private static final int TAB_WIDTH = 4;

    // Keep a Changelog opens each version, Unreleased included, with "## [". Any other second-level heading,
    // such as the index, is a section.
    private static final int VERSION_HEADING_LEVEL = 2;
    private static final String VERSION_HEADING_TEXT_PREFIX = "[";

    /**
     * Holds the versions unmodifiable, however they were built.
     *
     * @param preamble see the record
     * @param versions see the record
     */
    public ChangelogOutline {

        Objects.requireNonNull(preamble, "preamble");
        versions = List.copyOf(versions);
    }

    /**
     * How this outline differs from a reference outline. Versions are compared first, by heading. Then the
     * preamble and each version both files hold are compared section by section: list items by depth, and
     * code spans.
     *
     * @param referenceOutline the outline this one should match
     * @param referenceTag     how a phrase names the reference
     * @return one phrase per difference; empty when the two match
     */
    public List<String> describeDifferencesFrom(ChangelogOutline referenceOutline, String referenceTag) {

        var differences = new ArrayList<>(describeVersionDifferences(referenceOutline, referenceTag));

        differences.addAll(preamble.describeDifferencesFrom(referenceOutline.preamble(), referenceTag));

        for (var version : versions) {

            referenceOutline.findVersion(version.blockName())
                .ifPresent(referenceVersion ->
                    differences.addAll(version.describeDifferencesFrom(referenceVersion, referenceTag)));
        }
        return differences;
    }

    /**
     * Reads a changelog's lines.
     *
     * @param lines the file's lines, without their line endings
     * @return their outline
     */
    public static ChangelogOutline parseOutline(List<String> lines) {

        var preamble = new BlockBuilder(BlockOutline.PREAMBLE_NAME);
        var versions = new ArrayList<BlockBuilder>();
        var block = preamble;

        // The indents of the list items the current line is nested under, outermost first. A heading or an
        // unindented paragraph ends the list.
        var openItemIndents = new ArrayDeque<Integer>();
        var isInsideFence = false;

        for (var line : lines) {

            if (FENCE.matcher(line).matches()) {

                isInsideFence = !isInsideFence;
                continue;
            }
            if (isInsideFence) {
                continue;
            }
            var heading = HEADING.matcher(line);

            if (heading.matches()) {

                openItemIndents.clear();

                var headingLevel = heading.group(1).length();
                var headingText = heading.group(2).strip();

                if (headingLevel == VERSION_HEADING_LEVEL && headingText.startsWith(VERSION_HEADING_TEXT_PREFIX)) {

                    block = new BlockBuilder(line.strip());
                    versions.add(block);

                } else {

                    block.openSection(headingLevel, headingText);
                }
                continue;
            }
            block.collectCodeSpans(line);

            var listItem = LIST_ITEM.matcher(line);

            if (listItem.matches()) {

                block.countItem(resolveItemDepth(openItemIndents, measureIndent(listItem.group(1))));

            } else if (!line.isBlank() && measureIndent(line) == 0) {

                openItemIndents.clear();
            }
        }
        return new ChangelogOutline(
            preamble.buildBlock(),
            versions.stream()
                .map(BlockBuilder::buildBlock)
                .toList());
    }

    /**
     * Reads a changelog file.
     *
     * @param changelogFile the file to read, as UTF-8
     * @return its outline
     */
    public static ChangelogOutline readOutline(Path changelogFile) {

        try {
            return parseOutline(Files.readAllLines(changelogFile, StandardCharsets.UTF_8));

        } catch (IOException ioException) {

            throw new UncheckedIOException("Could not read " + changelogFile.toAbsolutePath(), ioException);
        }
    }

    private static int measureIndent(String text) {

        var indent = 0;

        for (var index = 0; index < text.length(); index++) {

            var character = text.charAt(index);

            if (character == ' ') {
                indent++;
            } else if (character == '\t') {
                indent += TAB_WIDTH - indent % TAB_WIDTH;
            } else {
                break;
            }
        }
        return indent;
    }

    // Indents are compared, not counted in fixed steps, so nesting by two spaces and by four read the same.
    private static int resolveItemDepth(Deque<Integer> openItemIndents, int itemIndent) {

        while (!openItemIndents.isEmpty() && openItemIndents.peekLast() > itemIndent) {
            openItemIndents.removeLast();
        }
        if (openItemIndents.isEmpty() || openItemIndents.peekLast() < itemIndent) {
            openItemIndents.addLast(itemIndent);
        }
        return openItemIndents.size() - 1;
    }

    // Order is reported only at the first misplaced version, since the ones after it usually move with it.
    // Equal heading sets with unequal counts mean a version is declared twice.
    private List<String> describeVersionDifferences(ChangelogOutline referenceOutline, String referenceTag) {

        var headingLines = listVersionHeadingLines();
        var referenceHeadingLines = referenceOutline.listVersionHeadingLines();
        var differences = new ArrayList<String>();

        referenceHeadingLines.stream()
            .filter(headingLine -> !headingLines.contains(headingLine))
            .forEach(headingLine -> differences.add(
                "lacks version " + headingLine + ", which " + referenceTag + " declares"));

        headingLines.stream()
            .filter(headingLine -> !referenceHeadingLines.contains(headingLine))
            .forEach(headingLine -> differences.add(
                "declares version " + headingLine + ", which " + referenceTag + " does not"));

        if (!differences.isEmpty()) {
            return differences;
        }
        for (var index = 0; index < Math.min(headingLines.size(), referenceHeadingLines.size()); index++) {

            if (!headingLines.get(index).equals(referenceHeadingLines.get(index))) {

                return List.of("places version " + headingLines.get(index) + " at position " + (index + 1)
                    + ", where " + referenceTag + " places " + referenceHeadingLines.get(index));
            }
        }
        if (headingLines.size() != referenceHeadingLines.size()) {

            return List.of("declares " + headingLines.size() + " versions, where " + referenceTag + " declares "
                + referenceHeadingLines.size());
        }
        return List.of();
    }

    private Optional<BlockOutline> findVersion(String headingLine) {

        return versions.stream()
            .filter(version -> version.blockName().equals(headingLine))
            .findFirst();
    }

    private List<String> listVersionHeadingLines() {

        return versions.stream()
            .map(BlockOutline::blockName)
            .toList();
    }

    // Items read are counted in the last section opened.
    private static final class BlockBuilder {

        private final String blockName;
        private final List<SectionBuilder> sections = new ArrayList<>();

        BlockBuilder(String blockName) {

            this.blockName = blockName;
            sections.add(new SectionBuilder(0, ""));
        }

        BlockOutline buildBlock() {

            return new BlockOutline(
                blockName,
                sections.stream()
                    .map(SectionBuilder::buildSection)
                    .toList());
        }

        void collectCodeSpans(String line) {
            resolveOpenSection().collectCodeSpans(line);
        }

        void countItem(int depth) {
            resolveOpenSection().countItem(depth);
        }

        void openSection(int headingLevel, String headingText) {
            sections.add(new SectionBuilder(headingLevel, headingText));
        }

        private SectionBuilder resolveOpenSection() {
            return sections.get(sections.size() - 1);
        }
    }

    /**
     * The preamble or one version, as its sections in order.
     *
     * @param blockName       the version's heading line as written, or {@link #PREAMBLE_NAME}
     * @param sectionOutlines every section; the first holds what comes before any heading of its own
     */
    public record BlockOutline(
        String blockName,
        List<SectionOutline> sectionOutlines) {

        /** How a finding names what stands before the first version. */
        public static final String PREAMBLE_NAME = "the preamble";

        /**
         * Holds the sections unmodifiable, however they were built.
         *
         * @param blockName       see the record
         * @param sectionOutlines see the record
         */
        public BlockOutline {

            Objects.requireNonNull(blockName, "blockName");
            sectionOutlines = List.copyOf(sectionOutlines);
        }

        // Headings are compared by level only, because their text is translated. Items are compared only when
        // the headings match: one missing section would shift every pairing after it.
        List<String> describeDifferencesFrom(BlockOutline referenceBlock, String referenceTag) {

            var headingMarks = listHeadingMarks();
            var referenceHeadingMarks = referenceBlock.listHeadingMarks();

            if (!headingMarks.equals(referenceHeadingMarks)) {

                return List.of(blockName + " holds headings " + headingMarks + ", where " + referenceTag
                    + " holds " + referenceHeadingMarks);
            }
            var differences = new ArrayList<String>();

            for (var index = 0; index < sectionOutlines.size(); index++) {

                var section = sectionOutlines.get(index);
                var referenceSection = referenceBlock.sectionOutlines().get(index);

                var sectionPlace = blockName + " " + section.describePlace();
                var referencePlace = referenceTag + " " + referenceSection.describePlace();

                if (!section.itemCountsByDepth().equals(referenceSection.itemCountsByDepth())) {

                    differences.add(sectionPlace + " holds " + section.itemCountsByDepth()
                        + " list items by depth, where " + referencePlace + " holds "
                        + referenceSection.itemCountsByDepth());
                }
                var addedCodeSpans = subtractCodeSpans(section.codeSpans(), referenceSection.codeSpans());
                var missingCodeSpans = subtractCodeSpans(referenceSection.codeSpans(), section.codeSpans());

                if (!addedCodeSpans.isEmpty()) {
                    differences.add(sectionPlace + " holds code spans " + addedCodeSpans + " that "
                        + referencePlace + " does not");
                }
                if (!missingCodeSpans.isEmpty()) {
                    differences.add(sectionPlace + " lacks code spans " + missingCodeSpans + " that "
                        + referencePlace + " holds");
                }
            }
            return differences;
        }

        // What the first sorted list holds that the second does not, counting repeats: a span written twice
        // in one file and once in the other differs.
        private static List<String> subtractCodeSpans(List<String> codeSpans, List<String> subtractedCodeSpans) {

            var remainingCodeSpans = new ArrayList<>(codeSpans);

            subtractedCodeSpans.forEach(remainingCodeSpans::remove);

            return remainingCodeSpans;
        }

        // The lead section has no heading, so it is left out.
        private List<String> listHeadingMarks() {

            return sectionOutlines.stream()
                .filter(section -> section.headingLevel() > 0)
                .map(SectionOutline::formatHeadingMark)
                .toList();
        }
    }

    private static final class SectionBuilder {

        private final List<String> codeSpans = new ArrayList<>();
        private final int headingLevel;
        private final String headingText;
        private final List<Integer> itemCountsByDepth = new ArrayList<>();

        SectionBuilder(int headingLevel, String headingText) {

            this.headingLevel = headingLevel;
            this.headingText = headingText;
        }

        SectionOutline buildSection() {
            return new SectionOutline(headingLevel, headingText, itemCountsByDepth, codeSpans);
        }

        // Stripped as CommonMark strips a span's padding, so `` `x` `` and `x` read alike.
        void collectCodeSpans(String line) {

            var codeSpan = CODE_SPAN.matcher(line);

            while (codeSpan.find()) {
                codeSpans.add(codeSpan.group(2).strip());
            }
        }

        // The first item at a depth opens that depth's count.
        void countItem(int depth) {

            while (itemCountsByDepth.size() <= depth) {
                itemCountsByDepth.add(0);
            }
            itemCountsByDepth.set(depth, itemCountsByDepth.get(depth) + 1);
        }
    }

    /**
     * One section: its heading, how many list items it holds at each depth of nesting, and its code spans.
     *
     * @param headingLevel      how many {@code #} open its heading; zero for the lead section, which has none
     * @param headingText       its heading's text as written, empty for the lead section
     * @param itemCountsByDepth how many items stand at each depth, the outermost first, as deep as the
     *                          deepest item
     * @param codeSpans         the text of every code span below the heading, sorted, repeats kept
     */
    public record SectionOutline(
        int headingLevel,
        String headingText,
        List<Integer> itemCountsByDepth,
        List<String> codeSpans) {

        /**
         * Holds the lists unmodifiable, however they were built, and the code spans sorted: a translation
         * may move a span within its point, or reorder the points of a section.
         *
         * @param headingLevel      see the record
         * @param headingText       see the record
         * @param itemCountsByDepth see the record
         * @param codeSpans         see the record
         */
        public SectionOutline {

            Objects.requireNonNull(headingText, "headingText");
            itemCountsByDepth = List.copyOf(itemCountsByDepth);
            codeSpans = codeSpans.stream()
                .sorted()
                .toList();
        }

        // Names the section by its heading as written in its own file, which is what a reader searches for.
        String describePlace() {

            return headingLevel == 0
                ? "before its first heading"
                : "under " + formatHeadingMark() + " " + headingText;
        }

        String formatHeadingMark() {
            return "#".repeat(headingLevel);
        }
    }
}
