package p2p;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final BlockingQueue<RPC> queue;
    private final BiConsumer<String, Socket> onPeerConnected;
    private final Consumer<String> onPeerDisconnected;

    public ClientHandler(Socket socket, BlockingQueue<RPC> queue, BiConsumer<String, Socket> onPeerConnected, Consumer<String> onPeerDisconnected) {
        this.socket = socket;
        this.queue = queue;
        this.onPeerConnected = onPeerConnected;
        this.onPeerDisconnected = onPeerDisconnected;
    }

    @Override
    public void run() {
        String peerId = null;
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            peerId = in.readLine();
            if (peerId == null || peerId.isEmpty()) {
                System.err.println("Handshake failed: no peerId sent.");
                socket.close();
                return;
            }

            System.out.println("Handshake received: peerId= " + peerId);
            onPeerConnected.accept(peerId, socket);

            String line;
            while ((line = in.readLine()) != null) {
                if ("exit".equals(line)) {
                    System.out.println("Client disconnected: " + peerId);
                    break;
                }
                RPC rpc = new RPC(peerId, line.getBytes(), false);
                queue.put(rpc);
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("Client error: " + e.getMessage());
        } finally {
            if (peerId != null) {
                onPeerDisconnected.accept(peerId);
                System.out.println("Cleaned up peer: " + peerId);
            }
            try {
                socket.close();
            } catch (IOException e) {
                System.err.println("Error closing socket: " + e.getMessage());
            }

        }
    }
}
