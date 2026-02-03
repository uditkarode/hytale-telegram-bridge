package bridge;

public record TelegramMessage(String sender, TelegramMessageType type, String text, ReplyContext reply) {
}
