package su.stardust.kratos.commands;

import com.velocitypowered.api.command.SimpleCommand;
import su.stardust.kratos.Text;
import su.stardust.kratos.network.Containers;

import java.util.List;

public class WhatIsCommand implements SimpleCommand {
    @Override
    public void execute(Invocation invocation) {
        String[] args = invocation.arguments();
        if (args.length != 1) {
            invocation.source().sendMessage(Text.of("&c# /whatis <server>"));
            return;
        }
        Containers.get(args[0])
                .ifPresentOrElse(
                        c -> {
                            invocation.source().sendMessage(Text.of(""));
                            invocation.source().sendMessage(Text.of("&e" + c.id()));
                            invocation.source().sendMessage(Text.of("of type: &a" + c.type()));
                            invocation.source().sendMessage(Text.of("using &7" + c.image()));
                            invocation.source().sendMessage(Text.of("running on &d" + c.port()));
                            invocation.source().sendMessage(Text.of("with &6" + c.mem() + " MiB &fof RAM"));
                            invocation.source().sendMessage(Text.of(""));
                        },
                        () -> invocation.source().sendMessage(Text.of("&cServer not found."))
                );
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        return Containers.getAll().stream().map(Containers.Container::id).toList();
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("kratos.admin");
    }
}
