package kmlib.starsector.ui.coreui;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the rule that decides which member a caller's arguments reach, which is the whole of what
 * separates a usable reach from one that resolves nothing.
 *
 * <p>Every case here is one a direct call would be allowed to make, and the reach has to allow the
 * same set: a caller holding a boxed value and a member declaring a primitive is the ordinary
 * shape of the core UI's entry points, not an edge of it. Too strict and those members are
 * unreachable; too loose and one name resolves to several members and is refused as ambiguous.
 *
 * <p>Answered without any reflection at all, which is why the rule lives apart from the reach: the
 * arithmetic is over {@link Class} objects and can be pinned directly.
 */
class ParameterCompatibilityTest {

    @Nested
    class IsParameterCompatible {

        @Test
        void acceptsTheSameType() {

            assertThat(ParameterCompatibility.isParameterCompatible(String.class, String.class))
                .isTrue();
        }

        @Test
        void acceptsASubtypeForAReferenceParameter() {
            // Assignment compatibility rather than identity: the core UI's own signatures are
            // written in interfaces and base classes, and a caller holds the concrete thing.
            assertThat(ParameterCompatibility.isParameterCompatible(
                CharSequence.class, String.class))
                .isTrue();
        }

        @Test
        void refusesASupertypeForAReferenceParameter() {
            // The direction that does not hold: a member taking a String cannot be handed just any
            // CharSequence, and a reach that allowed it would resolve members a call could not.
            assertThat(ParameterCompatibility.isParameterCompatible(
                String.class, CharSequence.class))
                .isFalse();
        }

        @Test
        void unwrapsABoxForAPrimitiveParameter() {
            // The load-bearing case. A caller can only ever hand over a Float, and render(float) is
            // the member the whole reach exists to call.
            assertThat(ParameterCompatibility.isParameterCompatible(float.class, Float.class))
                .isTrue();
        }

        @Test
        void widensANarrowerPrimitive() {

            assertThat(ParameterCompatibility.isParameterCompatible(float.class, int.class))
                .isTrue();
        }

        @Test
        void widensAnUnwrappedBox() {
            // Both conversions in sequence, which is what makes a literal 1 reach a float parameter
            // - and the reason one such argument can fit two overloads at once.
            assertThat(ParameterCompatibility.isParameterCompatible(float.class, Integer.class))
                .isTrue();
        }

        @Test
        void refusesAWiderPrimitiveForANarrowerParameter() {
            // Narrowing is what a direct call refuses without an explicit cast, and the reach has
            // no cast to offer.
            assertThat(ParameterCompatibility.isParameterCompatible(int.class, double.class))
                .isFalse();
        }

        @Test
        void treatsCharAsANarrowInteger() {

            assertThat(ParameterCompatibility.isParameterCompatible(int.class, char.class))
                .isTrue();
        }

        @Test
        void widensNothingIntoBoolean() {
            // Boolean stands outside the numeric widening order entirely, so a member taking one is
            // reached by a boolean and by nothing else.
            assertThat(ParameterCompatibility.isParameterCompatible(boolean.class, int.class))
                .isFalse();
        }

        @Test
        void boxesAPrimitiveForAReferenceParameter() {
            // The other direction across the divide: a member declaring Object takes a float,
            // because a call would box it on the way in.
            assertThat(ParameterCompatibility.isParameterCompatible(Object.class, float.class))
                .isTrue();
        }

        @Test
        void acceptsNullForAReferenceParameter() {
            // A null argument brings no type to match, so the only question left is whether the
            // parameter can hold one.
            assertThat(ParameterCompatibility.isParameterCompatible(String.class, null))
                .isTrue();
        }

        @Test
        void refusesNullForAPrimitiveParameter() {

            assertThat(ParameterCompatibility.isParameterCompatible(int.class, null))
                .isFalse();
        }
    }

    @Nested
    class IsCallableWith {

        @Test
        void acceptsEachArgumentInItsOwnPosition() {

            assertThat(ParameterCompatibility.isCallableWith(
                new Class<?>[] {CharSequence.class, float.class},
                List.of(String.class, int.class)))
                .isTrue();
        }

        @Test
        void refusesAnArgumentThatFitsOnlyAnotherPosition() {
            // Position matters, and a check that merely counted compatible types would pass a call
            // the target refuses at runtime - where the failure is far from the cause.
            assertThat(ParameterCompatibility.isCallableWith(
                new Class<?>[] {String.class, float.class},
                List.of(float.class, String.class)))
                .isFalse();
        }

        @Test
        void refusesTooFewArguments() {

            assertThat(ParameterCompatibility.isCallableWith(
                new Class<?>[] {String.class, float.class},
                List.of(String.class)))
                .isFalse();
        }

        @Test
        void acceptsAMemberTakingNothing() {

            assertThat(ParameterCompatibility.isCallableWith(new Class<?>[0], List.of()))
                .isTrue();
        }

        @Test
        void acceptsANullArgumentInAReferencePosition() {
            // The list a null argument produces holds a null, which nothing downstream may treat as
            // "no constraint" by accident.
            assertThat(ParameterCompatibility.isCallableWith(
                new Class<?>[] {String.class},
                Arrays.asList((Class<?>) null)))
                .isTrue();
        }
    }

    @Nested
    class ReadArgumentTypes {

        @Test
        void unwrapsABoxToItsPrimitive() {
            // Done once per call rather than per candidate member, and this is where the boxed
            // float that a caller must hand over becomes the float a member declares.
            assertThat(ParameterCompatibility.readArgumentTypes(new Object[] {0.75f, 3}))
                .containsExactly(float.class, int.class);
        }

        @Test
        void keepsAnOrdinaryReferenceType() {

            assertThat(ParameterCompatibility.readArgumentTypes(new Object[] {"a label"}))
                .containsExactly(String.class);
        }

        @Test
        void contributesNoTypeForANullArgument() {
            // A null has no class to read, so its slot has to stay empty rather than collapse the
            // argument list and shift every later argument a position left.
            assertThat(ParameterCompatibility.readArgumentTypes(new Object[] {null, "a label"}))
                .containsExactly(null, String.class);
        }

        @Test
        void isEmptyForNoArguments() {

            assertThat(ParameterCompatibility.readArgumentTypes(new Object[0]))
                .isEmpty();
        }
    }

    @Nested
    class UnboxOrKeepType {

        @Test
        void unwrapsABox() {

            assertThat(ParameterCompatibility.unboxOrKeepType(Double.class))
                .isEqualTo(double.class);
        }

        @Test
        void leavesAnOrdinaryTypeAlone() {

            assertThat(ParameterCompatibility.unboxOrKeepType(String.class))
                .isEqualTo(String.class);
        }

        @Test
        void leavesAPrimitiveAlone() {

            assertThat(ParameterCompatibility.unboxOrKeepType(int.class))
                .isEqualTo(int.class);
        }
    }
}
