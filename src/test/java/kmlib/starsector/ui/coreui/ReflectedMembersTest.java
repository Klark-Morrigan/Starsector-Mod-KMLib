package kmlib.starsector.ui.coreui;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pins the searches that reach a member the game does not publish, and the two ways of naming what
 * is wanted that obfuscation leaves available.
 *
 * <p>A member the game publishes keeps its name across builds and is asked for by it. One it does
 * not is renamed with every build, so the only durable description left is its shape - what a field
 * is declared as, what a method takes and answers with, what a constructor takes. Both are pinned,
 * because a reach that offered only the first would be rewritten at every game release.
 *
 * <p>The refusals are pinned as hard as the matches. A description fitting nothing and a
 * description fitting several are both failures of the caller's assertion that it identified one
 * member, and resolving either by picking would make the answer turn on declaration order - which
 * an obfuscated build reshuffles freely.
 */
class ReflectedMembersTest {

    private static final String LABEL = "Starscape";

    @Nested
    class FindFieldsMatching {

        @Test
        void findFieldsMatchingReachesAFieldTheShapeKeepsToItself() {
            // What the whole search exists for: the fields worth reaching are private, so a search
            // that saw only public ones would see none of them.
            assertThat(ReflectedMembers.findFieldsMatching(ShapeFake.class,
                FieldQuery.named("hiddenLabel")))
                .singleElement()
                .extracting(ReflectedField::getType)
                .isEqualTo(String.class);
        }

        @Test
        void findFieldsMatchingLeavesOutWhatASuperclassDeclaresByDefault() {
            // A field is reached where it is declared, and a caller naming a subclass usually means
            // that subclass's own - so the wider search is asked for rather than assumed.
            assertThat(ReflectedMembers.findFieldsMatching(InheritingShapeFake.class,
                FieldQuery.named("hiddenLabel")))
                .isEmpty();
        }

        @Test
        void findFieldsMatchingReachesUpTheHierarchyWhenAsked() {
            // The core UI's shapes are deep, and the fields a probe wants are declared well above
            // the leaf class it is handed.
            assertThat(ReflectedMembers.findFieldsMatching(InheritingShapeFake.class,
                FieldQuery.named("hiddenLabel").searchingSuperclasses()))
                .hasSize(1);
        }

        @Test
        void findFieldsMatchingSelectsByDeclaredTypeWhenNoNameSurvives() {
            // The obfuscated case: no usable name, so the declared type is the whole description.
            assertThat(ReflectedMembers.findFieldsMatching(ShapeFake.class,
                FieldQuery.anyField().ofExactType(int.class)))
                .singleElement()
                .extracting(ReflectedField::getName)
                .isEqualTo("hiddenCount");
        }

        @Test
        void findFieldsMatchingSelectsAFieldItsValueCouldBeHandedOnFrom() {
            // Assignable rather than exact, for a caller that knows what it wants to do with the
            // value rather than exactly how the field was declared.
            assertThat(ReflectedMembers.findFieldsMatching(ShapeFake.class,
                FieldQuery.anyField().assignableTo(CharSequence.class)))
                .singleElement()
                .extracting(ReflectedField::getName)
                .isEqualTo("hiddenLabel");
        }

        @Test
        void findFieldsMatchingSelectsAFieldAValueCouldBePutIn() {
            // The opposite direction to the read above, and the one a write is aimed by: what the
            // field would accept rather than what its value could be handed to.
            assertThat(ReflectedMembers.findFieldsMatching(ShapeFake.class,
                FieldQuery.anyField().accepting(String.class)))
                .singleElement()
                .extracting(ReflectedField::getName)
                .isEqualTo("hiddenLabel");
        }

        @Test
        void findFieldsMatchingLeavesOutAnUntypedFieldOnANamelessTypeSearch() {
            // A field declared as Object accepts everything, so it answers every such search on
            // top of whatever the caller meant. Counted, it would turn a search that identified one
            // field into an ambiguity, and the caller would never learn which field it wanted.
            assertThat(ReflectedMembers.findFieldsMatching(ShapeFake.class,
                FieldQuery.anyField().accepting(String.class)))
                .noneMatch(field -> "hiddenAnythingFake".equals(field.getName()));
        }

        @Test
        void findFieldsMatchingKeepsAnUntypedFieldForACallerThatAskedForObject() {
            // The one case where those fields are the answer rather than noise, which is why the
            // exclusion turns on what was asked for and not on the field alone.
            assertThat(ReflectedMembers.findFieldsMatching(ShapeFake.class,
                FieldQuery.anyField().ofExactType(Object.class)))
                .singleElement()
                .extracting(ReflectedField::getName)
                .isEqualTo("hiddenAnythingFake");
        }

        @Test
        void findFieldsMatchingKeepsAnUntypedFieldWhenTheSearchNamesIt() {
            // A named search is already unambiguous, so the exclusion has nothing to protect and
            // would only hide a field the caller asked for outright.
            assertThat(ReflectedMembers.findFieldsMatching(ShapeFake.class,
                FieldQuery.named("hiddenAnythingFake")))
                .hasSize(1);
        }
    }

