package p2p;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import p2p.utils.DecodedMessage;
import p2p.utils.MessageUtils;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;

public class MessageUtilsTest {
    @Nested
    @DisplayName("encode and decode message")
    class EncodeDecodeTest {

        @Test
        @DisplayName("should correctly decode a valid message")
        void encodeDecodeMessage() throws IOException {
            byte type = 1;
            byte[] msg = "hello".getBytes();
            byte[] encoded = MessageUtils.encodeMessage(type, msg);

            DecodedMessage decodedMessage = MessageUtils.decodeMessage(new ByteArrayInputStream(encoded));

            assertNotNull(decodedMessage);
            assertEquals(type, decodedMessage.type());
            assertEquals("hello", decodedMessage.getMessageAsString());
        }
    }
}
