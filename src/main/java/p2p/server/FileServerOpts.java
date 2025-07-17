package p2p.server;

import p2p.transport.core.Transport;

public class FileServerOpts {
    private final int listenPort;
    private final String peerId;
    private final Transport transport;

    private FileServerOpts(Builder builder) {
        this.listenPort = builder.listenPort;
        this.peerId = builder.peerId;
        this.transport=builder.transport;
    }

    public int getListenPort() {
        return listenPort;
    }


    public String getPeerId() {
        return peerId;
    }

    public Transport getTransport() {
        return transport;
    }


    public static class Builder {
        private int listenPort = 3000; // default value
        private String peerId = "unknown";
        private Transport transport;

        public Builder listenPort(int port) {
            this.listenPort = port;
            return this;
        }

        public Builder peerId(String id) {
            this.peerId = id;
            return this;
        }
        public Builder transport(Transport t){
            this.transport=t;
            return this;
        }

        public FileServerOpts build() {
            return new FileServerOpts(this);
        }
    }
}
