package nl.rubixstudios.clunkybungee.spigot.listener;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.injector.temporary.MinimalInjector;
import com.comphenix.protocol.injector.temporary.TemporaryPlayerFactory;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import nl.rubixstudios.clunkybungee.spigot.BungeeBackS;
import nl.rubixstudios.clunkybungee.spigot.ClunkyBungeePlugin;
import nl.rubixstudios.clunkybungee.spigot.LegacyProtocol;
import nl.rubixstudios.clunkybungee.spigot.storage.ClunkyStorage;
import nl.rubixstudios.clunkybungee.spigot.storage.listener.AbstractBackgroundListener;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.net.InetSocketAddress;

/**
 * A handshake listener using ProtocolLib.
 */
public class PTBackgroundListener extends AbstractBackgroundListener {
    static boolean isLegacyProtocolLib = false; // Before 5.x series.

    public PTBackgroundListener(ClunkyBungeePlugin plugin, ClunkyStorage clunkyStorage) {
        super(plugin, clunkyStorage);
    }

    public void registerAdapter(Plugin plugin) {
        ProtocolLibrary.getProtocolManager().addPacketListener(new Adapter(plugin));
    }

    private final class Adapter extends PacketAdapter {

        Adapter(Plugin plugin) {
            super(plugin, ListenerPriority.LOWEST, PacketType.Handshake.Client.SET_PROTOCOL);
            try {
                Class.forName("com.comphenix.protocol.injector.temporary.MinimalInjector");
                ClunkyBungeePlugin.log("&f- &aUsing modern (v5) ProtocolLib adapter.");
            } catch (ClassNotFoundException e) {
                ClunkyBungeePlugin.log("&f- &aUsing legacy (v4) ProtocolLib adapter.");
                isLegacyProtocolLib = true;
            }
        }

        @Override
        public void onPacketReceiving(PacketEvent event) {
            PacketContainer packet = event.getPacket();

            // only handle the LOGIN phase
            PacketType.Protocol state = packet.getProtocols().read(0);
            if (state != PacketType.Protocol.LOGIN) {
                return;
            }

            String handshake = packet.getStrings().read(0);
            BungeeBackS decoded = BungeeBackS.decodeAndVerify(handshake, PTBackgroundListener.this.clunkyStorage);

            if (decoded instanceof BungeeBackS.Fail) {
                String ip = "null";
                Player player = event.getPlayer();
                InetSocketAddress address = player.getAddress();
                if (address != null) {
                    ip = address.getHostString();
                    if (ip.length() > 15) {
                        ip = BungeeBackS.encodeBase64(ip);
                    }
                }
                BungeeBackS.Fail fail = (BungeeBackS.Fail) decoded;
                ClunkyBungeePlugin.log("&cDenying connection from " + ip + " - " + fail.describeConnection() + " - reason: " + fail.reason().name());

                String kickMessage;
                if (fail.reason() == BungeeBackS.Fail.Reason.INVALID_HANDSHAKE) {
                    kickMessage = PTBackgroundListener.this.noDataKickMessage;
                } else {
                    kickMessage = PTBackgroundListener.this.invalidTokenKickMessage;
                }

                try {
                    closeConnection(player, kickMessage);
                } catch (Exception e) {
                    ClunkyBungeePlugin.log("&cAn error occurred while closing connection for " + player.getName() + ": " + e.getMessage());
                }

                // just in-case the connection didn't close, screw up the hostname
                // so Spigot can't pick up anything that might've been spoofed in nms.HandshakeListener
                packet.getStrings().write(0, "null");

                return;
            }

            // great, handshake was decoded and verified successfully.
            // we can re-encode the handshake now so Spigot can pick up the spoofed stuff.
            BungeeBackS.Success data = (BungeeBackS.Success) decoded;
            packet.getStrings().write(0, data.encode());
        }
    }

    private static void closeConnection(Player player, String kickMessage) throws Exception {
        WrappedChatComponent component = WrappedChatComponent.fromJson(ComponentSerializer.toString(TextComponent.fromLegacyText(kickMessage)));

        PacketContainer packet = new PacketContainer(PacketType.Login.Server.DISCONNECT);
        packet.getModifier().writeDefaults();
        packet.getChatComponents().write(0, component);

        // send custom disconnect message to client
        ProtocolLibrary.getProtocolManager().sendServerPacket(player, packet);

        if (isLegacyProtocolLib) {
            LegacyProtocol.kick(player);
        } else {
            // call PlayerConnection#disconnect to ensure the underlying socket is closed
            MinimalInjector injector = TemporaryPlayerFactory.getInjectorFromPlayer(player);
            injector.disconnect("");
        }
    }

}
