package su.stardust.kratos.party;

import com.velocitypowered.api.proxy.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Party {
    private UUID leader;
    private final Set<UUID> members = new HashSet<>();
    private boolean privateGames = false;

    public Party(UUID leader) {
        this.leader = leader;
        members.add(leader);
    }

    public UUID getLeader() {
        return leader;
    }

    public void setLeader(UUID leader) {
        this.leader = leader;
    }

    public Set<UUID> getMembers() {
        return members;
    }

    public boolean isPrivateGames() {
        return privateGames;
    }

    public void setPrivateGames(boolean privateGames) {
        this.privateGames = privateGames;
    }

    public boolean addMember(Player player) {
        return members.add(player.getUniqueId());
    }

    public boolean removeMember(Player player) {
        return members.remove(player.getUniqueId());
    }
}
