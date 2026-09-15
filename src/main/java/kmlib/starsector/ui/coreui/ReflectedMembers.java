/*
 * Copyright Starficz
 * Copyright 2026 Klark Morrigan
 *
 * The search rules are derived from org.magiclib.ReflectionUtils in MagicLib,
 * and modified in 2026: rewritten in Java, restated as query records rather than
 * default-argument overloads, and extended with a hierarchy walk for methods a
 * superclass declares without publishing.
 *
 * Licensed under the GNU Lesser General Public License version 3. See LICENSE at
 * the repository root, and THIRD-PARTY-NOTICES.md for what this derives from.
 */
package kmlib.starsector.ui.coreui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Which members a shape has, and reaching one of them by name or by signature.
 *
 * <p>Two ways of naming what is wanted, because obfuscation takes one of them away. A member the
 * game publishes keeps its name across builds and is asked for by it; one it does not is renamed
 * with every build, and the only durable thing left about it is its signature - what it takes, what
 * it answers with, what type it is declared as. So every search here matches on any combination of
 * the two, an unstated filter meaning "any".
 *
 * <p>Answers are memoised per search. A shape's members do not change within a run, while a walk
 * asking the same question each frame would otherwise re-read the shape's whole member list per
 * node. The maps are bounded by the shapes met times the searches the code makes, both being
 * properties of the asking code rather than of the run.
 *
 * <p>Holds no way into reflection of its own - {@link ReflectionBypass} owns that, and owns nothing
 * else - so the rules here can be read, changed and tested without touching what the game's ban
 * covers.
 */
final class ReflectedMembers {

    // A shape's own declared members stop at Object: everything inherits its handful, none of them
    // is what a search is ever after, and including them would have every shape answer a nameless
    // search alike.
    private static final Class<?> HIERARCHY_ROOT = Object.class;

    private static final Map<MemberSearch, List<ReflectedConstructor>> CONSTRUCTORS_BY_SEARCH =
        new ConcurrentHashMap<>();

    private static final Map<MemberSearch, List<ReflectedField>> FIELDS_BY_SEARCH =
        new ConcurrentHashMap<>();

    private static final Map<MemberSearch, List<ReflectedMethod>> METHODS_BY_SEARCH =
        new ConcurrentHashMap<>();

    private ReflectedMembers() {
    }

    /**
     * Builds one, resolving the constructor from the arguments handed in.
     *
     * @param shape     the class to build
     * @param arguments what to pass, which also selects the constructor
     * @return what it built
     * @throws IllegalArgumentException when no constructor takes those arguments, or more than one
     *                                  does
     */
    static Object construct(Class<?> shape, Object... arguments) {

        var argumentTypes = ParameterCompatibility.readArgumentTypes(arguments);
        var matches = findConstructorsMatching(shape, ConstructorQuery.taking(argumentTypes));

        return resolveSoleMatch(matches, "constructor", shape, argumentTypes.toString())
            .newInstance(arguments);
    }

    /**
     * The constructors of a shape that fit the query, its own and not what it inherits - a
     * constructor being inherited by nothing.
     *
     * @param shape the class to look in
     * @param query what to match on
     * @return every constructor that fits, in declaration order
     */
    static List<ReflectedConstructor> findConstructorsMatching(
        Class<?> shape,
        ConstructorQuery query) {

        return CONSTRUCTORS_BY_SEARCH.computeIfAbsent(
            new MemberSearch(shape, query),
            search -> selectConstructors(search.shape(), query));
    }

    /**
     * The fields of a shape that fit the query.
     *
     * @param shape the class to look in
     * @param query what to match on
     * @return every field that fits, in declaration order, a superclass's after its subclass's
     */
    static List<ReflectedField> findFieldsMatching(Class<?> shape, FieldQuery query) {

        return FIELDS_BY_SEARCH.computeIfAbsent(
            new MemberSearch(shape, query),
            search -> selectFields(search.shape(), query));
    }

