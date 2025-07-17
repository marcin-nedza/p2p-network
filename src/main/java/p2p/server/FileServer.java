package p2p.server;

import p2p.client.Client;
import p2p.common.MessageType;
import p2p.common.PeerMessage;
import p2p.transport.core.TransportConnection;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public class FileServer {
    private final FileServerOpts opts;
    private final TCPServer server;
    private final BlockingQueue<PeerMessage> queue = new LinkedBlockingQueue<>();
    private final Map<String, TransportConnection> peers = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final ScheduledExecutorService pingScheduler = Executors.newSingleThreadScheduledExecutor();

    public FileServer(FileServerOpts opts) {
        this.opts = opts;
        this.server=new TCPServer(opts.getTransport());
    }

    public FileServerOpts getOpts() {
        return opts;
    }

    public Map<String, TransportConnection> getPeers() {
        return peers;
    }

    public BlockingQueue<PeerMessage> getQueue() {
        return queue;
    }

    public void start() {
        startServer();
        startMessageProcessor();
        startPingTask();
    }

    private void startServer() {
        executor.submit(() -> {
            try {
                System.out.println("Start server");
                server.acceptLoop(
                        opts.getListenPort(),
                        queue,
                        this,
                        this::onPeer,
                        this::onPeerDisconnected
                );
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private void startMessageProcessor() {
        executor.submit(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    PeerMessage msg = queue.take();
                    handleMessage(msg);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
    }

    private void startPingTask() {
        pingScheduler.scheduleAtFixedRate(() -> {
            log("Pinging peers: " + peers.keySet());
            peers.keySet().forEach(peerId -> sendTo(peerId, MessageType.PING, ""));
        }, 5, 10, TimeUnit.SECONDS);
    }

    public void bootstrapNetwork(List<String> nodes) {
        nodes.stream()
                .map(hostPort -> hostPort.split(":"))
                .filter(parts -> parts.length == 2)
                .forEach(parts -> {
                    String host = parts[0];
                    int port = Integer.parseInt(parts[1]);
                    Client.connectTo(host, port, opts.getPeerId(), this);
                });

        executor.submit(() -> {
            try {
                Thread.sleep(2000);
                broadcastDiscoveryRequest();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    private void broadcastDiscoveryRequest() {
        peers.keySet().forEach(peerId -> sendTo(peerId, MessageType.DISCOVERY_REQUEST, "Discovery request"));
    }

    public void onPeer(String peerId, TransportConnection connection) {
        peers.put(peerId, connection);
        log("Peer added: " + peerId);

        String newPeerInfo = connection.getRemoteAddress().toString();

        peers.keySet().stream()
                .filter(id -> !id.equals(peerId))
                .forEach(existingPeer -> sendTo(existingPeer, MessageType.NEW_PEER_ANNOUNCEMENT, newPeerInfo));

        sendTo(peerId, MessageType.DISCOVERY_REQUEST, "");
        printPeersState("onPeer");
    }

    public void onPeerDisconnected(String peerId) {
        peers.remove(peerId);
        log("Peer removed: " + peerId);
        printPeersState("onPeerDisconnected");
    }

    private void handleMessage(PeerMessage rpc) {
        byte[] payload = rpc.payload();
        if (payload.length == 0) return;

        MessageType type = MessageType.fromCode(payload[0]);
        String msg = new String(payload, 1, payload.length - 1, StandardCharsets.UTF_8);

        switch (type) {
            case NORMAL_MESSAGE -> handleNormalMessage(rpc.from(), msg);
            case DISCOVERY_REQUEST -> handleDiscoveryRequest(rpc.from());
            case DISCOVERY_RESPONSE -> handleDiscoveryResponse(msg);
            case NEW_PEER_ANNOUNCEMENT -> handleNewPeerAnnouncement(msg);
            case PING -> handlePing(rpc.from());
            case PONG -> handlePong(rpc.from());
            default -> log("Unknown message type from " + rpc.from());
        }
    }

    private void handleNormalMessage(String from, String msg) {
        log("[" + opts.getPeerId() + "] Received from: " + from + " message: " + msg);
    }

    private void handleDiscoveryRequest(String from) {
        log("[" + opts.getPeerId() + "] Discovery request received from " + from);
        String peerList = getPeerHostStringExcept(from);
        sendTo(from, MessageType.DISCOVERY_RESPONSE, peerList);
    }

    private void handleDiscoveryResponse(String msg) {
        log("[" + opts.getPeerId() + "] Discovery response received");
        parseHosts(msg.split(","));
    }

    private void handleNewPeerAnnouncement(String msg) {
        log("[" + opts.getPeerId() + "] New peer announcement: " + msg);
        String[] parts = msg.split(":");
        if (parts.length != 2) return;

        String ip = parts[0];
        String announcedPeerId = parts[1].trim();

        if (!peers.containsKey(announcedPeerId)) {
            log("Discovered new peer: " + announcedPeerId + " at " + ip);
            try {
                int port = Integer.parseInt(announcedPeerId);
                Client.connectTo(ip, port, opts.getPeerId(), this);
            } catch (NumberFormatException e) {
                log("Invalid peerId as port: " + e.getMessage());
            }
        }
    }

    private void handlePing(String from) {
        log("[" + opts.getPeerId() + "] Ping received from " + from);
        sendTo(from, MessageType.PONG, "");
    }

    private void handlePong(String from) {
        log("[" + opts.getPeerId() + "] Pong received from " + from);
        printPeersState("----PONG");
    }

    public void sendTo(String peerId, MessageType type, String msg) {
        TransportConnection connection = peers.get(peerId);
        if (!isConnectionValid(connection)) {
            logErr("Socket is not usable for peerId: " + peerId);
            return;
        }

        try {
            OutputStream out = connection.getOutputStream();

            byte typeByte = type.getCode();
            byte[] msgBytes = msg.getBytes(StandardCharsets.UTF_8);
            ByteBuffer buffer = ByteBuffer.allocate(1 + 4 + msgBytes.length);

            buffer.put(typeByte);
            buffer.putInt(msgBytes.length);
            buffer.put(msgBytes);

            out.write(buffer.array());
            out.flush();
        } catch (IOException e) {
            logErr("Failed to send to peer " + peerId + ": " + e.getMessage());
        }
    }

    private boolean isConnectionValid(TransportConnection connection) {
        return connection != null && connection.isConnected();
    }

    public void broadcast(String msg) {
        log("Broadcasting to peers: " + peers.keySet());
        peers.keySet().forEach(peerId -> sendTo(peerId, MessageType.NORMAL_MESSAGE, msg));
    }

    public void registerOutgoingPeer(String peerId, TransportConnection connection) {
        peers.put(peerId, connection);
        log("Outgoing peer added: " + peerId);
        printPeersState("registerOutgoingPeer");
    }

    public void shutdown() {
        log("Shutting down FileServer...");

        executor.shutdownNow();
        pingScheduler.shutdownNow();

        peers.values().forEach(connection -> {
            try {
                if (!connection.isClosed()) connection.close();
            } catch (IOException e) {
                logErr("Failed to close socket: " + e.getMessage());
            }
        });
    }

    private String getPeerHostStringExcept(String excludePeerId) {
        return peers.entrySet().stream()
                .filter(e -> !e.getKey().equals(excludePeerId))
                .map(e -> e.getValue().getInetAddress().getHostAddress() + ":" + e.getKey())
                .distinct()
                .toList()
                .toString()
                .replaceAll("[\\[\\] ]", "");
    }

    private boolean hasPeer(String hostPort) {
        return peers.entrySet().stream()
                .map(e -> e.getValue().getInetAddress().getHostAddress() + ":" + e.getKey())
                .anyMatch(key -> key.equals(hostPort));
    }

    private void parseHosts(String[] hosts) {
        for (String hostPort : hosts) {
            String[] parts = hostPort.split(":");
            if (parts.length != 2) continue;
            String host = parts[0];
            int port = Integer.parseInt(parts[1]);
            if (!hasPeer(hostPort)) {
                log("Discovered new peer " + hostPort);
                Client.connectTo(host, port, opts.getPeerId(), this);
            }
        }
    }

    private void printPeersState(String context) {
        log("=== Peer State @ [" + opts.getPeerId() + "] - " + context + " ===");
        if (peers.isEmpty()) {
            log("No connected peers.");
        } else {
            peers.forEach((peerId, connection) -> {
                String direction = (connection.getPort() == opts.getListenPort()) ? "INCOMING" : "OUTGOING";
                System.out.printf("- [%s] %s:%d (local: %d) [%s]%n",
                        peerId,
                        connection.getInetAddress().getHostAddress(),
                        connection.getPort(),
                        connection.getLocalPort(),
                        direction);
            });
        }
        log("========================================");
    }

    private void log(String msg) {
        System.out.println(msg);
    }

    private void logErr(String err) {
        System.err.println(err);
    }
}
