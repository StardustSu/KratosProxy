package su.stardust.kratos.commands;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import su.stardust.kratos.Kratos;
import su.stardust.kratos.Text;

import java.util.List;

public class LobbyCommand implements SimpleCommand {
    @Override
    public void execute(Invocation invocation) {
        if (invocation.source() instanceof Player player) {
            var lobby = Kratos.pickLobby();
            player.createConnectionRequest(lobby).connectWithIndication();
        }
    }
}
