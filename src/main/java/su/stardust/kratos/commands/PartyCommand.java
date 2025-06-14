package su.stardust.kratos.commands;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import su.stardust.kratos.Kratos;
import su.stardust.kratos.Text;
import su.stardust.kratos.party.PartyManager;

import java.util.List;

public class PartyCommand implements SimpleCommand {

    private static final java.util.Set<String> TARGET_COMMANDS = java.util.Set.of("promote", "kick", "invite");

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
        var members = party.getMembers().stream()
                .map(n -> n.equals(party.getLeader()) ? "&6" + n + "&f" : n)
                .toList();
        player.sendMessage(Text.of("&aУчастники: " + String.join(", ", members)));
    }

    private void leave(Player player) {
        var party = PartyManager.getParty(player);
        if (party == null) {
            player.sendMessage(Text.of("&cВы не в пати."));
            return;
        }
        boolean wasLeader = party.getLeader().equals(player.getUsername());
        PartyManager.removePlayer(player);
        player.sendMessage(Text.of("&aВы покинули пати."));
        if (!party.getMembers().isEmpty()) {
            PartyManager.broadcast(party, "&e" + player.getUsername() + " покинул пати.");
            if (wasLeader) {
                var newLeaderName = party.getLeader();
                var newLeader = PartyManager.nameToPlayer(newLeaderName);
                if (newLeader != null) {
                    newLeader.sendMessage(Text.of("&eТеперь вы лидер пати."));
                }
            }
        }
    }

    private void warp(Player player) {
        var party = PartyManager.getParty(player);
        if (party == null || !party.getLeader().equals(player.getUsername())) {
            player.sendMessage(Text.of("&cТолько лидер может использовать warp."));
            return;
        }
        var server = player.getCurrentServer().orElse(null);
        if (server == null) {
            player.sendMessage(Text.of("&cВы не на сервере."));
            return;
        }
        for (String name : party.getMembers()) {
            var p = PartyManager.nameToPlayer(name);
            if (p != null && !p.equals(player)) {
                p.createConnectionRequest(server.getServer()).connectWithIndication();
            }
        }
    }

    private void promote(Player player, String name) {
        var party = PartyManager.getParty(player);
        if (party == null || !party.getLeader().equals(player.getUsername())) {
            player.sendMessage(Text.of("&cТолько лидер может повышать участников."));
            return;
        }
        var target = Kratos.getServer().getPlayer(name);
        if (target.isEmpty() || !party.getMembers().contains(target.get().getUsername())) {
            player.sendMessage(Text.of("&cИгрок не в вашей пати."));
            return;
        }
        party.setLeader(target.get().getUsername());
        PartyManager.broadcast(party, "&aНовый лидер пати: " + target.get().getUsername());
        target.get().sendMessage(Text.of("&eТеперь вы лидер пати."));
    }

    private void kick(Player player, String name) {
        var party = PartyManager.getParty(player);
        if (party == null || !party.getLeader().equals(player.getUsername())) {
            player.sendMessage(Text.of("&cТолько лидер может кикать участников."));
            return;
        }
        var target = Kratos.getServer().getPlayer(name);
        if (target.isEmpty() || !party.getMembers().contains(target.get().getUsername())) {
            player.sendMessage(Text.of("&cИгрок не в вашей пати."));
            return;
        }
        if (target.get().equals(player)) {
            player.sendMessage(Text.of("&cНельзя кикнуть себя."));
            return;
        }
        boolean wasLeader = party.getLeader().equals(target.get().getUsername());
        PartyManager.removePlayer(target.get());
        target.get().sendMessage(Text.of("&cВас кикнули из пати."));
        if (!party.getMembers().isEmpty()) {
            PartyManager.broadcast(party,
                    "&c" + player.getUsername() + " исключил " + target.get().getUsername() + " из пати.");
            if (wasLeader) {
                var newLeader = PartyManager.nameToPlayer(party.getLeader());
                if (newLeader != null)
                    newLeader.sendMessage(Text.of("&eТеперь вы лидер пати."));
            }
        }
    }

    private void disband(Player player) {
        var party = PartyManager.getParty(player);
        if (party == null || !party.getLeader().equals(player.getUsername())) {
            player.sendMessage(Text.of("&cТолько лидер может распустить пати."));
            return;
        }
        PartyManager.broadcast(party, "&cПати была распущена.");
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
        if (!party.getLeader().equals(player.getUsername())) {
            player.sendMessage(Text.of("&cТолько лидер может приглашать."));
            return;
        }
        if (PartyManager.getParty(target) != null) {
            player.sendMessage(Text.of("&cИгрок уже в пати."));
            return;
        }
        PartyManager.addPlayer(party, target);
        target.sendMessage(Text.of("&aВы были добавлены в пати."));
        PartyManager.broadcast(party, "&a" + target.getUsername() + " присоединился к пати.");
    }

    private void togglePrivate(Player player) {
        var party = PartyManager.getParty(player);
        if (party == null || !party.getLeader().equals(player.getUsername())) {
            player.sendMessage(Text.of("&cТолько лидер может менять приватность."));
            return;
        }
        if (!player.hasPermission("kratos.mini.private")) {
            player.sendMessage(Text.of("&cУ вас нет прав."));
            return;
        }
        party.setPrivateGames(!party.isPrivateGames());
        PartyManager.broadcast(party, "&aПриватные игры: " + (party.isPrivateGames() ? "включены" : "выключены"));
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
        if (args.length == 2 && switch (args[0].toLowerCase()) {
            case "promote", "kick", "invite" -> true;
            default -> false;
        }) {
            var prefix = args[1];
            return Kratos.getServer().getAllPlayers().stream()
                    .map(Player::getUsername)
                    .filter(n -> n.toLowerCase().startsWith(prefix.toLowerCase()))
                    .toList();
        }
        return List.of();
    }
}
