package bridge;

import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.event.events.player.PlayerChatEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.util.Config;
import com.hypixel.hytale.server.core.universe.Universe;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;

import javax.annotation.Nonnull;
import java.awt.Color;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;

public final class BridgePlugin extends JavaPlugin {
    private static final Color TELEGRAM_COLOR = new Color(135, 206, 250);
    private static final Color TELEGRAM_TAG_COLOR = new Color(255, 215, 0);
    private final Config<BridgeConfig> config = this.withConfig("Bridge", BridgeConfig.CODEC);
    private TelegramBot telegramBot = TelegramBot.DISABLED;
    private TelegramBotsLongPollingApplication botsApplication;
    private final Map<String, String> currentUsers = new LinkedHashMap<>();
    private final AtomicLong replyIdCounter = new AtomicLong();
    private volatile String lastReplyId;
    private final Map<String, ReplyContext> replyCache = Collections.synchronizedMap(new LinkedHashMap<>(256, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, ReplyContext> eldest) {
            return size() > 200;
        }
    });

    public BridgePlugin(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        config.save();
        getCommandRegistry().registerCommand(new TelegramReplyCommand(this));
        getEventRegistry().registerGlobal(PlayerChatEvent.class, this::onPlayerChat);
        getEventRegistry().registerGlobal(PlayerReadyEvent.class, this::onPlayerJoin);
        getEventRegistry().registerGlobal(PlayerDisconnectEvent.class, this::onPlayerDisconnect);
        getEntityStoreRegistry().registerSystem(new PlayerDeathSystem(this));
    }

    @Override
    protected void start() {
        BridgeConfig cfg = config.get();
        String token = cfg.getTelegramToken().trim();
        String chatId = cfg.getChatId().trim();
        if (token.isEmpty() || chatId.isEmpty()) {
            getLogger().at(Level.WARNING).log("Telegram bridge configuration is missing. Please fill in Bridge.json.");
            getLogger().at(Level.INFO).log("Looking for config at: " + getDataDirectory().resolve("Bridge.json").toAbsolutePath());
            return;
        }

        telegramBot = new TelegramBot(token, chatId, this::handleTelegramMessage);

        botsApplication = new TelegramBotsLongPollingApplication();
        try {
            botsApplication.registerBot(token, telegramBot);
            getLogger().at(Level.INFO).log("Telegram bridge started.");
        } catch (Exception e) {
            getLogger().at(Level.SEVERE).log("Failed to start Telegram bridge: " + e.getMessage());
        }

        sendToTelegram("<b><i>Server started!</i></b>");
    }

    @Override
    protected void shutdown() {
        if (botsApplication == null) return;

        try {
            botsApplication.close();
            getLogger().at(Level.INFO).log("Telegram bridge shut down.");
        } catch (Exception e) {
            getLogger().at(Level.SEVERE).log("Error shutting down Telegram bridge: " + e.getMessage());
        }
    }

    private void onPlayerChat(PlayerChatEvent event) {
        sendToTelegram("<b>" + event.getSender().getUsername() + "</b>: " + event.getContent());
    }

    private void onPlayerJoin(PlayerReadyEvent event) {
        String username = event.getPlayer().getPlayerRef().getUsername();
        if (currentUsers.containsKey(username)) return;
        currentUsers.put(username, event.getPlayer().getDisplayName());

        int current = currentUsers.size();
        int total = HytaleServer.get().getConfig().getMaxPlayers();
        sendToTelegram("<b>" + event.getPlayer().getDisplayName() + "</b> joined the server! <b>Player count</b>: " + current + " of " + total + ".");
    }

    private void onPlayerDisconnect(PlayerDisconnectEvent event) {
        String username = event.getPlayerRef().getUsername();
        String displayName = currentUsers.remove(username);
        if (displayName == null) return;

        int current = currentUsers.size();
        int total = HytaleServer.get().getConfig().getMaxPlayers();
        sendToTelegram("<b>" + displayName + "</b> disconnected. <b>Player count</b>: " + current + " of " + total + ".");
    }

    public void sendToTelegram(String message) {
        if (telegramBot == TelegramBot.DISABLED) return;
        if (message == null || message.isEmpty()) return;
        telegramBot.sendMessage(message);
    }

    private void handleTelegramMessage(TelegramMessage message) {
        if (message == null) return;

        String text = message.text();
        if (message.type() == TelegramMessageType.TEXT && (text == null || text.isEmpty())) return;

        if (message.type() == TelegramMessageType.TEXT && text.equals("/players")) {
            sendPlayersList();
            return;
        }

        String replyId = null;
        ReplyContext reply = message.reply();
        if (reply != null && reply.originalText() != null && !reply.originalText().isEmpty()) {
            replyId = storeReplyContext(reply);
        }

        broadcastToHytale(message, replyId);
    }

    private void broadcastToHytale(TelegramMessage message, String replyId) {
        Universe universe = Universe.get();
        if (universe == null) return;

        Message prefix = Message.raw("[telegram] ").color(TELEGRAM_COLOR).bold(true);
        Message replyTag = null;
        Message hint = null;
        if (replyId != null) {
            replyTag = Message.raw("[reply] ").color(TELEGRAM_COLOR).bold(true);
            hint = Message.raw("(use /tgreply)").color(Color.LIGHT_GRAY).bold(false);
        }

        Message body;
        String logText;
        if (message.type() == TelegramMessageType.TEXT) {
            String messageText = message.sender() + ": " + message.text();
            body = Message.raw(messageText).color(Color.WHITE).bold(false);
            logText = messageText;
        } else {
            String tagLabel = switch (message.type()) {
                case IMAGE -> "[image] ";
                case VIDEO -> "[video] ";
                case STICKER -> "[sticker] ";
                default -> "[media] ";
            };
            Message tag = Message.raw(tagLabel).color(TELEGRAM_TAG_COLOR).bold(true);
            String tail = "by " + message.sender();
            if (message.text() != null && !message.text().isEmpty()) {
                tail = tail + ": " + message.text();
            }
            Message tailMessage = Message.raw(tail).color(Color.WHITE).bold(false);
            body = Message.join(tag, tailMessage);
            logText = tagLabel.trim() + " " + tail;
        }

        if (replyTag != null) {
            universe.sendMessage(Message.join(prefix, replyTag, hint, Message.raw(" "), body));
        } else {
            universe.sendMessage(Message.join(prefix, body));
        }
        getLogger().at(Level.INFO).log("Broadcasted Telegram message to Hytale: " + logText);
    }

    private void sendPlayersList() {
        if (currentUsers.isEmpty()) {
            sendToTelegram("No players online.");
            return;
        }

        int max = HytaleServer.get().getConfig().getMaxPlayers();
        StringBuilder sb = new StringBuilder();
        sb.append("Online players (").append(currentUsers.size()).append("/").append(max).append("):\n\n");
        int i = 0;
        for (String name : currentUsers.values()) {
            sb.append(++i).append(". ").append(name).append('\n');
        }
        sendToTelegram(sb.toString());
    }

    private String storeReplyContext(ReplyContext reply) {
        String id = "r" + replyIdCounter.incrementAndGet();
        replyCache.put(id, reply);
        lastReplyId = id;
        return id;
    }

    public ReplyContext getReplyContext(String id) {
        if (id == null) return null;
        return replyCache.get(id);
    }

    public String getLastReplyId() {
        return lastReplyId;
    }
}
