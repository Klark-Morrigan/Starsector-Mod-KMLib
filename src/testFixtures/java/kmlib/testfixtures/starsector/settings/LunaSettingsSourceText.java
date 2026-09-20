package kmlib.testfixtures.starsector.settings;

import kmlib.settings.LabeledChoice;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A mod's shipped Java sources, read as text: which LunaLib field IDs they name, and what each
 * row's fallback constant is declared as.
 *
 * <p>Text rather than classes because a fallback constant is private to the class that reads its
 * row, which is what a settings class is for - so there is no accessor to call and no reflection
 * that would be any more honest than a regex. What it costs is that the sources have to be written
 * the way the walk expects; that is the point rather than the price, since a getter written some
 * other way fails the walk loudly instead of dropping out of it unnoticed.
 *
 * <p>The shapes it expects are the ones these conventions read settings through - a field ID held
 * as a string constant, fetched by a {@code readBoolean} / {@code readDouble} / {@code readFloat} /
 * {@code readInt} call naming that constant and its fallback - so the walk is shared rather than
 * re-spelled per mod.
 *
 * <p>A row's two ends are reached by following links, each anchored on the name the previous one
 * yielded: the field ID to the constant declaring it, that constant to the fallback passed beside
 * it at the read, and that fallback to the value it is declared as. Only the middle link varies by
 * type, so it is what parts the numeric walk from the boolean one - a row fetched through some
 * other kind of read then fails as an unfollowed link rather than being matched and held against
 * the wrong kind of default.
 *
 * <p>{@link LunaSettingsTable} is the other reading a settings check is made of, over the file
 * these sources name rows in.
 */
public final class LunaSettingsSourceText {

    /** Where a mod on these conventions keeps the sources that name its rows. */
    public static final Path MAIN_SOURCE_ROOT = Path.of("src", "main", "java");

    private static final String JAVA_SOURCE_SUFFIX = ".java";

    // The first link, the same whatever the row holds - a field ID is a field ID.
    private static final String FIELD_CONSTANT_PATTERN = "(\\w+)\\s*=\\s*\"%s\"";

    // Every typed read a numeric row can be fetched through. Named one by one rather than as a
    // wildcard so that a read this walk has no answer for - a choice or a boolean read against a
    // numeric row - fails as an unfollowed link instead of being matched wrongly.
    private static final String NUMERIC_FALLBACK_READ_PATTERN =
        "read(?:Double|Float|Int)\\(\\s*%s\\s*,\\s*(\\w+)\\s*\\)";

    private static final String NUMERIC_DEFAULT_PATTERN = "\\b%s\\s*=\\s*(-?[\\d.]+[fFdD]?)\\s*;";

    // The same middle link for a Boolean row. Only one read can fetch one, so unlike the numeric
    // alternation this names a single method - which is what makes a switch read through anything
    // else fail the walk rather than pass it.
    private static final String BOOLEAN_FALLBACK_READ_PATTERN =
        "readBoolean\\(\\s*%s\\s*,\\s*(\\w+)\\s*\\)";

    private static final String BOOLEAN_DEFAULT_PATTERN = "\\b%s\\s*=\\s*(true|false)\\s*;";

    // A named Java fallback as a settings class declares it: the constant, then the enum constant
    // it is assigned. Anchored on the constant's own name so two rows backed by the same enum with
    // different defaults are still told apart, and the enum is left unnamed so a choice moved to
    // another type still resolves.
    private static final String CHOICE_DEFAULT_PATTERN = "\\b%s\\s*=\\s*\\w+\\.([A-Z][A-Z0-9_]*)\\s*;";

    private final Pattern fieldIdLiteral;
    private final Path mainSourceRoot;

    /**
     * Opens a reading of one mod's settings sources.
     *
     * @param mainSourceRoot the shipped source tree, relative to the module the suite runs in;
     *                       {@link #MAIN_SOURCE_ROOT} is where these conventions put it
     * @param fieldIdPrefix  what every one of the mod's field IDs starts with, which is what makes
     *                       an ID literal recognisable among every other string the tree holds
     */
    public LunaSettingsSourceText(Path mainSourceRoot, String fieldIdPrefix) {
        Objects.requireNonNull(fieldIdPrefix, "fieldIdPrefix");
        this.mainSourceRoot = Objects.requireNonNull(mainSourceRoot, "mainSourceRoot");

        // Quoted, so a mention in prose or a comment does not count as reading the field.
        this.fieldIdLiteral = Pattern.compile(
            "\"(" + Pattern.quote(fieldIdPrefix) + "[A-Za-z0-9_]+)\"");
    }

    /**
     * The constant a Boolean row's getter passes as its fallback.
     *
     * @param fieldId the row to follow
     * @return the fallback constant's name
     */
    public String findBooleanFallbackConstant(String fieldId) {
        return findFallbackConstant(fieldId, BOOLEAN_FALLBACK_READ_PATTERN);
    }

    /**
     * The constant a numeric row's getter passes as its fallback, followed through any of the typed
     * reads a number may be fetched by.
     *
     * @param fieldId the row to follow
     * @return the fallback constant's name
     */
    public String findNumericFallbackConstant(String fieldId) {
        return findFallbackConstant(fieldId, NUMERIC_FALLBACK_READ_PATTERN);
    }

    /**
     * The state a switch's fallback constant is declared as.
     *
     * @param defaultConstant the constant to read
     * @return what it is declared as
     */
    public boolean readDeclaredFlag(String defaultConstant) {

        return Boolean.parseBoolean(findSoleMatch(
            BOOLEAN_DEFAULT_PATTERN.formatted(Pattern.quote(defaultConstant)),
            "a declaration of " + defaultConstant));
    }

