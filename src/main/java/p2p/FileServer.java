package p2p;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public class FileServer {
    private final FileServerOpts opts;
    private final BlockingQueue<RPC> queue = new LinkedBlockingQueue<>();
    private final Map<String, Socket> peers = new ConcurrentHashMap<>();
    private static final ExecutorService executor = Executors.newCachedThreadPool();

    public FileServer(FileServerOpts opts) {
        this.opts = opts;
    }

    public void start() {
        executor.submit(() -> {
            try {
                TCPServer.start(queue, opts.getListenPort(), this::onPeer, this::onPeerDisconnected);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        executor.submit(() -> {
            while (true) {
                try {
                    RPC rpc = queue.take();
                    handleMessage(rpc);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

        });
    }

    public void bootstrapNetwork(List<String> nodes) {
        for (String hostPort : nodes) {
            String[] parts = hostPort.split(":");
            if (parts.length != 2) continue;
            String host = parts[0];
            int port = Integer.parseInt(parts[1]);
            Client.connectTo(host, port, opts.getPeerId(), this);
        }
    }

    public void onPeer(String peerId, Socket socket) {
        peers.put(peerId, socket);
        System.out.println("Peer added:" + peerId);
    }

    public void onPeerDisconnected(String peerId) {
        peers.remove(peerId);
        System.out.println("Peer added:" + peerId);
        System.out.println(peers);
    }

    public void handleMessage(RPC rpc) {
        String from = rpc.getFrom();
        byte[] payload = rpc.getPayload();
        if (payload.length == 0) return;

        MessageType type = MessageType.fromCode(payload[0]);
        System.out.println(type);

        String msg = new String(payload, 1, payload.length - 1, StandardCharsets.UTF_8);

        switch (type) {
            case NORMAL_MESSAGE:
                System.out.println("Normal mesage");
                System.out.println("[" + opts.getPeerId() + "] Received from : " + from + " message : " + msg);
                break;
            case DISCOVERY_REQUEST:
                System.out.println("Discovery req");
                break;
            case DISCOVERY_RESPONSE:
                System.out.println("Discovery res");
                break;
        }
    }

    public void sendTo(String peerId, MessageType type, String msg) {
        Socket socket = peers.get(peerId);
        if (socket != null) {
            try {
                OutputStream out = socket.getOutputStream();
                byte[] payload = msg.getBytes(StandardCharsets.UTF_8);
                byte[] full = new byte[1 + payload.length];
                full[0] = type.getCode();
                System.arraycopy(payload, 0, full, 1, payload.length);
                out.write(full);
                out.write('\n');
                out.flush();
            } catch (IOException e) {
                System.err.println("Failed to send to peer: " + peerId + " : " + e.getMessage());
            }
        }

    }

    public void broadcast(String msg) {
        System.out.println("Broadcasting to peers: " + peers.keySet());
        System.out.println(peers);
        for (String peerId :peers.keySet() ) {
           sendTo(peerId,MessageType.NORMAL_MESSAGE,msg);
        }
    }

    public void registerOutgoingPeer(String peerId, Socket socket) {
        peers.put(peerId, socket);
        System.out.println("Outgoing peer added: " + peerId);
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
