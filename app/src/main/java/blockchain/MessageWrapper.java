package blockchain;

public class MessageWrapper {
    public enum MessageType {
        TX, // Transaction
        BLOCK // Block
    }

    public MessageType type;
    public String jsonData;

    public MessageWrapper() {}

    public MessageWrapper(MessageType type, String jsonData) {
        this.type = type;
        this.jsonData = jsonData;
    }
}
