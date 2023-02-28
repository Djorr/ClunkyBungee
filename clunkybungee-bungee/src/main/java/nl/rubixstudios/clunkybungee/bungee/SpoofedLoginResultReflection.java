package nl.rubixstudios.clunkybungee.bungee;

import net.md_5.bungee.connection.LoginResult;
import net.md_5.bungee.protocol.Property;

public class SpoofedLoginResultReflection extends SpoofedLoginResult {

    // online mode constructor
    public SpoofedLoginResultReflection(LoginResult oldProfile, String clunkybungeetoken) {
        super(oldProfile, clunkybungeetoken);
    }

    // offline mode constructor
    public SpoofedLoginResultReflection(String clunkybungeetoken) {
        super(clunkybungeetoken);
    }

    @Override
    public Property[] getProperties() {
        StackTraceElement[] trace = new Exception().getStackTrace();

        Class<?> caller = null;
        if (trace.length >= 2) {
            try {
                caller = Class.forName(trace[1].getClassName());
            } catch (ClassNotFoundException e) {
                // ignore
            }
        }

        return getSpoofedProperties(caller);
    }
}
