package p2p;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class ClientHandler implements Runnable {
    private final FileServer fileServer;
    private final Socket socket;
    private final BlockingQueue<RPC> queue;
    private final BiConsumer<String, Socket> onPeerConnected;
    private final Consumer<String> onPeerDisconnected;

    public ClientHandler(FileServer fileServer, Socket socket, BlockingQueue<RPC> queue, BiConsumer<String, Socket> onPeerConnected, Consumer<String> onPeerDisconnected) {
        this.fileServer = fileServer;
        this.socket = socket;
        this.queue = queue;
        this.onPeerConnected = onPeerConnected;
        this.onPeerDisconnected = onPeerDisconnected;
    }

    @Override
    public void run() {
        String peerId = null;
        try (
                InputStream input = socket.getInputStream();
                BufferedReader handshakeReader = new BufferedReader(new InputStreamReader(input))
        ) {
            peerId = handshakeReader.readLine();
            if (peerId == null || peerId.isEmpty()) {
                System.err.println("Handshake failed: no peerId sent.");
                socket.close();
                return;
            }

            System.out.println("Handshake received: peerId= " + peerId);
            onPeerConnected.accept(peerId, socket);

            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            System.out.println("\t PEERS" + fileServer.getPeers());
            out.println(fileServer.getOpts().getPeerId());
            while (true) {
                int typeByte = input.read();
                if (typeByte == -1) break;

                byte[] lengthBytes = input.readNBytes(4);
                if (lengthBytes.length < 4) break;

                int length = ByteBuffer.wrap(lengthBytes).getInt();

                byte[] msgBytes = input.readNBytes(length);
                if (msgBytes.length < length) break;

                byte[] payload = new byte[1 + 4 + msgBytes.length];
                payload[0] = (byte) typeByte;
                System.arraycopy(lengthBytes, 0, payload, 1, 4);
                System.arraycopy(msgBytes, 0, payload, 5, msgBytes.length);
                RPC rpc = new RPC(peerId, payload, true);
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
