package kmlib.testfixtures.starsector.save;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.xml.parsers.DocumentBuilderFactory;

/**
 * A mod's persisted objects driven through XStream, the serialiser the game writes a save with, for a suite pinning
 * that mod's save format. Class and field names are baked into every save, so a rename orphans a value on load - or
 * fails the load outright - rather than migrating it; the element paths an object graph writes are the part of the
 * format a suite can hold still, and reading the graph back is what the save does on load.
 *
 * <p>Collaborators a graph points at but does not own - a vanilla planet, a script, an intel - are named when the
 * fixture is built and written as empty elements: the field holding one still appears in the paths, while what it
 * holds is the game's, or another package's, to pin. Read back, such a field comes back {@code null}.
 *
 * <p>XStream reflects into the JDK's own collections as it builds its converters, which a modern JDK refuses unless
 * the test JVM opens those packages the way the game's own JVM does; the shared Starsector conventions open them for
 * every consumer's test task. Nothing XStream-typed crosses this class's surface, so a consuming suite compiles without
 * the game's XStream jar and needs it only at run time, where the conventions already place every core jar.
 */
public final class SaveFormatFixture {

    private final com.thoughtworks.xstream.XStream xstream;

    private SaveFormatFixture(com.thoughtworks.xstream.XStream xstream) {

        this.xstream = xstream;
    }

    /**
     * A serialiser writing every instance of {@code collaboratorTypes} - and of their subtypes - as an empty element.
     *
     * @param collaboratorTypes the types the graph points at without owning
     * @return the fixture
     */
    public static SaveFormatFixture createStandingInFor(Class<?>... collaboratorTypes) {

        var xstream = new com.thoughtworks.xstream.XStream(new com.thoughtworks.xstream.io.xml.DomDriver());

        xstream.registerConverter(new CollaboratorConverter(List.of(collaboratorTypes)));

        return new SaveFormatFixture(xstream);
    }

    /**
     * The non-blank lines of a checked-in expected-paths resource beside {@code anchor}, in file order. Kept sorted in
     * the file, the list compares element for element with {@link #listElementPaths(Object)}.
     *
     * @param anchor       the class whose package the resource sits in
     * @param resourceName the resource's file name
     * @return the expected paths
     */
    public static List<String> readExpectedPaths(Class<?> anchor, String resourceName) {

        try (var stream = anchor.getResourceAsStream(resourceName)) {

            var text = new String(
                Objects
                    .requireNonNull(stream, "No resource " + resourceName + " beside " + anchor.getName())
                    .readAllBytes(),
                StandardCharsets.UTF_8);

            return Arrays
                .stream(text.split("\\R"))
                .filter(line -> !line.isBlank())
                .toList();

        } catch (IOException exception) {

            throw new UncheckedIOException(exception);
        }
    }

    /**
     * Every element path {@code graph} writes, sorted - each element named by its path from the root, the way XStream
     * names a field by the field and an unnamed entry by its class. Attributes and values are left out: the paths are
     * the field set, which is what an existing save depends on.
     *
     * @param graph the object graph a save would carry
     * @return the paths, sorted
     */
    public SortedSet<String> listElementPaths(Object graph) {

        try {
            var document = DocumentBuilderFactory
                .newInstance()
                .newDocumentBuilder()
                .parse(new ByteArrayInputStream(writeSave(graph).getBytes(StandardCharsets.UTF_8)));

            var paths = new TreeSet<String>();

            collectElementPaths(document.getDocumentElement(), "", paths);

            return paths;

        } catch (Exception exception) {

            throw new IllegalStateException("The written save did not parse as XML.", exception);
        }
    }

    /**
     * Writes {@code graph} and reads it back as a load would, returning the restored copy.
     *
     * @param graph the object graph a save would carry
     * @param <T>   the graph's root type
     * @return the restored copy
     */
    @SuppressWarnings("unchecked")
    public <T> T restoreThroughSave(T graph) {

        return (T) xstream.fromXML(writeSave(graph));
    }

    /**
     * The XML {@code graph} writes.
     *
     * @param graph the object graph a save would carry
     * @return the written XML
     */
    public String writeSave(Object graph) {

        return xstream.toXML(graph);
    }

    private static void collectElementPaths(Element element, String parentPath, SortedSet<String> paths) {

        var path = parentPath.isEmpty()
            ? element.getTagName()
            : parentPath + "/" + element.getTagName();

        paths.add(path);

        var children = element.getChildNodes();

        for (var index = 0; index < children.getLength(); index++) {

            var child = children.item(index);

            if (child.getNodeType() == Node.ELEMENT_NODE) {
                collectElementPaths((Element) child, path, paths);
            }
        }
    }

    // Writes a collaborator as an empty element and reads one back as null. The element is still written, so the
    // field naming it stays in the paths.
    private static final class CollaboratorConverter implements com.thoughtworks.xstream.converters.Converter {

        private final List<Class<?>> collaboratorTypes;

        private CollaboratorConverter(List<Class<?>> collaboratorTypes) {

            this.collaboratorTypes = collaboratorTypes;
        }

        @Override
        @SuppressWarnings("rawtypes")
        public boolean canConvert(Class type) {

            return type != null
                && collaboratorTypes
                    .stream()
                    .anyMatch(collaborator -> collaborator.isAssignableFrom(type));
        }

        @Override
        public void marshal(
                Object source,
                com.thoughtworks.xstream.io.HierarchicalStreamWriter writer,
                com.thoughtworks.xstream.converters.MarshallingContext context) {
            // Nothing below the element: the field name is what the save format pins here.
        }

        @Override
        public Object unmarshal(
                com.thoughtworks.xstream.io.HierarchicalStreamReader reader,
                com.thoughtworks.xstream.converters.UnmarshallingContext context) {

            return null;
        }
    }
}
