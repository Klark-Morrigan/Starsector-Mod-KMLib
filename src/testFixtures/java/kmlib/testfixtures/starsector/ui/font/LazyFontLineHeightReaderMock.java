package kmlib.testfixtures.starsector.ui.font;

import kmlib.starsector.ui.font.LazyFontLineHeightReader;
import kmlib.starsector.ui.font.StarsectorFont;

import org.mockito.MockedStatic;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;

/**
 * The live line-height reader, {@link LazyFontLineHeightReader}, held still for code that composes a
 * native face from it directly - a look built statically per paint, with no reader handed in.
 *
 * <p>The live reader loads the atlas through LazyLib, which needs a running game, so a suite over such a
 * look cannot reach it. This answers every read from a {@link FaceLineHeightReaderFake} instead, which
 * is where the suite states what the install holds. Install it in setup and close it in teardown - a
 * leaked static mock poisons the next class in the run to touch the same type.
 */
public final class LazyFontLineHeightReaderMock implements AutoCloseable {

    private final MockedStatic<LazyFontLineHeightReader> readerStaticMock;

    private LazyFontLineHeightReaderMock(MockedStatic<LazyFontLineHeightReader> readerStaticMock) {
        this.readerStaticMock = readerStaticMock;
    }

    /**
     * Holds the live reader still, answering each face as {@code lineHeightsFake} does.
     *
     * @param lineHeightsFake what the install is taken to hold
     * @return the installed mock, to be closed when the case is done with it
     */
    public static LazyFontLineHeightReaderMock install(FaceLineHeightReaderFake lineHeightsFake) {

        var readerStaticMock = mockStatic(LazyFontLineHeightReader.class);

        readerStaticMock
            .when(() -> LazyFontLineHeightReader.readLineHeight(any(StarsectorFont.class)))
            .thenAnswer(call -> lineHeightsFake.readLineHeight(call.getArgument(0)));

        return new LazyFontLineHeightReaderMock(readerStaticMock);
    }

    /** Takes the live reader back down. */
    @Override
    public void close() {
        readerStaticMock.close();
    }
}
