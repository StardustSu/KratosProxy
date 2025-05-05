package su.stardust.kratos.commands;

import com.velocitypowered.api.command.SimpleCommand;
import su.stardust.kratos.Text;
import su.stardust.kratos.network.Containers;

import java.util.List;

public class ShutdownCommand implements SimpleCommand {
    @Override
    public void execute(Invocation invocation) {
        String[] args = invocation.arguments();
        if (args.length < 1) {
            invocation.source().sendMessage(Text.of("&c# /shutdown <server> [hard]"));
            return;
        }
        Containers.get(args[0])
                .ifPresentOrElse(
                        c -> {
                            var soft = args.length != 2 || !args[1].equals("true");
                            Containers.delete(c.id(), soft);
                            invocation.source().sendMessage(Text.of("&aSent SHUTDOWN request to " + c.id()));
                        },
                        () -> invocation.source().sendMessage(Text.of("&cServer not found."))
                );
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        if (invocation.arguments().length <= 1)
            return Containers.getAll().stream().map(Containers.Container::id).toList();
        return List.of("true", "false");
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("kratos.admin");
    }
}
