package blockchain;

import java.security.PrivateKey;
import java.security.PublicKey;

public class Transaction {
    private String transactionID;
    private String senderPublicKey;
    private String recipientAddress;

    private String data;
    private long timestamp;
    private byte[] signature;


    public Transaction() {}
    public Transaction(PublicKey sender, String recipient, String data) {
        this.senderPublicKey = CryptoUtil.keyToString(sender);
        this.recipientAddress = recipient;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
        this.transactionID = calculateHash();
    }

    private String calculateDataToSign() {
        return senderPublicKey + recipientAddress + data + timestamp;
    }
    private String calculateHash() {
        return CryptoUtil.hashSHA256(calculateDataToSign());
    }
    
    public void signTransaction(PrivateKey privateKey) {
        String dataToSign = calculateDataToSign();
        this.signature = CryptoUtil.sign(privateKey, dataToSign);
    }
    public Boolean verifySignature(PublicKey publicKey) {
        if (!CryptoUtil.keyToString(publicKey).equals(this.senderPublicKey)) {
            System.err.println("Verify Error: PublicKey is not equal.");
            return false;
        }
        String dataToString = calculateDataToSign();
        return CryptoUtil.verify(publicKey, dataToString, this.signature);
    }

    public String getTransactionID() { return transactionID; }
    public String getSenderPublicKey() { return senderPublicKey; }
    public String getRecipientAddress() { return recipientAddress; }
    public String getData() { return data; }
    public long getTimestamp() { return timestamp; }
    public byte[] getSignature() { return signature; }
}
