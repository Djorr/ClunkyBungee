package nl.rubixstudios.clunkybungee.spigot.storage.listener;


import nl.rubixstudios.clunkybungee.spigot.ClunkyBungeePlugin;
import nl.rubixstudios.clunkybungee.spigot.storage.ClunkyStorage;

/**
 * An abstract handshake listener.
 */
public abstract class AbstractBackgroundListener {

    protected final ClunkyBungeePlugin plugin;
    protected final ClunkyStorage clunkyStorage;

    protected final String noDataKickMessage;
    protected final String invalidTokenKickMessage;

    protected AbstractBackgroundListener(ClunkyBungeePlugin plugin, ClunkyStorage clunkyStorage) {
        this.plugin = plugin;
        this.clunkyStorage = clunkyStorage;
        this.noDataKickMessage = plugin.getMessage("CLUNKY_BUNGEE.NO_DATA_KICK_MESSAGE");
        this.invalidTokenKickMessage = plugin.getMessage("CLUNKY_BUNGEE.INVALID_TOKEN_KICK_MESSAGE");
    }
}