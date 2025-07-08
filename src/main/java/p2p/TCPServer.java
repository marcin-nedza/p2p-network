package p2p;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class TCPServer {
    public static final ExecutorService executor = Executors.newCachedThreadPool();

    public static void start(BlockingQueue<RPC> queue,
                             int port,
                             BiConsumer<String, Socket> onPeerConnected,
                             Consumer<String> onPeerDisconnects) throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server is listening on port: " + port);


            //accept and handle clients
            while (true) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("client connected");
                    executor.submit(new ClientHandler(clientSocket, queue, onPeerConnected,onPeerDisconnects));
                } catch (IOException e) {
                    System.err.println("Failed accept connection: " + e.getMessage());
                    break;
                }

            }
        }
    }
}