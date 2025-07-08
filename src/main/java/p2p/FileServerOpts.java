package p2p;

public class FileServerOpts {
    private final int listenPort;
    private final String peerId;

    private FileServerOpts(Builder builder) {
        this.listenPort = builder.listenPort;
        this.peerId = builder.peerId;
    }

    public int getListenPort() {
        return listenPort;
    }


    public String getPeerId() {
        return peerId;
    }


    public static class Builder {
        private int listenPort = 3000; // default value
        private String peerId = "unknown";

        public Builder listenPort(int port) {
            this.listenPort = port;
            return this;
        }

        public Builder peerId(String id) {
            this.peerId = id;
            return this;
        }

        public FileServerOpts build() {
            return new FileServerOpts(this);
        }
    }
}
