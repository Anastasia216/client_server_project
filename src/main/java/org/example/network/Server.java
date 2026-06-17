package org.example.network;

import org.example.database.DatabaseInitializer;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private static final int PORT = 5001;
    private static final ExecutorService executor = Executors.newCachedThreadPool();
    public static void main(String[] args){
        DatabaseInitializer.initialize();
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server started on port" + PORT);

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("New client connected from: " + socket.getRemoteSocketAddress());
                executor.submit(new ClientHandler(socket));
            }
        } catch (IOException e) {
            System.err.println("Could not start server on port" + PORT + ": " + e.getMessage());
        } finally {
            executor.shutdown();
        }
    }
}
