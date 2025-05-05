package su.stardust.kratos.commands;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import su.stardust.kratos.ServerUpgrade;
import su.stardust.kratos.Text;
import su.stardust.kratos.network.Containers;

import java.util.List;

public class UpgradeCommand implements SimpleCommand {
    @Override
    public void execute(Invocation invocation) {
        String[] args = invocation.arguments();
        if (args.length != 1 || !suggest(invocation).contains(args[0])) {
            invocation.source().sendMessage(Text.of("&c# /upgrade <micro|mini|mega|super>"));
            return;
        }
        if (invocation.source() instanceof Player p) {
            p.getCurrentServer().ifPresent(srv -> {
                ServerUpgrade.upgrade(srv.getServerInfo().getName(), args[0].toLowerCase());
                p.sendMessage(Text.of("&bSent an upgrade TO " + args[0].toLowerCase() + " request"));
            });
        }
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        return List.of("micro", "mini", "mega", "super");
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("kratos.admin");
    }
}
