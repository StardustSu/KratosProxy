package su.stardust.kratos.network;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.exception.NotModifiedException;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import su.stardust.kratos.Kratos;
import su.stardust.kratos.StarLogger;

import java.io.Closeable;
import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;

public class Containers {
    private static final DockerClientConfig config = DefaultDockerClientConfig
            .createDefaultConfigBuilder()
            .withDockerHost("unix:///var/run/docker.sock")
            .build();

    private static final DockerClient docker = DockerClientBuilder.getInstance(config).build();

    private static final HashMap<String, Container> containers = new HashMap<>();
    private static final List<String> images = new ArrayList<>();
    private static final List<Integer> freePorts = new ArrayList<>();

    public static void initializeDocker() {
        docker.pingCmd().exec();

        Messenger.consumeQueue("SPIN_UP", msg -> {
            // TYPE MEM IMAGE
            var data = msg.split(" ");
            if (data.length != 3)
                StarLogger.warn("Got unparsable message from SPINUP: " + msg);
            else {
                try {
                    var mem = Integer.parseInt(data[1]);
                    spinUp(data[0], mem, data[2]);
                } catch (NumberFormatException _e) {
                    spinUp(data[0], data[1], data[2]);
                }
            }
        });

        Messenger.consumeQueue("DOCKER_UPDATE", msg -> {
            // ALL | image
            if (msg.equalsIgnoreCase("all"))
                updateImages();
            else
                pullImage(msg, true, _a -> {
                });
        });

        StarLogger.info("Initialized Docker!");
    }

    public static void pullImage(String image, boolean force, Consumer<Boolean> callback) {
        if (images.contains(image) && !force) {
            callback.accept(true);
            return;
        }
        docker
                .pullImageCmd(image)
                // .withTag("latest")
                .exec(new ResultCallback<PullResponseItem>() {
                    @Override
                    public void close() throws IOException {

                    }

                    @Override
                    public void onStart(Closeable closeable) {
                        StarLogger.debug("[W] Pulling " + image);
                    }

                    @Override
                    public void onNext(PullResponseItem pullResponseItem) {

                    }

                    @Override
                    public void onError(Throwable throwable) {
                        callback.accept(false);
                    }

                    @Override
                    public void onComplete() {
                        if (!images.contains(image))
                            images.add(image);
                        callback.accept(true);
                    }
                });
    }

    public static void updateImages() {
        images.forEach(img -> {
            StarLogger.debug("Pulling " + img);
            pullImage(img, true, success -> {
                if (success)
                    StarLogger.success("Pulled " + img);
                else
                    StarLogger.error("Error pulling " + img);
            });
        });
    }

    public static String spinUp(String type, String mem, String image) {
        var id = Helper.createServerId(mem);
        var port = Helper.getPort();
        if (port == 0)
            throw new RuntimeException("Could not find free port to use!!!");
        int memory = switch (mem) {
            case "micro" -> 1024;
            case "mini" -> 2048;
            case "mega" -> 4096;
            case "super" -> 8192;
            default -> 2048;
        };
        var container = new Container(id, type, image, port, memory);
        pullImage(image, false, success -> {
            if (!success) {
                StarLogger.error("Failed pulling " + image);
                return;
            }
            var rabbit = Kratos.getRabbitSettings();
            docker.createContainerCmd(image)
                    .withEnv("SERVER_ID=" + id,
                            "SERVER_PORT=" + port,
                            "SERVER_TYPE=" + type,
                            "MAX_MEM=" + memory,
                            "RABBIT_HOST=" + rabbit[0],
                            "RABBIT_USER=" + rabbit[1],
                            "RABBIT_PASS=" + rabbit[2],
                            "RABBIT_VHOST=" + rabbit[3])
                    .withExposedPorts(ExposedPort.tcp(port), ExposedPort.sctp(port), ExposedPort.udp(port))
                    .withHostConfig(HostConfig.newHostConfig()
                            .withPortBindings(PortBinding.parse(port + ":" + port))
                            .withRestartPolicy(RestartPolicy.noRestart())
                            .withBinds(!type.equalsIgnoreCase("dev") ? List.of()
                                    : List.of(
                                            Bind.parse("/root/libs/KratosDev.jar:/app/plugins/Dev.jar"))))
                    .withName(id)
                    .withTty(true)
                    .withAttachStdin(true)
                    .exec();
            docker.startContainerCmd(id).exec();
            containers.put(id, container);
            StarLogger.debug("Running " + id + " with port " + port);
        });
        return id;
    }

    public static String spinUp(String type, int memory, String image) {
        String mem = switch (memory) {
            case 1024 -> "micro";
            case 2048 -> "mini";
            case 4096 -> "mega";
            case 8192 -> "super";
            default -> "mini";
        };
        return spinUp(type, mem, image);
    }

    public static void delete(String id) {
        delete(id, true);
    }

    public static void delete(String id, boolean soft) {
        if (soft) {
            pipe(id, "stop");
        } else {
            try {
                docker.stopContainerCmd(id).exec();
                docker.removeContainerCmd(id).exec();
            } catch (NotFoundException | NotModifiedException e) {
                if (e.getClass() == NotModifiedException.class)
                    try {
                        docker.removeContainerCmd(id).exec();
                    } catch (Exception ignored) {
                    }
                StarLogger.warn("Server " + id + " is probably already stopped or doesn't exist. Skipping...");
            } finally {
                StarLogger.success("Successfully stopped " + id);
                var container = containers.remove(id);
                freePorts.add(container.port);
            }
        }
    }

    public static void registerContainer(Container container) {
        containers.put(container.id, container);
    }

    public static void pipe(String id, String command) {
        var exec = docker.execCreateCmd(id)
                .withTty(true)
                .withCmd("sh", "-c", "echo '" + command + "' > /tmp/mc_cmd_pipe")
                .exec();
        docker.execStartCmd(id)
                .withExecId(exec.getId())
                .exec(new ResultCallback.Adapter<>());
    }

    public static Optional<Container> get(String id) {
        return Optional.ofNullable(containers.get(id));
    }

    public static Collection<Container> getAll() {
        return containers.values();
    }

    static class Helper {
        public static String createServerId(String pre) {
            return pre + Integer.toHexString(serverId++).toUpperCase(Locale.ROOT);
        }

        public static int getPort() {
            if (!freePorts.isEmpty())
                return freePorts.removeLast();
            return findPortRecur();
            // return serverId + 25566 - 16
        }

        private static int findPortRecur() {
            var port = serverId + 25566 - 16;
            if (containers.values().stream().anyMatch(c -> c.port == port)) {
                for (int i = 1; i < 100; i++) {
                    var next = port + i;
                    var match = containers.values().stream().anyMatch(c -> c.port == next);
                    if (!match)
                        return next;
                }
            } else
                return port;
            return 0;
        }

        public static String createServerId() {
            return createServerId("");
        }

        private static int serverId = 16;

        public static int getServerIdCache() {
            return serverId;
        }

        public static String getFreePorts() {
            return String.join(",", freePorts.stream().map(x -> x.toString()).toList());
        }

        public static void loadCache(int sid, String free) {
            Helper.serverId = sid;
            for (String split : free.split(",")) {
                if (split.length() < 2)
                    continue;
                freePorts.add(Integer.parseInt(split));
            }
        }
    }

    public record Container(
            String id,
            String type,
            String image,
            int port,
            int mem) {
    }
}
