package p2p;

import java.util.ArrayList;
import java.util.List;

public class FileServerOpts {
    private final int listenPort;
    private final List<String> bootstrapNodes;
    private final String peerId;

    private FileServerOpts(Builder builder) {
        this.listenPort = builder.listenPort;
        this.bootstrapNodes = builder.bootstrapNodes;
        this.peerId=builder.peerId;
    }

    public int getListenPort() {
        return listenPort;
    }

    public List<String> getBootstrapNodes() {
        return bootstrapNodes;
    }

    public String getPeerId() {
        return peerId;
    }


    public static class Builder {
        private int listenPort = 3000; // default value
        private List<String> bootstrapNodes = new ArrayList<>();
        private String  peerId="unknown";

        public Builder listenPort(int port) {
            this.listenPort = port;
            return this;
        }

        public Builder bootstrapNodes(List<String> nodes) {
            this.bootstrapNodes = nodes;
            return this;
        }
        public Builder peerId(String id){
            this.peerId=id;
            return this;
        }

        public FileServerOpts build() {
            return new FileServerOpts(this);
        }
    }
}
