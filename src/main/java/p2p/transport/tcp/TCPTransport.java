package p2p.transport.tcp;

import p2p.transport.core.Transport;
import p2p.transport.core.TransportConnection;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;

public class TCPTransport implements Transport {
    private ServerSocket serverSocket;

    @Override
    public void bind(int port) throws IOException {
        this.serverSocket = new ServerSocket(port);
        System.out.println("TCP Server bound on port " + port);
    }

    @Override
    public TransportConnection connect(String host, int port) throws IOException {
        Socket socket=new Socket(host,port);
        return new TcpConnection(socket);

    }

    @Override
    public TransportConnection accept() throws IOException {
        Socket socket = serverSocket.accept();
        return new TcpConnection(socket);
    }

    @Override
    public void send(byte[] data, SocketAddress address) throws IOException {
        throw new UnsupportedOperationException("Use TransportConnection for TCP send");
    }

    @Override
    public byte[] receive() throws IOException {
        throw new UnsupportedOperationException("Use TransportConnection for TCP receive");
    }

    @Override
    public boolean isConnected() {
        return !serverSocket.isClosed();
    }

    @Override
    public SocketAddress getRemoteAddress() {
        return serverSocket.getLocalSocketAddress();
    }

    @Override
    public void close() throws IOException {
        serverSocket.close();
    }
}
