package su.stardust.kratos.commands;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;

import su.stardust.kratos.Kratos;
import su.stardust.kratos.Text;

import java.util.List;

public class VerboseCommand implements SimpleCommand {
    @Override
    public void execute(Invocation invocation) {
        if (invocation.source() instanceof Player player) {
            var enabled = Kratos.verboseLogsPlayers.contains(player.getUsername());
            if (enabled) {
                Kratos.verboseLogsPlayers.remove(player.getUsername());
                player.sendMessage(Text.of("&cYou disabled verbose output."));
                Kratos.logVerbose("&c" + player.getUsername() + " disabled verbose output.");
            } else {
                Kratos.verboseLogsPlayers.add(player.getUsername());
                Kratos.logVerbose("&a" + player.getUsername() + " enabled verbose output!");
            }
        }
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
