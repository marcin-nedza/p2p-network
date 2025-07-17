package p2p;

import org.junit.jupiter.api.Test;
import p2p.common.Peer;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class PeerIntegrationTest {

    @Test
    void peerCanConnect() throws InterruptedException {
        Peer p1=new Peer(4000);
        Peer p2=new Peer(4001);

        p1.start();
        p2.start();

        p2.bootstrapNodes(List.of("localhost:4000"));

        Thread.sleep(2000);

        System.out.println("------"+p1.getFileServer().getPeers());
        assertTrue(!p1.getFileServer().getPeers().isEmpty());
        assertTrue(!p2.getFileServer().getPeers().isEmpty());

        p1.shutdown();
        p2.shutdown();
    }
}
