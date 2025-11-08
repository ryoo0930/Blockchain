package blockchain;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class Mempool {
    private ConcurrentLinkedQueue<Transaction> queue;

    public Mempool() {
        this.queue = new ConcurrentLinkedQueue<>();
    }

    public boolean addTransaction(Transaction tx) {
        for(Transaction pending : queue) {
            if(pending.getTransactionID().equals(tx.getTransactionID())){
                return false;
            }
        }
        queue.add(tx);
        return true;
    }
    
    public List<Transaction> getTransactionsForBlock(int maxTransactions) {
        List<Transaction> txs = new ArrayList<>();
        while(txs.size() < maxTransactions && !queue.isEmpty()) {
            Transaction tx = queue.poll();
            if(tx != null) {
                txs.add(tx);
            }
        }
        return txs;
    }

    public void removeTransactions(List<Transaction> minedTxs) {
        Set<String> minedTxIds = minedTxs.stream().map(Transaction::getTransactionID).collect(Collectors.toSet());
        queue.removeIf(tx -> minedTxIds.contains(tx.getTransactionID()));
    }

}
