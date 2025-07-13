package p2p;

import java.io.IOException;
import java.util.List;

public class Main {
    public static void main(String[] args) throws IOException, InterruptedException {
//        Peer p1 = new Peer(3000);
//        p1.start();
//
//        Peer p2 = new Peer(4000);
//        p2.start();
//        p2.bootstrapNodes(List.of("localhost:3000"));
//
//        Peer p3 = new Peer(4001);
//        p3.start();
//        p3.bootstrapNodes(List.of("localhost:3000","localhost:4000"));
//
//        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
//            Client.shutdown();
//            FileServer.shutdown();
//        }));
//
//
//        Thread.sleep(3000);
//        p2.sendMessageToAllConnected("hello from p2");
//        Thread.sleep(Long.MAX_VALUE);
        if (args.length < 1) {
            System.err.println("Usage: java p2p.Main <listeningPort> [bootstrapNodes...]");
            System.exit(1);
        }

        int port = Integer.parseInt(args[0]);
        Peer peer = new Peer(port);
        peer.start();

        // If any bootstrap nodes were passed as arguments, bootstrap them
        if (args.length > 1) {
            List<String> bootstrapNodes = List.of(args).subList(1, args.length);
            peer.bootstrapNodes(bootstrapNodes);
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            Client.shutdown();
            FileServer.shutdown();
        }));

        // Just as in your example, send a test message after 3 seconds
        Thread.sleep(3000);
        if (port == 4000) { // example: only peer 4000 sends the message
            peer.sendMessageToAllConnected("hello from " + port);
        }

        // Keep the program running
        Thread.sleep(Long.MAX_VALUE);
    }
}