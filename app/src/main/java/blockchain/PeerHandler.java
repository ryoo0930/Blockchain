package blockchain;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class PeerHandler extends Thread {
    private Socket socket;
    private PeerManager peerManager;
    private PrintWriter writer;

    public PeerHandler(Socket socket, PeerManager peerManager) {
        this.socket = socket;
        this.peerManager = peerManager;
    }

    public void run() {
        try {
            writer = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            peerManager.addPeer(writer);
            System.out.println("New peer added: " + socket.getRemoteSocketAddress());

            String message;
            while((message = reader.readLine()) != null){
                System.out.println("Message recevied from " + socket.getRemoteSocketAddress());
                peerManager.broadcast(message, writer);
            }
        } catch (IOException e) {
            System.out.println("Peer disconnected: " + socket.getRemoteSocketAddress());
        } finally {
            if (writer != null) {
                peerManager.removePeer(writer);
            }
            try {
                socket.close();
            } catch (IOException e) {
                System.out.println("Socket closed failed.");
            }
        }
    }

}
