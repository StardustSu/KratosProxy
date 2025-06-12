package su.stardust.kratos.party;

import com.velocitypowered.api.proxy.Player;
import su.stardust.kratos.Kratos;

import java.util.*;

public class PartyManager {
    private static final Map<UUID, Party> parties = new HashMap<>();

    public static Party getOrCreateParty(Player leader) {
        var party = parties.get(leader.getUniqueId());
        if (party == null) {
            party = new Party(leader.getUniqueId());
            parties.put(leader.getUniqueId(), party);
        }
        return party;
    }

    public static Party getParty(Player player) {
        return parties.get(player.getUniqueId());
    }

    public static void addPlayer(Party party, Player player) {
        party.addMember(player);
        parties.put(player.getUniqueId(), party);
    }

    public static void removePlayer(Player player) {
        var party = parties.remove(player.getUniqueId());
        if (party != null) {
            party.removeMember(player);
            if (party.getMembers().isEmpty()) {
                return;
            }
            if (party.getLeader().equals(player.getUniqueId())) {
                var next = party.getMembers().iterator().next();
                party.setLeader(next);
            }
        }
    }

    public static void disband(Party party) {
        for (UUID id : new HashSet<>(party.getMembers())) {
            parties.remove(id);
        }
        party.getMembers().clear();
    }

    public static Collection<UUID> getMembers(Party party) {
        return new HashSet<>(party.getMembers());
    }

    public static Player uuidToPlayer(UUID id) {
        return Kratos.getServer().getPlayer(id).orElse(null);
    }
}
