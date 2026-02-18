package bridge;

import com.hypixel.hytale.server.core.command.system.CommandManager;
import com.hypixel.hytale.server.core.console.ConsoleSender;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class TelegramBot implements LongPollingSingleThreadUpdateConsumer {
    public static final TelegramBot DISABLED = new TelegramBot();

    private final TelegramClient telegramClient;
    private final String chatId;
    private final Consumer<TelegramMessage> messageHandler;
    private final Set<Long> restartAllowedIds;

    private TelegramBot() {
        this.telegramClient = null;
        this.chatId = null;
        this.messageHandler = null;
        this.restartAllowedIds = Set.of();
    }

    public TelegramBot(String token, String chatId, String restartAllowedIds, Consumer<TelegramMessage> messageHandler) {
        this.telegramClient = new OkHttpTelegramClient(token);
        this.chatId = chatId;
        this.messageHandler = messageHandler;
        this.restartAllowedIds = parseAllowedIds(restartAllowedIds);
        registerCommands();
    }

    private void registerCommands() {
        SetMyCommands setMyCommands = SetMyCommands.builder()
                .commands(List.of(
                        BotCommand.builder().command("players").description("Show online players").build(),
                        BotCommand.builder().command("server_restart").description("Restart the server").build()
                ))
                .build();
        try {
            telegramClient.execute(setMyCommands);
        } catch (TelegramApiException e) {
            System.err.println("Failed to register bot commands: " + e.getMessage());
        }
    }

    @Override
    public void consume(Update update) {
        if (!update.hasMessage()) return;

        Message message = update.getMessage();
        TelegramMessageType type = resolveType(message);
        if (type == null) return;

        String text = extractContent(message, type);
        if (type == TelegramMessageType.TEXT && (text == null || text.isEmpty())) return;
        if (chatId == null || !String.valueOf(message.getChatId()).equals(chatId)) return;

        if (type == TelegramMessageType.TEXT && stripBotSuffix(text).equals("/server_restart")) {
            if (!restartAllowedIds.isEmpty()) {
                User sender = message.getFrom();
                if (sender == null || !restartAllowedIds.contains(sender.getId())) {
                    sendMessage("You are not allowed to use this command.");
                    return;
                }
            }
            sendMessage("Restarting server...");
            sendMessage("A message will be sent when the server is ready again...");
            CommandManager.get().handleCommand(ConsoleSender.INSTANCE, "stop");
            return;
        }

        if (messageHandler == null) return;

        String sender = prepareUserName(message.getFrom());
        if (sender.isEmpty()) sender = "unknown";

        ReplyContext replyContext = null;
        Message replyTo = message.getReplyToMessage();
        if (replyTo != null) {
            TelegramMessageType replyType = resolveType(replyTo);
            if (replyType != null) {
                String originalSender = prepareUserName(replyTo.getFrom());
                if (originalSender.isEmpty()) originalSender = "unknown";
                String originalText = formatPreview(replyType, extractContent(replyTo, replyType));
                String replyText = formatPreview(type, text);
                replyContext = new ReplyContext(
                        replyTo.getMessageId(),
                        originalSender,
                        originalText,
                        sender,
                        replyText
                );
            }
        }

        messageHandler.accept(new TelegramMessage(sender, type, text, replyContext));
    }

    public void sendMessage(String text) {
        if (telegramClient == null || chatId == null) return;

        SendMessage sendMessage = SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .parseMode("HTML")
                .build();
        try {
            telegramClient.execute(sendMessage);
        } catch (TelegramApiException e) {
            System.err.println("Failed to send message to Telegram: " + e.getMessage());
        }
    }

    private static String prepareUserName(User user) {
        if (user == null) return "";

        StringBuilder name = new StringBuilder();
        if (user.getFirstName() != null && !user.getFirstName().isEmpty()) {
            name.append(user.getFirstName());
        }
        if (user.getLastName() != null && !user.getLastName().isEmpty()) {
            if (name.length() > 0) {
                name.append(' ');
            }
            name.append(user.getLastName());
        }

        String combined = name.toString().trim();
        if (combined.isEmpty() && user.getUserName() != null && !user.getUserName().isEmpty()) {
            combined = user.getUserName();
        }

        return combined.replaceAll("[^\\p{L}\\p{N} ]+", "").trim();
    }

    private static TelegramMessageType resolveType(Message message) {
        if (message == null) return null;
        if (message.hasText()) return TelegramMessageType.TEXT;
        if (message.hasPhoto()) return TelegramMessageType.IMAGE;
        if (message.hasVideo() || message.hasAnimation()) return TelegramMessageType.VIDEO;
        if (message.hasSticker()) return TelegramMessageType.STICKER;
        return null;
    }

    private static String extractContent(Message message, TelegramMessageType type) {
        if (message == null || type == null) return "";
        switch (type) {
            case TEXT:
                return message.getText();
            case IMAGE:
            case VIDEO:
                return message.getCaption() == null ? "" : message.getCaption();
            case STICKER:
                if (message.getSticker() != null && message.getSticker().getEmoji() != null) {
                    return message.getSticker().getEmoji();
                }
                return "";
            default:
                return "";
        }
    }

    private static String stripBotSuffix(String text) {
        int at = text.indexOf('@');
        return at > 0 ? text.substring(0, at) : text;
    }

    private static Set<Long> parseAllowedIds(String raw) {
        if (raw == null || raw.trim().isEmpty()) return Set.of();
        return java.util.Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::valueOf)
                .collect(Collectors.toSet());
    }

    private static String formatPreview(TelegramMessageType type, String text) {
        String safeText = text == null ? "" : text.trim();
        if (type == TelegramMessageType.TEXT) {
            return safeText;
        }

        String label = switch (type) {
            case IMAGE -> "[image]";
            case VIDEO -> "[video]";
            case STICKER -> "[sticker]";
            default -> "[media]";
        };

        if (safeText.isEmpty()) {
            return label;
        }

        return label + " " + safeText;
    }
}
