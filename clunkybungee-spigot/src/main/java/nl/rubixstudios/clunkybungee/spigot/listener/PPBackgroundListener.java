package nl.rubixstudios.clunkybungee.spigot.listener;

import com.destroystokyo.paper.event.player.PlayerHandshakeEvent;
import nl.rubixstudios.clunkybungee.spigot.BungeeBackS;
import nl.rubixstudios.clunkybungee.spigot.ClunkyBungeePlugin;
import nl.rubixstudios.clunkybungee.spigot.storage.ClunkyStorage;
import nl.rubixstudios.clunkybungee.spigot.storage.listener.AbstractBackgroundListener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.lang.reflect.Method;
import java.util.logging.Logger;

/**
 * A handshake listener using Paper's {@link PlayerHandshakeEvent}.
 */
public class PPBackgroundListener extends AbstractBackgroundListener implements Listener {
    
    private static final Method getOriginalSocketAddressHostname;
    static {
        Method method = null;
        try {
            method = PlayerHandshakeEvent.class.getMethod("getOriginalSocketAddressHostname");
        } catch (NoSuchMethodException ignored) {
            // Paper added this method in 1.16
        }
        getOriginalSocketAddressHostname = method;
    }

    private final Logger logger;

    public PPBackgroundListener(ClunkyBungeePlugin plugin, ClunkyStorage clunkyStorage) {
        super(plugin, clunkyStorage);
        this.logger = plugin.getLogger();
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onHandshake(PlayerHandshakeEvent e) {
        BungeeBackS decoded = BungeeBackS.decodeAndVerify(e.getOriginalHandshake(), this.clunkyStorage);

        if (decoded instanceof BungeeBackS.Fail) {
            BungeeBackS.Fail fail = (BungeeBackS.Fail) decoded;
            String ip = "";
            if (getOriginalSocketAddressHostname != null) {
                try {
                    ip = getOriginalSocketAddressHostname.invoke(e) + " - ";
                } catch (ReflectiveOperationException ex) {
                    ClunkyBungeePlugin.log("&cUnable to get original address: " + ex.getMessage());
                }
            }

            ClunkyBungeePlugin.log("&cDenying connection from " + ip + fail.describeConnection() + " - reason: " + fail.reason().name());

            if (fail.reason() == BungeeBackS.Fail.Reason.INVALID_HANDSHAKE) {
                e.setFailMessage(this.noDataKickMessage);
            } else {
                e.setFailMessage(this.invalidTokenKickMessage);
            }

            e.setFailed(true);
            return;
        }

        BungeeBackS.Success data = (BungeeBackS.Success) decoded;
        e.setServerHostname(data.serverHostname());
        e.setSocketAddressHostname(data.socketAddressHostname());
        e.setUniqueId(data.uniqueId());
        e.setPropertiesJson(data.propertiesJson());
    }

}
