package su.stardust.kratos.commands;

import com.velocitypowered.api.command.SimpleCommand;
import net.kyori.adventure.text.event.ClickEvent;
import su.stardust.kratos.ServerUpgrade;
import su.stardust.kratos.Text;
import su.stardust.kratos.network.Keepalive;

import java.util.List;

public class UpdateCommand implements SimpleCommand {
    @Override
    public void execute(Invocation invocation) {
        String[] args = invocation.arguments();
        if (args.length != 2) {
            invocation.source().sendMessage(Text.of("&c# /update <type> <img>"));
            return;
        }
        invocation.source().sendMessage(Text.of(""));
        invocation.source().sendMessage(Text.of("&aSending an UPDATE signal for " + args[0]));
        ServerUpgrade.update(args[0], args[1]);
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        return List.of();
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("kratos.admin");
    }
}
