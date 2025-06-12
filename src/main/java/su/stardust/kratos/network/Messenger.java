package su.stardust.kratos.network;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import su.stardust.kratos.Kratos;
import su.stardust.kratos.StarLogger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

public class Messenger {
    private static Connection rabbit;
    private static Channel channel;
    private static final ArrayList<String> queues = new ArrayList<>();

    public static void initializeRabbit() {
        if (rabbit != null || channel != null) return;
        var factory = new ConnectionFactory();
        var rbt = Kratos.getRabbitSettings();
        factory.setHost(rbt[0]);
        factory.setUsername(rbt[1]);
        factory.setPassword(rbt[2]);
        factory.setVirtualHost(rbt[3]);
        factory.setAutomaticRecoveryEnabled(true);
        try {
            rabbit = factory.newConnection();
            channel = rabbit.createChannel(10);
        } catch (IOException | TimeoutException e) {
            StarLogger.fatal("Could not connect to RabbitMQ!");
            StarLogger.fatal("Shutting down");
            Kratos.getServer().shutdown();
        } finally {
            if (channel != null) {
                StarLogger.success("Connected to RabbitMQ!");
            }
        }
    }

    public static void consumeQueue(String queue, Consumer<String> consumer) {
        try {
            declareQueue(queue);
            StarLogger.log("* Consuming " + queue + " messages");
            channel.basicConsume(queue, true, (consumerTag, delivery) -> {
                String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                consumer.accept(message);
            }, consumerTag -> { });
        } catch (IOException e) {
            StarLogger.error("Could not open RabbitMQ channel! Shutting down...");
            Kratos.getServer().shutdown();
        }
    }

    public static void send(String queue, String message) {
        try {
            declareQueue(queue);
            if (!"KEEPALIVE".equals(queue))
                StarLogger.debug("* Sending " + queue + ": " + message);
            channel.basicPublish("", queue, null, message.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            StarLogger.error("Could not open RabbitMQ channel! Shutting down...");
            Kratos.getServer().shutdown();
        }
    }

    private static void declareQueue(String queue) throws IOException {
        if (queues.contains(queue)) return;
        channel.queueDeclare(queue, true, false, false, null);
        queues.add(queue);
    }
}
