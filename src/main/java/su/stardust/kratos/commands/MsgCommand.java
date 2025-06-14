package su.stardust.kratos.commands;

import java.util.List;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.api.proxy.Player;

import su.stardust.kratos.Kratos;
import su.stardust.kratos.Text;

public class MsgCommand implements SimpleCommand {
    @Override
    public void execute(Invocation invocation) {
        if (invocation.arguments().length < 2) {
            invocation.source().sendMessage(Text.of("&cИспользуйте: /msg <ник> <текст>"));
            return;
        }

        var name = invocation.arguments()[0];
        Kratos.getServer().getPlayer(name).ifPresentOrElse(target -> {
            var sourceName = "";
            if (invocation.source() instanceof Player p)
                sourceName = p.getUsername();
            if (invocation.source() instanceof ConsoleCommandSource)
                sourceName = "Kratos";
            if (sourceName.length() == 0)
                return;

            var args = invocation.arguments();
            var text = "";
            for (int i = 1; i < args.length; i++) {
                text += args[i] + " ";
            }

            invocation.source().sendMessage(Text.of(" &aЯ &6🡒 &a" + target.getUsername() + "&7: &f" + text.trim()));
            target.sendMessage(Text.of(" &a" + sourceName + " &6🡒 &aЯ&7: &f" + text.trim()));
        }, () -> {
            invocation.source().sendMessage(Text.of("&cИгрок не найден!"));
        });
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        if (invocation.arguments().length == 1) {
            var name = invocation.arguments()[0];
            if (name.length() < 2)
                return List.of();

            return Kratos.getServer().getAllPlayers()
                    .stream()
                    .map(Player::getUsername)
                    .filter(p -> p.startsWith(name))
                    .toList();
        }
        return List.of();
    }
}
