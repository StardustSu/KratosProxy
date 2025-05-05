package su.stardust.kratos.network;

import com.velocitypowered.api.proxy.Player;
import lombok.Getter;
import org.apache.commons.lang3.tuple.Pair;
import su.stardust.kratos.Kratos;
import su.stardust.kratos.StarLogger;
import su.stardust.kratos.Text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class Matchmaking {
    @Getter private static final HashMap<Integer, Request> requests = new HashMap<>();
    @Getter private static final ArrayList<String> startingUp = new ArrayList<>();
    @Getter private static final HashMap<String, String> rejoins = new HashMap<>();

    static {
        Messenger.consumeQueue("MATCHMAKING", msg -> {
            StarLogger.debug("[MM] Received: " + msg);
            var data = msg.split(" ");
            if (data[0].equals("REQ")) {
                // REQ rid OK sid max
                if (!data[2].equals("OK")) return;
                var rid = Integer.parseInt(data[1]);
                var sid = data[3];
                var online = Integer.parseInt(data[4]);
                var req = requests.get(rid);
                if (req == null) return;
                req.data.add(Pair.of(sid, online));
                requests.put(rid, req);
            }
            if (data[0].equals("PLAY")) {
                Kratos.getServer().getPlayer(data[1]).ifPresent(p -> {
                    findGame(p, data[2]);
                });
            }
            if (data[0].equals("START")) {
                Containers.get(data[1]).ifPresent(c -> {
                    if (!c.type().startsWith("game:")) return;
                    Containers.spinUp(c.type(), c.mem(), c.image());
                    Kratos.getServer(c.id()).ifPresent(srv ->
                        srv.getPlayersConnected().forEach(p ->
                            rejoins.put(p.getGameProfile().getName(), c.id())
                        )
                    );
                });
            }
        });
    }

    public static void findGame(Player issuer, String type) {
        if (requests.values().stream().anyMatch(r -> r.issuer.equals(issuer))) return;
        StarLogger.debug("[MM] Received search for " + issuer.getGameProfile().getName() + " to " + type);
        var req = new Request(i++, type, 1, issuer, new ArrayList<>());
        requests.put(req.id, req);
        var servers = Keepalive.getAlive("game:"+type);
        servers.stream().map(Containers.Container::id).forEach(id ->
                Messenger.send("MM:" + id,
                        "REQ " + req.id + " PARTY " + 1 + " HELO " + id
                )
        );

        Kratos.getServer().getScheduler().buildTask(Kratos.getInstance(), () -> {
            var request = requests.get(req.id);

            var res = request.data.stream()
                    .map(p -> new ServerResponse(
                            p.getLeft(),
                            Kratos.getServer(p.getLeft()).orElseThrow().getPlayersConnected().size(),
                            p.getRight()
                    ))
                    .sorted((a, b) -> b.online - a.online)
                    .toList();

            var flag = false;
            for (ServerResponse first : res) {
                if (first.max >= (first.online + request.party)) {
                    // todo. party
                    flag = true;
                    request.issuer.sendMessage(Text.of("&aПодключаемся к " + first.id + "..."));
                    request.issuer.createConnectionRequest(
                            Kratos.getServer(first.id).orElseThrow()
                    ).fireAndForget();
                    break;
                }
            }

            if (!flag) {
                request.issuer.sendMessage(Text.of("&cНе удалось найти игру! Попробуйте еще раз."));
            }

            requests.remove(request.id);
        }).delay(1000, TimeUnit.MILLISECONDS).schedule();
    }

    public static void rejoin(Player p) {
        var name = p.getGameProfile().getName();
        if (!rejoins.containsKey(name)) return;
        Kratos.getServer(rejoins.get(name)).ifPresentOrElse(srv ->
            p.createConnectionRequest(srv).fireAndForget(),
            () -> {
                p.sendMessage(Text.of("&cИгра уже завершилась, переподключаться некуда."));
                rejoins.remove(name);
            }
        );
    }

    private static int i = 1;
    public record Request(
            int id,
            String type,
            int party,
            Player issuer,
            ArrayList<Pair<String, Integer>> data
    ) {}

    private record ServerResponse(
            String id,
            int online,
            int max
    ) {}
}
