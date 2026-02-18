package bridge;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

public class BridgeConfig {
    public static final BuilderCodec<BridgeConfig> CODEC = BuilderCodec.builder(BridgeConfig.class, BridgeConfig::new)
            .append(new KeyedCodec<>("TelegramToken", Codec.STRING), (c, v) -> c.telegramToken = v, c -> c.telegramToken)
            .add()
            .append(new KeyedCodec<>("ChatId", Codec.STRING), (c, v) -> c.chatId = v, c -> c.chatId)
            .add()
            .append(new KeyedCodec<>("RestartAllowedIds", Codec.STRING), (c, v) -> c.restartAllowedIds = v, c -> c.restartAllowedIds)
            .add()
            .build();

    private String telegramToken = "";
    private String chatId = "";
    private String restartAllowedIds = "";

    public String getTelegramToken() {
        return telegramToken;
    }

    public String getChatId() {
        return chatId;
    }

    public String getRestartAllowedIds() {
        return restartAllowedIds;
    }
}
