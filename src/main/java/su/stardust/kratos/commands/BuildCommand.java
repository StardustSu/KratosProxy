package su.stardust.kratos.commands;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;

import su.stardust.kratos.Kratos;
import su.stardust.kratos.Text;
import su.stardust.kratos.listeners.BuildListener;
import su.stardust.kratos.network.Containers;
import su.stardust.kratos.network.Genesis;
import su.stardust.kratos.network.Keepalive;

import java.util.ArrayList;
import java.util.List;

public class BuildCommand implements SimpleCommand {
    @Override
    public void execute(Invocation invocation) {
        if (invocation.source() instanceof Player p) {
            BuildListener.sendBuildMenu(p);
        }
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        return List.of();
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("kratos.build");
    }
}
