package org.example.network;

import java.util.concurrent.ConcurrentHashMap;

public class ClientRegistry {
    private static final ConcurrentHashMap<Integer, ClientHandler> activeClients = new ConcurrentHashMap<>();

    public static void addClient(int userId, ClientHandler handler) {
        if (userId <= 0) return;

        activeClients.put(userId, handler);
        System.out.println("[REGISTRY] User with ID " + userId + " has been successfully added to the online registry.");
    }

    public static void removeClient(int userId) {
        if (userId > 0) {
            activeClients.remove(userId);
            System.out.println("[REGISTRY] User with ID " + userId + " has been removed from the online registry.");
        }
    }
    public static ClientHandler getHandler(int userId) {
        return activeClients.get(userId);
    }
    public static int getOnlineCount() {
        return activeClients.size();
    }
}