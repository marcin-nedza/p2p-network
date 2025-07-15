package p2p.client;

import p2p.common.PeerMessage;
import p2p.utils.DecodedMessage;
import p2p.utils.MessageUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;

public class SocketMessageReader implements Runnable {
    private final Socket socket;
    private final String peerId;
    private final BlockingQueue<PeerMessage> queue;

    public SocketMessageReader(Socket socket, String peerId, BlockingQueue<PeerMessage> queue) {
        this.socket = socket;
        this.peerId = peerId;
        this.queue = queue;
    }

    @Override
    public void run() {
        try (InputStream input = socket.getInputStream()) {
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
