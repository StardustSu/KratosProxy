package su.stardust.kratos.party;

import com.velocitypowered.api.proxy.Player;

import java.util.HashSet;
import java.util.Set;

public class Party {
    private String leader;
    private final Set<String> members = new HashSet<>();
    private boolean privateGames = false;

    public Party(String leader) {
        this.leader = leader;
        members.add(leader);
    }

    public String getLeader() {
        return leader;
    }

    public void setLeader(String leader) {
        this.leader = leader;
    }

    public Set<String> getMembers() {
        return members;
    }

    public boolean isPrivateGames() {
        return privateGames;
    }

    public void setPrivateGames(boolean privateGames) {
        this.privateGames = privateGames;
    }

    public boolean addMember(Player player) {
        return members.add(player.getUsername());
    }

    public boolean removeMember(Player player) {
        return members.remove(player.getUsername());
    }
}
