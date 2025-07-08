package p2p;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException, InterruptedException {
        Peer p1 = new Peer(3000,0);
        p1.start();

        Peer p2 = new Peer(4000,3000);
        p2.start();
        Thread.sleep(3000);
        p2.sendMessageToAllConnected("hello from p2");
        Thread.sleep(2000);
    }
}