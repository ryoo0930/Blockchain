package blockchain;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.PublicKey;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import com.google.gson.Gson;

public class Node {
    private int port;
    private PeerManager peerManager;
    private Wallet wallet;
    private Gson gson;

    private Set<String> mempool = new CopyOnWriteArraySet<>();

    public Node(int port) { 
        this.port = port; 
        this.peerManager = new PeerManager(); 
        this.wallet = new Wallet();
        this.gson = new Gson();

        System.out.println("Node Wallet Address (PublicKey): " + CryptoUtil.keyToString(wallet.getPublicKey()));
    }

    public void startServer() {
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(port)) {
                System.out.println("P2P Node listening on port: " + port);
                while(true) {
                    Socket clienSocket = serverSocket.accept();
                    new PeerHandler(clienSocket, peerManager, gson, this).start();
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

            new PeerHandler(socket, peerManager, gson, this).start();
        } catch (IOException e) {
            System.err.println("Connected Failed: " + e.getMessage());
        }
    }

    public void processReceivedTransaction(Transaction tx, PrintWriter originator) {
        String txID = tx.getTransactionID();
        if(mempool.contains(txID)) {
            System.out.println("Already processed transaction: " + txID);
            return;
        }
        mempool.add(txID);
        System.out.println("Added to mempool: " + txID);
        String txJson = gson.toJson(tx);
        peerManager.broadcast(txJson, originator);
    }

    public void createAndBroadcastTransaction(String recipientAddress, String data) {
        Transaction tx = new Transaction(
            this.wallet.getPublicKey(),
            recipientAddress,
            data
        );

        tx.signTransaction(this.wallet.getPrivateKey());

        System.out.println("Create & Signed Transaction: " + tx.toString());

        if(mempool.add(tx.getTransactionID())) {
            String txJson = gson.toJson(tx);
            peerManager.broadcast(txJson, null);
        } else {
            System.out.println("Transaction already exists.");
        }
        
    }

    public PublicKey getPublicKey() { return this.wallet.getPublicKey(); }
}
