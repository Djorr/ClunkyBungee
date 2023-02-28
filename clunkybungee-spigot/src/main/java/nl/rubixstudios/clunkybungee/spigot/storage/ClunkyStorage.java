package nl.rubixstudios.clunkybungee.spigot.storage;

import lombok.Getter;
import lombok.Setter;
import nl.rubixstudios.clunkybungee.spigot.ClunkyBungeePlugin;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * A store of allowed tokens.
 */

@Getter
@Setter
public class ClunkyStorage {

    private final ClunkyBungeePlugin plugin;
    private Set<String> allowedTokens = Collections.emptySet();

    public ClunkyStorage(ClunkyBungeePlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        this.plugin.reloadConfig();
        load();
    }

    public void load() {
        this.allowedTokens = new HashSet<>(this.plugin.getTokens());
        ClunkyBungeePlugin.log("Loaded " + this.allowedTokens.size() + " tokens.");
    }

    /**
     * Gets if a token is allowed.
     *
     * @param token the token
     * @return true if allowed
     */
    public boolean isAllowed(String token) {
        return this.allowedTokens.contains(token);
    }

    /**
     * Has the server owner bothered to configure their tokens correctly...?
     *
     * @return true if clunkybungee has not yet been configured
     */
    public boolean isUsingDefaultConfig() {
        return this.allowedTokens.contains("Put the token here");
    }

}