    @Nested
    class FindFieldsHoldingMethodMatching {

        @Test
        void findFieldsHoldingMethodMatchingSelectsByWhatTheFieldsTypeCanDo() {
            // The last way in when neither the field nor its type carries a usable name: an
            // obfuscated widget's parts are recognised by what they are capable of.
            assertThat(ReflectedMembers.findFieldsHoldingMethodMatching(
                ShapeFake.class, FieldQuery.anyField(), MethodQuery.named("readPartLabel")))
                .singleElement()
                .extracting(ReflectedField::getName)
                .isEqualTo("hiddenPartFake");
        }

        @Test
        void findFieldsHoldingMethodMatchingLeavesOutAFieldWhoseTypeCannot() {

            assertThat(ReflectedMembers.findFieldsHoldingMethodMatching(
                ShapeFake.class, FieldQuery.anyField(), MethodQuery.named("noSuchMemberAnywhere")))
                .isEmpty();
        }

        @Test
        void findFieldsHoldingMethodMatchingLeavesOutAnUntypedFieldHoweverItWasFilled() {
            // Matched against what the field is declared as, not against what it holds - a field
            // declared as Object says nothing about its contents until something reads it, and a
            // search cannot read every field of every shape it walks.
            var shapeFake = new ShapeFake();
            shapeFake.fillAnythingWithAPart();

            assertThat(ReflectedMembers.findFieldsHoldingMethodMatching(
                ShapeFake.class, FieldQuery.anyField(), MethodQuery.named("readPartLabel")))
                .noneMatch(field -> "hiddenAnythingFake".equals(field.getName()));
        }
    }

    @Nested
    class ReadFieldValue {

        @Test
        void readFieldValueReachesAFieldTheShapeKeepsToItself() {

            assertThat(ReflectedMembers.readFieldValue(new ShapeFake(),
                FieldQuery.named("hiddenLabel")))
                .isEqualTo(LABEL);
        }

