package su.stardust.kratos.listeners;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import su.stardust.kratos.Kratos;
import su.stardust.kratos.Text;

public class StreamListener {

    @Subscribe
    public void onChooseServer(PlayerChooseInitialServerEvent e) {
        if (e.getInitialServer().isEmpty()) {
            var lobby = Kratos.pickLobby();
            if (lobby == null) {
                e.getPlayer().disconnect(Text.of("&cСервер перезагружается, перезайдите позже."));
                return;
            }
            e.setInitialServer(lobby);
        }
    }

    @Subscribe
    public void onServerDisconnect(KickedFromServerEvent e) {
        if (e.kickedDuringServerConnect())
            return;

        var lobby = Kratos.pickLobby();
        if (lobby != null) {
            e.setResult(KickedFromServerEvent.RedirectPlayer.create(lobby));
        }
    }
}
