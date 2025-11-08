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

    @Override
    public String toString() {
        return "Block[" +
                "Hash=" + hash.substring(0, 10) + "..., " +
                "Number=" + header.getNumber() + ", " +
                "ParentHash=" + (header.getParentHash().isEmpty() || header.getParentHash().equals("0") ? "GENESIS(0)" : header.getParentHash().substring(0, 10)) + "..., " +
                "TxCount=" + transactions.size() + ", " +
                "MerkleRoot=" + (header.getTransactionsRoot().isEmpty() ? "empty" : header.getTransactionsRoot().substring(0, 10)) + "..., " +
                "Nonce=" + header.getNonce() +
                "]";
    }
}
