package cfb.ict;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Tangle {

    final Map<Hash, Vertex> vertices = new ConcurrentHashMap<>();

    boolean store(final Transaction transaction, final Neighbor sender) {

        final Vertex vertex = vertices.get(transaction.hash);
        if (vertex == null) {

            vertices.put(transaction.hash, new Vertex(transaction, sender));

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

        final Transaction transaction;
        final Set<Neighbor> senders;

        Vertex(final Transaction transaction, final Neighbor sender) {

            this.transaction = transaction;

            senders = ConcurrentHashMap.newKeySet();
            if (sender != null) {

                senders.add(sender);
            }
        }

        void addSender(final Neighbor sender) {

            senders.add(sender);
        }

        Set<Neighbor> senders() {

            return Collections.unmodifiableSet(senders);
        }
    }
}
