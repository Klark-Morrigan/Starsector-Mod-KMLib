package kmlib.testfixtures.localisation;

import kmlib.testfixtures.starsector.settings.LunaSettingsTable.FieldBehaviour;
import kmlib.testfixtures.starsector.strings.StringTemplates;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;

import static kmlib.testfixtures.starsector.json.ShippedJson.locateMember;

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
 */
public final class LocaleParity {

    // The last character the vanilla atlases can be relied on for. Anything past it draws as the fallback
    // glyph unless a core localisation has replaced the atlases.
    private static final int LATIN_1_LAST_CODE_POINT = 0xFF;

    private final LocalisationDirectory directory;
    private final String settingsFieldIdPrefix;

    /**
     * Opens a comparison over one mod's localisation directory.
     *
     * @param directory             the directory to compare the locales of
     * @param settingsFieldIdPrefix what every one of the mod's settings field IDs starts with; unread for a
     *                              mod whose manifest maps no settings table
     */
    public LocaleParity(LocalisationDirectory directory, String settingsFieldIdPrefix) {

        this.directory = Objects.requireNonNull(directory, "directory");
        this.settingsFieldIdPrefix = Objects.requireNonNull(settingsFieldIdPrefix, "settingsFieldIdPrefix");
    }

    /**
     * The declared locales with no bundle directory, which would ship nothing when built.
     *
     * @return one finding per such locale
     */
    public List<String> findDeclaredLocalesWithoutBundleDirectory() {

        var bundleDirectoryNames = directory.listBundleDirectoryNames();

        return directory
            .readManifest()
            .declaredLocalesByTag()
            .keySet()
            .stream()
            .filter(localeTag -> !bundleDirectoryNames.contains(localeTag))
            .map(localeTag -> localeTag + ": declared by the manifest, but has no bundle directory")
            .toList();
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

        return compareWithDefault(
            LocaleBundle.STRINGS_FILE_NAME,
            bundle -> flattenStrings(bundle.readStrings()),
            LocaleParity::describeFormatArgumentMismatches);
    }

    /**
     * The locales drawing characters the vanilla atlases lack while naming no core localisation to supply
     * them. Such a locale draws its text as a row of fallback glyphs with no error anywhere.
     *
     * @return one finding per such locale and file
     */
    public List<String> findLocalesMissingCoreLocalisation() {

        var findings = new ArrayList<String>();
        var manifest = directory.readManifest();

        for (var locale : manifest.declaredLocalesByTag().values()) {

            if (locale.coreLocalisation().isPresent()) {
                continue;
            }
            var bundle = directory.openBundle(locale);
            var displayedTextsByFileName = new TreeMap<String, Collection<String>>();

            if (isFileHeld(manifest, bundle, LocaleBundle.STRINGS_FILE_NAME)) {

                displayedTextsByFileName.put(
                    LocaleBundle.STRINGS_FILE_NAME,
                    flattenStrings(bundle.readStrings()).values());
            }
            if (isFileHeld(manifest, bundle, LocaleBundle.SETTINGS_FILE_NAME)) {

                displayedTextsByFileName.put(
                    LocaleBundle.SETTINGS_FILE_NAME,
                    bundle.openSettingsTable(settingsFieldIdPrefix).readDisplayedTexts());
            }
            displayedTextsByFileName.forEach((fileName, displayedTexts) -> {

                if (displayedTexts.stream().anyMatch(LocaleParity::hasCharacterOutsideLatin1)) {

                    findings.add(locale.localeTag() + ": " + fileName
                        + " draws characters outside Latin-1, but the manifest names no coreLocalisation"
                        + " for the locale");
                }
            });
        }
        return findings;
    }

