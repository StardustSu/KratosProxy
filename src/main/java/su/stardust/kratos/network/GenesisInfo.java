package su.stardust.kratos.network;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import lombok.SneakyThrows;
import su.stardust.kratos.Kratos;
import su.stardust.kratos.StarLogger;

public class GenesisInfo {
    private static final ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();

    public static void run() {
        service.scheduleAtFixedRate(GenesisInfo::sendOnline, 0, 5, TimeUnit.SECONDS);
    }

    @SneakyThrows
    public static void sendOnline() {
        var online = Kratos.getServer().getAllPlayers().size();
        if (online <= 0)
            online = 1;
        var string = "{\"online\":" + online + "}";
        var data = string.getBytes();
        // StarLogger.debug("Genesis Online sending: " + string);
        var url = URI.create("https://api.kratosmc.ru/info/online").toURL();
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setDoOutput(true);
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json");
        con.connect();
        try (OutputStream os = con.getOutputStream()) {
            os.write(data);
        }
        con.disconnect();
        if (con.getResponseCode() != 201)
            StarLogger.error("Genesis Online status: " + con.getResponseCode());
    }
}
