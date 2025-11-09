package blockchain;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Blockchain {
    private List<Block> chain;

    // 난이도 (PoW) - 4개의 0으로 시작하는 해시 찾기
    public static final int DIFFICULTY = 4;
    private String difficultyTarget;

    public Blockchain() {
        this.chain = new CopyOnWriteArrayList<>();
        this.difficultyTarget = "0".repeat(DIFFICULTY);
        createGenesisBlock();
    }

    private void createGenesisBlock() {
        long genesisTimestamp = 1678886400000L;

        BlockHeader genesisHeader = new BlockHeader("0", CryptoUtil.hashSHA256(""), genesisTimestamp, 0, DIFFICULTY, 0);
        Block genesisBlock = new Block(genesisHeader, new ArrayList<>());
        this.chain.add(genesisBlock);
    }

    public Block getLastBlock() {
        return chain.get(chain.size() - 1);
    }

    public boolean addBlock(Block newBlock) {
        Block previousBlock = getLastBlock();

        if (isValidBlock(newBlock, previousBlock)) {
            chain.add(newBlock);
            return true;
        }
        return false;
    }

    private boolean isValidBlock(Block newBlock, Block previousBlock) {
        if(!newBlock.getHeader().getParentHash().equals(previousBlock.getHash())) {
            System.err.println("Block validation failed: ParentHash mismatch");
            return false;
        }
        if(newBlock.getHeader().getNumber() != previousBlock.getHeader().getNumber() + 1) {
            System.err.println("Block validation failed: Block number incorrect");
            return false;
        }
        if (!isValidProofOfWork(newBlock.getHeader())) {
            System.err.println("Block validation failed: Invalid Proof of Work");
            return false;
        }
        String expectedMerkleRoot = MerkleTree.getMerkleRoot(newBlock.getTransactions());
        if (!newBlock.getHeader().getTransactionsRoot().equals(expectedMerkleRoot)) {
            System.err.println("Block validation failed: Merkle Root mismatch");
            return false;
        }
        return true;
    }

    public boolean isValidProofOfWork(BlockHeader header) {
        String hash = header.calculateHash();
        return hash.startsWith(difficultyTarget);
    }

    public String getDifficultyTarget() { return difficultyTarget; }
    public long getDifficulty() { return DIFFICULTY; }
    public int getChainSize() { return chain.size(); }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("--- Blockchain (Size: " + chain.size() + ") ---\n");
        for (Block block : chain) {
            sb.append(block.toString()).append("\n");
        }
        sb.append("----------------------------");
        return sb.toString();
    }
}
