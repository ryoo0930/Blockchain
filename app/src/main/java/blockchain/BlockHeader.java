package blockchain;

public class BlockHeader {
    private String parentHash;
    private String transactionsRoot;
    private long timestamp;
    private long number;
    private long difficulty;
    private long nonce;

    public BlockHeader(String parentHash, String transactionsRoot, long timestamp, long number, long difficulty, long nonce) {
        this.parentHash = parentHash;
        this.transactionsRoot = transactionsRoot;
        this.timestamp = timestamp;
        this.number = number;
        this.difficulty = difficulty;
        this.nonce = nonce;
    }

    public String calculateHash() {
        String dataToHash = parentHash + transactionsRoot + Long.toString(timestamp) + Long.toString(number) + Long.toString(difficulty) + Long.toString(nonce);
        return CryptoUtil.hashSHA256(dataToHash);
    }

    public String getParentHsh() { return parentHash; }
    public String getTransactionsRoot() { return transactionsRoot; }
    public long getTimestamp() { return timestamp; }
    public long getNumber() { return number; }
    public long getDifficulty() { return difficulty; }
    public long getNonce() { return nonce; }

    public void setNonce() { this.nonce = nonce; }
}
