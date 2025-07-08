package p2p;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

public class Client {
    public static void connectTo(String host, int port,String peerId,FileServer fileServer) {
        new Thread(() -> {
            try (Socket socket = new Socket(host, port);
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                 Scanner scanner = new Scanner(System.in)) {

                System.out.println("Connected to server");

                out.println(peerId);
                System.out.println("Handshake sent with peerId: " + peerId);
                fileServer.registerOutgoingPeer("peer-"+peerId,socket);

                new Thread(() -> {
                    String line;
                    try {
                        while ((line = in.readLine()) != null) {
                            System.out.println("Peer: "+peerId+" received: " + line);
                        }
                    } catch (IOException e) {
                        System.err.println("Error reading from  server: " + e.getMessage());
                    }
                }).start();
                while (true) {
                    String input = scanner.nextLine();
                    if ("exit".equalsIgnoreCase(input)) break;
                    out.println(input);
                }
            } catch (UnknownHostException e) {
                System.err.println("No host found");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }
}
