package p2p.utils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class MessageUtils {

    public static byte[] encodeMessage(byte type, byte[] msgBytes) {
        ByteBuffer buffer = ByteBuffer.allocate(1 + 4 + msgBytes.length);
        buffer.put(type);
        buffer.putInt(msgBytes.length);
        buffer.put(msgBytes);
        return buffer.array();
    }

    public static DecodedMessage decodeMessage(InputStream input) throws IOException {
        int typeByte = input.read();
        if (typeByte == -1) return null;

        byte[] lengthBytes = input.readNBytes(4);
        if (lengthBytes.length < 4) return null;

        int length = ByteBuffer.wrap(lengthBytes).getInt();
        byte[] msgBytes = input.readNBytes(length);
        if (msgBytes.length < length) return null;

        byte[] payload = new byte[1 + 4 + msgBytes.length];
        payload[0] = (byte) typeByte;
        System.arraycopy(lengthBytes, 0, payload, 1, 4);
        System.arraycopy(msgBytes, 0, payload, 5, msgBytes.length);

        return new DecodedMessage((byte) typeByte, msgBytes, payload);
    }

}
