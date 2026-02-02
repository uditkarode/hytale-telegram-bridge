package bridge;
 
import java.awt.Color;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.event.events.player.PlayerChatEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.util.Config;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.util.NotificationUtil;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;

import javax.annotation.Nonnull;
import java.util.logging.Level;

public final class BridgePlugin extends JavaPlugin {
    private final Config<BridgeConfig> config = this.withConfig("Bridge", BridgeConfig.CODEC);
    private TelegramBot telegramBot;
    private TelegramBotsLongPollingApplication botsApplication;

    public BridgePlugin(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        // PlayerChatEvent and PlayerReadyEvent are keyed by String, so use registerGlobal
        getEventRegistry().registerGlobal(PlayerChatEvent.class, this::onPlayerChat);
        getEventRegistry().registerGlobal(PlayerReadyEvent.class, this::onPlayerJoin);
        // PlayerDisconnectEvent is IBaseEvent<Void>, so use register
        getEventRegistry().register(PlayerDisconnectEvent.class, this::onPlayerDisconnect);
    }

    @Override
    protected void start() {
        BridgeConfig cfg = config.get();
        if (cfg.getTelegramToken().isEmpty() || cfg.getChatId().isEmpty()) {
            getLogger().at(Level.WARNING).log("Telegram bridge configuration is missing. Please fill in Bridge.json.");
            getLogger().at(Level.INFO).log("Looking for config at: " + getDataDirectory().resolve("Bridge.json").toAbsolutePath());
            return;
        }

        telegramBot = new TelegramBot(cfg.getTelegramToken(), cfg.getChatId(), this::broadcastToHytale);
        botsApplication = new TelegramBotsLongPollingApplication();
        try {
            botsApplication.registerBot(cfg.getTelegramToken(), telegramBot);
            getLogger().at(Level.INFO).log("Telegram bridge started.");
        } catch (Exception e) {
            getLogger().at(Level.SEVERE).log("Failed to start Telegram bridge: " + e.getMessage());
        }
    }

    @Override
    protected void shutdown() {
        if (botsApplication != null) {
            try {
                botsApplication.close();
                getLogger().at(Level.INFO).log("Telegram bridge shut down.");
            } catch (Exception e) {
                getLogger().at(Level.SEVERE).log("Error shutting down Telegram bridge: " + e.getMessage());
            }
        }
    }

    private void onPlayerChat(PlayerChatEvent event) {
        String username = event.getSender().getUsername();
        String message = username + ": " + event.getContent();
        forwardToTelegram(message);
    }

    private void onPlayerJoin(PlayerReadyEvent event) {
        String username = event.getPlayer().getPlayerRef().getUsername();
        String message = username + " has joined the server.";
        forwardToTelegram(message);
    }

    private void onPlayerDisconnect(PlayerDisconnectEvent event) {
        String username = event.getPlayerRef().getUsername();
        String message = username + " has left the server.";
        forwardToTelegram(message);
    }

    private void forwardToTelegram(String message) {
        if (telegramBot != null) {
            telegramBot.sendMessage(message);
        }
    }

    private void broadcastToHytale(String sender, String text) {
        Message message = Message.join(
            Message.raw("[telegram] ").color(Color.BLUE).bold(true),
            Message.raw(sender + ": " + text).color(Color.WHITE).bold(false)
        );
        
        Universe.get().sendMessage(message);
        getLogger().at(Level.INFO).log("Broadcasted Telegram message to Hytale: " + text);
    }
}
