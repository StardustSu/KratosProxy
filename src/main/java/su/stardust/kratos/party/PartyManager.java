package su.stardust.kratos.party;

import com.velocitypowered.api.proxy.Player;
import su.stardust.kratos.Kratos;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import su.stardust.kratos.Text;

public class PartyManager {
    private static final Map<String, Party> parties = new HashMap<>();

    public static Party getOrCreateParty(Player leader) {
        var party = parties.get(leader.getUsername());
        if (party == null) {
            party = new Party(leader.getUsername());
            parties.put(leader.getUsername(), party);
        }
        return party;
    }

    public static Party getParty(Player player) {
        return parties.get(player.getUsername());
    }

    public static void addPlayer(Party party, Player player) {
        party.addMember(player);
        parties.put(player.getUsername(), party);
    }

    public static void removePlayer(Player player) {
        var party = parties.remove(player.getUsername());
        if (party != null) {
            party.removeMember(player);
            if (party.getMembers().isEmpty()) {
                return;
            }
            if (party.getLeader().equals(player.getUsername())) {
                var next = party.getMembers().iterator().next();
                party.setLeader(next);
            }
        }
    }

    public static void disband(Party party) {
        parties.keySet().removeAll(party.getMembers());
        party.getMembers().clear();
    }

    public static Collection<String> getMembers(Party party) {
        return new HashSet<>(party.getMembers());
    }

    public static Player nameToPlayer(String name) {
        return Kratos.getServer().getPlayer(name).orElse(null);
    }

    public static void broadcast(Party party, String message) {
        party.getMembers().stream()
                .map(PartyManager::nameToPlayer)
                .filter(Objects::nonNull)
                .forEach(p -> p.sendMessage(Text.of(message)));
    }
}
