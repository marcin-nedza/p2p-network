package p2p.transport.core;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.SocketAddress;

public interface TransportConnection extends Closeable {
    InputStream getInputStream() throws IOException;
    OutputStream getOutputStream() throws IOException;
    SocketAddress getRemoteAddress();
    InetAddress getInetAddress();
    int getPort();
    int getLocalPort();
    boolean isClosed();
    boolean isConnected();
    void close() throws IOException;
}
