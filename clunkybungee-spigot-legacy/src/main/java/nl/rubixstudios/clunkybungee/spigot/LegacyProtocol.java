package nl.rubixstudios.clunkybungee.spigot;

import com.comphenix.protocol.injector.server.TemporaryPlayerFactory;
import org.bukkit.entity.Player;

import java.io.IOException;

/**
 * Hacky way to kick players with the legacy Protocol (4.x series)
 */
public class LegacyProtocol {

    public static void kick(Player player) throws IllegalAccessException, IOException {
        TemporaryPlayerFactory.getInjectorFromPlayer(player).getSocket().close();
    }

}
