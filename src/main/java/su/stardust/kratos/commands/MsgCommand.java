package su.stardust.kratos.commands;

import java.util.List;
import java.util.Arrays;

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
            if (sourceName.isEmpty())
                return;

            var args = invocation.arguments();
            var text = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

            invocation.source().sendMessage(Text.of(" &aЯ &6🡒 &a" + target.getUsername() + "&7: &f" + text));
            target.sendMessage(Text.of(" &a" + sourceName + " &6🡒 &aЯ&7: &f" + text));
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
