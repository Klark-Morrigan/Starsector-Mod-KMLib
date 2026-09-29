package kmlib.testfixtures.statics;

import org.mockito.MockedStatic;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.mockStatic;

/**
 * The static seams one arrangement has standing, closed in reverse on the way out so a seam opened
 * over another is never left standing when the inner one is already gone.
 *
 * <p>For an arrangement that stands in for more than one class at once, which otherwise carries its
 * own copy of the list, the reverse loop and the open-and-remember helper. What each arrangement
 * varies is which seams it opens and what it answers on them, which stays where it is read.
 *
 * <p>A seam some fixture opens with answers of its own - {@code Global}'s, which has to answer
 * loggers - is opened there and handed to {@link #holdSeam} rather than opened bare here.
 */
public final class StaticSeams {

    private final List<MockedStatic<?>> openSeams = new ArrayList<>();

    /**
     * Closes every seam this holds, innermost first, and forgets them - what an arrangement runs on
     * the way out, since a seam left standing stands in for its class for whatever runs next.
     */
    public void closeEverySeam() {

        for (var index = openSeams.size() - 1; index >= 0; index--) {
            openSeams.get(index).close();
        }
        openSeams.clear();
    }

    /**
     * Holds a seam opened elsewhere, so it closes with the rest and in its place in the order.
     *
     * @param openSeam the open seam this takes over closing
     * @param <T>      the stood-in class, so the caller states its answers without a cast
     * @return {@code openSeam}, handed back for the caller to answer on
     */
    public <T> MockedStatic<T> holdSeam(MockedStatic<T> openSeam) {

        openSeams.add(openSeam);
        return openSeam;
    }

    /**
     * @param seamedClass the class whose statics are stood in for
     * @param <T>         that class, so the caller states its answers without a cast
     * @return the open seam, held for closing and handed back for the caller to answer on
     */
    public <T> MockedStatic<T> openSeam(Class<T> seamedClass) {
        return holdSeam(mockStatic(seamedClass));
    }
}
