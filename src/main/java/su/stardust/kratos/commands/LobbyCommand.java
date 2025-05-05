package su.stardust.kratos.commands;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.event.ClickEvent;
import su.stardust.kratos.Kratos;
import su.stardust.kratos.Text;
import su.stardust.kratos.network.Keepalive;

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
