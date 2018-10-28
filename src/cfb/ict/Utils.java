package cfb.ict;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Utils {

    private static final String TRYTES = "9ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final byte[][] TRYTES_TRITS = new byte[][] {

            {0, 0, 0},
            {1, 0, 0},
            {-1, 1, 0},
            {0, 1, 0},
            {1, 1, 0},
            {-1, -1, 1},
            {0, -1, 1},
            {1, -1, 1},
            {-1, 0, 1},
            {0, 0, 1},
            {1, 0, 1},
            {-1, 1, 1},
            {0, 1, 1},
            {1, 1, 1},
            {-1, -1, -1},
            {0, -1, -1},
            {1, -1, -1},
            {-1, 0, -1},
            {0, 0, -1},
            {1, 0, -1},
            {-1, 1, -1},
            {0, 1, -1},
            {1, 1, -1},
            {-1, -1, 0},
            {0, -1, 0},
            {1, -1, 0},
            {-1, 0, 0}
    };

    static void log(final String message) {

        System.out.println((new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")).format(new Date(System.currentTimeMillis()))
                + ": " + message);
    }

    static String trytes(final byte[] trits, final int offset, final int length) { // length must be a multiple of 3

        final StringBuilder trytes = new StringBuilder();

        for (int i = 0; i < length / 3; i++) {

            int j = trits[offset + i * 3] + trits[offset + i * 3 + 1] * 3 + trits[offset + i * 3 + 2] * 9;
            if (j < 0) {

                j += TRYTES.length();
            }
            trytes.append(TRYTES.charAt(j));
        }

        return trytes.toString();
    }

    static byte[] trits(final String trytes) {

        final byte[] trits = new byte[trytes.length() * 3];

        for (int i = 0; i < trytes.length(); i++) {

            System.arraycopy(TRYTES_TRITS[TRYTES.indexOf(trytes.charAt(i))], 0, trits, i * 3, 3);
        }

        return trits;
    }

    static void convertTritsToBytesTrinary(final byte[] trits, int tritsOffset, int tritsLength, // tritsLength must be a multiple of 6
                                           final byte[] bytes, final int bytesOffset) {

        final ByteBuffer bytesBuffer = (ByteBuffer) ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).position(bytesOffset);

        do {

            int value = 0;

            for (int i = 6; i-- > 0; ) {

                value = value * 3 + trits[tritsOffset + i];
            }
            tritsOffset += 6;

            bytesBuffer.put((byte) value);

        } while ((tritsLength -= 6) > 0);
    }

    static void convertBytesToTritsTrinary(final byte[] bytes, final int bytesOffset, int bytesLength,
                                           final byte[] trits, int tritsOffset) {

        final ByteBuffer bytesBuffer = (ByteBuffer) ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).position(bytesOffset);

        do {

            final int value = bytesBuffer.get();

            int absoluteValue = value < 0 ? -value : value;
            for (int i = 0; i < 6; i++) {

                int remainder = absoluteValue % 3;
                absoluteValue /= 3;
                if (remainder > 1) {

                    remainder = -1;
                    absoluteValue++;
                }
                trits[tritsOffset++] = (byte) (value < 0 ? -remainder : remainder);
            }

        } while (--bytesLength > 0);
    }

    static void convertTritsToBytesBinary(final byte[] trits, int tritsOffset, int tritsLength, // tritsLength must be a multiple of 9
                                          final byte[] bytes, final int bytesOffset) {

        final ByteBuffer bytesBuffer = (ByteBuffer) ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).position(bytesOffset);

        do {

            int value = 0;

            for (int i = 9; i-- > 0; ) {

                value = value * 3 + trits[tritsOffset + i];
            }
            tritsOffset += 9;

            bytesBuffer.putShort((short) value);

        } while ((tritsLength -= 9) > 0);
    }

    static void convertBytesToTritsBinary(final byte[] bytes, final int bytesOffset, int bytesLength, // bytesLength must be a multiple of 2
                                          final byte[] trits, int tritsOffset) {

        final ByteBuffer bytesBuffer = (ByteBuffer) ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).position(bytesOffset);

        do {

            final int value = bytesBuffer.getShort();

            int absoluteValue = value < 0 ? -value : value;
            for (int i = 0; i < 9; i++) {

                int remainder = absoluteValue % 3;
                absoluteValue /= 3;
                if (remainder > 1) {

                    remainder = -1;
                    absoluteValue++;
                }
                trits[tritsOffset++] = (byte) (value < 0 ? -remainder : remainder);
            }

        } while ((bytesLength -= 2) > 0);
    }
}
