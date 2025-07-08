package p2p;

import java.io.Serializable;

public class Message implements Serializable {
    private final Object payload;

    public Message(Object payload) {
        this.payload = payload;
    }

    public Object getPayload() {
        return payload;
    }

}
