package blockchain;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Random;
import java.awt.event.ActionListener;

public class BlockchainDashboard extends JFrame implements NodeStateListener {

    private JTextArea globalLogArea;
    private JTextArea nodeInfoArea;
    private NetmapPanel netmapPanel;
    
    private JPanel interactionPanel;
    private JTextArea interactionHistoryArea;
    private JTextField interactionInputField;
    private JButton interactionButton;
    private TitledBorder interactionBorder;
    private JPanel inputPanel;
    private JButton addNodeButton;

    private Node activeNode = null;
    private List<Node> networkNodes;

    public BlockchainDashboard() {
        setTitle("Blockchain Network Simulator (Hacknet Edition)");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1280, 720);
        setLocationRelativeTo(null);

        // --- 스타일 ---
        Color backgroundColor = Color.BLACK;
        Color accentColor = new Color(0, 220, 255);
        Color terminalTextColor = Color.WHITE;
        Font monoFont = new Font("Monospaced", Font.PLAIN, 14);
        Font titleFont = monoFont.deriveFont(Font.BOLD);
        
        Container contentPane = getContentPane();
        contentPane.setBackground(backgroundColor);
        contentPane.setLayout(new BorderLayout(10, 10));
        ((JPanel) contentPane).setBorder(new EmptyBorder(10, 10, 10, 10));

        // --- 오른쪽: INTERACTION ---
        interactionBorder = createTitledBorder("INTERACTION", titleFont, accentColor);
        interactionPanel = new JPanel(new BorderLayout());
        interactionPanel.setBackground(backgroundColor);
        interactionPanel.setBorder(interactionBorder);
        
        interactionHistoryArea = createStyledTextArea(monoFont, backgroundColor, accentColor);
        interactionPanel.add(createStyledScrollPane(interactionHistoryArea), BorderLayout.CENTER);
        
        inputPanel = new JPanel(new BorderLayout(5, 0));
        inputPanel.setBackground(backgroundColor);
        interactionInputField = new JTextField();
        styleTextField(interactionInputField, monoFont, backgroundColor, accentColor);
        interactionButton = new JButton("SEND");
        styleButton(interactionButton, monoFont, backgroundColor, accentColor);
        inputPanel.add(interactionInputField, BorderLayout.CENTER);
        inputPanel.add(interactionButton, BorderLayout.EAST);
        
        addNodeButton = new JButton("ADD NODE");
        styleButton(addNodeButton, monoFont, backgroundColor, accentColor);

        interactionPanel.add(inputPanel, BorderLayout.SOUTH);
        
        interactionPanel.setPreferredSize(new Dimension(400, 0));
        contentPane.add(interactionPanel, BorderLayout.EAST);

        // --- 메인 컨텐츠 패널 (왼쪽 + 중앙) ---
        JPanel mainContentPanel = new JPanel(new BorderLayout(10, 10));
        mainContentPanel.setBackground(backgroundColor);
        contentPane.add(mainContentPanel, BorderLayout.CENTER);

        // --- 하단: NETMAP ---
        netmapPanel = new NetmapPanel();
        netmapPanel.setBorder(createTitledBorder("NETMAP", titleFont, accentColor));
        netmapPanel.setPreferredSize(new Dimension(0, 230));
        mainContentPanel.add(netmapPanel, BorderLayout.SOUTH);

        // --- 상단 컨테이너 (터미널 + 디스플레이) ---
        JPanel topPanelsContainer = new JPanel(new BorderLayout(10, 10));
        topPanelsContainer.setBackground(backgroundColor);
        mainContentPanel.add(topPanelsContainer, BorderLayout.CENTER);

        // --- 상단-왼쪽: TERMINAL ---
        JPanel terminalPanel = createTitledPanel("TERMINAL", titleFont, accentColor);
        globalLogArea = createStyledTextArea(monoFont, backgroundColor, terminalTextColor);
        terminalPanel.add(createStyledScrollPane(globalLogArea), BorderLayout.CENTER);
        terminalPanel.setPreferredSize(new Dimension(400, 0));
        topPanelsContainer.add(terminalPanel, BorderLayout.WEST);

        // --- 상단-중앙: DISPLAY ---
        JPanel displayPanel = createTitledPanel("DISPLAY", titleFont, accentColor);
        nodeInfoArea = createStyledTextArea(monoFont, backgroundColor, accentColor);
        displayPanel.add(createStyledScrollPane(nodeInfoArea), BorderLayout.CENTER);
        topPanelsContainer.add(displayPanel, BorderLayout.CENTER);

        deselectNode();
        setupInteractionHandling();
        setupGlobalKeyListener();

