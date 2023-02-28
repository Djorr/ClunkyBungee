package nl.rubixstudios.clunkybungee.bungee;

import net.md_5.bungee.connection.LoginResult;

import jdk.internal.reflect.Reflection;
import net.md_5.bungee.protocol.Property;

public class SpoofedLoginResultJdkInternal extends SpoofedLoginResult {

    // online mode constructor
    public SpoofedLoginResultJdkInternal(LoginResult oldProfile, String clunkybungeetoken) {
        super(oldProfile, clunkybungeetoken);
    }

    // offline mode constructor
    public SpoofedLoginResultJdkInternal(String clunkybungeetoken) {
        super(clunkybungeetoken);
    }

    @Override
    public Property[] getProperties() {
        Class<?> caller = Reflection.getCallerClass();
        return getSpoofedProperties(caller);
    }
}
