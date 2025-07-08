package p2p;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.*;

public class FileServer {
    private final FileServerOpts opts;
    private final BlockingQueue<RPC> queue = new LinkedBlockingQueue<>();
    private final Map<String, Socket> peers = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public FileServer(FileServerOpts opts) {
        this.opts = opts;
    }

    public void start() {
        executor.submit(() -> {
            try {
                TCPServer.start(queue, opts.getListenPort(), this::onPeer);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        executor.submit(() -> {
            while(true){
               try {
                RPC rpc=queue.take();
                handleMessage(rpc);
               } catch (InterruptedException e) {
                  Thread.currentThread().interrupt();
                  break;
               }
            }

        });
    }

    public void bootstrapNetwork() {
        for (String hostPort : opts.getBootstrapNodes()) {
            String[] parts = hostPort.split(":");
            if (parts.length != 2) continue;
            String host = parts[0];
            int port = Integer.parseInt(parts[1]);
            Client.connectTo(host, port,opts.getPeerId());
        }

    }

    public void onPeer(String peerId,Socket socket) {
        peers.put(peerId, socket);
        System.out.println("Peer added:" + peerId);
    }

    public void handleMessage(RPC rpc) {
        String from = rpc.getFrom();
        String msg = new String(rpc.getPayload(), StandardCharsets.UTF_8);
        System.out.println("Received from : " + from + " message : " + msg);

    }

    public void broadcast(String msg) {
        for (Map.Entry<String, Socket> entry : peers.entrySet()) {
            try {
                Socket socket = entry.getValue();
                OutputStream out = socket.getOutputStream();
                out.write((msg + "\n").getBytes(StandardCharsets.UTF_8));
                out.flush();
            } catch (IOException e) {
                System.err.println("Failed to sent to peer: " + entry.getKey() + " :" + e.getMessage());
            }
        }

    }

}
