package su.stardust.kratos.listeners;

import java.util.ArrayList;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;

import su.stardust.kratos.Kratos;
import su.stardust.kratos.ServerUpgrade;
import su.stardust.kratos.Text;
import su.stardust.kratos.network.Containers;
import su.stardust.kratos.network.Genesis;
import su.stardust.kratos.network.Keepalive;

public class BuildListener {
    @Subscribe
    public void onPluginMessageFromBackend(PluginMessageEvent event) {
        if (!Kratos.KRATOS_BUILD.equals(event.getIdentifier()))
            return;
        event.setResult(PluginMessageEvent.ForwardResult.handled());
        if (!(event.getSource() instanceof ServerConnection backend))
            return;

        ByteArrayDataInput in = ByteStreams.newDataInput(event.getData());
        var data = in.readLine();
        if (data.startsWith("SEND ")) {
            var d = data.split(" ");
            Kratos.getServer().getPlayer(d[1]).ifPresent(p -> {
                var w = d[2];
                switch (getServerStatus(w)) {
                    case "2":
                        p.sendMessage(Text.of("&cСервер еще запускается!"));
                        sendBuildMenu(p);
                        break;
                    case "1":
                        Kratos.getServer(Keepalive.getAlive("build:" + w).getFirst().id())
                                .ifPresent(s -> {
                                    p.sendMessage(Text.of("Подключаемся к " + s.getServerInfo().getName() + "..."));
                                    p.createConnectionRequest(s)
                                            .connectWithIndication();
                                });
                        break;
                    case "0":
                        var sid = Containers.spinUp("build:" + w, "mini", "thisisnico/kratos-build:latest");
                        p.sendMessage(Text.of("&aЗапускаем мир " + w + " на сервере " + sid + "..."));
                        ServerUpgrade.move(p, sid);
                        break;
                }
            });
        }
    }

    public static void sendBuildMenu(Player p) {
        p.getCurrentServer().ifPresentOrElse(s -> {
            p.sendMessage(Text.of("&aОткрываем меню..."));
            var worlds = Genesis.fetchWorlds();
            var data = new ArrayList<String>();
            for (String w : worlds) {
                var alive = getServerStatus(w);
                data.add(w + ":" + alive);
            }
            s.sendPluginMessage(Kratos.KRATOS_BUILD,
                    ("MENU " + p.getUsername() + " " + String.join(",", data)).getBytes());
        }, () -> {
            p.sendMessage(Text.of("&cкакого хуя тут творится?!!?!?!"));
        });
    }

    private static String getServerStatus(String w) {
        var alive = Keepalive.getAlive("build:" + w).isEmpty()
                ? Containers.getAll().stream().anyMatch(c -> c.type().equals("build:" + w))
                        ? "2"
                        : "0"
                : "1";
        return alive;
    }
}
