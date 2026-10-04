package kmlib.testfixtures.localisation;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Holds every locale of a mod to its default locale, file by file - the checks a mod runs over its
 * {@code localisation/} directory.
 *
 * <p>Strict where a gap has nothing true behind it, and reporting where it has. A strings key or a
 * settings row one locale lacks has no fallback, so every such gap is a finding. A launcher field a
 * fragment leaves out shows the base file's wording, which is degraded rather than broken and often the
 * right choice, so it is listed rather than found.
 *
 * <p>Every locale is compared with the default rather than with each other: the default is the locale a
 * mod is authored in, so a finding names the locale to fix and the wording to fix it against.
 *
 * <p>Each defect is found once. A locale lacking a bundle directory or a mapped file is found by the
 * check about that, and the comparisons over the file leave the locale out rather than failing on the
 * read; where the default itself lacks a file, nothing is compared over it.
 *
 * <p>Findings come back as sentences, each opening with the locale's tag, so a check asserts a list
 * empty and a failure reads as the edit to make.
 *
 * <p>Each kind of file has a checker of its own in this package; this class is the one entry to them.
 */
public final class LocaleParity {

    private final BundleLayoutParity bundleLayoutParity;
    private final ChangelogParity changelogParity;
    private final CoreLocalisationParity coreLocalisationParity;
    private final ModInfoParity modInfoParity;
    private final SettingsHighlightParity settingsHighlightParity;
    private final SettingsParity settingsParity;
    private final StringsParity stringsParity;

    /**
     * Opens a comparison over a mod shipping no settings table, whose manifest maps its other files alone.
     *
     * @param directory the directory to compare the locales of
     */
    public LocaleParity(LocalisationDirectory directory) {
        this(directory, Optional.empty());
    }

    /**
     * Opens a comparison over one mod's localisation directory.
     *
     * @param directory             the directory to compare the locales of
     * @param settingsFieldIdPrefix what every one of the mod's settings field IDs starts with
     */
    public LocaleParity(LocalisationDirectory directory, String settingsFieldIdPrefix) {
        this(directory, Optional.of(Objects.requireNonNull(settingsFieldIdPrefix, "settingsFieldIdPrefix")));
    }

    private LocaleParity(LocalisationDirectory directory, Optional<String> settingsFieldIdPrefix) {

        Objects.requireNonNull(directory, "directory");

        var bundleReadings = new LocaleBundleReadings(directory);

        this.bundleLayoutParity = new BundleLayoutParity(directory);
        this.changelogParity = new ChangelogParity(directory);
        this.modInfoParity = new ModInfoParity(directory);
        this.settingsParity = new SettingsParity(bundleReadings, settingsFieldIdPrefix);
        this.stringsParity = new StringsParity(bundleReadings);
        this.coreLocalisationParity = new CoreLocalisationParity(directory, settingsParity);
        this.settingsHighlightParity = new SettingsHighlightParity(bundleReadings, settingsParity);
    }

    /**
     * Every finding of every check below, so a mod holds its locales to the default in one assertion. Each
     * finding names its locale, its file and the edit to make, so the check it came from needs no naming.
     * Listed in the order a fix is best made: the directories and files first, since a gap there hides the
     * comparisons over them, then the strings, the settings table, the launcher fragment and the changelog.
     *
     * @return the findings of every check, in that order
     */
    public List<String> findAllMismatches() {

        return Stream.of(
                findDeclaredLocalesWithoutBundleDirectory(),
                findUndeclaredBundleDirectories(),
                findMissingBundleFiles(),
                findStringsKeyMismatches(),
                findBlankStrings(),
                findFormatArgumentMismatches(),
                findLocalesMissingCoreLocalisation(),
                findSettingsRowMismatches(),
                findSettingsBehaviourMismatches(),
                findSettingsTabMismatches(),
                findUnhighlightedSettingsRuns(),
                findModInfoFragmentMismatches(),
                findChangelogMismatches())
            .flatMap(List::stream)
            .toList();
    }

    /**
     * The strings that are present but blank, in any locale including the default. The key check passes
     * them, and the game draws each one as its fallback sentinel - so a sentence a notice needs reads as
     * {@code [REDACTED]} to the player in that locale only.
     *
     * @return one finding per such string
     */
    public List<String> findBlankStrings() {
        return stringsParity.findBlankStrings();
    }

    /**
     * The translated locales whose changelog is missing or does not match the mod's own point for point.
     * Every locale but the default carries a full translation, which its zip and its release notes show.
     * This finds a translation that fell behind: a missing version, an extra section, a dropped point, or
     * a point whose identifiers changed in one file only. Prose is not compared; code spans are, since a
     * translation keeps them verbatim. A mod with no changelog is asked for no translation.
     *
     * @return one finding per missing translation, and one per difference in a translation's outline
     */
    public List<String> findChangelogMismatches() {
        return changelogParity.findChangelogMismatches();
    }

