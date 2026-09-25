package kmlib.testfixtures.localisation;

import kmlib.testfixtures.starsector.json.ShippedJson;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static kmlib.testfixtures.starsector.json.ShippedJson.locateMember;
import static kmlib.testfixtures.starsector.json.ShippedJson.requireObject;
import static kmlib.testfixtures.starsector.json.ShippedJson.requireOnlyKeys;
import static kmlib.testfixtures.starsector.json.ShippedJson.requireString;

/**
 * The launcher text one locale translates, merged over the mod's base {@code mod_info.json} when that
 * locale is built.
 *
 * <p>Only the four fields the launcher shows as text may appear. The rest of the launcher file is
 * functional - the ID, the version, the jar list, the dependency graph - and a fragment varying any of
 * them would build a different mod per locale, which is what splitting the file exists to prevent.
 * Every field is optional: one a locale leaves out shows the base file's text, which is degraded rather
 * than broken.
 *
 * <p>Dependencies are keyed by ID rather than listed, a locale having no reason to restate a
 * dependency's version.
 *
 * @param nameText            the mod's name as the launcher lists it
 * @param descriptionText     the launcher's description of the mod
 * @param authorText          the author line
 * @param dependencyNamesById each dependency's name, keyed by the mod ID the base file declares it by,
 *                            as the launcher shows it when that dependency is missing
 */
public record ModInfoFragment(
    Optional<String> nameText,
    Optional<String> descriptionText,
    Optional<String> authorText,
    Map<String, String> dependencyNamesById) {

    private static final String NAME_KEY = "name";
    private static final String DESCRIPTION_KEY = "description";
    private static final String AUTHOR_KEY = "author";

    // Where the launcher file lists dependencies, which a fragment keys by ID instead.
    static final String DEPENDENCIES_KEY = "dependencies";

    // The launcher text fields a fragment may carry, as the launcher file spells them. The base is read
    // for the same three, being what a fragment falls back to.
    static final Set<String> TRANSLATABLE_TEXT_FIELD_NAMES = Set.of(NAME_KEY, DESCRIPTION_KEY, AUTHOR_KEY);

    // Every launcher field a fragment may carry: the text fields, and the dependency names.
    private static final Set<String> TRANSLATABLE_FIELD_NAMES = Stream
        .concat(TRANSLATABLE_TEXT_FIELD_NAMES.stream(), Stream.of(DEPENDENCIES_KEY))
        .collect(Collectors.toUnmodifiableSet());

    /**
     * Refuses blank text, which the launcher would draw as an empty row rather than fall back from.
     *
     * @param nameText            see the record
     * @param descriptionText     see the record
     * @param authorText          see the record
     * @param dependencyNamesById see the record
     */
    public ModInfoFragment {

        Objects.requireNonNull(nameText, NAME_KEY);
        Objects.requireNonNull(descriptionText, DESCRIPTION_KEY);
        Objects.requireNonNull(authorText, AUTHOR_KEY);
        Objects.requireNonNull(dependencyNamesById, DEPENDENCIES_KEY);

        nameText.ifPresent(text -> requireNonBlankText(text, NAME_KEY));
        descriptionText.ifPresent(text -> requireNonBlankText(text, DESCRIPTION_KEY));
        authorText.ifPresent(text -> requireNonBlankText(text, AUTHOR_KEY));

        dependencyNamesById.forEach((dependencyId, dependencyName) ->
            requireNonBlankText(dependencyName, locateMember(DEPENDENCIES_KEY, dependencyId)));

        dependencyNamesById = Collections.unmodifiableMap(new TreeMap<>(dependencyNamesById));
    }

    /**
     * The fragment of a locale that translates no launcher field, so every field shows the base file's
     * text.
     *
     * @return that fragment
     */
    public static ModInfoFragment createUntranslatedFragment() {
        return new ModInfoFragment(Optional.empty(), Optional.empty(), Optional.empty(), Map.of());
    }

    /**
     * Each translatable field the base carries that this fragment leaves to it - which the launcher then
     * shows in the base file's wording. A choice rather than a defect: an author line or another mod's
     * name is often best left as written.
     *
     * @param base the base this fragment is merged over
     * @return the fields falling back, text fields by name and then dependencies as
     *         {@code dependencies > <id>}, each sorted
     */
    public List<String> listFallbackFieldNames(ModInfoBase base) {

        var fallbackFieldNames = new ArrayList<String>();

        for (var fieldName : base.translatableFieldNames()) {

            if (resolveText(fieldName).isEmpty()) {
                fallbackFieldNames.add(fieldName);
            }
        }
        for (var dependencyId : base.dependencyIds()) {

            if (!dependencyNamesById.containsKey(dependencyId)) {
                fallbackFieldNames.add(locateMember(DEPENDENCIES_KEY, dependencyId));
            }
        }
        return fallbackFieldNames;
    }

    /**
     * Reads a locale's fragment. A locale carrying no fragment file translates no launcher field.
     *
     * @param fragmentFile the fragment to read
     * @return what it translates
     */
    public static ModInfoFragment readFragment(Path fragmentFile) {

        if (!Files.exists(fragmentFile)) {
            return createUntranslatedFragment();
        }
        var location = fragmentFile.toString();
        var fragment = ShippedJson.readObjectFile(fragmentFile);

        requireOnlyKeys(fragment, TRANSLATABLE_FIELD_NAMES, location);

        var nameText = readOptionalText(fragment.get(NAME_KEY), locateMember(location, NAME_KEY));
        var descriptionText = readOptionalText(fragment.get(DESCRIPTION_KEY), locateMember(location, DESCRIPTION_KEY));
        var authorText = readOptionalText(fragment.get(AUTHOR_KEY), locateMember(location, AUTHOR_KEY));
        var dependencyNamesById = readDependencyNames(
            fragment.get(DEPENDENCIES_KEY),
            locateMember(location, DEPENDENCIES_KEY));

        return ShippedJson.constructValueAt(
            location,
            () -> new ModInfoFragment(nameText, descriptionText, authorText, dependencyNamesById));
    }

    private static Map<String, String> readDependencyNames(Object dependenciesValue, String location) {

        if (dependenciesValue == null) {
            return Map.of();
        }
        var dependencyNamesById = new TreeMap<String, String>();

        requireObject(dependenciesValue, location)
            .forEach((dependencyId, dependencyName) ->
                dependencyNamesById.put(
                    dependencyId,
                    requireString(dependencyName, locateMember(location, dependencyId))));

        return dependencyNamesById;
    }

    private static Optional<String> readOptionalText(Object value, String location) {

        return value == null
            ? Optional.empty()
            : Optional.of(requireString(value, location));
    }

    private static void requireNonBlankText(String text, String fieldName) {

        if (text.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is blank");
        }
    }

    private Optional<String> resolveText(String fieldName) {

        return switch (fieldName) {
            case NAME_KEY -> nameText;
            case DESCRIPTION_KEY -> descriptionText;
            case AUTHOR_KEY -> authorText;
            default -> throw new IllegalArgumentException(fieldName + " is not a launcher text field");
        };
    }
}
