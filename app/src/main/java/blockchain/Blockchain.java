package blockchain;

import java.util.ArrayList;
import java.io.Serializable;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

public class Blockchain implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final int DIFFICULTY = 4;
    private static final Logger LOGGER = Logger.getLogger(Blockchain.class.getName());

    private List<Block> chain;
    private String difficultyTarget;

    public Blockchain() {
        this.chain = new CopyOnWriteArrayList<>();
        this.difficultyTarget = "0".repeat(DIFFICULTY);
        createGenesisBlock();
    }

    // 새로운 복사 생성자
    public Blockchain(Blockchain original) {
        this.chain = new CopyOnWriteArrayList<>(original.chain);
        this.difficultyTarget = original.difficultyTarget;
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

    public synchronized boolean addBlock(Block newBlock) {
        Block previousBlock = getLastBlock();

        if (isValidBlock(newBlock, previousBlock)) {
            chain.add(newBlock);
            return true;
        }
        return false;
    }

    public synchronized boolean replaceChain(List<Block> newChain) {
        if (newChain.size() > this.chain.size() && isChainValid(newChain)) {
            this.chain = new CopyOnWriteArrayList<>(newChain);
            LOGGER.info("Chain Replaced. New height: " + this.getChainSize());
            return true;
        }
        return false;
    }

    private boolean isChainValid(List<Block> chainToValidate) {
        Block previousBlock = null;
        for (int i = 0; i < chainToValidate.size(); i++) {
            Block currentBlock = chainToValidate.get(i);
            if (i == 0) {
                previousBlock = currentBlock;
                continue;
            }
            
            // 1. 부모 해시 검증
            if (!currentBlock.getHeader().getParentHash().equals(previousBlock.getHash())) {
                LOGGER.warning("Chain validation failed: ParentHash mismatch at block " + currentBlock.getHeader().getNumber());
                return false;
            }
            // 2. 작업 증명 검증
            if (!isValidProofOfWork(currentBlock.getHeader())) {
                LOGGER.warning("Chain validation failed: Invalid PoW at block " + currentBlock.getHeader().getNumber());
                return false;
            }
            // 3. 머클 루트 검증
            String expectedMerkleRoot = MerkleTree.getMerkleRoot(currentBlock.getTransactions());
            if (!currentBlock.getHeader().getTransactionsRoot().equals(expectedMerkleRoot)) {
                LOGGER.warning("Chain validation failed: Merkle Root mismatch at block " + currentBlock.getHeader().getNumber());
                return false;
            }

            previousBlock = currentBlock;
        }
        return true;
    }

    private boolean isValidBlock(Block newBlock, Block previousBlock) {
        if(!newBlock.getHeader().getParentHash().equals(previousBlock.getHash())) {
            // System.err.println("Block validation failed: ParentHash mismatch");
            return false;
        }
        if(newBlock.getHeader().getNumber() != previousBlock.getHeader().getNumber() + 1) {
            // System.err.println("Block validation failed: Block number incorrect");
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
    public List<Block> getChain() { return chain; }

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
