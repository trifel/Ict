package cfb.ict;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

public class Node {

    static final byte STOPPED = 0;
    static final byte RUNNING = 1;
    static final byte STOPPING = -1;

    static final int PACKET_SIZE = (Transaction.LENGTH / 9) * 2 + (Hash.LENGTH / 9) * 2;

    volatile byte phase;
    final Properties properties;
    final Tangle tangle;

    final List<Neighbor> neighbors = new ArrayList<>();

    Node(final Properties properties, final Tangle tangle) {

        this.properties = properties;
        this.tangle = tangle;
    }

    void run() {

        (new Thread(() -> {

            try {

                phase = RUNNING;

                neighbors.add(new Neighbor(properties.neighborAHost, properties.neighborAPort));
                neighbors.add(new Neighbor(properties.neighborBHost, properties.neighborBPort));
                neighbors.add(new Neighbor(properties.neighborCHost, properties.neighborCPort));

                final DatagramSocket socket = new DatagramSocket(properties.port, InetAddress.getByName(properties.host));
                final DatagramPacket packet = new DatagramPacket(new byte[PACKET_SIZE], PACKET_SIZE);
                final byte[] packetTrits = new byte[(PACKET_SIZE / 2) * 9];

                while (phase == RUNNING) {

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
                                    if (tangle.store(transaction)) {

                                        // TODO: Rebroadcast the transaction
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

        }, "Node")).start();
    }

    void stop() {

        if (phase == RUNNING) {

            phase = STOPPING;

            while (phase == STOPPING) {

                try {

                    Thread.sleep(1);

                } catch (final InterruptedException e) {

                    e.printStackTrace();
                }
            }
        }
    }
}
