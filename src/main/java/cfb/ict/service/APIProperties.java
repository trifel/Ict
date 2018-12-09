package cfb.ict.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class APIProperties {

    private static final Logger log = LoggerFactory.getLogger(APIProperties.class);

    private final static String PROPERTIES_FILE = "api.properties";
    private final static int MAX_REQUESTS_LIST;
    private final static int MAX_BODY_LENGTH;
    private final static List<String> REMOTE_LIMIT_API;
    private final static String REMOTE_AUTH;

    static {
        final java.util.Properties properties = new java.util.Properties();

        try (final FileInputStream propertiesInputStream = new FileInputStream(PROPERTIES_FILE)) {

            properties.load(propertiesInputStream);

        } catch (final Exception e) {

            // OK, default values will be used
            log.debug("api.properties not found, default values will be used");
        }

        MAX_REQUESTS_LIST = Integer.parseInt(properties.getProperty("maxRequestsList", "1000").trim ());
        MAX_BODY_LENGTH = Integer.parseInt(properties.getProperty("maxBodyLength", "1000000").trim ());
        REMOTE_LIMIT_API = Collections.unmodifiableList(Arrays.asList(properties.getProperty("remoteLimitApi", "").trim ().split(",")));
        REMOTE_AUTH = properties.getProperty("remoteAuth", "").trim ();

        log.debug("MAX_REQUESTS_LIST: " + APIProperties.getMaxRequestsList());
        log.debug("MAX_BODY_LENGTH: " + APIProperties.getMaxBodyLength());
        log.debug("REMOTE_LIMIT_API: " + APIProperties.getRemoteLimitApi());
        log.debug("REMOTE_AUTH: " + APIProperties.getRemoteAuth());
    }

    public static int getMaxRequestsList() {
        return MAX_REQUESTS_LIST;
    }

    public static int getMaxBodyLength() {
        return MAX_BODY_LENGTH;
    }

    public static List<String> getRemoteLimitApi() {
        return REMOTE_LIMIT_API;
    }

    public static String getRemoteAuth() {
        return REMOTE_AUTH;
    }
}
