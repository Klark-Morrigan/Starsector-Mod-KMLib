package kmlib.starsector.ui.coreui;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the by-shape reach: what a caller matching a signature is handed, and what it is handed the
 * signature in. Both halves matter to a match that has to hold across a game build - the set is what
 * decides whether a match is unique, and the parameter and return types are the only durable thing
 * about a member whose name is regenerated each time.
 *
 * <p>The two reads are pinned apart because their difference is the whole reason there are two. What
 * a shape declares is its own and may be private; what it publishes reaches up through everything it
 * extends. A match run over the second where it wanted the first meets every inherited member and
 * can find a second candidate that was never a candidate at all.
 */
class CoreUiMethodsTest {

    private static final String LABEL = "Starscape";

    @Nested
    class ReadDeclaredMethodsOf {

        @Test
        void readDeclaredMethodsOfReachesAMemberTheShapeKeepsToItself() {
            // The case the whole reach exists for: the members worth matching on are the ones the
            // game does not publish, so a read that could only see public members would see none of
            // them.
            var hiddenMember = findSoleMethodNamed("buildLabelledThing");

            assertThat(hiddenMember.getParameterTypes())
                .containsExactly(String.class, Object.class);
            assertThat(hiddenMember.getReturnType())
                .isEqualTo(String.class);
            assertThat(hiddenMember.invokeOn(new ShapeFake(), LABEL, null))
                .isEqualTo(LABEL);
        }

        @Test
        void readDeclaredMethodsOfDescribesAVoidMemberAsAnsweringVoid() {
            // What every match for a member that answers nothing is written against, and not what a
            // reader would assume: the absence is stated as a type rather than as no type at all.
            assertThat(findSoleMethodNamed("layOutThing").getReturnType())
                .isEqualTo(void.class);
        }

        @Test
        void readDeclaredMethodsOfLeavesOutWhatTheShapeMerelyInherits() {
            // The reason this read is not the one below it. Every shape inherits a good deal, and
            // any of it can happen to fit the signature being matched - which reads as a second
            // candidate and refuses a match that was never ambiguous.
            assertThat(CoreUiMethods.readDeclaredMethodsOf(ShapeFake.class))
                .noneMatch(shapeMethod -> "toString".equals(shapeMethod.getName()));
        }
    }

    @Nested
    class ReadPublicMethodsOf {

        @Test
        void readPublicMethodsOfReachesUpThroughWhatTheShapeExtends() {
            // The set for a member a caller is entitled to call anyway, which is a question about
            // access rather than about where the member was declared.
            assertThat(CoreUiMethods.readPublicMethodsOf(ShapeFake.class))
                .anyMatch(shapeMethod -> "toString".equals(shapeMethod.getName()));
        }

        @Test
        void readPublicMethodsOfLeavesOutWhatTheShapeKeepsToItself() {
            // The other half of the split, and why a match for a hidden member cannot be run over
            // this set.
            assertThat(CoreUiMethods.readPublicMethodsOf(ShapeFake.class))
                .noneMatch(shapeMethod -> "buildLabelledThing".equals(shapeMethod.getName()));
        }
    }

    private static CoreUiMethod findSoleMethodNamed(String methodName) {
        return CoreUiMethods.readDeclaredMethodsOf(ShapeFake.class).stream()
            .filter(shapeMethod -> methodName.equals(shapeMethod.getName()))
            .findFirst()
            .orElseThrow();
    }

    /**
     * A shape carrying one member of each kind a match has to tell apart: one it keeps to itself
     * that answers with something, and one that answers nothing. What it publishes it inherits,
     * which is exactly the difference the two reads are pinned on.
     */
    private static final class ShapeFake {

        private String buildLabelledThing(String label, Object shortcut) {
            return label;
        }

        private void layOutThing(Object thing, float width, float height) {
        }
    }
}
