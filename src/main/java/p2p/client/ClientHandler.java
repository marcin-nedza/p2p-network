package p2p.client;

import p2p.server.FileServer;
import p2p.common.PeerMessage;
import p2p.transport.core.TransportConnection;
import p2p.utils.DecodedMessage;
import p2p.utils.MessageUtils;

import java.io.*;
import java.util.concurrent.BlockingQueue;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class ClientHandler implements Runnable {
    private final FileServer fileServer;
    private final TransportConnection  connection;
    private final BlockingQueue<PeerMessage> queue;
    private final BiConsumer<String, TransportConnection> onPeerConnected;
    private final Consumer<String> onPeerDisconnected;

    public ClientHandler(FileServer fileServer, TransportConnection connection, BlockingQueue<PeerMessage> queue, BiConsumer<String, TransportConnection> onPeerConnected, Consumer<String> onPeerDisconnected) {
        this.fileServer = fileServer;
        this.connection = connection;
        this.queue = queue;
        this.onPeerConnected = onPeerConnected;
        this.onPeerDisconnected = onPeerDisconnected;
    }

    @Override
    public void run() {
        String peerId = null;
        try (
                InputStream input = connection.getInputStream();
                PrintWriter out = new PrintWriter(connection.getOutputStream(), true);
                BufferedReader reader = new BufferedReader(new InputStreamReader(input))
        ) {
            peerId = reader.readLine();
            if (peerId == null || peerId.isEmpty()) {
                System.err.println("Handshake failed: no peerId sent.");
                connection.close();
                return;
            }

            System.out.println("Handshake received: peerId= " + peerId);
            onPeerConnected.accept(peerId, connection);

            out.println(fileServer.getOpts().getPeerId());

            while (!Thread.currentThread().isInterrupted()) {
                DecodedMessage decoded = MessageUtils.decodeMessage(input);
                if (decoded == null) break;
                PeerMessage rpc = new PeerMessage(peerId, decoded.fullPayload());
                queue.put(rpc);
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("ClientHandler error: " + e.getMessage());
            Thread.currentThread().interrupt();
        } finally {
            cleanup(peerId);
        }
    }

    private void cleanup(String peerId) {
        if (peerId != null) {
            onPeerDisconnected.accept(peerId);
            System.out.println("Cleaned up peer: " + peerId);
        }
        try {
            connection.close();
        } catch (IOException e) {
            System.err.println("Error closing socket: " + e.getMessage());
        }
    }
}
