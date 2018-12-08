package cfb.ict.service;

import cfb.ict.IXI;
import cfb.ict.Node;
import cfb.ict.service.dto.AbstractResponse;
import cfb.ict.service.dto.AccessLimitedResponse;
import cfb.ict.service.dto.ErrorResponse;
import cfb.ict.service.dto.ExceptionResponse;
import cfb.ict.utils.IotaIOUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.undertow.Undertow;
import io.undertow.security.api.AuthenticationMode;
import io.undertow.security.impl.BasicAuthenticationMechanism;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.channels.StreamSinkChannel;
import org.xnio.streams.ChannelInputStream;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static io.undertow.Handlers.path;

/**
 * <p>
 *   The API makes it possible to interact with the node by requesting information or actions to be taken. 
 *   You can interact with it by passing a JSON object which at least contains a <tt>command</tt>.
 *   Upon successful execution of the command, the API returns your requested information in an {@link AbstractResponse}.
 * </p>
 * <p>
 *   If the request is invalid, an {@link ErrorResponse} is returned. 
 *   This, for example, happens when the command does not exist or there is no command section at all.
 *   If there is an error in the given data during the execution of a command, an {@link ErrorResponse} is also sent. 
 * </p>
 * <p>
 *   If an Exception is thrown during the execution of a command, an {@link ExceptionResponse} is returned.
 * </p>
 */
@SuppressWarnings("unchecked")
public class API {
    
    private static final Logger log = LoggerFactory.getLogger(API.class);
    private final IXI ixi;

    private Undertow server;

    private final Gson gson = new GsonBuilder().create();

    private final AtomicInteger counter = new AtomicInteger(0);

    private final int maxRequestList;
    private final int maxBodyLength;

    private Node instance;

    /**
     * Starts loading the IOTA API, parameters do not have to be initialized.
     * 
     * @param instance The data source we interact with during any API call.
     * @param ixi If a command is not in the standard API, 
     *        we try to process it as a Nashorn JavaScript module through {@link IXI}
     */
    public API(Node instance, IXI ixi) {
        this.instance = instance;
        this.ixi = ixi;
        maxRequestList = APIProperties.getMaxRequestsList();
        maxBodyLength = APIProperties.getMaxBodyLength();
    }