    /**
     * The mapped files a declared locale's bundle directory does not hold. Writing that locale would fail,
     * and nothing true stands behind the gap: the previous locale's copy would ship as this one's.
     *
     * @return one finding per missing file
     */
    public List<String> findMissingBundleFiles() {

        var findings = new ArrayList<String>();
        var manifest = directory.readManifest();
        var bundleDirectoryNames = directory.listBundleDirectoryNames();

        for (var locale : manifest.declaredLocalesByTag().values()) {

            // A locale with no directory at all is found once, by the check about that.
            if (!bundleDirectoryNames.contains(locale.localeTag())) {
                continue;
            }
            var bundle = directory.openBundle(locale);

            for (var bundleFileName : manifest.dataPathsByBundleFileName().keySet()) {

                if (!Files.isRegularFile(bundle.resolveBundleFile(bundleFileName))) {

                    findings.add(locale.localeTag() + ": holds no " + bundleFileName
                        + ", which the manifest maps");
                }
            }
        }
        return findings;
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

        var findings = new ArrayList<String>();
        var base = directory.readModInfoBase();

        for (var locale : directory.readManifest().declaredLocalesByTag().values()) {

            var bundle = directory.openBundle(locale);
            var filePrefix = locale.localeTag() + ": " + LocaleBundle.MOD_INFO_FILE_NAME;

            if (base.isEmpty()) {

                if (Files.exists(bundle.resolveBundleFile(LocaleBundle.MOD_INFO_FILE_NAME))) {

                    findings.add(filePrefix + " has no " + LocalisationDirectory.MOD_INFO_BASE_FILE_NAME
                        + " to be merged over");
                }
                continue;
            }
            var declaredDependencyIds = base.get().dependencyIds();

            for (var dependencyId : bundle.readModInfoFragment().dependencyNamesById().keySet()) {

                if (!declaredDependencyIds.contains(dependencyId)) {

                    findings.add(filePrefix + " names dependency " + dependencyId + ", which "
                        + LocalisationDirectory.MOD_INFO_BASE_FILE_NAME + " does not declare");
                }
            }
        }
        return findings;
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

        return compareWithDefault(
            LocaleBundle.SETTINGS_FILE_NAME,
            bundle -> bundle.openSettingsTable(settingsFieldIdPrefix).readBehavioursByFieldId(),
            LocaleParity::describeBehaviourMismatches);
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

        return compareWithDefault(
            LocaleBundle.SETTINGS_FILE_NAME,
            bundle -> bundle.openSettingsTable(settingsFieldIdPrefix).readDeclaredFieldIds(),
            LocaleParity::describeRowMismatches);
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

        return compareWithDefault(
            LocaleBundle.SETTINGS_FILE_NAME,
            bundle -> bundle.openSettingsTable(settingsFieldIdPrefix).readTabsByFieldId(),
            LocaleParity::describeTabMismatches);
    }

    /**
     * The strings a locale lacks or adds against the default. A missing key draws as the fallback
     * sentinel in that locale, with nothing to fall back to; an added one is wording nothing draws.
     *
     * @return one finding per missing or added string, named {@code <category> > <key>}
     */
    public List<String> findStringsKeyMismatches() {

        return compareWithDefault(
            LocaleBundle.STRINGS_FILE_NAME,
            bundle -> flattenStrings(bundle.readStrings()).keySet(),
            readings -> describeMissingAndAdded(readings, "string"));
    }

    /**
     * The bundle directories the manifest does not declare. Nothing builds them and nothing checks them,
     * so a translation kept in one is silently never shipped - a locale added on disk and never declared,
     * or a directory cased {@code zh-Hans} where the manifest declares {@code zh-hans}.
     *
     * @return one finding per such directory
     */
    public List<String> findUndeclaredBundleDirectories() {

        var declaredLocaleTags = directory.readManifest().declaredLocalesByTag().keySet();

        return directory.listBundleDirectoryNames()
            .stream()
            .filter(directoryName -> !declaredLocaleTags.contains(directoryName))
            .map(directoryName -> directoryName + ": a bundle directory the manifest does not declare")
            .toList();
    }

    /**
     * The launcher fields each locale leaves to the base, so a mod's author sees the choice being made
     * without being blocked by it. Empty where the mod commits no base, nothing then being merged.
     *
     * @return the falling-back fields of every declared locale, by tag, as
     *         {@link ModInfoFragment#listFallbackFieldNames} names them
     */
    public Map<String, List<String>> listModInfoFallbackFieldNames() {

        var fallbackFieldNamesByTag = new TreeMap<String, List<String>>();
        var base = directory.readModInfoBase();

        if (base.isEmpty()) {
            return fallbackFieldNamesByTag;
        }
        for (var locale : directory.readManifest().declaredLocalesByTag().values()) {

            fallbackFieldNamesByTag.put(
                locale.localeTag(),
                directory.openBundle(locale).readModInfoFragment().listFallbackFieldNames(base.get()));
        }
        return fallbackFieldNamesByTag;
    }

