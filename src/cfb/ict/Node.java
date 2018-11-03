package cfb.ict;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadLocalRandom;

public class Node {

    static final byte STOPPED = 0;
    static final byte RUNNING = 1;
    static final byte STOPPING = -1;

    static final int PACKET_SIZE = (Transaction.LENGTH / 9) * 2 + (Hash.LENGTH / 9) * 2;

    volatile byte phase;
    final Properties properties;
    final Tangle tangle;
    final List<Neighbor> neighbors = new ArrayList<>();
    DatagramSocket socket;
    final PriorityBlockingQueue<Envelope> envelopes = new PriorityBlockingQueue<>(1, (envelope1, envelope2) -> {

        if (envelope1.time == envelope2.time) {

            return 0; // TODO: Make sure that PriorityBlockingQueue doesn't break because of different transactions having the same time

        } else {

            return envelope1.time < envelope2.time ? -1 : 1;
        }

    });

    Node(final Properties properties, final Tangle tangle) {

        this.properties = properties;
        this.tangle = tangle;
    }

    void run() {

        phase = RUNNING;

        neighbors.add(new Neighbor(properties.neighborAHost, properties.neighborAPort));
        neighbors.add(new Neighbor(properties.neighborBHost, properties.neighborBPort));
        neighbors.add(new Neighbor(properties.neighborCHost, properties.neighborCPort));

        try {

            socket = new DatagramSocket(properties.port, InetAddress.getByName(properties.host));

        } catch (final Exception e) {

            throw new RuntimeException(e);
        }

        (new Thread(() -> {

            try {

                while (phase == RUNNING) {

                    final Envelope envelope = envelopes.take();
                    if (System.currentTimeMillis() > envelope.time) {

                        envelopes.put(envelope);

                        Thread.sleep(1);

                    } else {

                        final Set<Neighbor> senders = tangle.senders(envelope.transaction);
                        if (senders.size() < 2) {

                            for (final Neighbor neighbor : neighbors) {

                                if (!senders.contains(neighbor)) {

                                    neighbor.send(envelope.transaction);
                                }
                            }
                        }
                    }
                }

            } catch (final Exception e) {

                e.printStackTrace();
            }

        }, "Sender")).start();

        (new Thread(() -> {

            try {

                final DatagramPacket packet = new DatagramPacket(new byte[PACKET_SIZE], PACKET_SIZE);
                final byte[] packetTrits = new byte[(PACKET_SIZE / 2) * 9];

                long roundBeginningTime = 0;
                while (phase == RUNNING) {

                    if (System.currentTimeMillis() - roundBeginningTime >= properties.roundDuration) {

                        for (final Neighbor neighbor : neighbors) {

                            Utils.log(neighbor.toString());
                            neighbor.beginNewRound();
                        }

                        roundBeginningTime = System.currentTimeMillis();
                    }

                    socket.receive(packet);

                    for (final Neighbor neighbor : neighbors) {

                        if (packet.getSocketAddress().equals(neighbor.address)) {

                            if (packet.getLength() % ((Hash.LENGTH / 9) * 2) == 0) { // The packet length must be a multiple of 243

                                int offset;
                                for (offset = 0; offset < (Transaction.LENGTH + Hash.LENGTH) - (packet.getLength() / 2) * 9; offset++) {

                                    packetTrits[offset] = 0;
                                }
                                Utils.convertBytesToTritsBinary(packet.getData(), 0, packet.getLength(), packetTrits, offset);

                                try {

                                    final Transaction transaction = new Transaction(packetTrits);
                                    if (tangle.store(transaction, neighbor)) {

                                        envelopes.put(new Envelope(System.currentTimeMillis() + properties.minEchoDelay + ThreadLocalRandom.current().nextLong(properties.maxEchoDelay - properties.minEchoDelay),
                                                transaction));
                                    }

                                } catch (final RuntimeException e) {

                                    neighbor.numberOfInvalidTransactions++;
                                }
                            }

                            break;
                        }
                    }
                }

            } catch (final Exception e) {

                e.printStackTrace();
            }

            phase = STOPPED;

        }, "Receiver")).start();
    }

    void stop() {

        if (phase == RUNNING) {

            phase = STOPPING;

            socket.close();

            while (phase == STOPPING) {

                try {

                    Thread.sleep(1);

                } catch (final InterruptedException e) {

                    e.printStackTrace();
                }
            }
        }
    }

    static class Envelope {

        final long time;
        final Transaction transaction;

        Envelope(final long time, final Transaction transaction) {

            this.time = time;
            this.transaction = transaction;
        }
    }
}
