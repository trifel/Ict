package cfb.ict;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Tangle {

    final Map<Hash, Vertex> vertices = new ConcurrentHashMap<>();

    Tangle() {

        final Vertex vertex = new Vertex();
        vertex.transaction = Transaction.NULL;
        vertices.put(Hash.NULL, vertex);
    }

    boolean store(final Transaction transaction, final Neighbor sender) {

        final Vertex vertex = vertices.computeIfAbsent(transaction.hash, k -> new Vertex());
        if (vertex.transaction == null) {

            vertex.transaction = transaction;

            vertex.trunkVertex = vertices.computeIfAbsent(transaction.trunkTransactionHash, k -> new Vertex());
            vertex.trunkVertex.referrers.add(vertex);

            if (transaction.branchTransactionHash.equals(transaction.trunkTransactionHash)) {

                vertex.branchVertex = vertex.trunkVertex;

            } else {

                vertex.branchVertex = vertices.computeIfAbsent(transaction.branchTransactionHash, k -> new Vertex());
                vertex.branchVertex.referrers.add(vertex);
            }

            return true;

        } else {

            vertex.addSender(sender);

            return false;
        }
    }

    Set<Neighbor> senders(final Transaction transaction) {

        final Vertex vertex = vertices.get(transaction.hash);

        return vertex == null ? Collections.emptySet() : vertex.senders();
    }

    static class Vertex {

        Transaction transaction;
        Vertex trunkVertex, branchVertex;
        final Set<Vertex> referrers = ConcurrentHashMap.newKeySet();
        final Set<Neighbor> senders = ConcurrentHashMap.newKeySet();

        void addSender(final Neighbor sender) {

            if (sender != null) {

                senders.add(sender);
            }
        }

        Set<Neighbor> senders() {

            return Collections.unmodifiableSet(senders);
        }
    }
}
