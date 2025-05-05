package su.stardust.kratos.commands;

import com.velocitypowered.api.command.SimpleCommand;
import net.kyori.adventure.text.event.ClickEvent;
import su.stardust.kratos.Text;
import su.stardust.kratos.network.Containers;
import su.stardust.kratos.network.Keepalive;

import java.util.List;

public class FindServerCommand implements SimpleCommand {
    @Override
    public void execute(Invocation invocation) {
        String[] args = invocation.arguments();
        if (args.length != 1) {
            invocation.source().sendMessage(Text.of("&c# /findserver <type>"));
            return;
        }
        invocation.source().sendMessage(Text.of(""));
        invocation.source().sendMessage(Text.of("&aServers with type &a" + args[0]));
        Keepalive.getAlive(args[0]).forEach(c -> invocation.source().sendMessage(
                Text.of("- &6" + c.id())
                        .append(Text.of(" &b[WARP]")
                                .hoverEvent(Text.of("&aclick to warp!"))
                                .clickEvent(ClickEvent.runCommand("/server " + c.id())))
        ));
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
