package blockchain;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.PublicKey;

import com.google.gson.Gson;

public class Node {
    private int port;
    private PeerManager peerManager;
    private Wallet wallet;
    private Gson gson;

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

    public void createAndBroadcastTransaction(String recipientAddress, String data) {
        Transaction tx = new Transaction(
            this.wallet.getPublicKey(),
            recipientAddress,
            data
        );

        tx.signTransaction(this.wallet.getPrivateKey());

        System.out.println("Create & Signed Transaction: " + tx.toString());

        String txJson = gson.toJson(tx);
        peerManager.broadcast(txJson, null);
    }

    public PublicKey getPublicKey() { return this.wallet.getPublicKey(); }
}
