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

        final Node node = new Node(properties, new Tangle());

        node.run();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {

            try {

                node.stop();

                Utils.log("Ict " + VERSION + " is shut down.");

            } catch (final Exception e) {

                e.printStackTrace();
            }

        }, "Shutdown Hook"));
    }
}
