package cfb.ict;

import java.io.FileInputStream;

public class Properties {

    String host = "localhost";
    int port = 14265;
    int roundDuration = 60000; // In milliseconds
    int minEchoDelay = 0, maxEchoDelay = 5000; // In milliseconds

    long timestampLowerBoundDelta = 90, timestampUpperBoundDelta = 90; // In seconds

    String neighborAHost, neighborBHost, neighborCHost;
    int neighborAPort, neighborBPort, neighborCPort;

    Properties() {
    }

    Properties(final String fileName) {

        final java.util.Properties properties = new java.util.Properties();

        try (final FileInputStream propertiesInputStream = new FileInputStream(fileName)) {

            properties.load(propertiesInputStream);

        } catch (final Exception e) {

            throw new RuntimeException(e);
        }

        host = properties.getProperty("host", host);
        port = Integer.parseInt(properties.getProperty("port", Integer.valueOf(port).toString()));
        roundDuration = Integer.parseInt(properties.getProperty("roundDuration", Integer.valueOf(roundDuration).toString()));
        minEchoDelay = Integer.parseInt(properties.getProperty("minEchoDelay", Integer.valueOf(minEchoDelay).toString()));
        maxEchoDelay = Integer.parseInt(properties.getProperty("maxEchoDelay", Integer.valueOf(maxEchoDelay).toString()));

        timestampLowerBoundDelta = Long.parseLong(properties.getProperty("timestampLowerBoundDelta", Long.valueOf(timestampLowerBoundDelta).toString()));
        timestampUpperBoundDelta = Long.parseLong(properties.getProperty("timestampUpperBoundDelta", Long.valueOf(timestampUpperBoundDelta).toString()));

        neighborAHost = properties.getProperty("neighborAHost");
        neighborAPort = Integer.parseInt(properties.getProperty("neighborAPort"));
        neighborBHost = properties.getProperty("neighborBHost");
        neighborBPort = Integer.parseInt(properties.getProperty("neighborBPort"));
        neighborCHost = properties.getProperty("neighborCHost");
        neighborCPort = Integer.parseInt(properties.getProperty("neighborCPort"));
    }
}
