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
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Which members a shape has, and reaching one of them by name or by signature.
 *
 * <p>Two ways of naming what is wanted, because obfuscation takes one of them away. A member the
 * game publishes keeps its name across builds and is asked for by it; one it does not is renamed
 * with every build, and the only durable thing left about it is its signature - what it takes, what
 * it answers with, what type it is declared as. So every search here matches on any combination of
 * the two, an unstated criterion meaning "any".
 *
 * <p>Every search is memoised. A shape's members do not change within a run, while a walk asking
 * the same question each frame would otherwise re-read the shape's whole member list per node. The
 * maps are bounded by the shapes met times the searches the code makes, both being properties of
 * the asking code rather than of the run. Caching lives here and nowhere else, so a caller never
 * has to keep a memo of its own.
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

    private static final Map<MemberSearch<ConstructorQuery>, List<ReflectedConstructor>> CONSTRUCTORS_BY_SEARCH =
        new ConcurrentHashMap<>();

    private static final Map<MemberSearch<FieldQuery>, List<ReflectedField>> FIELDS_BY_SEARCH =
        new ConcurrentHashMap<>();

    private static final Map<MemberSearch<MethodQuery>, List<ReflectedMethod>> METHODS_BY_SEARCH =
        new ConcurrentHashMap<>();

    // Kept apart from the method searches beside it because the question is answered differently:
    // this one scans names and builds nothing, where a search wraps every match. Its own map rather
    // than a wrapped search reused, so the walk that asks per node per frame stays off that cost.
    private static final Map<MemberSearch<String>, Boolean> NAME_PRESENCE_BY_SEARCH =
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
        var matches = findConstructorsMatching(
            shape, ConstructorQuery.anyConstructor().takingArgumentTypes(argumentTypes));

        return resolveSoleMatch(
            matches, new SoughtMember("constructor", shape, argumentTypes.toString()))
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
            new MemberSearch<>(shape, query),
            search -> selectMatching(
                Arrays.asList((Object[]) search.shape().getDeclaredConstructors()),
                constructor -> isParameterListMatching(
                    ReflectionBypass.readConstructorParameterTypes(constructor),
                    search.query().parameterCount(),
                    search.query().parameterTypes()),
                ReflectedConstructor::new));
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
            new MemberSearch<>(shape, query),
            search -> selectMatching(
                readReachableFieldsOf(search.shape(), search.query().searchSuperclasses()),
                field -> isFieldMatching(field, search.query()),
                ReflectedField::new));
    }

    /**
     * The fields of a shape whose own declared type offers a method fitting the method query, for
     * reaching a member by what the thing it holds can do.
     *
     * <p>The way in when neither the field nor what it holds carries a usable name: an obfuscated
     * widget's parts are reached by recognising what they are capable of. Matched against the
     * field's declared type rather than against what it holds, so a field declared as
     * {@code Object} matches nothing however it was filled.
     *
     * <p>The only search here with no memo of its own, and it needs none: both halves it composes
     * are memoised, leaving it a filter over lists already in hand.
     *
     * @param shape       the class to look in
     * @param fieldQuery  which of the shape's fields to consider - {@link FieldQuery#anyField()}
     *                    for the usual case, where what the field holds is the whole criterion
     * @param methodQuery what the field's type has to offer
     * @return every field that fits, in declaration order
     */
    static List<ReflectedField> findFieldsHoldingMethodMatching(
            Class<?> shape,
            FieldQuery fieldQuery,
            MethodQuery methodQuery) {

        return findFieldsMatching(shape, fieldQuery).stream()
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
            new MemberSearch<>(shape, query),
            search -> selectMatching(
                readReachableMethodsOf(search.shape(), search.query().searchSuperclasses()),
                method -> isMethodMatching(method, search.query()),
                ReflectedMethod::new));
    }

    /**
     * Whether the shape declares or inherits any method of this name, whatever it takes.
     *
     * <p>Answered without building a match, so the common case a walk meets - a leaf carrying no
     * such name - costs a memo lookup rather than a thrown exception, and costs the scan behind it
     * only the first time that shape is met.
     *
     * @param shape      the class to look in
     * @param methodName the name to look for
     * @return whether anything of that name is there
     */
    static boolean hasMethodNamed(Class<?> shape, String methodName) {

        return NAME_PRESENCE_BY_SEARCH.computeIfAbsent(
            new MemberSearch<>(shape, methodName),
            search -> scanForMethodNamed(search.shape(), search.query()));
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
     * <p>Its own read rather than the one above with null handed in: a {@link Class} passed where
     * an instance is expected would have the search run over {@code java.lang.Class} itself, which
     * compiles and finds nothing the caller meant.
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
     * <p>Its own write for the reason {@link #readStaticFieldValue} gives.
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
            matches, new SoughtMember("method '" + methodName + "'", shape, argumentTypes.toString()));
    }

    private static ReflectedField resolveSoleFieldFor(Class<?> shape, FieldQuery query) {

        return resolveSoleMatch(
            findFieldsMatching(shape, query), new SoughtMember("field", shape, query.toString()));
    }

    // The single member a caller asserted was there, or a refusal saying which way the assertion
    // failed. Both directions refuse rather than guess: nothing to call is unreachable, and more
    // than one means the caller's description stopped identifying a member - picking one of them
    // would make which it got turn on declaration order, which an obfuscated build reshuffles.
    private static <T> T resolveSoleMatch(List<T> matches, SoughtMember sought) {

        if (matches.isEmpty()) {
            throw new IllegalArgumentException("No " + sought + ".");
        }

        if (matches.size() > 1) {
            throw new IllegalArgumentException(
                "More than one " + sought + ", so it does not identify one of them.");
        }
        return matches.get(0);
    }

    // The one shape of every search here: walk the members in scope, keep the ones the criteria
    // admit, hand each back wrapped. Shared rather than written per member kind, the three differing
    // only in which members they walk and what they wrap them as.
    private static <M> List<M> selectMatching(
            Iterable<Object> members,
            Predicate<Object> isMatching,
            Function<Object, M> wrapMember) {

        var matches = new ArrayList<M>();

        for (var member : members) {
            if (isMatching.test(member)) {
                matches.add(wrapMember.apply(member));
            }
        }
        return List.copyOf(matches);
    }

    private static boolean scanForMethodNamed(Class<?> shape, String methodName) {

        for (var method : readReachableMethodsOf(shape, false)) {
            if (methodName.equals(ReflectionBypass.readMethodName(method))) {
                return true;
            }
        }
        return false;
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

        if (query.accepting() != null
                && !ParameterCompatibility.isParameterCompatible(fieldType, query.accepting())) {
            return false;
        }

        if (query.assignableTo() != null && !query.assignableTo().isAssignableFrom(fieldType)) {
            return false;
        }

        // A field declared as Object satisfies every type criterion there is, so a nameless search
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

    // What was looked for, as the refusals say it. One value rather than three arguments threaded
    // through, and it owns the wording so the two refusals differ only in how they open.
    private record SoughtMember(
        String memberKind,
        Class<?> shape,
        String criteria) {

        @Override
        public String toString() {
            return memberKind + " on " + shape.getName() + " matching " + criteria;
        }
    }

    // One key for every cache: what was searched and what was asked of it. A query holds its
    // parameter types as a list rather than an array so a record's own equality covers them - two
    // callers asking the same thing have to meet in the map, and array equality is by identity.
    private record MemberSearch<Q>(
        Class<?> shape,
        Q query) {
    }
}
