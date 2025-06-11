package su.stardust.kratos.commands;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.server.RegisteredServer;

import su.stardust.kratos.Kratos;
import su.stardust.kratos.StarLogger;
import su.stardust.kratos.network.Containers;
import su.stardust.kratos.network.Genesis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

public class OnlineStatCron implements SimpleCommand {
    @Override
    public void execute(Invocation invocation) {
        var full = Kratos.getServer().getAllPlayers().size();
        if (full < 0)
            full = 0;
        var map = new HashMap<String, AtomicInteger>();
        for (RegisteredServer server : Kratos.getServer().getAllServers()) {
            var sid = server.getServerInfo().getName();
            var c = Containers.get(sid).orElse(null);
            var type = sid;
            if (c != null)
                type = c.type();
            if (!map.containsKey(type))
                map.put(type, new AtomicInteger(0));
            var at = map.get(type);
            var online = server.getPlayersConnected().size();
            if (online >= 0)
                at.addAndGet(online);
        }
        var data = new ArrayList<String>();
        for (Entry<String, AtomicInteger> entry : map.entrySet()) {
            data.add("\"" + entry.getKey() + "\": " + entry.getValue().get());
        }
        var json = "{" + String.join(",", data) + "}";
        // StarLogger.debug(String.valueOf(full));
        // StarLogger.debug(json);
        Genesis.sendOnlineStats(full, json);
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        return Collections.emptyList();
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("kratos.admin");
    }
}
