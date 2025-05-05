package su.stardust.kratos;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class Text {
    private static final LegacyComponentSerializer serializer =
            LegacyComponentSerializer.builder()
                    .character('&')
                    .extractUrls()
                    .hexColors()
                    .build();

    public static Component of(String text) {
        return serializer.deserialize(text);
    }
}
