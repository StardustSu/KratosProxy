package su.stardust.kratos.commands;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import su.stardust.kratos.Text;
import su.stardust.kratos.network.Messenger;
import su.stardust.kratos.party.PartyManager;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class PlayCommand implements SimpleCommand {
    @Override
    public void execute(Invocation invocation) {
        if (!(invocation.source() instanceof Player player)) {
            invocation.source().sendMessage(Text.of("&cТолько игроки могут использовать эту команду."));
            return;
        }

        if (invocation.arguments().length != 1) {
            player.sendMessage(Text.of("&cИспользуйте: /play <игра>"));
            return;
        }

        performPlay(player, invocation.arguments()[0]);
    }

    public static void performPlay(Player player, String game) {
        var party = PartyManager.getParty(player);
        if (party != null && !party.getLeader().equals(player.getUniqueId())) {
            player.sendMessage(Text.of("&cТолько лидер пати может искать игру."));
            return;
        }

        int count = 1;
        String members = player.getUsername();
        if (party != null) {
            var names = party.getMembers().stream()
                    .map(PartyManager::uuidToPlayer)
                    .filter(Objects::nonNull)
                    .map(Player::getUsername)
                    .collect(Collectors.toList());
            count = names.size();
            members = String.join(",", names);
        }

        Messenger.send("MATCHMAKING", "PLAY " + game + " " + count + " " + members);
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        return List.of();
    }
}
