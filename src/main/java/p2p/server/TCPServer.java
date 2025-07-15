package p2p.server;

import p2p.common.PeerMessage;
import p2p.client.ClientHandler;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class TCPServer {
    public static final ExecutorService executor = Executors.newCachedThreadPool();

    public static ServerSocket startServer(int port) throws IOException {
        ServerSocket serverSocket = new ServerSocket(port);
        System.out.println("Server is listening on port: " + port);
        return serverSocket;
    }

    public static void acceptLoop(ServerSocket serverSocket,
                                  BlockingQueue<PeerMessage> queue,
                                  FileServer fileServer,
                                  BiConsumer<String, Socket> onPeerConnected,
                                  Consumer<String> onPeerDisconnected) {
        while (!serverSocket.isClosed()) {
            try {
                Socket clientSocket = serverSocket.accept();
                executor.submit(new ClientHandler(fileServer,clientSocket, queue, onPeerConnected, onPeerDisconnected));
            } catch (IOException e) {
                System.err.println("Failed to accept connection: " + e.getMessage());
            }
        }
    }

    public static void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}