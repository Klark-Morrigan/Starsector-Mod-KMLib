package kmlib.math.hashing;

/**
 * murmur3's fmix32 avalanche finalizer: spreads every input bit across all 32
 * output bits, so inputs that differ in a single bit - or share their low bits -
 * map to unrelated outputs. Meant for combining hashes by summing or XOR-ing,
 * where feeding raw values into a commutative combine lets small or related inputs
 * cancel; avalanching each one first makes a cancellation need a full 32-bit
 * coincidence.
 *
 * <p>Bijective: distinct inputs always map to distinct outputs, so it never
 * manufactures a collision of its own. Its one fixed point is 0 ({@code
 * mixBits(0) == 0}); a caller that must keep a 0 input observable in the combine
 * should XOR in a non-zero seed before mixing.
 */
public final class Avalanche {
    // fmix32's shift/multiply schedule. These are the constants murmur3 specifies
    // for the finalizer; they carry no meaning to name one by one beyond that.
    private static final int SHIFT_HIGH = 16;
    private static final int SHIFT_MID = 13;
    private static final int MULTIPLY_1 = 0x85ebca6b;
    private static final int MULTIPLY_2 = 0xc2b2ae35;

    private Avalanche() {
    }

    /**
     * Avalanches a 32-bit value through murmur3's fmix32 finalizer.
     *
     * @param bits the value to mix
     * @return the input with every bit spread across all 32 output bits
     */
    public static int mixBits(int bits) {
        bits ^= (bits >>> SHIFT_HIGH);
        bits *= MULTIPLY_1;
        bits ^= (bits >>> SHIFT_MID);
        bits *= MULTIPLY_2;
        bits ^= (bits >>> SHIFT_HIGH);
        return bits;
    }
}
