package bridge;

public record ReplyContext(int originalMessageId, String originalSender, String originalText, String replySender, String replyText) {
}
