package p2p.utils;

import java.nio.charset.StandardCharsets;

public record DecodedMessage(byte type, byte[] messageBytes, byte[] fullPayload) {

    public String getMessageAsString() {
        return new String(messageBytes, StandardCharsets.UTF_8);
    }
}
