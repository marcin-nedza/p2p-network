package p2p;

import java.util.Arrays;

public class RPC {
    private final String from;
    private final byte[] payload;
    private final boolean stream;


    public RPC(String from, byte[] payload, boolean stream) {
        this.from = from;
        this.payload = payload;
        this.stream = stream;
    }

    public String getFrom() {
        return from;
    }

    public byte[] getPayload() {
        return payload;
    }

    public boolean isStream() {
        return stream;
    }

    @Override
    public String toString() {
        return "RPC{" +
                "from='" + from + '\'' +
                ", payload=" + Arrays.toString(payload) +
                ", stream=" + stream +
                '}';
    }
}
