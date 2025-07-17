package p2p.client;

import p2p.server.FileServer;
import p2p.transport.core.Transport;
import p2p.transport.core.TransportConnection;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Client {
    private static final ExecutorService executor = Executors.newCachedThreadPool();

    private static void handleConnection(String host, int port, String peerId, FileServer fileServer) {
        try {
            Transport transport=fileServer.getOpts().getTransport();
            TransportConnection connection= transport.connect(host,port);
            System.out.println("Connected to server");

            PrintWriter out = new PrintWriter(connection.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));

            out.println(peerId);
            System.out.println("Handshake sent with peerId: " + peerId);

            String remoteId = in.readLine().trim();
            System.out.println("Received peerId from remote: " + remoteId);

            fileServer.registerOutgoingPeer(remoteId, connection);

            Runnable messageReader = new SocketMessageReader(connection, remoteId, fileServer.getQueue());
            new Thread(messageReader).start();
        } catch (IOException e) {
            System.err.println("Connection error: " + host);
        }
    }

    public static void connectTo(String host, int port, String peerId, FileServer fileServer) {
        System.out.println("conect to");
        executor.submit(() -> handleConnection(host.trim(), port, peerId, fileServer));
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
