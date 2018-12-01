package cfb.ict;

import java.util.Arrays;

public class Hash {

    static final int LENGTH = 243;
    static final Hash NULL = new Hash(new byte[LENGTH], 0, LENGTH);

    final byte[] trits;

    private final int hashCode;

    Hash(final byte[] trits, final int offset, final int length) {

        this.trits = new byte[LENGTH];
        System.arraycopy(trits, offset, this.trits, 0, Math.min(LENGTH, length));

        hashCode = Arrays.hashCode(this.trits);
    }

    @Override
    public boolean equals(final Object obj) {

        return Arrays.equals(trits, ((Hash) obj).trits); // The class check is omitted on purpose
    }

    @Override
    public int hashCode() {

        return hashCode;
    }

    @Override
    public String toString() {

        return Utils.trytes(trits, 0, trits.length);
    }
}
