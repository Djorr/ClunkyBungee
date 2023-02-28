package nl.rubixstudios.clunkybungee.spigot;

import nl.rubixstudios.clunkybungee.spigot.listener.PPBackgroundListener;
import nl.rubixstudios.clunkybungee.spigot.listener.PTBackgroundListener;

import nl.rubixstudios.clunkybungee.spigot.storage.ClunkyStorage;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

/**
 * Simple plugin which overrides the BungeeCord handshake protocol, and cancels all
 * connections which don't contain a special auth token set by the proxy.
 *
 * The token is included within the player's profile properties, but removed during the handshake.
 */
public final class ClunkyBungeePlugin extends JavaPlugin {

    private ClunkyStorage clunkyStorage;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.clunkyStorage = new ClunkyStorage(this);
        this.clunkyStorage.load();

        log("&7===&f=============================================&f===");
        log("- &fName&7: &6&lClunkyBungee");
        log("- &fVersion&7: &a" + this.getDescription().getVersion());
        log("- &fAuthor&7: &a" + this.getDescription().getAuthors());
        log("- &fDiscord&7: &ahttps://discord.rubixdevelopment.nl");
        log("");

        if (!getServer().spigot().getSpigotConfig().getBoolean("settings.bungeecord", false)) {
            log("&7===&f=============================================&f===");
            log("&cYou must set 'settings.bungeecord' to true in spigot.yml.");
            log("");
            log("&cClunkyBungee will not work unless this property is set to true.");
            log("&cThe server will now shutdown!");
            log("&7===&f=============================================&f===");
            getServer().shutdown();
            return;
        }

        if (isPaperHandshakeEvent()) {
            log("&f- &aUsing Paper's PlayerHandshakeEvent to listen for connections.");

            PPBackgroundListener listener = new PPBackgroundListener(this, this.clunkyStorage);
            getServer().getPluginManager().registerEvents(listener, this);

        } else if (hasProtocolLib()) {
            log("&f- &aUsing ProtocolLib to listen for connections.");

            PTBackgroundListener listener = new PTBackgroundListener(this, this.clunkyStorage);
            listener.registerAdapter(this);

        } else {
            log("&7===&f=============================================&f===");
            log("&cClunkyBungee is unable to listen for handshakes! The server will shutdown now.");
            log("");
            if (isPaperServer()) {
                log("&cPlease install ProtocolLib in order to use this plugin.");
            } else {
                log("&eIf your server is using 1.9.4 or newer, please upgrade to Paper - https://papermc.io");
                log("&eIf your server is using 1.8.8 or older, please install ProtocolLib.");
            }
            log("&7===&f=============================================&f===");
            getServer().shutdown();
        }

        log("");
        log("- &aSuccesfully enabled &r&6&lClunkyBungee&a plugin.");
        log("&7===&f=============================================&7===");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof ConsoleCommandSender)) {
            sender.sendMessage(ChatColor.RED + "Sorry, this command can only be ran from the console.");
            return true;
        }

        if (args.length == 0 || !args[0].equalsIgnoreCase("reload")) {
            sender.sendMessage(ChatColor.RED + "Running ClunkyBungee v" + getDescription().getVersion());
            sender.sendMessage(ChatColor.GRAY + "Use '/clunkybungee reload' to reload the configuration.");
            return true;
        }

        this.clunkyStorage.reload();
        sender.sendMessage(ChatColor.RED + "ClunkyBungee configuration reloaded.");
        return true;
    }

    public String getMessage(String key) {
        return ChatColor.translateAlternateColorCodes('&', getConfig().getString(key));
    }

    public List<String> getTokens() {
        return getConfig().getStringList("CLUNKY_BUNGEE.ALLOWED_TOKENS");
    }

    private static boolean isPaperHandshakeEvent() {
        return classExists("com.destroystokyo.paper.event.player.PlayerHandshakeEvent");
    }

    private static boolean isPaperServer() {
        return classExists("com.destroystokyo.paper.PaperConfig");
    }

    private boolean hasProtocolLib() {
        return getServer().getPluginManager().getPlugin("ProtocolLib") != null;
    }

    private static boolean classExists(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static void log(String message) {
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', message));
    }
}
