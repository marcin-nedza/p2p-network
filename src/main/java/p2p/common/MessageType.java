package p2p.common;

public enum MessageType {
    DISCOVERY_REQUEST((byte) 0),
    DISCOVERY_RESPONSE((byte) 1),
    NORMAL_MESSAGE((byte) 2),
    PING((byte)3),
    PONG((byte)4),
    NEW_PEER_ANNOUNCEMENT((byte)5);

    private final byte code;

    MessageType(byte code) {
        this.code = code;
    }

    public byte getCode() {
        return code;
    }

    public static MessageType  fromCode(byte  code){
       for (MessageType m :values() ) {
          if (m.code==code)return m;
       }
       throw new IllegalArgumentException("Unknown message type code: "+code);
    }
}
