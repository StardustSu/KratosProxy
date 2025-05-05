package su.stardust.kratos.commands;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import su.stardust.kratos.Text;
import su.stardust.kratos.network.Matchmaking;

public class PlayCommand implements SimpleCommand {
    @Override
    public void execute(Invocation invocation) {
        if (invocation.arguments().length != 1)
            invocation.source().sendMessage(Text.of("&aИспользуйте NPC в лобби чтобы попасть в игру!"));
        else if (invocation.source() instanceof Player issuer) {
            var type = invocation.arguments()[0];
            Matchmaking.findGame(issuer, type);
        }
    }
}
