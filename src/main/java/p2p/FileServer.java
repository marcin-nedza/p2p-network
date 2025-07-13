package p2p;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
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
        ScheduledExecutorService pingScheduler = Executors.newSingleThreadScheduledExecutor();
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
        pingScheduler.scheduleAtFixedRate(() -> {
            for (String peerId : peers.keySet()) {
                sendTo(peerId, MessageType.PING, "");
            }
        }, 5, 10, TimeUnit.SECONDS);

    }

    public void bootstrapNetwork(List<String> nodes) {
        for (String hostPort : nodes) {
            String[] parts = hostPort.split(":");
            if (parts.length != 2) continue;
            String host = parts[0];
            int port = Integer.parseInt(parts[1]);
            Client.connectTo(host, port, opts.getPeerId(), this);
        }
        executor.submit(() -> {

            try {
                Thread.sleep(500);
                for (String peerId : peers.keySet()) {
                    sendTo(peerId, MessageType.DISCOVERY_REQUEST, "");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

        });
    }

    public void onPeer(String peerId, Socket socket) {
        peers.put(peerId, socket);
        System.out.println("Peer added:" + peerId);
        printPeersState("onPeer");
    }

    public void onPeerDisconnected(String peerId) {
        peers.remove(peerId);
        System.out.println("Peer added:" + peerId);
        printPeersState("onPeerDisconnects");
    }

    public void handleMessage(RPC rpc) {
        String from = rpc.getFrom();
        byte[] payload = rpc.getPayload();
        if (payload.length == 0) return;

        MessageType type = MessageType.fromCode(payload[0]);
        String msg = new String(payload, 1, payload.length - 1, StandardCharsets.UTF_8);

        switch (type) {
            case NORMAL_MESSAGE:
                System.out.println("Normal mesage");
                System.out.println("[" + opts.getPeerId() + "] Received from : " + from + " message : " + msg);
                break;
            case DISCOVERY_REQUEST:
                System.out.println("[" + opts.getPeerId() + "] Discovery request received from " + from);
                String peerList = getPeerHostStringExcept(from);
                System.out.println("PPPPPPP" + peerList);
                sendTo(from, MessageType.DISCOVERY_RESPONSE, peerList);

                break;
            case DISCOVERY_RESPONSE:
                System.out.println("Discovery res");
                break;
            case PING:
                System.out.println("[" + opts.getPeerId() + "] Ping received from " + from);
                sendTo(from, MessageType.PONG, "");
                break;

            case PONG:
                System.out.println("[" + opts.getPeerId() + "] Pong received from " + from);
                break;
        }
    }

    public void sendTo(String peerId, MessageType type, String msg) {
        Socket socket = peers.get(peerId);
        if (socket == null || socket.isClosed() || !socket.isConnected()) {
            System.err.println("Socket is not usable for peerId: " + peerId);
            return;
        }
        try {
            OutputStream out = socket.getOutputStream();

            byte typeByte = type.getCode();
            byte[] msgBytes = msg.getBytes(StandardCharsets.UTF_8);
            int length = msgBytes.length;

            ByteBuffer buffer = ByteBuffer.allocate(1 + 4 + length);
            buffer.put(typeByte);
            buffer.putInt(length);
            buffer.put(msgBytes);

            out.write(buffer.array());
            out.flush();

        } catch (IOException e) {
            System.err.println("Failed to send to peer: " + peerId + " : " + e.getMessage());
        }

    }

    public void broadcast(String msg) {
        System.out.println("Broadcasting to peers: " + peers.keySet());
        System.out.println(peers);
        for (String peerId : peers.keySet()) {
            sendTo(peerId, MessageType.NORMAL_MESSAGE, msg);
        }
    }

    public void registerOutgoingPeer(String peerId, Socket socket) {
        peers.put(peerId, socket);
        System.out.println("Outgoing peer added: " + peerId);
        printPeersState("registerOutgoingPeer");
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

    private String getPeerHostStringExcept(String excludePeerId) {
        return peers.entrySet().stream()
                .filter(p -> !p.getKey().equals(excludePeerId))
                .map(entry -> {
                    Socket s = entry.getValue();
                    return s.getInetAddress().getHostAddress() + ":" + entry.getKey();
                })
                .distinct()
                .toList()
                .toString()
                .replaceAll("[\\[\\] ]", "");
    }

    private List<String> getPeerHost() {
        return peers
                .entrySet()
                .stream()
                .map(entry -> {
                    Socket s = entry.getValue();
                    return s.getInetAddress().getHostAddress() + ":" + entry.getKey();
                })
                .toList();
    }

    private boolean hasPeer(String hostPort) {
        return peers.values().stream()
                .anyMatch(socket -> (socket.getInetAddress().getHostAddress() + ":" + socket.getPort()).equals(hostPort));
    }

    private void printPeersState(String context) {
        System.out.println("=== Peer State @ [" + opts.getPeerId() + "] - " + context + " ===");
        if (peers.isEmpty()) {
            System.out.println("No connected peers.");
        } else {
            for (Map.Entry<String, Socket> entry : peers.entrySet()) {
                String peerId = entry.getKey();
                Socket s = entry.getValue();
                String direction = (s.getPort() == opts.getListenPort()) ? "INCOMING" : "OUTGOING";
                System.out.printf("- [%s] %s:%d (local: %d) [%s]%n",
                        peerId,
                        s.getInetAddress().getHostAddress(),
                        s.getPort(),
                        s.getLocalPort(),
                        direction
                );
            }
        }
        System.out.println("========================================");
    }
}
