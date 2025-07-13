package p2p;

import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Client {
    private static final ExecutorService executor = Executors.newCachedThreadPool();

    private static void startReader(Socket socket, String peerId) {
        executor.submit(() -> {
            try (InputStream in = socket.getInputStream()) {
                while (true) {
                    int typeByte = in.read();
                    if (typeByte == -1) break;//end of stream

                    byte[] lengthBytes = in.readNBytes(4);
                    if (lengthBytes.length < 4) break;//stream closed or incomplete?

                    int length = ByteBuffer.wrap(lengthBytes).getInt();
                    byte[] msgBytes = in.readNBytes(length);
                    if (msgBytes.length < length) break;

                    String msg = new String(msgBytes, StandardCharsets.UTF_8);
                    MessageType type = MessageType.fromCode((byte) typeByte);
                    System.out.println("Peer: " + peerId + " received type: " + type + " message: " + msg);
                }


            } catch (IOException e) {
                System.err.println("Error reading from server: " + e.getMessage());
            }
        });
    }

    private static void handleConnection(String host, int port, String peerId, FileServer fileServer) {
        try (Socket socket = new Socket(host, port);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             Scanner scanner = new Scanner(System.in)) {

            System.out.println("Connected to server");

            out.println(peerId);
            System.out.println("Handshake sent with peerId: " + peerId);
            fileServer.registerOutgoingPeer(peerId, socket);

            startReader(socket, peerId);

            while (true) {
                String input = scanner.nextLine();
                if ("exit".equalsIgnoreCase(input)) break;
                out.println(input);
            }

        } catch (IOException e) {
            System.err.println("Connection error: " + e.getMessage());
        }
    }

    public static void connectTo(String host, int port, String peerId, FileServer fileServer) {
        executor.submit(() -> handleConnection(host, port, peerId, fileServer));
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
