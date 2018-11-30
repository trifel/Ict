package cfb.ict;

import cfb.ict.service.API;

public class Ict {

    static final String VERSION = "0.3.1";

    static Properties properties;

    public static API api;
    static IXI ixi;

    public static void main(final String[] args) {

        Utils.log("Ict " + VERSION + " is launched.");

        try {

            properties = new Properties(args[0]);

        } catch (final RuntimeException e) {

            properties = new Properties();
        }

        final Node node = new Node(properties, new Tangle());

        ixi = new IXI(node, VERSION);
        api = new API(node, ixi);

        try {
            api.init();
            ixi.init();
        } catch (Exception e) {
            Utils.log("error: could not initialize ixi");
            e.printStackTrace();
        }

        node.run();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {

            try {

                ixi.shutdown();
                api.shutDown();
                
                node.stop();

                Utils.log("Ict " + VERSION + " is shut down.");

            } catch (final Exception e) {

                e.printStackTrace();
            }

        }, "Shutdown Hook"));
    }
}