    /**
     * Prepares the IOTA API for usage. Until this method is called, no HTTP requests can be made.
     * The order of loading is as follows
     * <ol>
     *    <li>
     *        Builds a secure {@link Undertow} server with the port and host.
     *        If {@link APIProperties#getRemoteAuth()} is defined, remote authentication is blocked for anyone except
     *         those defined in {@link APIProperties#getRemoteAuth()} or localhost.
     *        This is done with {@link BasicAuthenticationMechanism} in a {@link AuthenticationMode#PRO_ACTIVE} mode.
     *        By default, this authentication is disabled.
     *    </li>
     *    <li>
     *        Starts the server, opening it for HTTP API requests
     *    </li>
     * </ol>
     */
    public void init() {

        final int apiPort = instance.getProperties().getPort();
        final String apiHost = instance.getProperties().getHost();

        log.debug("Binding JSON-REST API Undertow server on {}:{}", apiHost, apiPort);

        server = Undertow.builder().addHttpListener(apiPort, apiHost)
                .setHandler(path().addPrefixPath("/", addSecurity(new HttpHandler() {
                    @Override
                    public void handleRequest(final HttpServerExchange exchange) throws Exception {
                        HttpString requestMethod = exchange.getRequestMethod();
                        if (Methods.OPTIONS.equals(requestMethod)) {
                            String allowedMethods = "GET,HEAD,POST,PUT,DELETE,TRACE,OPTIONS,CONNECT,PATCH";
                            //return list of allowed methods in response headers
                            exchange.setStatusCode(StatusCodes.OK);
                            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, MimeMappings.DEFAULT_MIME_MAPPINGS.get("txt"));
                            exchange.getResponseHeaders().put(Headers.CONTENT_LENGTH, 0);
                            exchange.getResponseHeaders().put(Headers.ALLOW, allowedMethods);
                            exchange.getResponseHeaders().put(new HttpString("Access-Control-Allow-Origin"), "*");
                            exchange.getResponseHeaders().put(new HttpString("Access-Control-Allow-Headers"), "Origin, X-Requested-With, Content-Type, Accept, X-IOTA-API-Version");
                            exchange.getResponseSender().close();
                            return;
                        }

                        if (exchange.isInIoThread()) {
                            exchange.dispatch(this);
                            return;
                        }
                        processRequest(exchange);
                    }
                }))).build();
        server.start();
    }
    
    /**
     * Sends the API response back as JSON to the requester. 
     * Status code of the HTTP request is also set according to the type of response.
     * <ul>
     *     <li>{@link ErrorResponse}: 400</li>
     *     <li>{@link AccessLimitedResponse}: 401</li>
     *     <li>{@link ExceptionResponse}: 500</li>
     *     <li>Default: 200</li>
     * </ul>
     * 
     * @param exchange Contains information about what the client sent to us
     * @param res The response of the API. 
     *            See {@link #processRequest(HttpServerExchange)} 
     *            and {@link #process(String, InetSocketAddress)} for the different responses in each case.
     * @param beginningTime The time when we received the request, in milliseconds. 
     *                      This will be used to set the response duration in {@link AbstractResponse#setDuration(Integer)}
     * @throws IOException When connection to client has been lost - Currently being caught.
     */
    private void sendResponse(HttpServerExchange exchange, AbstractResponse res, long beginningTime) throws IOException {
        res.setDuration((int) (System.currentTimeMillis() - beginningTime));
        final String response = gson.toJson(res);

        if (res instanceof ErrorResponse) {
            // bad request or invalid parameters
            exchange.setStatusCode(400); 
        } else if (res instanceof AccessLimitedResponse) {
            // API method not allowed
            exchange.setStatusCode(401); 
        } else if (res instanceof ExceptionResponse) {
            // internal error
            exchange.setStatusCode(500); 
        }

        setupResponseHeaders(exchange);

        ByteBuffer responseBuf = ByteBuffer.wrap(response.getBytes(StandardCharsets.UTF_8));
        exchange.setResponseContentLength(responseBuf.array().length);
        StreamSinkChannel sinkChannel = exchange.getResponseChannel();
        sinkChannel.getWriteSetter().set( channel -> {
            if (responseBuf.remaining() > 0) {
                try {
                    sinkChannel.write(responseBuf);
                    if (responseBuf.remaining() == 0) {
                        exchange.endExchange();
                    }
                } catch (IOException e) {
                    log.error("Lost connection to client - cannot send response");
                    exchange.endExchange();
                    sinkChannel.getWriteSetter().set(null);
                }
            }
            else {
                exchange.endExchange();
            }
        });
        sinkChannel.resumeWrites();
    }

    /**
     * <p>
     *     Processes an API HTTP request. 
     *     No checks have been done until now, except that it is not an OPTIONS request.
     *     We can be sure that we are in a thread that allows blocking.
     * </p>
     * <p>
     *     The request process duration is recorded. 
     *     During this the request gets verified. If it is incorrect, an {@link ErrorResponse} is made.
     *     Otherwise it is processed in {@link #process(String, InetSocketAddress)}.
     *     The result is sent back to the requester.
     * </p>
     *   
     * @param exchange Contains the data the client sent to us
     * @throws IOException If the body of this HTTP request cannot be read
     */
    private void processRequest(final HttpServerExchange exchange) throws IOException {
        final ChannelInputStream cis = new ChannelInputStream(exchange.getRequestChannel());
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");

        final long beginningTime = System.currentTimeMillis();
        final String body = IotaIOUtils.toString(cis, StandardCharsets.UTF_8);
        final AbstractResponse response;

        if (!exchange.getRequestHeaders().contains("X-IOTA-API-Version")) {
            response = ErrorResponse.create("Invalid API Version");
        } else if (body.length() > maxBodyLength) {
            response = ErrorResponse.create("Request too long");
        } else {
            response = process(body, exchange.getSourceAddress());
        }
        sendResponse(exchange, response, beginningTime);
    }

    /**
     * Handles an API request body. 
     * Its returned {@link AbstractResponse} is created using the following logic
     * <ul>
     *     <li>
     *         {@link ExceptionResponse} if the body cannot be parsed.
     *     </li>
     *     <li>
     *         {@link ErrorResponse} if the body does not contain a '<tt>command</tt>' section.
     *     </li>
     *     <li>
     *         {@link AccessLimitedResponse} if the command is not allowed on this node.
     *     </li>
     *     <li>
     *         {@link ErrorResponse} if the command contains invalid parameters.
     *     </li>
     *     <li>
     *         {@link ExceptionResponse} if we encountered an unexpected exception during command processing.
     *     </li>
     *     <li>
     *         {@link AbstractResponse} when the command is successfully processed. 
     *         The response class depends on the command executed.
     *     </li>
     * </ul>
     * 
     * @param requestString The JSON encoded data of the request.
     *                      This String is attempted to be converted into a {@code Map<String, Object>}.
     * @param sourceAddress The address from the sender of this API request.
     * @return The result of this request. 
     * @throws java.io.UnsupportedEncodingException If the requestString cannot be parsed into a Map.
                                            Currently caught and turned into a {@link ExceptionResponse}.
     */
    private AbstractResponse process(final String requestString, InetSocketAddress sourceAddress) 
            throws UnsupportedEncodingException {

        try {
            // Request JSON data into map
            final Map<String, Object> request = gson.fromJson(requestString, Map.class);
            if (request == null) {
                return ExceptionResponse.create("Invalid request payload: '" + requestString + "'");
            }

            // Did the requester ask for a command?
            final String command = (String) request.get("command");
            if (command == null) {
                return ErrorResponse.create("COMMAND parameter has not been specified in the request.");
            }

            // Is this command allowed to be run from this request address? 
            // We check the remote limit API configuration.
            if (APIProperties.getRemoteLimitApi().contains(command) &&
                    !sourceAddress.getAddress().isLoopbackAddress()) {
                return AccessLimitedResponse.create("COMMAND " + command + " is not available on this node");
            }

            log.debug("# {} -> Requesting command '{}'", counter.incrementAndGet(), command);

            switch (command) {
                default: {
                    AbstractResponse response = ixi.processCommand(command, request);
                    return response == null ?
                            ErrorResponse.create("Command [" + command + "] is unknown") :
                            response;
                }
            }

        } catch (final Exception e) {
            log.error("API Exception: {}", e.getLocalizedMessage(), e);
            return ExceptionResponse.create(e.getLocalizedMessage());
        }
    }

    /**
     * Updates the {@link HttpServerExchange} {@link HeaderMap} with the proper response settings.
     * @param exchange Contains information about what the client has send to us 
     */
    private static void setupResponseHeaders(HttpServerExchange exchange) {
        final HeaderMap headerMap = exchange.getResponseHeaders();
        headerMap.add(new HttpString("Access-Control-Allow-Origin"),"*");
        headerMap.add(new HttpString("Keep-Alive"), "timeout=500, max=100");
    }

    /**
     * Sets up the {@link HttpHandler} to have correct security settings.
     * Remote authentication is blocked for anyone except 
     * those defined in {@link APIProperties#getRemoteAuth()} or localhost.
     * This is done with {@link BasicAuthenticationMechanism} in a {@link AuthenticationMode#PRO_ACTIVE} mode.
     * 
     * @param toWrap the path handler used in creating the server.
     * @return The updated handler
     */
    private HttpHandler addSecurity(HttpHandler toWrap) {
        String credentials = APIProperties.getRemoteAuth();
        if (credentials == null || credentials.isEmpty()) {
            return toWrap;
        }

        // API open to all hosts
        return toWrap;
        /*final Map<String, char[]> users = new HashMap<>(2);
        users.put(credentials.split(":")[0], credentials.split(":")[1].toCharArray());

        IdentityManager identityManager = new MapIdentityManager(users);
        HttpHandler handler = toWrap;
        handler = new AuthenticationCallHandler(handler);
        handler = new AuthenticationConstraintHandler(handler);
        final List<AuthenticationMechanism> mechanisms = 
                Collections.singletonList(new BasicAuthenticationMechanism("Iota Realm"));
        
        handler = new AuthenticationMechanismsHandler(handler, mechanisms);
        handler = new SecurityInitialHandler(AuthenticationMode.PRO_ACTIVE, identityManager, handler);
        return handler;*/
    }

    /**
     * If a server is running, stops the server from accepting new incoming requests.
     * Does not remove the instance, so the server may be restarted without having to recreate it.
     */
    public void shutDown() {
        if (server != null) {
            server.stop();
        }
    }
}