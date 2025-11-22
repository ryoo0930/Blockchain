package blockchain;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.security.KeyFactory;
import java.util.Base64;
import java.util.List;
import java.util.logging.Logger;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;

import blockchain.MessageWrapper.MessageType;

public class PeerHandler extends Thread {
    private Socket socket;
    private Node parentNode;
    private PeerManager peerManager;
    private PrintWriter writer;
    private Gson gson;
    
    private Blockchain blockchain;
    private Mempool mempool;

    public PeerHandler(Socket socket, Node parentNode, PeerManager peerManager, Gson gson, Blockchain blockchain, Mempool mempool) {
        this.socket = socket;
        this.parentNode = parentNode;
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

            String message;
            while((message = reader.readLine()) != null){
                try {
                    MessageWrapper wrapper = gson.fromJson(message, MessageWrapper.class);
                    if(wrapper == null || wrapper.type == null) continue;

                    switch (wrapper.type) {
                        case TX:
                            handleTransaction(wrapper.jsonData);
                            break;
                        case BLOCK:
                            handleBlock(wrapper.jsonData);
                            break;
                        case GET_CHAIN:
                            handleGetChain();
                            break;
                        case REPLY_CHAIN:
                            handleReplyChain(wrapper.jsonData);
                            break;
                    }
                } catch (JsonSyntaxException e) {
                    // Ignore invalid JSON
                } catch (Exception e) {
                    parentNode.addHistory("ERR: Msg processing failed.");
                }
            }
        } catch (IOException e) {
            // Peer disconnected
        } finally {
            if (writer != null) peerManager.removePeer(writer);
            try { socket.close(); } catch (IOException e) { /* ignore */ }
        }
    }

    private void handleTransaction(String txJson) {
        try {
            Transaction tx = gson.fromJson(txJson, Transaction.class);
            if(tx == null || tx.getSenderPublicKey() == null || tx.getSignature() == null) return;

            PublicKey senderKey = getPublicKeyFromString(tx.getSenderPublicKey());
            if(tx.verifySignature(senderKey) && mempool.addTransaction(tx)) {
                parentNode.addHistory("Received TX: " + tx.getTransactionID().substring(0, 10) + "...");
                MessageWrapper msg = new MessageWrapper(MessageType.TX, txJson);
                peerManager.broadcast(gson.toJson(msg), writer);
            }
        } catch (Exception e) {
            parentNode.addHistory("ERR: TX handling failed.");
        }
    }

    private void handleBlock(String blockJson) {
        try {
            Block block = gson.fromJson(blockJson, Block.class);
            if (blockchain.addBlock(block)) {
                parentNode.addHistory("Added Block #" + block.getHeader().getNumber());
                mempool.removeTransactions(block.getTransactions());
                MessageWrapper msg = new MessageWrapper(MessageType.BLOCK, blockJson);
                peerManager.broadcast(gson.toJson(msg), writer);
                parentNode.notifyStateChanged(); // 상태 변경 알림
            } else {
                // 내 체인에 붙일 수 없다면, 체인 동기화 요청
                Logger.getLogger(PeerHandler.class.getName()).info("[Node " + parentNode.getPort() + "] Fork detected. Requesting chain sync...");
                sendMessage(MessageType.GET_CHAIN, "");
            }
        } catch (Exception e) {
            parentNode.addHistory("ERR: Block handling failed.");
        }
    }

    private void handleGetChain() {
        parentNode.addHistory("Chain request received. Replying...");
        String chainJson = gson.toJson(blockchain.getChain());
        sendMessage(MessageType.REPLY_CHAIN, chainJson);
    }

    private void handleReplyChain(String chainJson) {
        try {
            Type blockListType = new TypeToken<List<Block>>() {}.getType();
            List<Block> newChain = gson.fromJson(chainJson, blockListType);
            if (blockchain.replaceChain(newChain)) {
                parentNode.addHistory("Chain synchronized successfully.");
                for (Block block : newChain) {
                    mempool.removeTransactions(block.getTransactions());
                }
                MessageWrapper msg = new MessageWrapper(MessageType.REPLY_CHAIN, chainJson);
                peerManager.broadcast(gson.toJson(msg), writer);
                parentNode.notifyStateChanged(); // 상태 변경 알림
            } else {
                parentNode.addHistory("Received chain is not longer/valid.");
            }
        } catch (Exception e) {
            parentNode.addHistory("ERR: Chain sync failed.");
        }
    }

    private void sendMessage(MessageType type, String jsonData) {
        String message = gson.toJson(new MessageWrapper(type, jsonData));
        writer.println(message);
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