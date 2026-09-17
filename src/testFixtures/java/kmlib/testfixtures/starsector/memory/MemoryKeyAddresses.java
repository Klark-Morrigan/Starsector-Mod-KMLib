package kmlib.testfixtures.starsector.memory;

import kmlib.starsector.memory.MemoryKeyAddress;

/**
 * Stand-in addresses for the suites that have to store a value at one point on an axis without being
 * about what the axis is. Each appends one segment to the base key it is handed, which is the simplest
 * partitioning an address can be and the only shape these cases need.
 *
 * <p>Two of them, distinct and nothing else. A case whose subject is that two addresses keep a value
 * apart needs only that they are two, and a consumer's real axis - what its points are and how it spells
 * them - is that consumer's to pin.
 */
public final class MemoryKeyAddresses {

    // What parts a base key from an address segment. The separator is the address's, not the store's,
    // so a stand-in states it here as a real one would.
    private static final String SEGMENT_SEPARATOR = "_";

    private MemoryKeyAddresses() {
    }

    /**
     * @return a second address, composing a key that is not {@link #createStandInAddress()}'s
     */
    public static MemoryKeyAddress createOtherStandInAddress() {
        return createAddressSuffixed("other");
    }

    /**
     * @return an address of no particular identity, for cases whose subject is anything but which
     *         point on the axis holds the value
     */
    public static MemoryKeyAddress createStandInAddress() {
        return createAddressSuffixed("test");
    }

    private static MemoryKeyAddress createAddressSuffixed(String segment) {
        return baseKey -> baseKey + SEGMENT_SEPARATOR + segment;
    }
}
