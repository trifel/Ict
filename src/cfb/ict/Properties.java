package cfb.ict;

import java.io.FileInputStream;

public class Properties {

    final String host;
    final int port;
    final int roundDuration; // In milliseconds
    final int minEchoDelay, maxEchoDelay; // In milliseconds

    final long timestampLowerBoundDelta, timestampUpperBoundDelta; // In seconds

    Properties(final String fileName) {

        final java.util.Properties properties = new java.util.Properties();

        try (final FileInputStream propertiesInputStream = new FileInputStream(fileName)) {

            properties.load(propertiesInputStream);

        } catch (final Exception e) {

            throw new RuntimeException(e);
        }

        host = properties.getProperty("host", "localhost");
        port = Integer.parseInt(properties.getProperty("port", "14265"));
        roundDuration = Integer.parseInt(properties.getProperty("roundDuration", "60000"));
        minEchoDelay = Integer.parseInt(properties.getProperty("minEchoDelay", "0"));
        maxEchoDelay = Integer.parseInt(properties.getProperty("maxEchoDelay", "5000"));

        timestampLowerBoundDelta = Long.parseLong(properties.getProperty("timestampLowerBoundDelta", "90"));
        timestampUpperBoundDelta = Long.parseLong(properties.getProperty("timestampUpperBoundDelta", "90"));
    }
}
