package cfb.ict;

import java.util.LinkedList;
import java.util.List;

public class IXI {

    final Tangle tangle;
    final Node node;

    IXI(final Tangle tangle, final Node node) {

        this.tangle = tangle;
        this.node = node;
    }

    List<Transaction> getTransactions(final List<Hash> hashes) {

        final List<Transaction> result = new LinkedList<>();

        for (final Hash hash : hashes) {

            final Tangle.Vertex vertex = tangle.get(hash);
            if (vertex != null && vertex.transaction != null) {

                result.add(vertex.transaction);
            }
        }

        return result;
    }

    List<Hash> putTransactions(final List<Transaction> transactions) {

        final List<Hash> result = new LinkedList<>();

        for (final Transaction transaction : transactions) {

            if (tangle.put(transaction, null)) {

                node.replicate(transaction);

                result.add(transaction.hash);
            }
        }

        return result;
    }

    List<Hash> removeTransactions(final List<Hash> hashes) {

        final List<Hash> result = new LinkedList<>();

        for (final Hash hash : hashes) {

            if (tangle.remove(hash)) {

                result.add(hash);
            }
        }

        return result;
    }
}
