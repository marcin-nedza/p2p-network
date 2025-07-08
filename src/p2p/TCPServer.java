package p2p;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.function.BiConsumer;

public class TCPServer {
    public static void start(BlockingQueue<RPC> queue, int port, BiConsumer<String, Socket> onPeerConnected) throws IOException {
        ServerSocket serverSocket = new ServerSocket(port);
        System.out.println("Server is listening on port: " + port);

        new Thread(() -> {
            while (true) {
                try {
                    RPC rpc = queue.take();
                    System.out.println(new String(rpc.getPayload(), StandardCharsets.UTF_8));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

        }).start();

        //accept and handle clients
        while (true) {
            Socket clientSocket = serverSocket.accept();
            System.out.println("client connected");

//            onPeerConnected.accept(clientSocket);

            new Thread(new ClientHandler(clientSocket, queue, onPeerConnected)).start();
        }
    }

    static class ClientHandler implements Runnable {
        private final Socket socket;
        private final BlockingQueue<RPC> queue;
        private final BiConsumer<String, Socket> onPeerConected;

        public ClientHandler(Socket socket, BlockingQueue<RPC> queue, BiConsumer<String, Socket> onPeerConected) {
            this.socket = socket;
            this.queue = queue;
            this.onPeerConected = onPeerConected;
        }

        @Override
        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                String peerId = in.readLine();
                if (peerId == null || peerId.isEmpty()) {
                    System.err.println("Handshake failed: no peerId sent.");
                    socket.close();
                    return;
                }

                System.out.println("Handshake received: peerId= " + peerId);
                onPeerConected.accept(peerId, socket);

                String line;
                while ((line = in.readLine()) != null) {
                    if ("exit".equals(line)) {
                        System.out.println("Client disconected: " + peerId);
                        break;
                    }
                    RPC rpc = new RPC(peerId, line.getBytes(), false);
                    queue.put(rpc);
                }
            } catch (IOException | InterruptedException e) {
                System.err.println("Client error: " + e.getMessage());
            }
        }
    }
}