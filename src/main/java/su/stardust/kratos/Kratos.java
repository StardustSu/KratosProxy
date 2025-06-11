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
import lombok.SneakyThrows;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import su.stardust.kratos.commands.*;
import su.stardust.kratos.listeners.BuildListener;
import su.stardust.kratos.listeners.StreamListener;
import su.stardust.kratos.network.Containers;
import su.stardust.kratos.network.Genesis;
import su.stardust.kratos.network.GenesisInfo;
import su.stardust.kratos.network.Keepalive;
import su.stardust.kratos.network.Messenger;
import su.stardust.kratos.network.Containers.Container;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Optional;

@Plugin(id = "kratosproxy", name = "KratosProxy", version = BuildConstants.VERSION, description = "<3", authors = {
        "Nico" }, dependencies = {
                @Dependency(id = "luckperms")
        })
public class Kratos {

    public static final ArrayList<String> verboseLogsPlayers = new ArrayList<>();
    public static final MinecraftChannelIdentifier KRATOS_BUILD = MinecraftChannelIdentifier.from("kratos:build");

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
        Containers.initializeDocker();
        Keepalive.initialize();
        GenesisInfo.run();

        // var boot = loadInstructions("proxy_boot");
        // useInstructions(boot);

        Genesis.getContainersState();

        server.getEventManager().register(this, new StreamListener());
        server.getEventManager().register(this, new BuildListener());

        registerCommand(new FindServerCommand(), "finds");
        registerCommand(new BootCommand(), "boot");
        registerCommand(new ShutdownCommand(), "sendstop");
        registerCommand(new UpdateCommand(), "update");
        registerCommand(new UpgradeCommand(), "upgrade", "migrate");
        registerCommand(new WhatIsCommand(), "whatis");
        registerCommand(new LobbyCommand(), "lobby", "l", "hub");
        registerCommand(new PlayCommand(), "play");
        registerCommand(new RejoinCommand(), "rejoin");
        registerCommand(new OnlineStatCron(), "onlinestat");
        registerCommand(new BuildCommand(), "build");
        registerCommand(new VerboseCommand(), "proxyverbose");
        registerCommand(new MsgCommand(), "msg", "t", "tell", "message", "w", "whisper");

        server.getChannelRegistrar().register(KRATOS_BUILD);
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent e) {
        // Containers.getAll().forEach(c -> Containers.delete(c.id(), false));
        Genesis.saveContainersState();
    }

    public static void registerServer(String id, int port) {
        StarLogger.debug("Registering " + id + " on port " + port);
        _server.registerServer(new ServerInfo(id, new InetSocketAddress(
                "127.0.0.1",
                port)));
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
        var host = env.getOrDefault("RABBIT_HOST", "ai.lampamc.ru");
        var user = env.getOrDefault("RABBIT_USER", "kratos");
        var pass = env.getOrDefault("RABBIT_PASS", "u_)KgHeHk4qedZ");
        var vhost = env.getOrDefault("RABBIT_VHOST", "kratos");
        return new String[] { host, user, pass, vhost };
    }

    @SneakyThrows
    public static String[] loadInstructions(String instructions) {
        URL url = URI.create("https://api.kratosmc.ru/static/" + instructions).toURL();
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        int status = con.getResponseCode();
        if (status != 200) {
            StarLogger.fatal("Could not read instruction " + instructions);
            return new String[] {};
        }
        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuilder content = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
            content.append("\n");
        }
        in.close();
        StarLogger.debug("Read " + instructions + ": ");
        var value = content.toString().split("\n");
        for (String s : value) {
            StarLogger.debug("| " + s + " |");
        }
        return value;
    }

    public static void useInstructions(String... instructions) {
        for (String instruction : instructions) {
            var args = instruction.split(" ");
            Containers.spinUp(args[0], args[1], args[2]);
        }
    }

    public static @Nullable RegisteredServer pickLobby() {
        var lobbies = Keepalive.getAlive("lobby");
        if (lobbies.isEmpty())
            return null;
        // todo. filtering
        var lobby = lobbies.getFirst().id();
        var server = getServer(lobby);
        return server.orElse(null);
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
}
