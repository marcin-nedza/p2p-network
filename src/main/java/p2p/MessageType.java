package p2p;

public enum MessageType {
    DISCOVERY_REQUEST((byte) 0),
    DISCOVERY_RESPONSE((byte) 1),
    NORMAL_MESSAGE((byte) 2);

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
