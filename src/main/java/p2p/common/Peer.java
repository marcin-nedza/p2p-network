package p2p.common;

import p2p.server.FileServer;
import p2p.server.FileServerOpts;

import java.util.List;

public class Peer {
    private final int listeningPort;
    private final String peerId;
    private final FileServer fileServer;

    public Peer(int listeningPort) {
        this.listeningPort = listeningPort;
        this.peerId = String.valueOf(listeningPort);

        FileServerOpts opts = new FileServerOpts.Builder()
                .listenPort(listeningPort)
                .peerId(peerId)
                .build();
        this.fileServer = new FileServer(opts);
    }

    public int getListeningPort() {
        return listeningPort;
    }

    public String getPeerId() {
        return peerId;
    }

    public void bootstrapNodes(List<String> nodes) {
        fileServer.bootstrapNetwork(nodes);
    }

    public void start() {
        fileServer.start();
    }

    public void shutdown(){
       fileServer.shutdown();
    }
    public void sendMessageToAllConnected(String msg) {
        fileServer.broadcast(msg);
    }

}
