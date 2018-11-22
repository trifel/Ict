package cfb.ict;

public class Ict {

    static final String VERSION = "0.3.1";

    static Properties properties;

    static Ixi ixi;

    public static void main(final String[] args) {

        Utils.log("Ict " + VERSION + " is launched.");

        try {

            properties = new Properties(args[0]);

        } catch (final RuntimeException e) {

            properties = new Properties();
        }

        final Node node = new Node(properties, new Tangle());

        ixi = new Ixi(node, VERSION);
        try {
            ixi.init();
        } catch (Exception e) {
            Utils.log("error: could not initialize ixi");
            e.printStackTrace();
        }

        node.run();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {

            try {

                ixi.shutdown();
                
                node.stop();

                Utils.log("Ict " + VERSION + " is shut down.");

            } catch (final Exception e) {

                e.printStackTrace();
            }

        }, "Shutdown Hook"));
    }
}
