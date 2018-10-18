package cfb.ict;

import java.net.DatagramSocket;
import java.net.InetAddress;

public class Node {

    static final byte STOPPED = 0;
    static final byte RUNNING = 1;
    static final byte STOPPING = -1;

    volatile byte phase;
    final Properties properties;

    Node(final Properties properties) {

        this.properties = properties;
    }

    void run() {

        (new Thread(() -> {

            try {

                phase = RUNNING;

                final DatagramSocket socket = new DatagramSocket(properties.port, InetAddress.getByName(properties.host));

                while (phase == RUNNING) {

                    Thread.sleep(1); // TODO: Remove
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
