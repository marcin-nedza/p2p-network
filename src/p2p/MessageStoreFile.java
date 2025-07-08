package p2p;

import java.io.Serializable;

public class MessageStoreFile implements Serializable {
    private final String key;
    private final int size;


    public MessageStoreFile(String key, int size) {
        this.key = key;
        this.size = size;
    }

    public String getKey() {
        return key;
    }

    public int getSize() {
        return size;
    }

    @Override
    public String toString() {
        return "MessageStoreFile{" +
                "key='" + key + '\'' +
                ", size=" + size +
                '}';
    }
}
