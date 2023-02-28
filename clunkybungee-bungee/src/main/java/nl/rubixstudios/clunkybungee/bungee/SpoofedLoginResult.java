package nl.rubixstudios.clunkybungee.bungee;

import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.ServerConnector;
import net.md_5.bungee.connection.InitialHandler;
import net.md_5.bungee.connection.LoginResult;
import net.md_5.bungee.protocol.Property;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Arrays;

/**
 * Extension of {@link LoginResult} which returns a modified Property array when
 * {@link #getProperties()} is called by the ServerConnector implementation.
 *
 * To achieve this, the stack trace is analyzed. This is kinda crappy, but is the only way
 * to modify the properties without leaking the token to other clients via the tablist.
 */
abstract class SpoofedLoginResult extends LoginResult {
    private static final Field PROFILE_FIELD;
    private static final Constructor<? extends SpoofedLoginResult> OFFLINE_MODE_IMPL;
    private static final Constructor<? extends SpoofedLoginResult> ONLINE_MODE_IMPL;

    static {
        Class<? extends SpoofedLoginResult> implClass;
        if (classExists("java.lang.StackWalker")) {
            implClass = SpoofedLoginResultJava9.class;
        } else if (classExists("jdk.internal.reflect.Reflection")) {
            implClass = SpoofedLoginResultJdkInternal.class;
        } else {
            implClass = SpoofedLoginResultReflection.class;
        }

        try {
            PROFILE_FIELD = InitialHandler.class.getDeclaredField("loginProfile");
            PROFILE_FIELD.setAccessible(true);

            OFFLINE_MODE_IMPL = implClass.getConstructor(String.class);
            ONLINE_MODE_IMPL = implClass.getConstructor(LoginResult.class, String.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    static void inject(InitialHandler handler, String token) {
        LoginResult profile = handler.getLoginProfile();
        LoginResult newProfile;

        try {
            // profile is null for offline mode servers
            if (profile == null) {
                newProfile = OFFLINE_MODE_IMPL.newInstance(token);
            } else {
                newProfile = ONLINE_MODE_IMPL.newInstance(profile, token);
            }
        } catch (ReflectiveOperationException e) {
            BungeeCord.getInstance().getLogger().log(java.util.logging.Level.SEVERE, "Failed to inject clunkybungee token 1", e);
            throw new RuntimeException(e);
        }

        try {
            PROFILE_FIELD.set(handler, newProfile);
        } catch (IllegalAccessException e) {
            BungeeCord.getInstance().getLogger().log(java.util.logging.Level.SEVERE, "Failed to inject clunkybungee token 2", e);
            throw new RuntimeException(e);
        }
    }

    private static boolean classExists(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private final Property clunkybungeetoken;
    private final Property[] clunkybungeetokenArray;
    private final boolean offline;

    // online mode constructor
    protected SpoofedLoginResult(LoginResult oldProfile, String clunkybungeetoken) {
        super(oldProfile.getId(), oldProfile.getName(), oldProfile.getProperties());
        this.clunkybungeetoken = new Property("clunkybungee-token", clunkybungeetoken, "");
        this.clunkybungeetokenArray = new Property[]{this.clunkybungeetoken};
        this.offline = false;
    }

    // offline mode constructor
    protected SpoofedLoginResult(String clunkybungeetoken) {
        super(null, null, new Property[0]);
        this.clunkybungeetoken = new Property("clunkybungee-token", clunkybungeetoken, "");
        this.clunkybungeetokenArray = new Property[]{this.clunkybungeetoken};
        this.offline = true;
    }

    protected Property[] getSpoofedProperties(Class<?> caller) {
        // if the getProperties method is being called by the server connector, include our token in the properties
        if (caller == ServerConnector.class) {
            return addTokenProperty(super.getProperties());
        } else {
            return super.getProperties();
        }
    }

    private Property[] addTokenProperty(Property[] properties) {
        if (properties.length == 0) {
            return this.clunkybungeetokenArray;
        }

        Property[] newProperties = Arrays.copyOf(properties, properties.length + 1);
        newProperties[properties.length] = this.clunkybungeetoken;
        return newProperties;
    }

    @Override
    public String getId() {
        if (this.offline) {
            throw new RuntimeException("getId called for offline variant of SpoofedLoginResult");
        }
        return super.getId();
    }

    @Override
    public String getName() {
        if (this.offline) {
            throw new RuntimeException("getId called for offline variant of SpoofedLoginResult");
        }
        return super.getId();
    }
}
