package shared.msg;

public class ChatMessage extends Message {
    public static final transient String HEADER = "YECA/0.1 Chat message";

    private String from;
    private String to;
    private String destinationChat;
    private String messageBody;

    public ChatMessage(String from, String messageBody) {
        this.from = from;
        this.messageBody = messageBody;
    }

    public ChatMessage(String from, String to, String destinationChat, String messageBody) {
        this.from = from;
        this.to = to;
        this.destinationChat = destinationChat;
        this.messageBody = messageBody;
    }

    @Override
    String getHeader() {
        return HEADER;
    }

    @Override
    String getBody() {
        return gson.toJson(this);
    }

    @Override
    void clear() {
        this.from = null;
        this.to = null;
        this.messageBody = null;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getDestinationChat() {
        return destinationChat;
    }

    public void setDestinationChat(String destinationChat) {
        this.destinationChat = destinationChat;
    }

    public String getMessageBody() {
        return messageBody;
    }

    public void setMessageBody(String messageBody) {
        this.messageBody = messageBody;
    }
}
