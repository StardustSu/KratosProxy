package su.stardust.kratos.network;

import su.stardust.kratos.Kratos;
import su.stardust.kratos.ServerUpgrade;
import su.stardust.kratos.StarLogger;
import su.stardust.kratos.listeners.StreamListener;
import su.stardust.kratos.network.Containers.Container;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Keepalive {
    private static final ConcurrentHashMap<String, Long> serverHeartbeats = new ConcurrentHashMap<>();
    private static final ScheduledExecutorService heartbeatChecker = Executors.newSingleThreadScheduledExecutor();
    private static final List<String> shutdown = new ArrayList<>();

    public static void initialize() {
        heartbeatChecker.scheduleAtFixedRate(Keepalive::checkMissingHeartbeats, 0, 10, TimeUnit.SECONDS);

        Messenger.consumeQueue("ONLINE", msg -> {
            // id
            var query = Containers.get(msg);
            query.ifPresent(Keepalive::registerContainer);
        });

        Messenger.consumeQueue("KEEPALIVE", msg -> {
            // id ms
            var data = msg.split(" ");
            var id = data[0];
            var ms = Long.parseLong(data[1]);
            serverHeartbeats.put(id, ms);

            var diff = System.currentTimeMillis() - ms;
            if (diff >= 3000) {
                StarLogger.warn("Server " + id + " might be lagging! d:" + diff);
            }
        });

        Messenger.consumeQueue("SHUTDOWN", msg -> {
            // id
            shutdown.add(msg);
            StarLogger.log("Received SHUTDOWN signal from " + msg);
            StreamListener.onServerShutdown(msg);
            HealthCheck.lobbyCount();
        });

        StarLogger.info("Running KeepAlive service!");
    }

    public static void registerContainer(Container container) {
        Kratos.registerServer(container.id(), container.port());
        serverHeartbeats.put(container.id(), System.currentTimeMillis());
        ServerUpgrade.checkUpgrade(container.id());
        ServerUpgrade.checkMove(container.id());
    }

    private static void checkMissingHeartbeats() {
        long currentTime = System.currentTimeMillis();
        var toRemove = new ArrayList<String>();
        serverHeartbeats.forEach((serverId, lastHeartbeatTime) -> {
            if (currentTime - lastHeartbeatTime > TimeUnit.SECONDS.toMillis(30)) { // wtf is going on here
                // Server is considered down
                toRemove.add(serverId);
            }
        });

        for (String sid : toRemove) {
            serverHeartbeats.remove(sid);
            shutdown.remove(sid);
            Kratos.unregisterServer(sid);
            Containers.delete(sid, false);
        }

        if (!toRemove.isEmpty()) {
            HealthCheck.check();
        }
    }

    public static boolean isShuttingDown(String id) {
        return shutdown.contains(id);
    }

    public static boolean isAlive(String id) {
        return serverHeartbeats.containsKey(id) && !isShuttingDown(id);
    }

    public static List<Containers.Container> getAlive(String type) {
        return Containers.getAll()
                .stream()
                .filter(c -> c.type().equals(type))
                .filter(c -> isAlive(c.id()) && !isShuttingDown(c.id()))
                .toList();
    }
}