    /**
     * The number a fallback constant is declared as. A float literal's trailing suffix is not part
     * of the number and is dropped.
     *
     * @param defaultConstant the constant to read
     * @return what it is declared as
     */
    public double readDeclaredNumber(String defaultConstant) {

        var declared = findSoleMatch(
            NUMERIC_DEFAULT_PATTERN.formatted(Pattern.quote(defaultConstant)),
            "a declaration of " + defaultConstant);

        return Double.parseDouble(declared.replaceAll("[fFdD]$", ""));
    }

    /**
     * The option label a named choice fallback constant resolves to, so a caller holds the shipped
     * constant rather than a copy of it.
     *
     * @param defaultConstant the constant to read
     * @param choices         the enum's constants, whose labels are what a Radio row stores
     * @return the label the constant resolves to
     */
    public String readFallbackLabel(String defaultConstant, LabeledChoice[] choices) {

        var declaredChoice = findDeclaredChoiceName(defaultConstant);

        return Arrays
            .stream(choices)
            .filter(choice -> ((Enum<?>) choice).name().equals(declaredChoice))
            .map(LabeledChoice::getLabel)
            .findFirst()
            .orElseThrow(() -> new AssertionError(
                defaultConstant + " is declared as " + declaredChoice
                    + ", which is no option of the enum this row's table names"));
    }

    /**
     * Every field ID the shipped sources name, wherever they hold it.
     *
     * @return those IDs
     */
    public Set<String> readFieldIdLiteralsInMainSources() {
        try (var sources = Files.walk(mainSourceRoot)) {
            return sources
                .filter(source -> source.toString().endsWith(JAVA_SOURCE_SUFFIX))
                .flatMap(this::findFieldIdLiterals)
                .collect(Collectors.toSet());
        } catch (IOException failure) {
            // Surfaced for the reason the CSV read is: an unreadable source tree means the walk is
            // looking in the wrong place, not that every field is read.
            throw new UncheckedIOException(
                "Could not walk " + mainSourceRoot.toAbsolutePath(),
                failure);
        }
    }

    private static String readSource(Path source) {
        try {
            return Files.readString(source, StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new UncheckedIOException("Could not read " + source.toAbsolutePath(), failure);
        }
    }

    // The enum constant a fallback is declared as. Exactly one declaration is expected: none means
    // the caller's table names a constant the sources no longer hold, and two would leave the walk
    // holding whichever the file listed first.
    private String findDeclaredChoiceName(String defaultConstant) {

        var pattern = Pattern.compile(CHOICE_DEFAULT_PATTERN.formatted(defaultConstant));

        try (var sources = Files.walk(mainSourceRoot)) {

            var declarations = sources
                .filter(source -> source.toString().endsWith(JAVA_SOURCE_SUFFIX))
                .flatMap(source -> pattern.matcher(readSource(source)).results())
                .map(match -> match.group(1))
                .toList();

            requireSoleMatch(declarations, "declarations of " + defaultConstant);

            return declarations.get(0);

        } catch (IOException failure) {
            throw new UncheckedIOException(
                "Could not walk " + mainSourceRoot.toAbsolutePath(),
                failure);
        }
    }

    // The constant a row's getter passes as its fallback, found by following the first two links.
    private String findFallbackConstant(String fieldId, String fallbackReadPattern) {

        var fieldConstant = findSoleMatch(
            FIELD_CONSTANT_PATTERN.formatted(Pattern.quote(fieldId)),
            "the constant holding field id " + fieldId);

        return findSoleMatch(
            fallbackReadPattern.formatted(Pattern.quote(fieldConstant)),
            "the fallback passed beside " + fieldConstant);
    }

    private Stream<String> findFieldIdLiterals(Path source) {
        return fieldIdLiteral
            .matcher(readSource(source))
            .results()
            .map(match -> match.group(1));
    }

    // The one capture the pattern finds across every shipped source. Exactly one is expected: none
    // means the sources no longer spell the thing this walk follows, and two would leave it holding
    // whichever file happened to be read first.
    private String findSoleMatch(String pattern, String soughtDescription) {

        var matches = Pattern
            .compile(pattern)
            .matcher(readMainSourceText())
            .results()
            .map(match -> match.group(1))
            .distinct()
            .toList();

        requireSoleMatch(matches, soughtDescription);

        return matches.get(0);
    }

    // Every shipped source as one text, so a walk that follows a link across classes - a field ID
    // declared in one and read in another - sees both ends of it.
    private String readMainSourceText() {
        try (var sources = Files.walk(mainSourceRoot)) {
            return sources
                .filter(source -> source.toString().endsWith(JAVA_SOURCE_SUFFIX))
                .map(LunaSettingsSourceText::readSource)
                .collect(Collectors.joining("\n"));
        } catch (IOException failure) {
            // Surfaced for the reason the CSV read is: an unreadable source tree means the walk is
            // looking in the wrong place, not that every fallback agrees.
            throw new UncheckedIOException(
                "Could not walk " + mainSourceRoot.toAbsolutePath(),
                failure);
        }
    }

    // Fails a walk that found no end to follow, or more than one. Raised here rather than left to
    // the suite because the walk cannot go on either way, and a caller handed an empty list would
    // have to re-state the same condition to say so.
    private void requireSoleMatch(List<String> matches, String soughtDescription) {

        if (matches.size() != 1) {
            throw new AssertionError(
                "Expected exactly one match for " + soughtDescription + " under " + mainSourceRoot
                    + " but found " + matches.size() + ": " + matches);
        }
    }
}
