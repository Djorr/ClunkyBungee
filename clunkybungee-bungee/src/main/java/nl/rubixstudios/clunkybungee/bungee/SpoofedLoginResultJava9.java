package nl.rubixstudios.clunkybungee.bungee;

import net.md_5.bungee.connection.LoginResult;
import net.md_5.bungee.protocol.Property;

import java.lang.invoke.MethodHandles;

public class SpoofedLoginResultJava9 extends SpoofedLoginResult{

    // online mode constructor
    protected SpoofedLoginResultJava9(LoginResult oldProfile, String clunkybungeetoken) {
        super(oldProfile, clunkybungeetoken);
    }

    // offline mode constructor
    public SpoofedLoginResultJava9(String clunkybungeetoken) {
        super(clunkybungeetoken);
    }

    @Override
    public Property[] getProperties() {
        Class<?> caller = MethodHandles.lookup().lookupClass();
        return getSpoofedProperties(caller);
    }
}
