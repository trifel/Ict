package cfb.ict;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Tangle {

    final Map<Hash, Transaction> transactions = new ConcurrentHashMap<>();

    boolean store(final Transaction transaction) {

        return transactions.putIfAbsent(transaction.hash, transaction) == null;
    }
}
