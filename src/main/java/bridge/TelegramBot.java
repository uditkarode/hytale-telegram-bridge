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
    private final TelegramClient telegramClient;
    private final String chatId;
    private final BiConsumer<String, String> hytaleBroadcaster;

    public TelegramBot(String token, String chatId, BiConsumer<String, String> hytaleBroadcaster) {
        this.telegramClient = new OkHttpTelegramClient(token);
        this.chatId = chatId;
        this.hytaleBroadcaster = hytaleBroadcaster;
    }

    @Override
    public void consume(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            Message message = update.getMessage();
            String receivedChatId = String.valueOf(message.getChatId());

            if (receivedChatId.equals(chatId)) {
                String sender = message.getFrom().getFirstName();
                String text = message.getText();
                hytaleBroadcaster.accept(sender, text);
            }
        }
    }

    public void sendMessage(String text) {
        SendMessage sendMessage = SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .build();
        try {
            telegramClient.execute(sendMessage);
        } catch (TelegramApiException e) {
            // Log error but don't rethrow to avoid crashing the plugin
            System.err.println("Failed to send message to Telegram: " + e.getMessage());
        }
    }
}
