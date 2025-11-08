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

import blockchain.MessageWrapper.MessageType;

public class PeerHandler extends Thread {
    private Socket socket;
    private PeerManager peerManager;
    private PrintWriter writer;
    private Gson gson;
    
    private Blockchain blockchain;
    private Mempool mempool;

    public PeerHandler(Socket socket, PeerManager peerManager, Gson gson, Blockchain blockchain, Mempool mempool) {
        this.socket = socket;
        this.peerManager = peerManager;
        this.gson = gson;
        this.blockchain = blockchain;
        this.mempool = mempool;
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
                    MessageWrapper wrapper = gson.fromJson(message, MessageWrapper.class);

                    if(wrapper == null || wrapper.type == null) {
                        System.err.println("Received invalid message format.");
                        continue;
                    }

                    switch (wrapper.type) {
                        case TX:
                            handleTransaction(wrapper.jsonData);
                            break;
                        case BLOCK:
                            handleBlock(wrapper.jsonData);
                            break;
                    }
                } catch (JsonSyntaxException e) {
                    System.err.println("Not a valid JSON Message: " + message.substring(0, Math.min(message.length(), 50)));
                } catch (Exception e) {
                    System.err.println("Error processing message " + e.getMessage());
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

    private void handleTransaction(String txJson) {
        try {
            Transaction tx = gson.fromJson(txJson, Transaction.class);
            if(tx == null || tx.getSenderPublicKey() == null || tx.getSignature() == null) {
                System.err.println("Invalid TX format received.");
                return;
            }

            PublicKey senderKey = getPublicKeyFromString(tx.getSenderPublicKey());
            if(tx.verifySignature(senderKey)) {
                if(mempool.addTransaction(tx)) {
                    System.out.println("PeerHandler: Verified TX added to mempool. Broadcasting...");
                    MessageWrapper msg = new MessageWrapper(MessageWrapper.MessageType.TX, txJson);
                    peerManager.broadcast(gson.toJson(msg), writer);                    } else {
                        System.out.println("PeerHandler: Received duplicate TX. Ignoring.");
                    }
                } else {
                    System.err.println("PeerHandler: Signautre Verification failed. Dropping TX");
            }
        } catch (Exception e) {
            System.err.println("Error handling transaction: " + e.getMessage());
        }
    }

    private void handleBlock(String blockJson) {
        try {
            Block block = gson.fromJson(blockJson, Block.class);
            System.out.println("PeerHandler: Received new block #" + block.getHeader().getNumber());

            
            if (blockchain.addBlock(block)) {
                System.out.println("PeerHandler: Block is valid and added to chain. Broadcasting...");
                
                mempool.removeTransactions(block.getTransactions());

                MessageWrapper msg = new MessageWrapper(MessageWrapper.MessageType.BLOCK, blockJson);
                peerManager.broadcast(gson.toJson(msg), writer);
            } else {
                System.out.println("PeerHandler: Received invalid block or old block. Ignoring.");
            }
        } catch (Exception e) {
            System.err.println("Error handling block: " + e.getMessage());
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
