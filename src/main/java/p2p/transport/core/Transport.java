package p2p.transport.core;

import java.io.Closeable;
import java.io.IOException;
import java.net.SocketAddress;

public interface Transport extends Closeable {
    void bind(int port) throws IOException;
    TransportConnection connect(String host, int port)throws IOException;

    TransportConnection accept() throws IOException;

    void send(byte[] data, SocketAddress address) throws IOException;

    byte[] receive() throws IOException;

    boolean isConnected();

    SocketAddress getRemoteAddress();

    void close() throws IOException;
}
