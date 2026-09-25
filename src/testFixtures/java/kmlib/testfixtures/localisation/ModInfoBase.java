package kmlib.testfixtures.localisation;

import kmlib.testfixtures.starsector.json.ShippedJson;

import java.nio.file.Path;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import static kmlib.testfixtures.starsector.json.ShippedJson.locateElement;
import static kmlib.testfixtures.starsector.json.ShippedJson.locateMember;
import static kmlib.testfixtures.starsector.json.ShippedJson.requireList;
import static kmlib.testfixtures.starsector.json.ShippedJson.requireObject;
import static kmlib.testfixtures.starsector.json.ShippedJson.requireString;

/**
 * The committed {@code mod_info.base.json} every locale's launcher fragment is merged over, as far as a
 * fragment reaches into it: which of the translatable text fields it carries, and the IDs of the
 * dependencies it declares.
 *
 * <p>The rest of the file - the version, the jar list, the plugin class - is the launcher's business and
 * the build's, and no fragment may touch it, so it is not read here.
 *
 * <p>Both sets are sorted, the parser keeping no member order.
 *
 * @param translatableFieldNames the text fields a fragment may translate that the base carries, and so
 *                               falls back to where a fragment leaves them out
 * @param dependencyIds          the mod IDs the base declares as dependencies, the only IDs a fragment
 *                               may name
 */
public record ModInfoBase(
    Set<String> translatableFieldNames,
    Set<String> dependencyIds) {

    // How the launcher file names a dependency, as against the display name a fragment translates.
    private static final String DEPENDENCY_ID_KEY = "id";

    /**
     * Holds both sets sorted, however they were built, and the field names to those a fragment can
     * translate: a fallback is reported per field, and a name no fragment could carry has none.
     *
     * @param translatableFieldNames see the record
     * @param dependencyIds          see the record
     */
    public ModInfoBase {

        Objects.requireNonNull(translatableFieldNames, "translatableFieldNames");
        Objects.requireNonNull(dependencyIds, "dependencyIds");

        for (var fieldName : translatableFieldNames) {

            if (!ModInfoFragment.TRANSLATABLE_TEXT_FIELD_NAMES.contains(fieldName)) {

                throw new IllegalArgumentException(
                    fieldName + " is not a launcher text field; they are "
                        + new TreeSet<>(ModInfoFragment.TRANSLATABLE_TEXT_FIELD_NAMES));
            }
        }

        translatableFieldNames = Collections.unmodifiableSet(new TreeSet<>(translatableFieldNames));
        dependencyIds = Collections.unmodifiableSet(new TreeSet<>(dependencyIds));
    }

    /**
     * Reads a base file.
     *
     * @param baseFile the base to read
     * @return what a fragment merged over it can reach
     */
    public static ModInfoBase readBase(Path baseFile) {

        var location = baseFile.toString();
        var base = ShippedJson.readObjectFile(baseFile);
        var translatableFieldNames = new TreeSet<String>();

        for (var fieldName : ModInfoFragment.TRANSLATABLE_TEXT_FIELD_NAMES) {

            if (base.containsKey(fieldName)) {

                requireString(base.get(fieldName), locateMember(location, fieldName));
                translatableFieldNames.add(fieldName);
            }
        }
        var dependenciesValue = base.get(ModInfoFragment.DEPENDENCIES_KEY);
        var dependencyIds = dependenciesValue == null
            ? Set.<String>of()
            : readDependencyIds(dependenciesValue, locateMember(location, ModInfoFragment.DEPENDENCIES_KEY));

        return new ModInfoBase(translatableFieldNames, dependencyIds);
    }

    private static Set<String> readDependencyIds(Object dependenciesValue, String location) {

        var dependencyIds = new TreeSet<String>();
        var dependencies = requireList(dependenciesValue, location);

        for (var index = 0; index < dependencies.size(); index++) {

            var entryLocation = locateElement(location, index);
            var dependency = requireObject(dependencies.get(index), entryLocation);

            dependencyIds.add(requireString(
                dependency.get(DEPENDENCY_ID_KEY),
                locateMember(entryLocation, DEPENDENCY_ID_KEY)));
        }
        return dependencyIds;
    }
}