        setVisible(true);
    }

    @Override
    public void onStateChanged() {
        SwingUtilities.invokeLater(() -> {
            if (activeNode != null) {
                updateDisplayPanel();
                updateInteractionPanel();
            }
        });
    }

    public void setNodeList(List<Node> nodes) { this.networkNodes = nodes; }
    public NetmapPanel getNetmapPanel() { return netmapPanel; }

    private void setActiveNode(Node node) {
        if (this.activeNode != null) {
            this.activeNode.setListener(null); // 이전 노드 리스너 해제
        }
        this.activeNode = node;
        if (this.activeNode != null) {
            this.activeNode.setListener(this); // 새 노드에 리스너 등록
            updateDisplayPanel();
            updateInteractionPanel();
            setInteractionPanelMode(true);
        }
        netmapPanel.setSelectedNode(node);
    }

    private void deselectNode() {
        if (this.activeNode != null) {
            this.activeNode.setListener(null); // 리스너 해제
        }
        this.activeNode = null;
        netmapPanel.setSelectedNode(null);
        nodeInfoArea.setText("Click a node from the NETMAP below.");
        setInteractionPanelMode(false);
    }
    
    public JTextArea getGlobalLogArea() { return globalLogArea; }

    //<editor-fold desc="메소드 본문 (UI 업데이트, 스타일링 등)">
    private void updateDisplayPanel() {
        if (activeNode == null) return;
        String info = "--- Node " + activeNode.getPort() + " Info ---\n\n" +
                      "Public Key:\n" +
                      CryptoUtil.keyToString(activeNode.getWallet().getPublicKey()) + "\n\n" +
                      "Blockchain State:\n" +
                      activeNode.getBlockchain().toString();
        nodeInfoArea.setText(info);
        nodeInfoArea.setCaretPosition(0);
    }

    private void updateInteractionPanel() {
        if (activeNode == null) return;
        String currentText = interactionHistoryArea.getText();
        String newText = String.join("\n", activeNode.getHistory());
        if (!currentText.equals(newText)) {
            interactionHistoryArea.setText(newText);
            interactionHistoryArea.setCaretPosition(interactionHistoryArea.getDocument().getLength());
        }
    }

    private void setInteractionPanelMode(boolean isNodeSelected) {
        interactionPanel.remove(inputPanel);
        interactionPanel.remove(addNodeButton);

        if (isNodeSelected) {
            interactionPanel.add(inputPanel, BorderLayout.SOUTH);
            interactionHistoryArea.setVisible(true);
            interactionBorder.setTitle("INTERACTION: Node " + activeNode.getPort());
        } else {
            interactionPanel.add(addNodeButton, BorderLayout.SOUTH);
            interactionHistoryArea.setVisible(true);
            String guideText = String.format(
                "Select a node from the NETMAP to interact with it.%n%n" +
                "Available commands:%n" +
                "  send <publicKey> <message>%n" +
                "  mine"
            );
            interactionHistoryArea.setText(guideText);
            interactionBorder.setTitle("INTERACTION");
        }
        interactionPanel.revalidate();
        interactionPanel.repaint();
    }

    private void setupInteractionHandling() {
        ActionListener commandAction = e -> processNodeCommand(interactionInputField);
        interactionButton.addActionListener(commandAction);
        interactionInputField.addActionListener(commandAction);
        addNodeButton.addActionListener(e -> App.addNewNode(this.networkNodes, this.netmapPanel));
    }

    private void setupGlobalKeyListener() {
        Toolkit.getDefaultToolkit().addAWTEventListener(event -> {
            if (event instanceof KeyEvent) {
                KeyEvent keyEvent = (KeyEvent) event;
                if (keyEvent.getID() == KeyEvent.KEY_PRESSED && keyEvent.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    deselectNode();
                }
            }
        }, AWTEvent.KEY_EVENT_MASK);
    }

    private void processNodeCommand(JTextField inputField) {
        if (activeNode == null) return;
        String command = inputField.getText().trim();
        if (command.isEmpty()) return;

        activeNode.addHistory("> " + command);
        if (command.startsWith("send ")) {
            try {
                String[] parts = command.split(" ", 3);
                String recipientKey = parts[1];
                String data = parts[2];
                activeNode.createAndBroadcastTransaction(recipientKey, data);
                activeNode.addHistory("Transaction broadcast initiated.");
            } catch (Exception ex) {
                activeNode.addHistory("Error: Usage: send <key> <data>");
            }
        } else if (command.equalsIgnoreCase("mine")) {
            activeNode.startMining();
        } else {
            activeNode.addHistory("Error: Unknown command. Use 'send or 'mine'.");
        }
        inputField.setText("");
    }

    private JTextArea createStyledTextArea(Font font, Color bg, Color fg) {
        JTextArea textArea = new JTextArea();
        textArea.setEditable(false);
        textArea.setBackground(bg);
        textArea.setForeground(fg);
        textArea.setFont(font);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setCaretColor(fg);
        textArea.setBorder(new EmptyBorder(5, 5, 5, 5));
        return textArea;
    }

    private JScrollPane createStyledScrollPane(Component view) {
        JScrollPane scrollPane = new JScrollPane(view);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(Color.BLACK);
        scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(0, 0));
        scrollPane.getHorizontalScrollBar().setPreferredSize(new Dimension(0, 0));
        return scrollPane;
    }

    private TitledBorder createTitledBorder(String title, Font font, Color color) {
        return BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(EtchedBorder.LOWERED, color, Color.DARK_GRAY),
            title, 0, 0, font, color
        );
    }

    private JPanel createTitledPanel(String title, Font font, Color color) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.BLACK);
        panel.setBorder(createTitledBorder(title, font, color));
        return panel;
    }

    private void styleButton(JButton button, Font font, Color bg, Color fg) {
        button.setFont(font);
        button.setBackground(bg);
        button.setForeground(fg);
        button.setBorder(BorderFactory.createLineBorder(fg, 1));
        button.setFocusPainted(false);
    }

    private void styleTextField(JTextField field, Font font, Color bg, Color fg) {
        field.setFont(font);
        field.setBackground(new Color(10, 10, 10));
        field.setForeground(fg);
        field.setCaretColor(fg);
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(fg, 1),
            new EmptyBorder(5, 5, 5, 5)
        ));
    }
    //</editor-fold>

    // --- Inner Classes for Netmap ---
    class NetmapPanel extends JPanel {
        private List<Node> nodes = new ArrayList<>();
        private Map<Node, NodeCircle> nodeComponents = new HashMap<>();
        private Node selectedNode = null;

        public NetmapPanel() {
            setBackground(Color.BLACK);
            setLayout(null);
        }

        public void addNode(Node newNode) {
            this.nodes.add(newNode);
            Random rand = new Random();
            int padding = 30;
            NodeCircle circle = new NodeCircle(newNode);
            
            SwingUtilities.invokeLater(() -> {
                int x = padding + rand.nextInt(Math.max(1, this.getWidth() - (2 * padding) - circle.getWidth()));
                int y = padding + rand.nextInt(Math.max(1, this.getHeight() - (2 * padding) - circle.getHeight()));
                circle.setLocation(x, y);
                
                this.add(circle);
                this.nodeComponents.put(newNode, circle);
                this.revalidate();
                this.repaint();
            });
        }

        public void setNodes(List<Node> nodes) {
            this.removeAll();
            this.nodes.clear();
            this.nodeComponents.clear();
            for (Node node : nodes) {
                addNode(node);
            }
        }

        public void setSelectedNode(Node node) {
            if (selectedNode != null && nodeComponents.containsKey(selectedNode)) {
                nodeComponents.get(selectedNode).setSelected(false);
            }
            this.selectedNode = node;
            if (selectedNode != null && nodeComponents.containsKey(selectedNode)) {
                nodeComponents.get(selectedNode).setSelected(true);
            }
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            for (Node sourceNode : nodes) {
                NodeCircle sourceCircle = nodeComponents.get(sourceNode);
                if (sourceCircle == null) continue;

                Point sourcePoint = getCenter(sourceCircle);
                for (Node peerNode : sourceNode.getPeers()) {
                    if (sourceNode.getPort() < peerNode.getPort()) {
                        NodeCircle peerCircle = nodeComponents.get(peerNode);
                        if (peerCircle != null) {
                            if (sourceNode == selectedNode || peerNode == selectedNode) {
                                g2d.setColor(Color.WHITE);
                                g2d.setStroke(new BasicStroke(1.5f));
                            } else {
                                g2d.setColor(new Color(80, 80, 80));
                                g2d.setStroke(new BasicStroke(1.0f));
                            }
                            Point peerPoint = getCenter(peerCircle);
                            g2d.drawLine(sourcePoint.x, sourcePoint.y, peerPoint.x, peerPoint.y);
                        }
                    }
                }
            }
        }

        private Point getCenter(Component c) {
            return new Point(c.getX() + c.getWidth() / 2, c.getY() + c.getHeight() / 2);
        }
    }

    class NodeCircle extends JComponent {
        private static final int DIAMETER = 24;
        private Node node;
        private boolean isSelected = false;
        private Timer animationTimer;

        public NodeCircle(Node node) {
            this.node = node;
            setSize(DIAMETER, DIAMETER);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    setActiveNode(node);
                }
            });

            animationTimer = new Timer(12, e -> repaint());
            animationTimer.start();
        }

        public void setSelected(boolean selected) {
            this.isSelected = selected;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            Color mainColor;
            Color accentColor;

            if (isSelected) {
                mainColor = new Color(100, 255, 150);
                accentColor = new Color(200, 255, 220, 180);
            } else if (node.isMining()) {
                mainColor = new Color(255, 223, 0);
                accentColor = new Color(255, 255, 150, 200);
            } else {
                mainColor = new Color(0, 120, 220);
                accentColor = new Color(50, 180, 255, 180);
            }

            g2d.setColor(accentColor);
            g2d.setStroke(new BasicStroke(1.5f));
            g2d.drawOval(0, 0, DIAMETER - 1, DIAMETER - 1);

            g2d.setColor(mainColor);
            g2d.fillOval(4, 4, DIAMETER - 8, DIAMETER - 8);

            double angle = (System.currentTimeMillis() % 3000) / 3000.0 * 360.0;

            g2d.setColor(accentColor);
            g2d.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2d.drawArc(2, 2, DIAMETER - 4, DIAMETER - 4, 45 + (int)angle, 90);
            g2d.drawArc(2, 2, DIAMETER - 4, DIAMETER - 4, 225 + (int)angle, 90);
        }
    }
}