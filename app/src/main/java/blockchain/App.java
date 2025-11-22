package blockchain;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

public class App {
    private static final int INITIAL_NODE_COUNT = 20;
    private static final int INITIAL_CONNECTIONS = 3;
    private static final int STARTING_PORT = 8000;
    private static final Logger LOGGER = Logger.getLogger(App.class.getName());

    public static void main(String[] args) {
        List<Node> nodes = Collections.synchronizedList(new ArrayList<>());
        System.setProperty("java.awt.headless", "false");

        SwingUtilities.invokeLater(() -> {
            BlockchainDashboard dashboard = new BlockchainDashboard();
            
            // 전역 로거를 대시보드의 터미널 영역에 연결
            Logger rootLogger = Logger.getLogger("");
            rootLogger.addHandler(new TextAreaHandler(dashboard.getGlobalLogArea()));
            rootLogger.setLevel(Level.INFO);

            dashboard.setNodeList(nodes);

            // 1. Create all initial nodes first
            for (int i = 0; i < INITIAL_NODE_COUNT; i++) {
                int port = STARTING_PORT + i;
                Node newNode = new Node(port);
                nodes.add(newNode);
                new Thread(newNode::startServer).start();
            }

            // 2. Connect them after they are all created
            for (Node sourceNode : nodes) {
                Collections.shuffle(nodes);
                int connections = 0;
                for (Node targetNode : nodes) {
                    if (sourceNode.equals(targetNode) || sourceNode.getPeers().contains(targetNode)) continue;
                    if (connections >= INITIAL_CONNECTIONS) break;
                    
                    sourceNode.connectToPeer("localhost", targetNode.getPort());
                    sourceNode.addPeer(targetNode);
                    targetNode.addPeer(sourceNode);
                    connections++;
                }
            }
            
            // 3. Add them to the UI
            dashboard.getNetmapPanel().setNodes(nodes);
        });
    }

    public static void addNewNode(List<Node> nodes, BlockchainDashboard.NetmapPanel netmapPanel) {
        int port = STARTING_PORT + nodes.size();
        Node newNode = new Node(port);
        new Thread(newNode::startServer).start();

        new Thread(() -> {
            try { Thread.sleep(500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            
            if (!nodes.isEmpty()) {
                Collections.shuffle(nodes);
                int connections = 0;
                for (Node targetNode : nodes) {
                    if (connections >= INITIAL_CONNECTIONS) break;
                    newNode.connectToPeer("localhost", targetNode.getPort());
                    newNode.addPeer(targetNode);
                    targetNode.addPeer(newNode);
                    connections++;
                }

                if (!newNode.getPeers().isEmpty()) {
                    Node bestPeer = null;
                    int longestChainSize = 0;
                    for (Node peer : newNode.getPeers()) {
                        if (peer.getBlockchain().getChainSize() > longestChainSize) {
                            longestChainSize = peer.getBlockchain().getChainSize();
                            bestPeer = peer;
                        }
                    }

                    if (bestPeer != null && bestPeer.getBlockchain().getChainSize() > newNode.getBlockchain().getChainSize()) {
                        newNode.setBlockchain(new Blockchain(bestPeer.getBlockchain()));
                        LOGGER.info("[Node:" + newNode.getPort() + "] Synced longest chain from peer:" + bestPeer.getPort() + ". New Height: " + newNode.getBlockchain().getChainSize());
                    }
                }
            }
            
            nodes.add(newNode);
            SwingUtilities.invokeLater(() -> netmapPanel.addNode(newNode));
            LOGGER.info("[Network] New node added on port: " + port);
        }).start();
    }
}
