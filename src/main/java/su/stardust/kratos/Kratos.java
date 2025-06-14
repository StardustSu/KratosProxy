package su.stardust.kratos;

import com.google.inject.Inject;
import com.velocitypowered.api.command.Command;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import su.stardust.kratos.commands.*;
import su.stardust.kratos.listeners.StreamListener;
import su.stardust.kratos.listeners.PlayListener;
import su.stardust.kratos.network.Messenger;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Optional;

@Plugin(id = "kratosproxy", name = "KratosProxy", version = BuildConstants.VERSION, description = "<3", authors = {
        "Nico" }, dependencies = {
                @Dependency(id = "luckperms")
        })
public class Kratos {

    public static final ArrayList<String> verboseLogsPlayers = new ArrayList<>();
    public static final MinecraftChannelIdentifier KRATOS_BUILD = MinecraftChannelIdentifier.from("kratos:build");
    public static final MinecraftChannelIdentifier KRATOS_PLAY = MinecraftChannelIdentifier.from("kratos:play");

    @Getter
    private static Kratos instance;

    @Inject
    private Logger logger;

    @Inject
    private ProxyServer server;

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        instance = this;
        _logger = logger;
        _server = server;

        Messenger.initializeRabbit();
        Messenger.consumeQueue("PROXY_CONTROL", Kratos::handleControlMessage);

        server.getEventManager().register(this, new StreamListener());
        server.getEventManager().register(this, new PlayListener());

        registerCommand(new LobbyCommand(), "lobby", "l", "hub");
        registerCommand(new VerboseCommand(), "proxyverbose");
        registerCommand(new MsgCommand(), "msg", "t", "tell", "message", "w", "whisper");
        registerCommand(new PartyCommand(), "party", "p");
        registerCommand(new PlayCommand(), "play");

        server.getChannelRegistrar().register(KRATOS_BUILD);
        server.getChannelRegistrar().register(KRATOS_PLAY);

        Messenger.send("status-updates", "{\"type\":\"proxy\",\"status\":\"online\"}");
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent e) {
        // Containers.getAll().forEach(c -> Containers.delete(c.id(), false));
    }

    public static void registerServer(String id, String address, int port) {
        StarLogger.debug("Registering " + id + " at " + address + ":" + port);
        unregisterServer(id);
        _server.registerServer(new ServerInfo(id, new InetSocketAddress(address, port)));
    }

    public static void registerServer(String id, int port) {
        registerServer(id, "127.0.0.1", port);
    }

    public static void unregisterServer(String id) {
        _server.getServer(id).ifPresent(srv -> {
            _server.unregisterServer(srv.getServerInfo());
            StarLogger.debug("Unregistered " + id + " from port " + srv.getServerInfo().getAddress().getPort());
        });
    }

    public static Optional<RegisteredServer> getServer(String id) {
        return _server.getServer(id);
    }

    public static Optional<ServerInfo> getServerInfo(String id) {
        var srv = _server.getServer(id);
        return srv.map(RegisteredServer::getServerInfo);
    }

    private static Logger _logger;

    public static Logger getLogger() {
        return _logger;
    }

    private static ProxyServer _server;

    public static ProxyServer getServer() {
        return _server;
    }

    public static String[] getRabbitSettings() {
        var env = System.getenv();
        var host = env.getOrDefault("RABBIT_HOST", "rabbit.lampamc.ru");
        var user = env.getOrDefault("RABBIT_USER", "kratos");
        var pass = env.getOrDefault("RABBIT_PASS", "u_)KgHeHk4qedZ");
        var vhost = env.getOrDefault("RABBIT_VHOST", "kratos");
        return new String[] { host, user, pass, vhost };
    }

    public static @Nullable RegisteredServer pickLobby() {
        return _server.getAllServers().stream()
                .filter(s -> s.getServerInfo().getName().startsWith("lobby"))
                .findFirst()
                .orElse(null);
    }

    private void registerCommand(Command command, String... aliases) {
        var cmd = server.getCommandManager()
                .metaBuilder(aliases[0])
                .aliases(aliases)
                .plugin(this)
                .build();
        server.getCommandManager().register(cmd, command);
    }

    public static void logVerbose(String msg) {
        verboseLogsPlayers.forEach(name -> {
            Kratos.getServer().getPlayer(name).ifPresent(p -> {
                p.sendMessage(Text.of(msg));
            });
        });
    }

    private static void handleControlMessage(String message) {
        var args = message.split(" ");
        if (args.length == 0)
            return;

        var command = args[0].toUpperCase();
        switch (command) {
            case "SEND" -> {
                if (args.length != 3)
                    return;
                var playerName = args[1];
                var serverName = args[2];
                var playerOpt = _server.getPlayer(playerName);
                var serverOpt = _server.getServer(serverName);
                if (playerOpt.isEmpty() || serverOpt.isEmpty()) {
                    StarLogger.warn("Cannot route " + playerName + " to " + serverName + " - missing player or server");
                    return;
                }
                playerOpt.get().createConnectionRequest(serverOpt.get()).connectWithIndication();
            }
            case "REGISTER" -> {
                if (args.length != 4)
                    return;
                var name = args[1];
                var addr = args[2];
                int port;
                try {
                    port = Integer.parseInt(args[3]);
                } catch (NumberFormatException e) {
                    StarLogger.warn("Invalid port for REGISTER: " + args[3]);
                    return;
                }
                registerServer(name, addr, port);
            }
            case "UNREGISTER" -> {
                if (args.length != 2)
                    return;
                var param = args[1];
                if (_server.getServer(param).isPresent()) {
                    unregisterServer(param);
                    return;
                }
                var hostPort = param.split(":");
                if (hostPort.length == 2) {
                    try {
                        int port = Integer.parseInt(hostPort[1]);
                        _server.getAllServers().stream()
                                .filter(s -> s.getServerInfo().getAddress().getHostString().equals(hostPort[0]) &&
                                        s.getServerInfo().getAddress().getPort() == port)
                                .findFirst()
                                .ifPresent(s -> unregisterServer(s.getServerInfo().getName()));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
            case "REDIRECT" -> {
                if (args.length != 3)
                    return;
                var fromOpt = _server.getServer(args[1]);
                var toOpt = _server.getServer(args[2]);
                if (fromOpt.isEmpty() || toOpt.isEmpty()) {
                    StarLogger.warn("Cannot redirect - missing server");
                    return;
                }
                var to = toOpt.get();
                fromOpt.get().getPlayersConnected().forEach(p -> p.createConnectionRequest(to).connectWithIndication());
            }
            default -> StarLogger.warn("Unknown control command: " + command);
        }
    }
}
