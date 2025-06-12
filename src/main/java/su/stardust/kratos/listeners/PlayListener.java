package su.stardust.kratos.listeners;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.ServerConnection;
import su.stardust.kratos.Kratos;
import su.stardust.kratos.commands.PlayCommand;

public class PlayListener {
    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        if (!Kratos.KRATOS_PLAY.equals(event.getIdentifier()))
            return;

        event.setResult(PluginMessageEvent.ForwardResult.handled());
        if (!(event.getSource() instanceof ServerConnection))
            return;

        ByteArrayDataInput in = ByteStreams.newDataInput(event.getData());
        var msg = in.readLine();
        if (msg == null)
            return;
        var parts = msg.split(" ");
        if (parts.length == 3 && parts[0].equalsIgnoreCase("PLAY")) {
            var issuerOpt = Kratos.getServer().getPlayer(parts[1]);
            issuerOpt.ifPresent(p -> PlayCommand.performPlay(p, parts[2]));
        }
    }
}
