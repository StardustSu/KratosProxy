package su.stardust.kratos.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.Nullable;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.velocitypowered.api.proxy.Player;

import lombok.SneakyThrows;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.node.Node;
import su.stardust.kratos.StarLogger;
import su.stardust.kratos.Text;
import su.stardust.kratos.interfaces.GenesisPlayerDTO;
import su.stardust.kratos.interfaces.GenesisWorldDTO;
import su.stardust.kratos.network.Containers.Container;

public class Genesis {

    private static <T> T fetch(String url, Class<T> dto) {
        URL uri;
        try {
            uri = new URI("https://api.kratosmc.ru" + url).toURL();
        } catch (MalformedURLException | URISyntaxException e) {
            StarLogger.error("FAILED FETCHING GENESIS " + url);
            e.printStackTrace();
            return null;
        }
        HttpURLConnection connection;
        try {
            connection = (HttpURLConnection) uri.openConnection();
        } catch (IOException e) {
            StarLogger.error("FAILED FETCHING GENESIS " + url);
            e.printStackTrace();
            return null;
        }
        try {
            connection.setRequestProperty("accept", "application/json");
            if (connection.getResponseCode() >= 299)
                return null;
            InputStream responseStream = connection.getInputStream();
            ObjectMapper mapper = new ObjectMapper();
            T data = mapper.readValue(responseStream, dto);
            return data;
        } catch (IOException e) {
            StarLogger.error("FAILED PARSING GENESIS " + url);
            e.printStackTrace();
            return null;
        }
    }

    private static String fetchString(String url) {
        URL uri;
        try {
            uri = new URI("https://api.kratosmc.ru" + url).toURL();
        } catch (MalformedURLException | URISyntaxException e) {
            StarLogger.error("FAILED FETCHING GENESIS " + url);
            e.printStackTrace();
            return null;
        }
        HttpURLConnection connection;
        try {
            connection = (HttpURLConnection) uri.openConnection();
        } catch (IOException e) {
            StarLogger.error("FAILED FETCHING GENESIS " + url);
            e.printStackTrace();
            return null;
        }
        try {
            connection.setRequestProperty("accept", "text/plain");
            if (connection.getResponseCode() >= 299)
                return null;

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(connection.getInputStream()));
            String inputLine;
            StringBuilder content = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
                content.append("\n");
            }
            in.close();
            var value = content.toString();

            return value;
        } catch (IOException e) {
            StarLogger.error("FAILED PARSING GENESIS " + url);
            e.printStackTrace();
            return null;
        }
    }

    @SneakyThrows
    private static void post(String uri, String data) {
        var url = URI.create("https://api.kratosmc.ru" + uri).toURL();
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setDoOutput(true);
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json");
        con.connect();
        try (OutputStream os = con.getOutputStream()) {
            os.write(data.getBytes());
        }
        con.disconnect();
        if (con.getResponseCode() != 201) {
            StarLogger.error("POST " + uri + " status: " + con.getResponseCode());
            // BufferedReader in = new BufferedReader(
            // new InputStreamReader(con.getInputStream()));
            // String inputLine;
            // StringBuilder content = new StringBuilder();
            // while ((inputLine = in.readLine()) != null) {
            // content.append(inputLine);
            // content.append("\n");
            // }
            // in.close();
            // StarLogger.error(content.toString());
        }
    }

    public static @Nullable GenesisPlayerDTO fetchPlayer(String name) {
        return fetch("/genesis/player/" + name, GenesisPlayerDTO.class);
    }

    public static String[] fetchWorlds() {
        return fetch("/genesis/worlds", String[].class);
    }

    public static @Nullable GenesisWorldDTO fetchWorld(String name) {
        return fetch("/genesis/worlds/" + name, GenesisWorldDTO.class);
    }

    public static void syncPlayer(Player player) {
        var gplayer = Genesis.fetchPlayer(player.getUsername());
        if (gplayer != null) {
            LuckPerms lp = LuckPermsProvider.get();
            var lplayer = lp.getUserManager().getUser(player.getUsername());

            var time = System.currentTimeMillis();
            var nodes = lplayer.getNodes();

            var hasPlus = gplayer.plus_until > time;
            var plusDuration = gplayer.plus_until - time;

            nodes
                    .stream()
                    .filter(node -> node.getKey().equalsIgnoreCase("group.plus"))
                    .forEach(node -> {
                        lplayer.data().remove(node);
                    });

            if (hasPlus) {
                lplayer.data().add(Node
                        .builder("group.plus")
                        .expiry(plusDuration, TimeUnit.MILLISECONDS)
                        .build());
            }

            if (!nodes.stream().anyMatch(node -> node.getKey().equalsIgnoreCase("group." + gplayer.group))) {
                lplayer.data().add(Node
                        .builder("group." + gplayer.group)
                        .build());
            }

            lp.getUserManager().saveUser(lplayer);
            player.sendMessage(Text.of("&aДанные загружены!"));
        } else {
            player.sendMessage(Text.of("&cНе удалось получить сведения о профиле!"));
        }
    }

    public static void sendOnlineStats(int full, String byType) {
        try {
            var data = "{\"online\":" + full + ",\"modes\":\"" + byType.replace("\"", "\\\"") + "\"}";
            // StarLogger.debug(data);
            post("/genesis/stats/online", data);
        } catch (Exception ex) {
            ex.printStackTrace();
            StarLogger.error("Failed posting Online Stats!!");
        }
    }

    public static void saveContainersState() {
        var containers = Containers.getAll();
        var instructions = new StringBuilder();
        instructions
                .append(Containers.Helper.getServerIdCache())
                .append(" ")
                .append(Containers.Helper.getFreePorts())
                .append("\\n");
        for (Container container : containers) {
            instructions
                    .append(container.id())
                    .append(" ")
                    .append(container.type())
                    .append(" ")
                    .append(container.port())
                    .append(" ")
                    .append(container.image())
                    .append(" ")
                    .append(container.mem())
                    .append("\\n");
        }
        var data = "{\"data\":\"" + instructions.toString() + "\"}";
        StarLogger.debug(data);
        post("/genesis/containers/cache", data);
    }

    public static void getContainersState() {
        var state = fetchString("/genesis/containers/cache");
        if (state == null)
            return;
        var first = true;
        for (String instruction : state.split("\n")) {
            if (instruction.length() < 3)
                continue;
            var split = instruction.split(" ");
            if (first) {
                first = false;
                var freep = split.length == 2 ? split[1] : "";
                Containers.Helper.loadCache(Integer.parseInt(split[0]), freep);
                StarLogger.debug("Port info:");
                StarLogger.debug(split[0]);
                StarLogger.debug(freep);
                continue;
            }
            StarLogger.debug("Got container " + split[0] + " with type " + split[1]);
            var container = new Containers.Container(
                    split[0],
                    split[1],
                    split[3],
                    Integer.parseInt(split[2]),
                    Integer.parseInt(split[4]));
            Containers.registerContainer(container);
            Keepalive.registerContainer(container);
        }
    }
}