    // Rows present in only one of the two are the row comparison's; only a row both declare is compared.
    private static List<String> describeBehaviourMismatches(ComparedReadings<Map<String, FieldBehaviour>> readings) {

        var findings = new ArrayList<String>();

        readings.reading().forEach((fieldId, behaviour) -> {

            var referenceBehaviour = readings.referenceReading().get(fieldId);

            if (referenceBehaviour != null) {

                behaviour.describeDifferencesFrom(referenceBehaviour).forEach(difference -> findings.add(
                    readings.describeFinding("row " + fieldId + " differs from " + readings.referenceTag()
                        + " in " + difference)));
            }
        });
        return findings;
    }

    // The first position where a locale's rows stand in another order than the default's, reported alone:
    // every position after it is likely out of place for the same reason. Both hold the same IDs by now,
    // so lists of different lengths mean one declares a row twice.
    private static List<String> describeFirstMisplacedRow(ComparedReadings<List<String>> readings) {

        var fieldIds = readings.reading();
        var referenceFieldIds = readings.referenceReading();

        for (var index = 0; index < Math.min(fieldIds.size(), referenceFieldIds.size()); index++) {

            var fieldId = fieldIds.get(index);
            var referenceFieldId = referenceFieldIds.get(index);

            if (!fieldId.equals(referenceFieldId)) {

                return List.of(readings.describeFinding("places " + fieldId + " at row " + (index + 1)
                    + ", where " + readings.referenceTag() + " places " + referenceFieldId));
            }
        }
        if (fieldIds.size() != referenceFieldIds.size()) {

            return List.of(readings.describeFinding("declares " + fieldIds.size() + " rows, where "
                + readings.referenceTag() + " declares " + referenceFieldIds.size()));
        }
        return List.of();
    }

    // Strings present in only one of the two are the key comparison's; only a string both declare is
    // compared.
    private static List<String> describeFormatArgumentMismatches(ComparedReadings<SortedMap<String, String>> readings) {

        var findings = new ArrayList<String>();

        readings.reading().forEach((stringKey, wording) -> {

            var referenceWording = readings.referenceReading().get(stringKey);

            if (referenceWording == null) {
                return;
            }
            var conversions = StringTemplates.readArgumentConversions(wording);
            var referenceConversions = StringTemplates.readArgumentConversions(referenceWording);

            if (!conversions.equals(referenceConversions)) {

                findings.add(readings.describeFinding(stringKey + " takes " + conversions
                    + " where " + readings.referenceTag() + " takes " + referenceConversions));
            }
        });
        return findings;
    }

    private static List<String> describeMissingAndAdded(
            ComparedReadings<? extends Collection<String>> readings,
            String itemNoun) {

        var items = readings.reading();
        var referenceItems = readings.referenceReading();
        var findings = new ArrayList<String>();

        referenceItems.stream()
            .filter(item -> !items.contains(item))
            .forEach(item -> findings.add(readings.describeFinding(
                "lacks " + itemNoun + " " + item + ", which " + readings.referenceTag() + " declares")));

        items.stream()
            .filter(item -> !referenceItems.contains(item))
            .forEach(item -> findings.add(readings.describeFinding(
                "declares " + itemNoun + " " + item + ", which " + readings.referenceTag() + " does not")));

        return findings;
    }

    private static List<String> describeRowMismatches(ComparedReadings<List<String>> readings) {

        var findings = describeMissingAndAdded(readings, "row");

        return findings.isEmpty()
            ? describeFirstMisplacedRow(readings)
            : findings;
    }

