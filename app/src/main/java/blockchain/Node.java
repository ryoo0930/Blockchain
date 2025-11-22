package blockchain;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.PublicKey;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import com.google.gson.Gson;

import blockchain.MessageWrapper.MessageType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Node {
    private int port;
    private PeerManager peerManager;
    private Wallet wallet;
    private Gson gson;

    private Blockchain blockchain;
    private Mempool mempool;
    private List<String> history;
    private List<Node> peers;
    private volatile boolean isMining = false;
    private Thread miningThread;
    private NodeStateListener listener;

    public Node(int port) { 
        this.port = port; 
        this.peerManager = new PeerManager(); 
        this.wallet = new Wallet();
        this.gson = new Gson();

        this.blockchain = new Blockchain();
        this.mempool = new Mempool();
        this.history = new CopyOnWriteArrayList<>();
        this.peers = new CopyOnWriteArrayList<>();
    }

    public void setListener(NodeStateListener listener) {
        this.listener = listener;
    }

    public void notifyStateChanged() {
        if (listener != null) {
            listener.onStateChanged();
        }
    }

    public boolean isMining() {
        return isMining;
    }

    public void addHistory(String event) {
        this.history.add(event);
        if (this.history.size() > 50) {
            this.history.remove(0);
        }
        notifyStateChanged(); // 히스토리가 변경될 때도 UI 업데이트
    }

    public List<String> getHistory() {
        return history;
    }

    public void addPeer(Node peer) {
        this.peers.add(peer);
    }

    public List<Node> getPeers() {
        return peers;
    }

    public void startServer() {
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(port)) {
                while(true) {
                    Socket clienSocket = serverSocket.accept();
                    new PeerHandler(clienSocket, this, peerManager, gson, blockchain, mempool).start();
                }
            } catch (IOException e) {
                System.err.println("[Node:" + port + "] Server Error: " + e.getMessage());
            }
        }).start();
    }

    public void connectToPeer(String host, int peerPort) {
        try {
            Socket socket = new Socket(host, peerPort);
            new PeerHandler(socket, this, peerManager, gson, blockchain, mempool).start();
        } catch (IOException e) {
            // Connection failures are common, so no error log needed
        }
    }

    public void createAndBroadcastTransaction(String recipientAddress, String data) {
        Transaction tx = new Transaction(
            this.wallet.getPublicKey(),
            recipientAddress,
            data
        );
        tx.signTransaction(this.wallet.getPrivateKey());

        if(mempool.addTransaction(tx)) {
            // addHistory("Broadcasted TX: " + tx.getTransactionID().substring(0, 10) + "...");
            String txJson = gson.toJson(tx);
            MessageWrapper msg = new MessageWrapper(MessageWrapper.MessageType.TX, txJson);
            peerManager.broadcast(gson.toJson(msg), null);
        }
    }

    public void startMining() {
        if (this.isMining) {
            addHistory("Miner is already running.");
            return;
        }
        this.isMining = true;
        this.miningThread = new Thread(() -> {
            addHistory("Miner Started.");
            while(this.isMining) {
                Block parentBlock = blockchain.getLastBlock();
                long newBlockNumber = parentBlock.getHeader().getNumber() + 1;
                
                List<Transaction> txs = mempool.getTransactionsForBlock(10);
                String txRoot = MerkleTree.getMerkleRoot(txs);
                long timestamp = System.currentTimeMillis();
                long nonce = 0;
                BlockHeader newHeader = new BlockHeader(parentBlock.getHash(), txRoot, timestamp, newBlockNumber, blockchain.getDifficulty(), nonce);

                while(this.isMining) {
                    if (blockchain.getLastBlock().getHeader().getNumber() >= newBlockNumber) {
                        break; 
                    }

                    newHeader.setNonce(nonce);
                    String hash = newHeader.calculateHash();

                    if(hash.startsWith(blockchain.getDifficultyTarget())) {
                        Block newBlock = new Block(newHeader, txs);

                        if(blockchain.addBlock(newBlock)) {
                            addHistory("MINED Block #" + newBlock.getHeader().getNumber());
                            String blockJson = gson.toJson(newBlock);
                            MessageWrapper msg = new MessageWrapper(MessageWrapper.MessageType.BLOCK, blockJson);
                            peerManager.broadcast(gson.toJson(msg), null);
                        } else {
                            addHistory("Discarded own Block #" + newBlock.getHeader().getNumber());
                        }
                        break; 
                    } 
                    nonce++;
                }
                
                try { 
                    Thread.sleep(100);
                } catch (InterruptedException e) { 
                    // This allows stopping the thread gracefully
                    Thread.currentThread().interrupt(); 
                }
            } 
            addHistory("Miner Stopped.");
        });
        this.miningThread.start();
    }
    
    public Blockchain getBlockchain() { return blockchain; }
    public void setBlockchain(Blockchain blockchain) { this.blockchain = blockchain; }
    public int getPort() { return port; }
    public Wallet getWallet() { return wallet; }
}
