package nl.rubixstudios.clunkybungee.spigot;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import nl.rubixstudios.clunkybungee.spigot.storage.ClunkyStorage;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Encapsulates a BungeeCord "ip forwarding" handshake result.
 */
public class BungeeBackS {

    /** The name of the BungeeGuard auth token. */
    private static final String CLUNKYBUNGEE_TOKEN_NAME = "clunkybungee-token";
    /** The key used to define the name of properties in the handshake. */
    private static final String PROPERTY_NAME_KEY = "name";
    /** The key used to define the value of properties in the handshake. */
    private static final String PROPERTY_VALUE_KEY = "value";
    /** The maximum allowed length of the handshake. */
    private static final int HANDSHAKE_LENGTH_LIMIT = 2500;

    /** Shared Gson instance. */
    private static final Gson GSON = new Gson();
    /** The type of the property list in the handshake. */
    private static final Type PROPERTY_LIST_TYPE = new TypeToken<List<JsonObject>>(){}.getType();

    /**
     * Decodes a BungeeCord handshake, additionally ensuring it contains a
     * BungeeGuard token allowed by the {@link ClunkyStorage}.
     *
     * @param handshake the handshake data
     * @param clunkyStorage the token store
     * @return the handshake result
     */
    public static BungeeBackS decodeAndVerify(String handshake, ClunkyStorage clunkyStorage) {
        try {
            return decodeAndVerify0(handshake, clunkyStorage);
        } catch (Exception e) {
            new Exception("Failed to decode handshake", e).printStackTrace();
            return new Fail(Fail.Reason.INVALID_HANDSHAKE, encodeBase64(handshake));
        }
    }

    private static BungeeBackS decodeAndVerify0(String handshake, ClunkyStorage clunkyStorage) throws Exception {
        if (clunkyStorage.isUsingDefaultConfig()) {
            return new Fail(Fail.Reason.INCORRECT_TOKEN, "Allowed tokens have not been configured!");
        }

        if (handshake.length() > HANDSHAKE_LENGTH_LIMIT) {
            return new Fail(Fail.Reason.INVALID_HANDSHAKE, "handshake length " + handshake.length() + " is > " + HANDSHAKE_LENGTH_LIMIT);
        }

        String[] split = handshake.split("\00");
        if (split.length != 3 && split.length != 4) {
            return new Fail(Fail.Reason.INVALID_HANDSHAKE, encodeBase64(handshake));
        }

        String serverHostname = split[0];
        String socketAddressHostname = split[1];
        UUID uniqueId = UUID.fromString(split[2].replaceFirst("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5"));

        String connectionDescription = uniqueId + " @ " + encodeBase64(socketAddressHostname);

        if (split.length == 3) {
            return new Fail(Fail.Reason.NO_TOKEN, connectionDescription);
        }

        List<JsonObject> properties = new LinkedList<>(GSON.fromJson(split[3], PROPERTY_LIST_TYPE));
        if (properties.isEmpty()) {
            return new Fail(Fail.Reason.NO_TOKEN, connectionDescription);
        }

        String clunkybungeetoken = null;
        for (Iterator<JsonObject> iterator = properties.iterator(); iterator.hasNext(); ) {
            JsonObject property = iterator.next();
            if (property.get(PROPERTY_NAME_KEY).getAsString().equals(CLUNKYBUNGEE_TOKEN_NAME)) {
                if (clunkybungeetoken != null) {
                    return new Fail(Fail.Reason.INCORRECT_TOKEN, connectionDescription + " - more than one token");
                }

                clunkybungeetoken = property.get(PROPERTY_VALUE_KEY).getAsString();
                iterator.remove();
            }
        }

        if (clunkybungeetoken == null) {
            return new Fail(Fail.Reason.NO_TOKEN, connectionDescription);
        }

        if (!clunkyStorage.isAllowed(clunkybungeetoken)) {
            return new Fail(Fail.Reason.INCORRECT_TOKEN, connectionDescription + " - " + encodeBase64(clunkybungeetoken));
        }

        String newPropertiesString = GSON.toJson(properties, PROPERTY_LIST_TYPE);
        return new Success(serverHostname, socketAddressHostname, uniqueId, newPropertiesString);
    }

    public static String encodeBase64(String s) {
        return Base64.getEncoder().encodeToString(s.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Encapsulates a successful handshake.
     */
    public static final class Success extends BungeeBackS {
        private final String serverHostname;
        private final String socketAddressHostname;
        private final UUID uniqueId;
        private final String propertiesJson;

        Success(String serverHostname, String socketAddressHostname, UUID uniqueId, String propertiesJson) {
            this.serverHostname = serverHostname;
            this.socketAddressHostname = socketAddressHostname;
            this.uniqueId = uniqueId;
            this.propertiesJson = propertiesJson;
        }

        public String serverHostname() {
            return this.serverHostname;
        }

        public String socketAddressHostname() {
            return this.socketAddressHostname;
        }

        public UUID uniqueId() {
            return this.uniqueId;
        }

        public String propertiesJson() {
            return this.propertiesJson;
        }

        /**
         * Re-encodes this handshake to the format used by BungeeCord.
         *
         * @return an encoded string for the handshake
         */
        public String encode() {
            return this.serverHostname + "\00" + this.socketAddressHostname + "\00" + this.uniqueId + "\00" + this.propertiesJson;
        }
    }

    /**
     * Encapsulates an unsuccessful handshake.
     */
    public static final class Fail extends BungeeBackS {
        private final Reason reason;
        private final String connectionDescription;

        Fail(Reason reason, String connectionDescription) {
            this.reason = reason;
            this.connectionDescription = connectionDescription;
        }

        public Reason reason() {
            return this.reason;
        }

        public String describeConnection() {
            return this.connectionDescription;
        }

        public enum Reason {
            INVALID_HANDSHAKE, NO_TOKEN, INCORRECT_TOKEN
        }
    }

}
