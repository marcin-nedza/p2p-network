package p2p;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;

public class SocketMessageReader implements Runnable {
    private final Socket socket;
    private final String peerId;
    private final BlockingQueue<RPC> queue;

    public SocketMessageReader(Socket socket, String peerId, BlockingQueue<RPC> queue) {
        this.socket = socket;
        this.peerId = peerId;
        this.queue = queue;
    }

    @Override
    public void run() {
        try (InputStream input = socket.getInputStream()) {
            while (true) {
                int typeByte = input.read();
                if (typeByte == -1) break;

                byte[] lenBytes = input.readNBytes(4);
                int len = ByteBuffer.wrap(lenBytes).getInt();
                byte[] msgBytes = input.readNBytes(len);

                byte[] fullPayload = new byte[1 + 4 + msgBytes.length];
                fullPayload[0] = (byte) typeByte;
                System.arraycopy(lenBytes, 0, fullPayload, 1, 4);
                System.arraycopy(msgBytes, 0, fullPayload, 5, msgBytes.length);

                queue.put(new RPC(peerId, fullPayload, false)); // false = INCOMING
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("Socket read error: " + e.getMessage());
        }
    }
}
