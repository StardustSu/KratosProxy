package su.stardust.kratos.interfaces;

public class GenesisPlayerDTO {
    public int id;
    public String nickname;
    public int balance;
    public long whitelisted_until;
    public long plus_until;
    public String group;

    public GenesisPlayerDTO() {
    }

    public GenesisPlayerDTO(String nickname, int balance, long whitelisted_until, long plus_until, String group) {
        this.nickname = nickname;
        this.balance = balance;
        this.whitelisted_until = whitelisted_until;
        this.plus_until = plus_until;
        this.group = group;
    }
}