    /**
     * The declared locales with no bundle directory, which would ship nothing when built.
     *
     * @return one finding per such locale
     */
    public List<String> findDeclaredLocalesWithoutBundleDirectory() {
        return bundleLayoutParity.findDeclaredLocalesWithoutBundleDirectory();
    }

    /**
     * The strings whose slots take different arguments in a locale than in the default. One call site
     * fills every locale's wording, so a slot dropped or retyped in a translation renders as the fallback
     * sentinel or with a figure missing, and only in that locale. Slots may be reordered, a translation
     * numbering them where its sentence runs the other way.
     *
     * @return one finding per such string
     */
    public List<String> findFormatArgumentMismatches() {
        return stringsParity.findFormatArgumentMismatches();
    }

    /**
     * The locales drawing characters the vanilla atlases lack while naming no core localisation to supply
     * them. Such a locale draws its text as a row of fallback glyphs with no error anywhere.
     *
     * @return one finding per such locale and file
     */
    public List<String> findLocalesMissingCoreLocalisation() {
        return coreLocalisationParity.findLocalesMissingCoreLocalisation();
    }

    /**
     * The mapped files a declared locale's bundle directory does not hold. Writing that locale would fail,
     * and nothing true stands behind the gap: the previous locale's copy would ship as this one's.
     *
     * @return one finding per missing file
     */
    public List<String> findMissingBundleFiles() {
        return bundleLayoutParity.findMissingBundleFiles();
    }

    /**
     * The launcher fragments that cannot be merged over the base: one naming a dependency the base does
     * not declare, which would name a mod the launcher never lists, and one where the mod commits no base
     * at all, whose text would never reach the launcher. A fragment carrying a field no locale may vary is
     * refused on the read itself, before it can be compared.
     *
     * @return one finding per such fragment or dependency
     */
    public List<String> findModInfoFragmentMismatches() {
        return modInfoParity.findModInfoFragmentMismatches();
    }

    /**
     * The settings rows that behave differently in a locale than in the default: another type, default,
     * option list or bound. These decide what is stored and how, so a locale varying one ships a
     * different setting under the same ID. A Radio's options above all: LunaLib stores the label of the
     * option picked rather than its position, so a translated option list would reset that setting for
     * every player moving between builds.
     *
     * @return one finding per differing column of such a row
     */
    public List<String> findSettingsBehaviourMismatches() {
        return settingsParity.findSettingsBehaviourMismatches();
    }

    /**
     * The settings rows a locale lacks, adds, or places in another order than the default. A missing row
     * is a setting the code reads and falls back from, an added one a setting nothing reads, and the order
     * is what binds a row to the section heading it sits under. Order is only compared once the rows
     * agree, a missing row shifting every position after it.
     *
     * @return one finding per missing or added row, or one naming the first row out of place
     */
    public List<String> findSettingsRowMismatches() {
        return settingsParity.findSettingsRowMismatches();
    }

    /**
     * The tabs a locale splits or merges. Tab names are translated, but rows sharing a tab in the default
     * must share one in every locale and rows on different tabs must stay apart, so a translation renames
     * a tab rather than redrawing the screen. Each tab of the default maps to exactly one tab of the
     * locale, and the other way round.
     *
     * @return one finding per split or merged tab
     */
    public List<String> findSettingsTabMismatches() {
        return settingsParity.findSettingsTabMismatches();
    }

    /**
     * The strings a locale lacks or adds against the default. A missing key draws as the fallback
     * sentinel in that locale, with nothing to fall back to; an added one is wording nothing draws.
     *
     * @return one finding per missing or added string, named {@code <category> > <key>}
     */
    public List<String> findStringsKeyMismatches() {
        return stringsParity.findStringsKeyMismatches();
    }

    /**
     * The bundle directories the manifest does not declare. Nothing builds them and nothing checks them,
     * so a translation kept in one is silently never shipped - a locale added on disk and never declared,
     * or a directory cased {@code zh-Hans} where the manifest declares {@code zh-hans}.
     *
     * @return one finding per such directory
     */
    public List<String> findUndeclaredBundleDirectories() {
        return bundleLayoutParity.findUndeclaredBundleDirectories();
    }

    /**
     * The bracketed runs in any locale's settings table that the game would leave plain. LunaLib asks the
     * game to highlight each run, and the game highlights one only when the character on each side is
     * whitespace or ASCII punctuation. A run beside a CJK character or full-width punctuation such as a
     * Chinese full stop is drawn plain, with no error. Every locale is checked, the default included.
     *
     * @return one finding per run left plain
     */
    public List<String> findUnhighlightedSettingsRuns() {
        return settingsHighlightParity.findUnhighlightedSettingsRuns();
    }

    /**
     * The launcher fields each locale leaves to the base, so a mod's author sees the choice being made
     * without being blocked by it. Empty where the mod commits no base, nothing then being merged.
     *
     * @return the falling-back fields of every declared locale, by tag, as
     *         {@link ModInfoFragment#listFallbackFieldNames} names them
     */
    public Map<String, List<String>> listModInfoFallbackFieldNames() {
        return modInfoParity.listModInfoFallbackFieldNames();
    }
}
