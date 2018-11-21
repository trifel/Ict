package cfb.ict;

import java.util.Arrays;

public class Hash {

    static final int LENGTH = 243;
    static final Hash NULL = new Hash(new byte[LENGTH], 0);

    final byte[] trits;

    private final int hashCode;

    Hash(final byte[] trits, final int offset) {

        this.trits = Arrays.copyOfRange(trits, offset, offset + LENGTH);

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
