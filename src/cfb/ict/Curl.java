package cfb.ict;

public class Curl { // TODO: Replace with a cryptographic hash function

    static final int HASH_LENGTH = 243;
    private static final int STATE_LENGTH = 3 * HASH_LENGTH;
    private static final byte[] TRUTH_TABLE = {1, 0, -1, 1, -1, 0, -1, 1, 0};
    private static final byte[] INITIAL_STATE = new byte[STATE_LENGTH];

    private final byte[] state = new byte[STATE_LENGTH];
    private final byte[] scratchpad = new byte[STATE_LENGTH];

    void reset() {

        System.arraycopy(INITIAL_STATE, 0, state, 0, STATE_LENGTH);
    }

    void absorb(final byte[] trits, int offset, int length) {

        do {

            System.arraycopy(trits, offset, state, 0, length < HASH_LENGTH ? length : HASH_LENGTH);
            transform();
            offset += HASH_LENGTH;

        } while ((length -= HASH_LENGTH) > 0);
    }

    void squeeze(final byte[] trits, int offset, int length) {

        do {

            System.arraycopy(state, 0, trits, offset, length < HASH_LENGTH ? length : HASH_LENGTH);
            transform();
            offset += HASH_LENGTH;

        } while ((length -= HASH_LENGTH) > 0);
    }

    void getInnerState(final byte[] trits, final int offset) {

        System.arraycopy(state, HASH_LENGTH, trits, offset, (STATE_LENGTH - HASH_LENGTH));
    }

    void setInnerState(final byte[] trits, final int offset) {

        System.arraycopy(trits, offset, state, HASH_LENGTH, (STATE_LENGTH - HASH_LENGTH));
    }

    private void transform() {

        int scratchpadIndex = 0;
        for (int round = 123; round-- > 0; ) {

            System.arraycopy(state, 0, scratchpad, 0, STATE_LENGTH);
            for (int stateIndex = 0; stateIndex < STATE_LENGTH; stateIndex++) {

                state[stateIndex] = TRUTH_TABLE[scratchpad[scratchpadIndex] + scratchpad[scratchpadIndex += (scratchpadIndex < 365 ? 364 : -365)] * 3 + 4];
            }
        }
    }
}
