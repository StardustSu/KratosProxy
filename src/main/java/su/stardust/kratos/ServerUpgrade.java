package su.stardust.kratos;

import su.stardust.kratos.network.Containers;
import su.stardust.kratos.network.Keepalive;

import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import com.velocitypowered.api.proxy.Player;

public class ServerUpgrade {
    private static final HashMap<String, String> upgrades = new HashMap<>();
    private static final HashMap<String, String> moves = new HashMap<>();

    public static void upgrade(String id, String mem) {
        Containers.get(id).ifPresent(container -> {
            var newSrv = Containers.spinUp(container.type(), mem, container.image());
            upgrades.put(newSrv, container.id());
        });
    }

    public static void checkUpgrade(String id) {
        if (upgrades.containsKey(id)) {
            var oldId = upgrades.remove(id);
            if (!Keepalive.isAlive(id))
                return;
            Kratos.getServer(oldId).ifPresentOrElse(srv -> {
                var newSrv = Kratos.getServer(id).get();
                srv.getPlayersConnected().forEach(
                        p -> p.createConnectionRequest(newSrv).connectWithIndication());
                Containers.delete(oldId, true);
            }, () -> Containers.delete(oldId, false));
        }
    }

    public static void update(String type, String image) {
        Containers.pullImage(image, true, pull -> {
            if (!pull) {
                StarLogger.error("Could not update " + type);
                return;
            }

            var alive = Keepalive.getAlive(type);
            if (alive.isEmpty()) {
                StarLogger.warn("Updated image for " + type + ", but there is no servers to upgrade.");
                return;
            }

            if (true) {
                StarLogger.warn("Manual restart required.");
                return;
            }

            AtomicReference<String> first = new AtomicReference<>("");
            alive.forEach(c -> {
                var id = Containers.spinUp(c.type(), c.mem(), c.image());
                if (first.get().isEmpty())
                    first.set(id);
            });

            try (var clearer = Executors.newSingleThreadScheduledExecutor()) {
                clearer.scheduleAtFixedRate(() -> {
                    if (Keepalive.isAlive(first.get())) {
                        alive.forEach(c -> Containers.delete(c.id(), true));
                        clearer.close();
                    }
                }, 15, 5, TimeUnit.SECONDS);
            }
        });
    }

    public static void move(Player p, String id) {
        moves.put(id, p.getUsername());
    }

    public static void checkMove(String id) {
        var player = moves.remove(id);
        if (player == null)
            return;
        Kratos.getServer().getPlayer(player).ifPresent(p -> {
            Kratos.getServer(id).ifPresentOrElse(s -> {
                p.sendMessage(Text.of("&aПодключаемся к " + id + "..."));
                p.createConnectionRequest(s)
                        .connectWithIndication();
            }, () -> {
                p.sendMessage(Text.of("&cОшибка подключения к " + id + "..."));
            });
        });
    }
}
