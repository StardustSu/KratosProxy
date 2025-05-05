package su.stardust.kratos.network;

public class HealthCheck {
    public static void check() {
        lobbyCount();
    }

    public static void lobbyCount() {
        var lobbies = Keepalive.getAlive("lobby");
        if (lobbies.size() < 2) {
            for (int i = 0; i < 2 - lobbies.size(); i++) {
                Containers.spinUp("lobby", "micro", "thisisnico/kratos-dynamic-lobby");
            }
        }
    }
}
