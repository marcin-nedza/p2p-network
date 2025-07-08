package p2p;

import java.util.List;

public class Peer {
    private final int listeningPort;
    private final int connectPort;
    private final String peerId;
    private final FileServer fileServer;

    public Peer(int listeningPort, int connectPort) {
        this.listeningPort = listeningPort;
        this.connectPort = connectPort;
        this.peerId="peer-"+listeningPort;

        FileServerOpts opts = new FileServerOpts.Builder()
                .listenPort(listeningPort)
                .bootstrapNodes(connectPort > 0 ? List.of("localhost:" + connectPort) : List.of())
                .peerId(peerId)
                .build();
        this.fileServer = new FileServer(opts);
    }

    public void start() {
        fileServer.start();

        if (connectPort > 0) {
            try {
                Thread.sleep(1000);
                fileServer.bootstrapNetwork();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

    }
    public void sendMessageToAllConnected(String  msg){
        fileServer.broadcast(msg);
    }
}
