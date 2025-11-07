package blockchain;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Node {
    private int port;
    private PeerManager peerManager;

    public Node(int port) { this.port = port; this.peerManager = new PeerManager(); }

    public void startServer() {
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(port)) {
                System.out.println("P2P Node listening on port: " + port);
                while(true) {
                    Socket clienSocket = serverSocket.accept();
                    new PeerHandler(clienSocket, peerManager).start();
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

            new PeerHandler(socket, peerManager).start();
        } catch (IOException e) {
            System.err.println("Connected Failed: " + e.getMessage());
        }
    }

    public void broadcastFromLocal(String message) {
        peerManager.broadcast("[Node " + port + "]: " + message, null);
    }
}
