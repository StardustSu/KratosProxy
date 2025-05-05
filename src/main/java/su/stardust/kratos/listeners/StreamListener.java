package su.stardust.kratos.listeners;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.proxy.server.RegisteredServer;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeBuilder;
import su.stardust.kratos.Kratos;
import su.stardust.kratos.Text;
import su.stardust.kratos.network.Containers;
import su.stardust.kratos.network.Genesis;
import su.stardust.kratos.network.Keepalive;
import su.stardust.kratos.network.Matchmaking;

import java.util.HashMap;

public class StreamListener {

    @Subscribe
    public void onChooseServer(PlayerChooseInitialServerEvent e) {
        // sync player
        Genesis.syncPlayer(e.getPlayer());

        if (e.getInitialServer().isEmpty()) {// which should be every time
            var rejoin = Matchmaking.getRejoins().get(e.getPlayer().getGameProfile().getName());
            if (rejoin != null) {
                var server = Kratos.getServer(rejoin);
                if (server.isPresent()) {
                    e.setInitialServer(server.get());
                    return;
                }
            }
            var lobby = Kratos.pickLobby();
            if (lobby == null) {
                e.getPlayer().disconnect(Text.of("&cСервер запускается. Перезайдите позже."));
                return;
            }
            e.setInitialServer(lobby);
        }
    }

    @Subscribe
    public void onServerDisconnect(KickedFromServerEvent e) {
        if (e.kickedDuringServerConnect())
            return;

        if (redirects.containsKey(e.getServer())) {
            var dest = redirects.get(e.getServer());
            e.setResult(KickedFromServerEvent.RedirectPlayer.create(dest));
        } else {
            var lobby = Kratos.pickLobby();
            if (lobby != null) {
                e.setResult(KickedFromServerEvent.RedirectPlayer.create(lobby));
            }
        }
    }

    private static final HashMap<RegisteredServer, RegisteredServer> redirects = new HashMap<>();

    public static void onServerShutdown(String id) {
        var containerOpt = Containers.get(id);
        if (containerOpt.isEmpty())
            return;
        var serverOpt = Kratos.getServer(id);
        if (serverOpt.isEmpty())
            return;
        var container = containerOpt.get();
        var server = serverOpt.get();

        var similar = container.type().startsWith("game:") ? "lobby" : container.type();
        var simc = Keepalive.getAlive(similar);
        if (simc.isEmpty() && similar.equals("lobby")) {
            redirects.remove(server);
            return;
        }
        if (simc.isEmpty())
            simc = Keepalive.getAlive("lobby");
        if (simc.isEmpty()) {
            redirects.remove(server);
            return;
        }

        Kratos.getServer(simc.getFirst().id()).ifPresentOrElse(
                srv -> {
                    redirects.put(server, srv);
                    server.getPlayersConnected().forEach(p -> {
                        if (!container.type().startsWith("game:")) {
                            p.sendMessage(Text.of("&c===================================="));
                            p.sendMessage(Text.of("&c  Сервер, на котором вы находились"));
                            p.sendMessage(Text.of("&c  сейчас перезагружается. Переносим"));
                            p.sendMessage(Text.of("&c  вас на другой свободный сервер."));
                            p.sendMessage(Text.of("&c===================================="));
                        }
                    });
                },
                () -> redirects.remove(server));
    }

}