        @Test
        void readFieldValueThrowsWhenNothingMatches() {
            // Raised rather than answered null, so a caller can tell a field holding null from a
            // description that reached nothing at all.
            assertThatThrownBy(() -> ReflectedMembers.readFieldValue(new ShapeFake(),
                FieldQuery.named("noSuchFieldAnywhere")))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void readFieldValueThrowsWhenMoreThanOneMatches() {
            // Two fields of one type is ordinary, and picking either would make the answer turn on
            // declaration order - which an obfuscated build reshuffles between releases.
            assertThatThrownBy(() -> ReflectedMembers.readFieldValue(new ShapeFake(),
                FieldQuery.anyField().ofExactType(boolean.class)))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class WriteFieldValue {

        @Test
        void writeFieldValueReachesAFieldTheShapeKeepsToItself() {

            var shapeFake = new ShapeFake();

            ReflectedMembers.writeFieldValue(shapeFake, FieldQuery.named("hiddenCount"), 7);

            assertThat(shapeFake.readHiddenCount())
                .isEqualTo(7);
        }

        @Test
        void writeFieldValueThrowsWhenTheValueDoesNotFitTheField() {
            // Refused by the write itself rather than checked beforehand, so the failure names the
            // field rather than a guess made about it.
            assertThatThrownBy(() -> ReflectedMembers.writeFieldValue(new ShapeFake(),
                FieldQuery.named("hiddenCount"), LABEL))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class FindConstructorsMatching {

        @Test
        void findConstructorsMatchingSelectsByWhatItTakes() {
            // A constructor has no name of its own, so its parameter list is the only thing telling
            // one from another.
            assertThat(ReflectedMembers.findConstructorsMatching(BuiltShapeFake.class,
                ConstructorQuery.anyConstructor().taking(String.class)))
                .singleElement()
                .extracting(constructor -> constructor.getParameterTypes().length)
                .isEqualTo(1);
        }

        @Test
        void findConstructorsMatchingSelectsByHowManyItTakes() {

            assertThat(ReflectedMembers.findConstructorsMatching(BuiltShapeFake.class,
                ConstructorQuery.anyConstructor().takingCount(0)))
                .hasSize(1);
        }

        @Test
        void findConstructorsMatchingReachesOneTheShapeKeepsToItself() {

            assertThat(ReflectedMembers.findConstructorsMatching(BuiltShapeFake.class,
                ConstructorQuery.anyConstructor().takingArgumentTypes(List.of(int.class))))
                .hasSize(1);
        }
    }

    @Nested
    class Construct {

        @Test
        void constructBuildsThroughAConstructorTheShapeKeepsToItself() {
            // The whole point of reaching a constructor at all: the game's own widget classes are
            // built by code the mod cannot call directly.
            var built = (BuiltShapeFake) ReflectedMembers.construct(BuiltShapeFake.class, 7);

            assertThat(built.readLabel())
                .isEqualTo("7");
        }

        @Test
        void constructSelectsTheConstructorTheArgumentsFit() {

            var built = (BuiltShapeFake) ReflectedMembers.construct(BuiltShapeFake.class, LABEL);

            assertThat(built.readLabel())
                .isEqualTo(LABEL);
        }

        @Test
        void constructThrowsWhenNoConstructorTakesThoseArguments() {

            assertThatThrownBy(() -> ReflectedMembers.construct(BuiltShapeFake.class, 1.5d))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class InvokeStaticByName {

        @Test
        void invokeStaticByNameReachesAStaticMemberTheShapeKeepsToItself() {
            // Distinct from calling on an object: there is no object, and a reach that always
            // passed one would fail on every static member the game declares.
            assertThat(ReflectedMembers.invokeStaticByName(ShapeFake.class, "buildStaticLabel"))
                .isEqualTo(LABEL);
        }

        @Test
        void invokeStaticByNameThrowsWhenTheMemberIsAbsent() {

            assertThatThrownBy(() -> ReflectedMembers
                .invokeStaticByName(ShapeFake.class, "noSuchMemberAnywhere"))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class FindMethodsMatching {

        @Test
        void findMethodsMatchingSelectsByWhatItAnswersWith() {
            // The obfuscated case for methods: no usable name, so the signature is the whole
            // description.
            assertThat(ReflectedMembers.findMethodsMatching(ShapeFake.class,
                MethodQuery.anyMethod().returning(Number.class).takingCount(0)))
                .singleElement()
                .extracting(ReflectedMethod::getName)
                .isEqualTo("readHiddenCount");
        }

        @Test
        void findMethodsMatchingSelectsByWhatACallerWouldPass() {

            assertThat(ReflectedMembers.findMethodsMatching(ShapeFake.class,
                MethodQuery.named("buildLabelledThing").taking(String.class, Object.class)))
                .hasSize(1);
        }

        @Test
        void findMethodsMatchingIsEmptyWhenTheArgumentShapeFitsNothingOfThatName() {

            assertThat(ReflectedMembers.findMethodsMatching(ShapeFake.class,
                MethodQuery.named("buildLabelledThing").taking(int.class)))
                .isEmpty();
        }

        @Test
        void findMethodsMatchingLeavesOutANonPublicInheritedMethodByDefault() {
            // The hole the two sets a shape offers leave between them: a method neither declared
            // here nor published is in neither. Left open by default because widening the candidate
            // set turns a name that identified one method into an ambiguity, and the reads taken
            // per frame are of public accessors the two sets already cover.
            assertThat(ReflectedMembers.findMethodsMatching(InheritingShapeFake.class,
                MethodQuery.named("readHiddenCount")))
                .isEmpty();
        }

        @Test
        void findMethodsMatchingReachesANonPublicInheritedMethodWhenAsked() {
            // The way to close it, for a caller reaching a member the game declares on a base class
            // and keeps to itself - which a deep obfuscated hierarchy is full of.
            assertThat(ReflectedMembers.findMethodsMatching(InheritingShapeFake.class,
                MethodQuery.named("readHiddenCount").searchingSuperclasses()))
                .hasSize(1);
        }
    }

    /**
     * A shape carrying one member of each kind a search has to tell apart: fields it keeps to
     * itself, one declared as an untyped Object, a pair sharing a type so a search over it is
     * genuinely ambiguous, a field whose own type offers a recognisable member, and a static.
     */
    @SuppressWarnings("unused")
    private static class ShapeFake {

        // Two statics rather than one, and both of a type no search here narrows by: reading and
        // writing need different fields, or whichever test ran first would decide what the other
        // saw.
        private static final long hiddenStaticReading = 11L;

        private static long hiddenStaticWritable;

        private final String hiddenLabel = LABEL;
        private final PartFake hiddenPartFake = new PartFake();
        private final boolean isHiddenFlagSet = true;
        private final boolean isHiddenOtherFlagSet = false;

        // Not final, unlike its neighbours: a write through the reach is one of the things under
        // test, and a final instance field is refused before the type of the value is ever looked
        // at - which would leave the refusal saying the wrong thing.
        private int hiddenCount = 3;
        private Object hiddenAnythingFake;

        private static String buildStaticLabel() {
            return LABEL;
        }

        static long readStaticWritable() {
            return hiddenStaticWritable;
        }

        void fillAnythingWithAPart() {
            hiddenAnythingFake = new PartFake();
        }

        Integer readHiddenCount() {
            return hiddenCount;
        }

        private String buildLabelledThing(String label, Object shortcut) {
            return label;
        }
    }

    private static final class InheritingShapeFake extends ShapeFake {
    }

    /** Stands for the thing a widget holds, recognised by what it offers rather than by its name. */
    @SuppressWarnings("unused")
    private static final class PartFake {

        String readPartLabel() {
            return LABEL;
        }
    }

    /**
     * A shape built rather than read, carrying constructors that differ only in what they take -
     * the shape a search over constructors has to resolve.
     */
    @SuppressWarnings("unused")
    private static final class BuiltShapeFake {

        private final String label;

        private BuiltShapeFake() {
            this.label = LABEL;
        }

        private BuiltShapeFake(String label) {
            this.label = label;
        }

        private BuiltShapeFake(int count) {
            this.label = String.valueOf(count);
        }

        String readLabel() {
            return label;
        }
    }
}
