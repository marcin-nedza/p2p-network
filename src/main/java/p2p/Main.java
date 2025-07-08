package p2p;

import java.io.IOException;
import java.util.List;

public class Main {
    public static void main(String[] args) throws IOException, InterruptedException {
        Peer p1 = new Peer(3000);
        p1.start();

        Peer p2 = new Peer(4000);
        p2.start();
        p2.bootstrapNodes(List.of("localhost:3000"));

        Peer p3 = new Peer(4001);
        p3.start();
        p3.bootstrapNodes(List.of("localhost:3000","localhost:4000"));

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            Client.shutdown();
            FileServer.shutdown();
        }));


        Thread.sleep(3000);
        p2.sendMessageToAllConnected("hello from p2");
        Thread.sleep(Long.MAX_VALUE);
    }
}