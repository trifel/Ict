package cfb.ict;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Tangle {

    final Map<Hash, Vertex> verticesByHash = new ConcurrentHashMap<>();
    final Map<Hash, Set<Vertex>> verticesByAddress = new ConcurrentHashMap<>();
    final Map<Hash, Set<Vertex>> verticesByTag = new ConcurrentHashMap<>();

    Tangle() {

        final Vertex vertex = new Vertex(Hash.NULL);
        vertex.transaction = Transaction.NULL;
        verticesByHash.put(Hash.NULL, vertex);
    }

    Vertex get(final Hash hash) {

        return verticesByHash.get(hash);
    }

    boolean put(final Transaction transaction, final Neighbor sender) {

        final Vertex vertex = verticesByHash.computeIfAbsent(transaction.hash, k -> new Vertex(transaction.hash));
        vertex.addSender(sender);
        if (vertex.transaction == null) {

            vertex.transaction = transaction;

            vertex.trunkVertex = verticesByHash.computeIfAbsent(transaction.trunkTransactionHash, k -> new Vertex(transaction.trunkTransactionHash));
            vertex.trunkVertex.referrers.add(vertex);
            if (transaction.branchTransactionHash.equals(transaction.trunkTransactionHash)) {

                vertex.branchVertex = vertex.trunkVertex;

            } else {

                vertex.branchVertex = verticesByHash.computeIfAbsent(transaction.branchTransactionHash, k -> new Vertex(transaction.branchTransactionHash));
                vertex.branchVertex.referrers.add(vertex);
            }

            if (!transaction.address.equals(Hash.NULL)) {

                (verticesByAddress.computeIfAbsent(transaction.address, k -> ConcurrentHashMap.newKeySet())).add(vertex);
            }
            if (!transaction.tag.equals(Hash.NULL)) {

                (verticesByTag.computeIfAbsent(transaction.tag, k -> ConcurrentHashMap.newKeySet())).add(vertex);
            }

            return true;

        } else {

            return false;
        }
    }

    boolean remove(final Hash hash) { // TODO: Rewrite to avoid race conditions

        final Vertex vertex = verticesByHash.get(hash);
        if (vertex == null || vertex.transaction == null) {

            return false;

        } else {

            if (!vertex.transaction.address.equals(Hash.NULL)) {

                final Set<Vertex> vertices = verticesByAddress.get(vertex.transaction.address);
                vertices.remove(vertex);
                if (vertices.isEmpty()) {

                    verticesByAddress.remove(vertex.transaction.address);
                }
            }
            if (!vertex.transaction.tag.equals(Hash.NULL)) {

                final Set<Vertex> vertices = verticesByTag.get(vertex.transaction.tag);
                vertices.remove(vertex);
                if (vertices.isEmpty()) {

                    verticesByTag.remove(vertex.transaction.tag);
                }
            }

            vertex.trunkVertex.referrers.remove(vertex);
            if (vertex.trunkVertex.referrers.isEmpty() && vertex.trunkVertex.transaction == null) {

                verticesByHash.remove(vertex.trunkVertex.hash);
            }
            if (vertex.trunkVertex != vertex.branchVertex) {

                vertex.branchVertex.referrers.remove(vertex);
                if (vertex.branchVertex.referrers.isEmpty() && vertex.branchVertex.transaction == null) {

                    verticesByHash.remove(vertex.branchVertex.hash);
                }
            }

            vertex.transaction = null;

            return true;
        }
    }

    Set<Neighbor> senders(final Transaction transaction) {

        final Vertex vertex = verticesByHash.get(transaction.hash);

        return vertex == null ? Collections.emptySet() : vertex.senders();
    }

    static class Vertex {

        Transaction transaction;
        Vertex trunkVertex, branchVertex;
        final Set<Vertex> referrers = ConcurrentHashMap.newKeySet();
        final Set<Neighbor> senders = ConcurrentHashMap.newKeySet();

        private final Hash hash;

        Vertex(final Hash hash) {

            this.hash = hash;
        }

        void addSender(final Neighbor sender) {

            if (sender != null) {

                senders.add(sender);
            }
        }

        Set<Neighbor> senders() {

            return Collections.unmodifiableSet(senders);
        }

        @Override
        public boolean equals(final Object obj) {

            return hash.equals(((Vertex) obj).hash); // The class check is omitted on purpose
        }

        @Override
        public int hashCode() {

            return hash.hashCode();
        }
    }
}
