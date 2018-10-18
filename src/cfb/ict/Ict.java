package cfb.ict;

public class Ict {

    static final String VERSION = "0.2.0";

    static Properties properties;

    public static void main(final String[] args) {

        Utils.log("Ict " + VERSION + " is launched.");

        try {

            properties = new Properties(args[0]);

        } catch (final RuntimeException e) {

            properties = new Properties();
        }

        Utils.log("Ict " + VERSION + " is shut down.");
    }
}
