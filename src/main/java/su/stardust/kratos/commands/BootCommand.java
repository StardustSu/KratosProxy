package su.stardust.kratos.commands;

import com.velocitypowered.api.command.SimpleCommand;
import net.kyori.adventure.text.event.ClickEvent;
import su.stardust.kratos.Kratos;
import su.stardust.kratos.Text;
import su.stardust.kratos.network.Keepalive;

import java.util.List;

public class BootCommand implements SimpleCommand {
    @Override
    public void execute(Invocation invocation) {
        String[] args = invocation.arguments();
        if (args.length != 1) {
            invocation.source().sendMessage(Text.of("&c# /boot <instruction>"));
            return;
        }
        invocation.source().sendMessage(Text.of("&aLooking up instructions"));

        var instructions = Kratos.loadInstructions(args[0]);
        if (instructions.length == 0) {
            invocation.source().sendMessage(Text.of("&cCould not find instructions for " + args[0]));
            return;
        }

        for (String instruction : instructions) {
            invocation.source().sendMessage(Text.of("&d" + instruction));
            Kratos.useInstructions(instruction);
        }
        invocation.source().sendMessage(Text.of("&aLoading done!"));
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
