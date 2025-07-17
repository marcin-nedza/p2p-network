package p2p.client;

import p2p.common.PeerMessage;
import p2p.transport.core.TransportConnection;
import p2p.utils.DecodedMessage;
import p2p.utils.MessageUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.BlockingQueue;

public class SocketMessageReader implements Runnable {
    private final TransportConnection connection;
    private final String peerId;
    private final BlockingQueue<PeerMessage> queue;

    public SocketMessageReader(TransportConnection connection, String peerId, BlockingQueue<PeerMessage> queue) {
        this.connection = connection;
        this.peerId = peerId;
        this.queue = queue;
    }

    @Override
    public void run() {
        try (InputStream input = connection.getInputStream()) {
            while (!Thread.currentThread().isInterrupted()) {
                DecodedMessage decoded = MessageUtils.decodeMessage(input);
                if (decoded == null) break;

                PeerMessage rpc = new PeerMessage(peerId, decoded.fullPayload());
                queue.put(rpc);
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("SocketMessageReader error for peer " + peerId + ": " + e.getMessage());
            Thread.currentThread().interrupt(); // preserve interrupt status
        }
    }
}
