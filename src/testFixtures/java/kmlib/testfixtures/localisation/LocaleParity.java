package kmlib.testfixtures.localisation;

import kmlib.testfixtures.starsector.strings.StringTemplates;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import static kmlib.testfixtures.starsector.json.ShippedJson.locateMember;

/**
 * Holds every locale of a mod to its default locale, file by file - the checks a mod runs over its
 * {@code localisation/} directory.
 *
 * <p>Strict where a gap has nothing true behind it, and reporting where it has. A strings key or a
 * settings row one locale lacks has no fallback - the player sees {@code [REDACTED]}, or a row missing
 * from the screen - so every such gap is a finding. A launcher field a fragment leaves out shows the base
 * file's wording, which is degraded rather than broken and often the right choice, so it is listed rather
 * than found.
 *
 * <p>What a row or a string does is held identical across locales; only what it says may vary. A Radio's
 * options above all: LunaLib stores the label of the option picked rather than its position, so a
 * translated option list would reset that setting for every player moving between builds.
 *
 * <p>Every locale is compared with the default rather than with each other: the default is the locale a
 * mod is authored in, so a finding names the locale to fix and the wording to fix it against.
 *
 * <p>Each defect is found once. A locale lacking a bundle directory or a mapped file is found by the
 * reading about that, and the comparisons over the file leave the locale out rather than failing on the
 * read; where the default itself lacks a file, nothing is compared over it.
 *
 * <p>Findings come back as sentences, by locale, each opening with the locale's tag - so a check asserts
 * a list empty and a failure reads as the edit to make.
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

        var findings = new ArrayList<String>();
        var manifest = directory.readManifest();

        pairBundlesHolding(manifest, LocaleBundle.STRINGS_FILE_NAME).ifPresent(pairing -> {

            var referenceTag = pairing.referenceBundle().getLocale().localeTag();
            var referenceWordings = flattenStrings(pairing.referenceBundle().readStrings());

            for (var bundle : pairing.comparedBundles()) {

                var localeTag = bundle.getLocale().localeTag();

                flattenStrings(bundle.readStrings()).forEach((stringKey, wording) -> {

                    var referenceWording = referenceWordings.get(stringKey);

                    if (referenceWording == null) {
                        return;
                    }
                    var conversions = StringTemplates.readArgumentConversions(wording);
                    var referenceConversions = StringTemplates.readArgumentConversions(referenceWording);

                    if (!conversions.equals(referenceConversions)) {

                        findings.add(localeTag + ": " + LocaleBundle.STRINGS_FILE_NAME + " " + stringKey
                            + " takes " + conversions + " where " + referenceTag + " takes " + referenceConversions);
                    }
                });
            }
        });
        return findings;
    }

    /**
     * The locales drawing characters the vanilla atlases lack while naming no core localisation to supply
     * them. Such a locale draws its text as a row of fallback glyphs with no error anywhere, and the
     * manifest is where the release body and the README learn what a player has to install.
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

            // A locale with no directory at all is found once, by the reading about that.
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
            var localeTag = locale.localeTag();

            if (base.isEmpty()) {

                if (Files.exists(bundle.resolveBundleFile(LocaleBundle.MOD_INFO_FILE_NAME))) {

                    findings.add(localeTag + ": " + LocaleBundle.MOD_INFO_FILE_NAME + " has no "
                        + LocalisationDirectory.MOD_INFO_BASE_FILE_NAME + " to be merged over");
                }
                continue;
            }
            var declaredDependencyIds = base.get().dependencyIds();

            for (var dependencyId : bundle.readModInfoFragment().dependencyNamesById().keySet()) {

                if (!declaredDependencyIds.contains(dependencyId)) {

                    findings.add(localeTag + ": " + LocaleBundle.MOD_INFO_FILE_NAME + " names dependency "
                        + dependencyId + ", which " + LocalisationDirectory.MOD_INFO_BASE_FILE_NAME
                        + " does not declare");
                }
            }
        }
        return findings;
    }

    /**
     * The settings rows that behave differently in a locale than in the default: another type, default,
     * option list or bound. These decide what is stored and how, so a locale varying one ships a
     * different setting under the same ID - a Radio's options above all, whose labels are what LunaLib
     * stores.
     *
     * @return one finding per such row
     */
    public List<String> findSettingsBehaviourMismatches() {

        var findings = new ArrayList<String>();
        var manifest = directory.readManifest();

        pairBundlesHolding(manifest, LocaleBundle.SETTINGS_FILE_NAME).ifPresent(pairing -> {

            var referenceTag = pairing.referenceBundle().getLocale().localeTag();
            var referenceBehaviours = pairing.referenceBundle()
                .openSettingsTable(settingsFieldIdPrefix)
                .readBehavioursByFieldId();

            for (var bundle : pairing.comparedBundles()) {

                var localeTag = bundle.getLocale().localeTag();
                var behaviours = bundle.openSettingsTable(settingsFieldIdPrefix).readBehavioursByFieldId();

                behaviours.forEach((fieldId, behaviour) -> {

                    var referenceBehaviour = referenceBehaviours.get(fieldId);

                    if (referenceBehaviour == null) {
                        return;
                    }
                    for (var difference : behaviour.describeDifferencesFrom(referenceBehaviour)) {

                        findings.add(localeTag + ": " + LocaleBundle.SETTINGS_FILE_NAME + " row " + fieldId
                            + " differs from " + referenceTag + " in " + difference);
                    }
                });
            }
        });
        return findings;
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

        var findings = new ArrayList<String>();
        var manifest = directory.readManifest();

        pairBundlesHolding(manifest, LocaleBundle.SETTINGS_FILE_NAME).ifPresent(pairing -> {

            var referenceTag = pairing.referenceBundle().getLocale().localeTag();
            var referenceFieldIds = pairing.referenceBundle()
                .openSettingsTable(settingsFieldIdPrefix)
                .readDeclaredFieldIds();

            for (var bundle : pairing.comparedBundles()) {

                var localeTag = bundle.getLocale().localeTag();
                var fieldIds = bundle.openSettingsTable(settingsFieldIdPrefix).readDeclaredFieldIds();
                var rowFindings = new ArrayList<String>();

                describeMissingAndAdded(
                    rowFindings,
                    localeTag + ": " + LocaleBundle.SETTINGS_FILE_NAME,
                    "row",
                    new TreeSet<>(fieldIds),
                    new TreeSet<>(referenceFieldIds),
                    referenceTag);

                if (rowFindings.isEmpty()) {
                    describeFirstMisplacedRow(rowFindings, localeTag, fieldIds, referenceFieldIds, referenceTag);
                }
                findings.addAll(rowFindings);
            }
        });
        return findings;
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

        var findings = new ArrayList<String>();
        var manifest = directory.readManifest();

        pairBundlesHolding(manifest, LocaleBundle.SETTINGS_FILE_NAME).ifPresent(pairing -> {

            var referenceTag = pairing.referenceBundle().getLocale().localeTag();
            var referenceTabsByFieldId = pairing.referenceBundle()
                .openSettingsTable(settingsFieldIdPrefix)
                .readTabsByFieldId();

            for (var bundle : pairing.comparedBundles()) {

                var localeTag = bundle.getLocale().localeTag();
                var tabsByFieldId = bundle.openSettingsTable(settingsFieldIdPrefix).readTabsByFieldId();
                var tabsByReferenceTab = new TreeMap<String, SortedSet<String>>();
                var referenceTabsByTab = new TreeMap<String, SortedSet<String>>();

                referenceTabsByFieldId.forEach((fieldId, referenceTab) -> {

                    var tab = tabsByFieldId.get(fieldId);

                    // A row the locale lacks is found by the row comparison.
                    if (tab == null) {
                        return;
                    }
                    tabsByReferenceTab.computeIfAbsent(referenceTab, ignored -> new TreeSet<>()).add(tab);
                    referenceTabsByTab.computeIfAbsent(tab, ignored -> new TreeSet<>()).add(referenceTab);
                });
                var filePrefix = localeTag + ": " + LocaleBundle.SETTINGS_FILE_NAME;

                tabsByReferenceTab.forEach((referenceTab, tabs) -> {

                    if (tabs.size() > 1) {
                        findings.add(filePrefix + " splits " + referenceTag + " tab " + referenceTab
                            + " across tabs " + tabs);
                    }
                });
                referenceTabsByTab.forEach((tab, referenceTabs) -> {

                    if (referenceTabs.size() > 1) {
                        findings.add(filePrefix + " merges " + referenceTag + " tabs " + referenceTabs
                            + " into tab " + tab);
                    }
                });
            }
        });
        return findings;
    }

    /**
     * The strings a locale lacks or adds against the default. A missing key draws as the fallback
     * sentinel in that locale, with nothing to fall back to; an added one is wording nothing draws.
     *
     * @return one finding per missing or added string, named {@code <category> > <key>}
     */
    public List<String> findStringsKeyMismatches() {

        var findings = new ArrayList<String>();
        var manifest = directory.readManifest();

        pairBundlesHolding(manifest, LocaleBundle.STRINGS_FILE_NAME).ifPresent(pairing -> {

            var referenceTag = pairing.referenceBundle().getLocale().localeTag();
            var referenceKeys = flattenStrings(pairing.referenceBundle().readStrings()).keySet();

            for (var bundle : pairing.comparedBundles()) {

                describeMissingAndAdded(
                    findings,
                    bundle.getLocale().localeTag() + ": " + LocaleBundle.STRINGS_FILE_NAME,
                    "string",
                    flattenStrings(bundle.readStrings()).keySet(),
                    referenceKeys,
                    referenceTag);
            }
        });
        return findings;
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

    // The first position where a locale's rows stand in another order than the default's, reported alone:
    // every position after it is likely out of place for the same reason. Both hold the same IDs by now,
    // so lists of different lengths mean one declares a row twice.
    private static void describeFirstMisplacedRow(
            List<String> findings,
            String localeTag,
            List<String> fieldIds,
            List<String> referenceFieldIds,
            String referenceTag) {

        var filePrefix = localeTag + ": " + LocaleBundle.SETTINGS_FILE_NAME;

        for (var index = 0; index < Math.min(fieldIds.size(), referenceFieldIds.size()); index++) {

            var fieldId = fieldIds.get(index);
            var referenceFieldId = referenceFieldIds.get(index);

            if (!fieldId.equals(referenceFieldId)) {

                findings.add(filePrefix + " places " + fieldId + " at row " + (index + 1)
                    + ", where " + referenceTag + " places " + referenceFieldId);
                return;
            }
        }
        if (fieldIds.size() != referenceFieldIds.size()) {

            findings.add(filePrefix + " declares " + fieldIds.size() + " rows, where " + referenceTag
                + " declares " + referenceFieldIds.size());
        }
    }

    private static void describeMissingAndAdded(
            List<String> findings,
            String filePrefix,
            String itemNoun,
            Collection<String> items,
            Collection<String> referenceItems,
            String referenceTag) {

        referenceItems.stream()
            .filter(item -> !items.contains(item))
            .forEach(item -> findings.add(
                filePrefix + " lacks " + itemNoun + " " + item + ", which " + referenceTag + " declares"));

        items.stream()
            .filter(item -> !referenceItems.contains(item))
            .forEach(item -> findings.add(
                filePrefix + " declares " + itemNoun + " " + item + ", which " + referenceTag + " does not"));
    }

    // Names each string by category and key together, the pair being what identifies it: one key may
    // stand in two categories.
    private static TreeMap<String, String> flattenStrings(Map<String, Map<String, String>> stringsByCategory) {

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

    // The default locale's bundle to compare against, and every other bundle holding the file. Nothing
    // where the manifest maps no such file or the default holds none, there then being nothing to compare
    // with.
    private Optional<BundlePairing> pairBundlesHolding(LocaleManifest manifest, String bundleFileName) {

        var referenceBundle = directory.openBundle(manifest.getDefaultLocale());

        if (!isFileHeld(manifest, referenceBundle, bundleFileName)) {
            return Optional.empty();
        }
        var comparedBundles = manifest.declaredLocalesByTag()
            .values()
            .stream()
            .filter(locale -> !locale.equals(manifest.getDefaultLocale()))
            .map(directory::openBundle)
            .filter(bundle -> isFileHeld(manifest, bundle, bundleFileName))
            .toList();

        return Optional.of(new BundlePairing(referenceBundle, comparedBundles));
    }

    /**
     * The bundles one comparison runs over.
     *
     * @param referenceBundle the default locale's bundle, which every other is held to
     * @param comparedBundles every other declared locale's bundle holding the file
     */
    private record BundlePairing(
        LocaleBundle referenceBundle,
        List<LocaleBundle> comparedBundles) {
    }
}
