package p2p.server;

import p2p.common.PeerMessage;
import p2p.client.ClientHandler;
import p2p.transport.core.Transport;
import p2p.transport.core.TransportConnection;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class TCPServer {
    private final Transport transport;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public TCPServer(Transport transport) {
        this.transport = transport;
    }

    public void acceptLoop(int port,
                           BlockingQueue<PeerMessage> queue,
                           FileServer fileServer,
                           BiConsumer<String, TransportConnection> onPeerConnected,
                           Consumer<String> onPeerDisconnected) throws IOException {
        transport.bind(port);
        while (transport.isConnected() && !Thread.currentThread().isInterrupted()) {
            try {
                TransportConnection connection = transport.accept();
                executor.submit(new ClientHandler(fileServer, connection, queue, onPeerConnected, onPeerDisconnected));
            } catch (IOException e) {
                System.err.println("Failed to accept connection: " + e.getMessage());
            }
        }
    }

    public  void shutdown() throws IOException {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        transport.close();
    }
}