    /**
     * The fields of a shape whose own declared type offers a method fitting the query, for reaching
     * a member by what the thing it holds can do.
     *
     * <p>The way in when neither the field nor what it holds carries a usable name: an obfuscated
     * widget's parts are reached by recognising what they are capable of. Matched against the
     * field's declared type rather than against what it holds, so a field declared as
     * {@code Object} matches nothing however it was filled.
     *
     * @param shape               the class to look in
     * @param methodQuery         what the field's type has to offer
     * @param searchSuperclasses  whether to take the fields a superclass declares as well
     * @return every field that fits, in declaration order
     */
    static List<ReflectedField> findFieldsHoldingMethodMatching(
        Class<?> shape,
        MethodQuery methodQuery,
        boolean searchSuperclasses) {

        var fields = findFieldsMatching(shape, FieldQuery.anyField(searchSuperclasses));

        return fields.stream()
            .filter(field -> !findMethodsMatching(field.getType(), methodQuery).isEmpty())
            .toList();
    }

    /**
     * The methods of a shape that fit the query: what it declares itself, whatever the access
     * level, plus what it publishes through everything it extends.
     *
     * @param shape the class to look in
     * @param query what to match on
     * @return every method that fits
     */
    static List<ReflectedMethod> findMethodsMatching(Class<?> shape, MethodQuery query) {

        return METHODS_BY_SEARCH.computeIfAbsent(
            new MemberSearch(shape, query),
            search -> selectMethods(search.shape(), query));
    }

