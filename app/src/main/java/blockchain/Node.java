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

public class Node {
    private int port;
    private PeerManager peerManager;
    private Wallet wallet;
    private Gson gson;

    private Blockchain blockchain;
    private Mempool mempool;

    public Node(int port) { 
        this.port = port; 
        this.peerManager = new PeerManager(); 
        this.wallet = new Wallet();
        this.gson = new Gson();

        this.blockchain = new Blockchain();
        this.mempool = new Mempool();

        System.out.println("Node Wallet Address (PublicKey): " + CryptoUtil.keyToString(wallet.getPublicKey()));
    }

    public void startServer() {
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(port)) {
                System.out.println("P2P Node listening on port: " + port);
                while(true) {
                    Socket clienSocket = serverSocket.accept();
                    new PeerHandler(clienSocket, peerManager, gson, blockchain, mempool).start();
                }
            } catch (IOException e) {
                System.err.println("Server Error: " + e.getMessage());
            }
        }).start();
    }

    public void connectToPeer(String host, int port) {
        try {
            Socket socket = new Socket(host, port);
            System.out.println("Connected to peer: " + host + ":" + port);

            new PeerHandler(socket, peerManager, gson, blockchain, mempool).start();
        } catch (IOException e) {
            System.err.println("Connected Failed: " + e.getMessage());
        }
    }

    public void createAndBroadcastTransaction(String recipientAddress, String data) {
        Transaction tx = new Transaction(
            this.wallet.getPublicKey(),
            recipientAddress,
            data
        );

        tx.signTransaction(this.wallet.getPrivateKey());

        System.out.println("Create & Signed Transaction: " + tx.toString());

        if(mempool.addTransaction(tx)) {
            System.out.println("Created & Signed TX: " + tx.toString().substring(0, 40));
            String txJson = gson.toJson(tx);
            MessageWrapper msg = new MessageWrapper(MessageWrapper.MessageType.TX, txJson);
            peerManager.broadcast(gson.toJson(msg), null);
        } else {
            System.out.println("Transaction already exists.");
        }
        
    }

    public void startMining() {
        System.out.println("MINER: Starting Miner Thread...");

        new Thread(() -> {
            while(true) {
                Block parentBlock = blockchain.getLastBlock();
                String parentHash = parentBlock.getHash();
                long newBlockNumber = parentBlock.getHeader().getNumber() + 1;
                long difficulty = blockchain.getDifficulty();
                String difficultyTarget = blockchain.getDifficultyTarget();

                List<Transaction> txs = mempool.getTransactionsForBlock(10);
                String txRoot = MerkleTree.getMerkleRoot(txs);
                long timestamp = System.currentTimeMillis();

                long nonce = 0;
                BlockHeader newHeader = new BlockHeader(parentHash, txRoot, timestamp, newBlockNumber, difficulty, nonce);

                System.out.println("MINER: Mining new block #" + newBlockNumber + " (Parent: " + parentHash.substring(0, 6) + ")...");

                while(true) {
                    newHeader.setNonce(nonce);
                    String hash = newHeader.calculateHash();

                    if(hash.startsWith(difficultyTarget)) {
                        System.out.println("MINER: BLOCK MINED! Nonce=" + nonce + ", Hash=" + hash);

                        Block newBlock = new Block(newHeader, txs);

                        if(blockchain.addBlock(newBlock)) {
                            String blockJson = gson.toJson(newBlock);
                            MessageWrapper msg = new MessageWrapper(MessageWrapper.MessageType.BLOCK, blockJson);
                            peerManager.broadcast(gson.toJson(msg), null);
                        } else {
                            System.err.println("\"MINER: Mined block was invalid? (Race condition, fork?)");
                        }
                        break;
                    } nonce++;
                } // end of PoW loop
            } // end of mining loop
        }).start();
    }
    
    public Blockchain getBlockchain() { return blockchain; }
}
