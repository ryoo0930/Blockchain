package blockchain;

public class MessageWrapper {
    public enum MessageType {
        TX,         // Transaction
        BLOCK,      // Single Block
        GET_CHAIN,  // Request for the full blockchain
        REPLY_CHAIN // Response with the full blockchain
    }

    public MessageType type;
    public String jsonData;

    public MessageWrapper() {}

    public MessageWrapper(MessageType type, String jsonData) {
        this.type = type;
        this.jsonData = jsonData;
    }
}
