package blockchain;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

public class PeerHandler extends Thread {
    private Socket socket;
    private PeerManager peerManager;
    private PrintWriter writer;
    private Gson gson;
    private Node node;

    public PeerHandler(Socket socket, PeerManager peerManager, Gson gson, Node node) {
        this.socket = socket;
        this.peerManager = peerManager;
        this.gson = gson;
        this.node = node;
    }

    public void run() {
        try {
            writer = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            peerManager.addPeer(writer);
            System.out.println("New peer added: " + socket.getRemoteSocketAddress());

            String message;
            while((message = reader.readLine()) != null){
                try {
                    Transaction tx = gson.fromJson(message, Transaction.class);

                    if(tx == null || tx.getSenderPublicKey() == null || tx.getSignature() == null) {
                        System.err.println("Invalid Transaction format received from " + socket.getRemoteSocketAddress());
                        continue;
                    }

                    System.out.println("Received Transaction: " + tx.toString());

                    PublicKey senderKey = getPublicKeyFromString(tx.getSenderPublicKey());

                    if (tx.verifySignature(senderKey)) {
                        System.out.println("Signature Verified (Valid). Broadcasting....");
                        peerManager.broadcast(message, writer);
                    } else {
                        System.err.println("Signature Verification Failed (Invalid). Dropping Transaction.");
                    }
                } catch (JsonSyntaxException e) {
                    System.err.println("Not a valid Transaction (JSON) Message: " + message);
                } catch (Exception e) {
                    System.err.println("Error processing Transaction: " + e.getMessage());
                }

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

    private PublicKey getPublicKeyFromString(String keySting) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(keySting);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            KeyFactory kf = KeyFactory.getInstance("EC");
            return kf.generatePublic(spec);
        } catch (Exception e) {
            throw new RuntimeException("PublicKey Restore Failed.");
        }
    }

}
