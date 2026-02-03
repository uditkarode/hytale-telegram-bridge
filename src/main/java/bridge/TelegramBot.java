package bridge;

import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.function.BiConsumer;

public class TelegramBot implements LongPollingSingleThreadUpdateConsumer {
    public static final TelegramBot DISABLED = new TelegramBot();

    private final TelegramClient telegramClient;
    private final String chatId;
    private final BiConsumer<String, String> hytaleBroadcaster;

    private TelegramBot() {
        this.telegramClient = null;
        this.chatId = null;
        this.hytaleBroadcaster = null;
    }

    public TelegramBot(String token, String chatId, BiConsumer<String, String> hytaleBroadcaster) {
        this.telegramClient = new OkHttpTelegramClient(token);
        this.chatId = chatId;
        this.hytaleBroadcaster = hytaleBroadcaster;
    }

    @Override
    public void consume(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()) return;
        
        Message message = update.getMessage();
        if (!String.valueOf(message.getChatId()).equals(chatId)) return;

        String sender = message.getFrom().getFirstName();
        String text = message.getText();
        hytaleBroadcaster.accept(sender, text);
    }

    public void sendMessage(String text) {
        if (telegramClient == null) return;

        SendMessage sendMessage = SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .build();
        try {
            telegramClient.execute(sendMessage);
        } catch (TelegramApiException e) {
            System.err.println("Failed to send message to Telegram: " + e.getMessage());
        }
    }
}
