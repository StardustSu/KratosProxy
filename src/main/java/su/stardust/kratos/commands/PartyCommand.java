package su.stardust.kratos.commands;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import su.stardust.kratos.Kratos;
import su.stardust.kratos.Text;
import su.stardust.kratos.party.Party;
import su.stardust.kratos.party.PartyManager;

import java.util.List;
import java.util.UUID;

public class PartyCommand implements SimpleCommand {

    @Override
    public void execute(Invocation invocation) {
        if (!(invocation.source() instanceof Player player)) {
            invocation.source().sendMessage(Text.of("&cТолько игроки могут использовать эту команду."));
            return;
        }

        var args = invocation.arguments();
        if (args.length == 0) {
            displayInfo(player);
            return;
        }

        switch (args[0].toLowerCase()) {
            case "leave" -> leave(player);
            case "warp" -> warp(player);
            case "promote" -> {
                if (args.length < 2) {
                    player.sendMessage(Text.of("&cУкажите ник."));
                    return;
                }
                promote(player, args[1]);
            }
            case "kick" -> {
                if (args.length < 2) {
                    player.sendMessage(Text.of("&cУкажите ник."));
                    return;
                }
                kick(player, args[1]);
            }
            case "disband" -> disband(player);
            case "invite" -> {
                if (args.length < 2) {
                    player.sendMessage(Text.of("&cУкажите ник."));
                    return;
                }
                invite(player, args[1]);
            }
            case "private" -> togglePrivate(player);
            default -> invite(player, args[0]);
        }
    }

    private void displayInfo(Player player) {
        var party = PartyManager.getParty(player);
        if (party == null) {
            player.sendMessage(Text.of("&eВы не в пати."));
            return;
        }
        var builder = new StringBuilder("&aУчастники: ");
        for (UUID id : party.getMembers()) {
            var member = PartyManager.uuidToPlayer(id);
            if (member != null) {
                if (id.equals(party.getLeader())) builder.append("&6");
                builder.append(member.getUsername()).append("&f, ");
            }
        }
        var msg = builder.toString();
        if (msg.endsWith(", ")) msg = msg.substring(0, msg.length() - 2);
        player.sendMessage(Text.of(msg));
    }

    private void leave(Player player) {
        var party = PartyManager.getParty(player);
        if (party == null) {
            player.sendMessage(Text.of("&cВы не в пати."));
            return;
        }
        PartyManager.removePlayer(player);
        player.sendMessage(Text.of("&aВы покинули пати."));
    }

    private void warp(Player player) {
        var party = PartyManager.getParty(player);
        if (party == null || !party.getLeader().equals(player.getUniqueId())) {
            player.sendMessage(Text.of("&cТолько лидер может использовать warp."));
            return;
        }
        var server = player.getCurrentServer().orElse(null);
        if (server == null) {
            player.sendMessage(Text.of("&cВы не на сервере."));
            return;
        }
        for (UUID id : party.getMembers()) {
            var p = PartyManager.uuidToPlayer(id);
            if (p != null && !p.equals(player)) {
                p.createConnectionRequest(server.getServer()).connectWithIndication();
            }
        }
    }

    private void promote(Player player, String name) {
        var party = PartyManager.getParty(player);
        if (party == null || !party.getLeader().equals(player.getUniqueId())) {
            player.sendMessage(Text.of("&cТолько лидер может повышать участников."));
            return;
        }
        var target = Kratos.getServer().getPlayer(name);
        if (target.isEmpty() || !party.getMembers().contains(target.get().getUniqueId())) {
            player.sendMessage(Text.of("&cИгрок не в вашей пати."));
            return;
        }
        party.setLeader(target.get().getUniqueId());
        player.sendMessage(Text.of("&aНовый лидер: " + target.get().getUsername()));
    }

    private void kick(Player player, String name) {
        var party = PartyManager.getParty(player);
        if (party == null || !party.getLeader().equals(player.getUniqueId())) {
            player.sendMessage(Text.of("&cТолько лидер может кикать участников."));
            return;
        }
        var target = Kratos.getServer().getPlayer(name);
        if (target.isEmpty() || !party.getMembers().contains(target.get().getUniqueId())) {
            player.sendMessage(Text.of("&cИгрок не в вашей пати."));
            return;
        }
        if (target.get().equals(player)) {
            player.sendMessage(Text.of("&cНельзя кикнуть себя."));
            return;
        }
        PartyManager.removePlayer(target.get());
        target.get().sendMessage(Text.of("&cВас кикнули из пати."));
    }

    private void disband(Player player) {
        var party = PartyManager.getParty(player);
        if (party == null || !party.getLeader().equals(player.getUniqueId())) {
            player.sendMessage(Text.of("&cТолько лидер может распустить пати."));
            return;
        }
        for (UUID id : PartyManager.getMembers(party)) {
            var p = PartyManager.uuidToPlayer(id);
            if (p != null) {
                p.sendMessage(Text.of("&cПати была распущена."));
            }
        }
        PartyManager.disband(party);
    }

    private void invite(Player player, String name) {
        var targetOpt = Kratos.getServer().getPlayer(name);
        if (targetOpt.isEmpty()) {
            player.sendMessage(Text.of("&cИгрок не найден."));
            return;
        }
        var target = targetOpt.get();
        var party = PartyManager.getParty(player);
        if (party == null) {
            party = PartyManager.getOrCreateParty(player);
        }
        if (!party.getLeader().equals(player.getUniqueId())) {
            player.sendMessage(Text.of("&cТолько лидер может приглашать."));
            return;
        }
        if (PartyManager.getParty(target) != null) {
            player.sendMessage(Text.of("&cИгрок уже в пати."));
            return;
        }
        PartyManager.addPlayer(party, target);
        target.sendMessage(Text.of("&aВы были добавлены в пати."));
        player.sendMessage(Text.of("&aИгрок добавлен в пати."));
    }

    private void togglePrivate(Player player) {
        var party = PartyManager.getParty(player);
        if (party == null || !party.getLeader().equals(player.getUniqueId())) {
            player.sendMessage(Text.of("&cТолько лидер может менять приватность."));
            return;
        }
        if (!player.hasPermission("kratos.mini.private")) {
            player.sendMessage(Text.of("&cУ вас нет прав."));
            return;
        }
        party.setPrivateGames(!party.isPrivateGames());
        player.sendMessage(Text.of("&aПриватные игры: " + (party.isPrivateGames() ? "включены" : "выключены")));
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        if (!(invocation.source() instanceof Player)) {
            return List.of();
        }
        var args = invocation.arguments();
        if (args.length == 1) {
            return List.of("leave", "warp", "promote", "kick", "disband", "invite", "private");
        }
        if (args.length == 2 && switch (args[0].toLowerCase()) { case "promote", "kick", "invite" -> true; default -> false; }) {
            var prefix = args[1];
            return Kratos.getServer().getAllPlayers().stream()
                    .map(Player::getUsername)
                    .filter(n -> n.toLowerCase().startsWith(prefix.toLowerCase()))
                    .toList();
        }
        return List.of();
    }
}
