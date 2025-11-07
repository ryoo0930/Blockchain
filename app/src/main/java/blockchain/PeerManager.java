package blockchain;

import java.io.PrintWriter;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class PeerManager {
    private Set<PrintWriter> peerWriters = new CopyOnWriteArraySet<>();
    
    public void addPeer(PrintWriter writer) { peerWriters.add(writer); }
    public void removePeer(PrintWriter writer) { peerWriters.remove(writer); }

    public void broadcast(String message, PrintWriter originator) {
        System.out.println("Broadcasting: " + message);
        for(PrintWriter writer : peerWriters) {
            if(writer != originator) {
                try {
                    writer.println(message);
                    writer.flush();
                } catch (Exception e) {
                    peerWriters.remove(writer);
                }
            }
        }
    }
}
