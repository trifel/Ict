package cfb.ict;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Utils {

    static void log(final String message) {

        System.out.println((new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")).format(new Date(System.currentTimeMillis()))
                + ": " + message);
    }
}