    // Walks the rows both declare, mapping each tab of the default to the tabs its rows land on in the
    // locale and back; more than one either way is a split or a merge.
    private static List<String> describeTabMismatches(ComparedReadings<Map<String, String>> readings) {

        var tabsByReferenceTab = new TreeMap<String, SortedSet<String>>();
        var referenceTabsByTab = new TreeMap<String, SortedSet<String>>();

        readings.referenceReading().forEach((fieldId, referenceTab) -> {

            var tab = readings.reading().get(fieldId);

            if (tab != null) {

                tabsByReferenceTab.computeIfAbsent(referenceTab, ignored -> new TreeSet<>()).add(tab);
                referenceTabsByTab.computeIfAbsent(tab, ignored -> new TreeSet<>()).add(referenceTab);
            }
        });
        var findings = new ArrayList<String>();

        tabsByReferenceTab.forEach((referenceTab, tabs) -> {

            if (tabs.size() > 1) {
                findings.add(readings.describeFinding("splits " + readings.referenceTag() + " tab " + referenceTab
                    + " across tabs " + tabs));
            }
        });
        referenceTabsByTab.forEach((tab, referenceTabs) -> {

            if (referenceTabs.size() > 1) {
                findings.add(readings.describeFinding("merges " + readings.referenceTag() + " tabs " + referenceTabs
                    + " into tab " + tab));
            }
        });
        return findings;
    }

    // Names each string by category and key together, the pair being what identifies it: one key may
    // stand in two categories.
    private static SortedMap<String, String> flattenStrings(Map<String, Map<String, String>> stringsByCategory) {

        var wordingsByStringKey = new TreeMap<String, String>();

        stringsByCategory.forEach((category, wordingsByKey) ->
            wordingsByKey.forEach((key, wording) -> wordingsByStringKey.put(locateMember(category, key), wording)));

        return wordingsByStringKey;
    }

    private static boolean hasCharacterOutsideLatin1(String text) {
        return text.codePoints().anyMatch(codePoint -> codePoint > LATIN_1_LAST_CODE_POINT);
    }

    // Whether the manifest maps a file and a bundle holds it: the one condition under which a comparison
    // reads it, a gap in either being found elsewhere.
    private static boolean isFileHeld(LocaleManifest manifest, LocaleBundle bundle, String bundleFileName) {

        return manifest.dataPathsByBundleFileName().containsKey(bundleFileName)
            && Files.isRegularFile(bundle.resolveBundleFile(bundleFileName));
    }

    // Reads one file out of the default locale's bundle and out of every other bundle holding it, and
    // hands each locale's reading beside the default's to the comparison. Nothing where the manifest maps
    // no such file or the default holds none, there then being nothing to compare with.
    private <T> List<String> compareWithDefault(
            String bundleFileName,
            Function<LocaleBundle, T> readBundleFile,
            Function<ComparedReadings<T>, List<String>> describeMismatches) {

        var manifest = directory.readManifest();
        var defaultLocale = manifest.getDefaultLocale();
        var referenceBundle = directory.openBundle(defaultLocale);

        if (!isFileHeld(manifest, referenceBundle, bundleFileName)) {
            return List.of();
        }
        var referenceReading = readBundleFile.apply(referenceBundle);
        var findings = new ArrayList<String>();

        for (var locale : manifest.declaredLocalesByTag().values()) {

            var bundle = directory.openBundle(locale);

            if (locale.equals(defaultLocale) || !isFileHeld(manifest, bundle, bundleFileName)) {
                continue;
            }
            var readings = new ComparedReadings<>(
                bundleFileName,
                locale.localeTag(),
                readBundleFile.apply(bundle),
                defaultLocale.localeTag(),
                referenceReading);

            findings.addAll(describeMismatches.apply(readings));
        }
        return findings;
    }

    /**
     * One locale's reading of a bundle file beside the default locale's reading of the same file.
     *
     * @param bundleFileName   the file both were read from
     * @param localeTag        the locale compared
     * @param reading          what that locale's file holds
     * @param referenceTag     the default locale, which the other is held to
     * @param referenceReading what the default's file holds
     * @param <T>              the reading's shape
     */
    private record ComparedReadings<T>(
        String bundleFileName,
        String localeTag,
        T reading,
        String referenceTag,
        T referenceReading) {

        // Every finding over a file opens by naming the locale and the file, so it reads as where to look.
        String describeFinding(String findingText) {
            return localeTag + ": " + bundleFileName + " " + findingText;
        }
    }
}
