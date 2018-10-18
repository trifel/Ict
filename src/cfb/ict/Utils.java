package cfb.ict;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Utils {

    static void log(final String message) {

        System.out.println((new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")).format(new Date(System.currentTimeMillis()))
                + ": " + message);
    }

    static void convertTritsToBytes(final byte[] trits, int tritsOffset, int tritsLength, // tritsLength must be a multiple of 9
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

    static void convertBytesToTrits(final byte[] bytes, final int bytesOffset, int bytesLength, // bytesLength must be a multiple of 2
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
