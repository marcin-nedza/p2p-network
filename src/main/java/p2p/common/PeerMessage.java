package p2p.common;

import java.util.Arrays;

public record PeerMessage(String from, byte[] payload) {


    @Override
    public String toString() {
        return "RPC{" +
                "from='" + from + '\'' +
                ", payload=" + Arrays.toString(payload) +
                '}';
    }
}