    /**
     * Whether the shape declares or inherits any method of this name, whatever it takes.
     *
     * <p>Answered without building a match, so the common case a walk meets - a leaf carrying no
     * such name - costs a scan rather than a thrown exception.
     *
     * @param shape      the class to look in
     * @param methodName the name to look for
     * @return whether anything of that name is there
     */
    static boolean hasMethodNamed(Class<?> shape, String methodName) {

        for (var method : readReachableMethodsOf(shape, false)) {
            if (methodName.equals(ReflectionBypass.readMethodName(method))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Resolves a method by name against the arguments handed in, and calls it on the object.
     *
     * <p>The arguments select the overload, matched by assignment compatibility rather than by
     * identity - so a name fitting more than one method is refused rather than resolved to one of
     * them, and a caller naming a member is asserting the name is unique on the target.
     *
     * @param instance   the object to call on
     * @param methodName the method to resolve
     * @param arguments  what to pass, which also selects the overload
     * @return whatever it answered, or null for a void one
     * @throws IllegalArgumentException when nothing of that name takes those arguments, or more
     *                                  than one thing does
     */
    static Object invokeByName(Object instance, String methodName, Object... arguments) {

        return resolveMethodFor(instance.getClass(), methodName, arguments)
            .invokeOn(instance, arguments);
    }

    /**
     * Resolves a static method by name against the arguments handed in, and calls it.
     *
     * @param shape      the class the method is declared on
     * @param methodName the method to resolve
     * @param arguments  what to pass, which also selects the overload
     * @return whatever it answered, or null for a void one
     * @throws IllegalArgumentException on the terms {@link #invokeByName} gives
     */
    static Object invokeStaticByName(Class<?> shape, String methodName, Object... arguments) {

        // No object to call on is what makes the call a static one.
        return resolveMethodFor(shape, methodName, arguments)
            .invokeOn(null, arguments);
    }

    /**
     * Reads the one field of the object that fits the query.
     *
     * @param instance the object to read off
     * @param query    what to match on, which has to identify a single field
     * @return what that field holds
     * @throws IllegalArgumentException when no field fits, or more than one does
     */
    static Object readFieldValue(Object instance, FieldQuery query) {

        return resolveSoleFieldFor(instance.getClass(), query)
            .readFrom(instance);
    }

    /**
     * Reads the one static field of the shape that fits the query.
     *
     * @param shape the class the field is declared on
     * @param query what to match on, which has to identify a single field
     * @return what that field holds
     * @throws IllegalArgumentException on the terms {@link #readFieldValue} gives
     */
    static Object readStaticFieldValue(Class<?> shape, FieldQuery query) {

        return resolveSoleFieldFor(shape, query)
            .readFrom(null);
    }

    /**
     * Writes the one field of the object that fits the query.
     *
     * @param instance the object to write on
     * @param query    what to match on, which has to identify a single field
     * @param value    what to put in it
     * @throws IllegalArgumentException when no field fits, or more than one does
     */
    static void writeFieldValue(Object instance, FieldQuery query, Object value) {

        resolveSoleFieldFor(instance.getClass(), query)
            .writeTo(instance, value);
    }

    /**
     * Writes the one static field of the shape that fits the query.
     *
     * @param shape the class the field is declared on
     * @param query what to match on, which has to identify a single field
     * @param value what to put in it
     * @throws IllegalArgumentException on the terms {@link #writeFieldValue} gives
     */
    static void writeStaticFieldValue(Class<?> shape, FieldQuery query, Object value) {

        resolveSoleFieldFor(shape, query)
            .writeTo(null, value);
    }

    private static ReflectedMethod resolveMethodFor(
        Class<?> shape,
        String methodName,
        Object[] arguments) {

        var argumentTypes = ParameterCompatibility.readArgumentTypes(arguments);
        var matches = findMethodsMatching(shape, MethodQuery.named(methodName)
            .takingArgumentTypes(argumentTypes));

        return resolveSoleMatch(
            matches, "method '" + methodName + "'", shape, argumentTypes.toString());
    }

    private static ReflectedField resolveSoleFieldFor(Class<?> shape, FieldQuery query) {
        return resolveSoleMatch(findFieldsMatching(shape, query), "field", shape, query.toString());
    }

    // The single member a caller asserted was there, or a refusal saying which way the assertion
    // failed. Both directions refuse rather than guess: nothing to call is unreachable, and more
    // than one means the caller's description stopped identifying a member - picking one of them
    // would make which it got turn on declaration order, which an obfuscated build reshuffles.
    private static <T> T resolveSoleMatch(
        List<T> matches,
        String soughtDescription,
        Class<?> shape,
        String queryDescription) {

        if (matches.isEmpty()) {
            throw new IllegalArgumentException("No " + soughtDescription + " on "
                + shape.getName() + " matches " + queryDescription + ".");
        }

        if (matches.size() > 1) {
            throw new IllegalArgumentException("More than one " + soughtDescription + " on "
                + shape.getName() + " matches " + queryDescription
                + ", so it does not identify one of them.");
        }
        return matches.get(0);
    }

    private static List<ReflectedConstructor> selectConstructors(
        Class<?> shape,
        ConstructorQuery query) {

        var matches = new ArrayList<ReflectedConstructor>();

        // Bare objects for the reason given on ReflectionBypass: the member types are the ones the
        // ban covers, where the array they arrive in is nothing the loader has to be asked about.
        Object[] declaredConstructors = shape.getDeclaredConstructors();

        for (var constructor : declaredConstructors) {

            var parameterTypes = ReflectionBypass.readConstructorParameterTypes(constructor);

            if (isParameterListMatching(parameterTypes, query.parameterCount(),
                query.parameterTypes())) {
                matches.add(new ReflectedConstructor(constructor));
            }
        }
        return List.copyOf(matches);
    }

    private static List<ReflectedField> selectFields(Class<?> shape, FieldQuery query) {

        var matches = new ArrayList<ReflectedField>();

        for (var field : readReachableFieldsOf(shape, query.searchSuperclasses())) {

            if (isFieldMatching(field, query)) {
                matches.add(new ReflectedField(field));
            }
        }
        return List.copyOf(matches);
    }

    private static List<ReflectedMethod> selectMethods(Class<?> shape, MethodQuery query) {

        var matches = new ArrayList<ReflectedMethod>();

        for (var method : readReachableMethodsOf(shape, query.searchSuperclasses())) {

            if (isMethodMatching(method, query)) {
                matches.add(new ReflectedMethod(method));
            }
        }
        return List.copyOf(matches);
    }

    private static boolean isFieldMatching(Object field, FieldQuery query) {

        if (query.name() != null
            && !query.name().equals(ReflectionBypass.readFieldName(field))) {
            return false;
        }

        if (!query.isTypeConstrained()) {
            return true;
        }

        var fieldType = ReflectionBypass.readFieldType(field);

        if (query.exactType() != null && !query.exactType().equals(fieldType)) {
            return false;
        }

        // Whether a value of the accepted type could be put in the field, and whether what the
        // field holds could be handed where the assignable type is expected. Opposite directions,
        // and a caller wanting one of them almost never wants the other.
        if (query.accepting() != null
            && !ParameterCompatibility.isParameterCompatible(fieldType, query.accepting())) {
            return false;
        }

        if (query.assignableTo() != null && !query.assignableTo().isAssignableFrom(fieldType)) {
            return false;
        }

        // A field declared as Object satisfies every type constraint there is, so a nameless search
        // by type would answer with every untyped field a shape carries on top of what it meant.
        // Kept only for a caller that asked for Object itself, which is the one case where those
        // fields are the answer rather than noise.
        return !isUntypedFieldExcludedBy(fieldType, query);
    }

    private static boolean isUntypedFieldExcludedBy(Class<?> fieldType, FieldQuery query) {

        return HIERARCHY_ROOT.equals(fieldType)
            && query.name() == null
            && !HIERARCHY_ROOT.equals(query.exactType())
            && !HIERARCHY_ROOT.equals(query.assignableTo())
            && !HIERARCHY_ROOT.equals(query.accepting());
    }

    private static boolean isMethodMatching(Object method, MethodQuery query) {

        if (query.name() != null
            && !query.name().equals(ReflectionBypass.readMethodName(method))) {
            return false;
        }

        // Assignable rather than equal, so a caller naming a supertype of what it actually wants
        // still reaches the member - the same leniency the parameter side is matched with.
        if (query.returnType() != null
            && !query.returnType().isAssignableFrom(ReflectionBypass.readMethodReturnType(method))) {
            return false;
        }

        if (query.parameterCount() == null && query.parameterTypes() == null) {
            return true;
        }

        return isParameterListMatching(
            ReflectionBypass.readMethodParameterTypes(method),
            query.parameterCount(),
            query.parameterTypes());
    }

    private static boolean isParameterListMatching(
        Class<?>[] parameterTypes,
        Integer requiredCount,
        List<Class<?>> requiredTypes) {

        if (requiredCount != null && requiredCount != parameterTypes.length) {
            return false;
        }

        return requiredTypes == null
            || ParameterCompatibility.isCallableWith(parameterTypes, requiredTypes);
    }

    // The fields a search runs over. A shape's own by default, since a field is reached where it is
    // declared and a subclass's own are what a caller naming that subclass meant; the hierarchy on
    // request, for the members the game declares on a base class several levels up.
    private static Set<Object> readReachableFieldsOf(Class<?> shape, boolean searchSuperclasses) {

        var reachableFields = new LinkedHashSet<>();

        for (var currentShape = shape;
             currentShape != null && !HIERARCHY_ROOT.equals(currentShape);
             currentShape = currentShape.getSuperclass()) {

            Object[] declaredFields = currentShape.getDeclaredFields();
            Collections.addAll(reachableFields, declaredFields);

            if (!searchSuperclasses) {
                break;
            }
        }
        return reachableFields;
    }

    // What the shape declares plus what it publishes. Both, because the members worth matching a
    // signature against are the ones it keeps to itself, while the methods a walk takes by name are
    // declared well above the leaf classes it meets. Deduplicated because a public declared member
    // is in both, and a match counting it twice reads as an ambiguity that is not there.
    //
    // The pair leaves one hole, which is what the hierarchy walk is offered for: a method that is
    // neither declared here nor public - one a base class keeps to its own package or its subtypes
    // - is in neither list. Off by default all the same, since widening the candidate set turns a
    // name that identified one method into an ambiguity, and the reads taken per frame are of
    // public accessors that the pair already covers.
    private static Set<Object> readReachableMethodsOf(Class<?> shape, boolean searchSuperclasses) {

        Object[] declaredMethods = shape.getDeclaredMethods();
        Object[] publicMethods = shape.getMethods();

        var reachableMethods = new LinkedHashSet<>(declaredMethods.length + publicMethods.length);
        Collections.addAll(reachableMethods, declaredMethods);
        Collections.addAll(reachableMethods, publicMethods);

        if (searchSuperclasses) {
            for (var currentShape = shape.getSuperclass();
                 currentShape != null && !HIERARCHY_ROOT.equals(currentShape);
                 currentShape = currentShape.getSuperclass()) {

                Object[] inheritedMethods = currentShape.getDeclaredMethods();
                Collections.addAll(reachableMethods, inheritedMethods);
            }
        }
        return reachableMethods;
    }

    /**
     * What a constructor has to look like to be a match, an unstated filter meaning "any".
     *
     * @param parameterCount how many it takes, or null for any number
     * @param parameterTypes what a caller would pass, or null to match on the count alone; matched
     *                       by assignment compatibility, so these are argument types rather than
     *                       the declared ones
     */
    record ConstructorQuery(
        Integer parameterCount,
        List<Class<?>> parameterTypes) {

        static ConstructorQuery taking(List<Class<?>> parameterTypes) {
            return new ConstructorQuery(null, parameterTypes);
        }

        static ConstructorQuery takingCount(int parameterCount) {
            return new ConstructorQuery(parameterCount, null);
        }
    }

    /**
     * What a field has to look like to be a match, an unstated filter meaning "any".
     *
     * @param name               what it is called, or null for any name
     * @param exactType          the type it is declared as, matched exactly, or null for any
     * @param assignableTo       a type its value could be handed to, or null for any
     * @param accepting          a type whose values could be put in it, or null for any
     * @param searchSuperclasses whether the fields a superclass declares count too
     */
    record FieldQuery(
        String name,
        Class<?> exactType,
        Class<?> assignableTo,
        Class<?> accepting,
        boolean searchSuperclasses) {

        static FieldQuery anyField(boolean searchSuperclasses) {
            return new FieldQuery(null, null, null, null, searchSuperclasses);
        }

        static FieldQuery accepting(Class<?> accepting) {
            return new FieldQuery(null, null, null, accepting, false);
        }

        static FieldQuery assignableTo(Class<?> assignableTo) {
            return new FieldQuery(null, null, assignableTo, null, false);
        }

        static FieldQuery named(String name) {
            return new FieldQuery(name, null, null, null, false);
        }

        static FieldQuery ofExactType(Class<?> exactType) {
            return new FieldQuery(null, exactType, null, null, false);
        }

        /** @return the same query, run over what the shape's superclasses declare as well */
        FieldQuery searchingSuperclasses() {
            return new FieldQuery(name, exactType, assignableTo, accepting, true);
        }

        /** @return whether anything about the field's type is being matched on */
        boolean isTypeConstrained() {
            return exactType != null || assignableTo != null || accepting != null;
        }
    }

    /**
     * What a method has to look like to be a match, an unstated filter meaning "any".
     *
     * @param name               what it is called, or null for any name
     * @param returnType         a type its answer could be handed to, or null for any
     * @param parameterCount     how many it takes, or null for any number
     * @param parameterTypes     what a caller would pass, or null to match on the count alone;
     *                           matched by assignment compatibility, so these are argument types
     *                           rather than the declared ones
     * @param searchSuperclasses whether to take in the methods a superclass declares but does not
     *                           publish, which are in neither set a shape offers on its own
     */
    record MethodQuery(
        String name,
        Class<?> returnType,
        Integer parameterCount,
        List<Class<?>> parameterTypes,
        boolean searchSuperclasses) {

        static MethodQuery anyMethod() {
            return new MethodQuery(null, null, null, null, false);
        }

        static MethodQuery named(String name) {
            return new MethodQuery(name, null, null, null, false);
        }

        /** @return the same query, narrowed to methods a caller could pass these to */
        MethodQuery taking(Class<?>... parameterTypes) {
            return takingArgumentTypes(List.of(parameterTypes));
        }

        /** @return the same query, narrowed to methods a caller could pass these to */
        MethodQuery takingArgumentTypes(List<Class<?>> parameterTypes) {
            return new MethodQuery(
                name, returnType, parameterCount, parameterTypes, searchSuperclasses);
        }

        /** @return the same query, narrowed to methods taking this many */
        MethodQuery takingCount(int parameterCount) {
            return new MethodQuery(
                name, returnType, parameterCount, parameterTypes, searchSuperclasses);
        }

        /** @return the same query, narrowed to methods whose answer fits this type */
        MethodQuery returning(Class<?> returnType) {
            return new MethodQuery(
                name, returnType, parameterCount, parameterTypes, searchSuperclasses);
        }

        /** @return the same query, run over what the shape's superclasses declare as well */
        MethodQuery searchingSuperclasses() {
            return new MethodQuery(name, returnType, parameterCount, parameterTypes, true);
        }
    }

    // One key for all three caches: what was searched and what was asked of it. The queries hold
    // their parameter types as lists rather than arrays so a record's own equality covers them -
    // two callers asking the same thing have to meet in the map, and array equality is by identity.
    private record MemberSearch(
        Class<?> shape,
        Object query) {
    }
}
