package blockchain;

import java.util.List;

public class Block {
    private BlockHeader header;
    private String hash;
    private List<Transaction> transactions;

    public Block(BlockHeader header, List<Transaction> transactions) {
        this.header = header;
        this.transactions = transactions;
        this.hash = header.calculateHash();
    }

    public BlockHeader getHeader() { return header; }
    public String getHash() { return hash; }
    public List<Transaction> getTransactions() { return transactions; }
}
