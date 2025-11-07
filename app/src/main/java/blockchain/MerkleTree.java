package blockchain;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MerkleTree {
    public static String getMerkleRoot(List<Transaction> transactions) {
        List<String> transactionIDs = transactions.stream().map(Transaction::getTransactionID).collect(Collectors.toList());

        if(transactionIDs.isEmpty()) { return CryptoUtil.hashSHA256(""); }
        if(transactionIDs.size() == 1) { return transactionIDs.get(0); }

        List<String> currentlevelHashes = new ArrayList<>(transactionIDs);

        while(currentlevelHashes.size() > 1) { currentlevelHashes = calculateNextHashLevel(currentlevelHashes); }
        
        return currentlevelHashes.get(0);
    }

    private static List<String> calculateNextHashLevel(List<String> hashes) {
        List<String> nextLevel = new ArrayList<>();

        for (int i = 0; i < hashes.size(); i += 2) {
            String left = hashes.get(i);
            String right;

            if (i + 1 < hashes.size()) { right = hashes.get(i + 1); }
            else { right = left; }

            String combinedData = left + right;
            String combinedHash = CryptoUtil.hashSHA256(combinedData);

            nextLevel.add(combinedHash);
        }
        return nextLevel;
    }
}
