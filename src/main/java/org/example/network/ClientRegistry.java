package org.example.network;

import org.example.protocol.CommandType;
import org.example.protocol.Message;
import org.example.protocol.MessagePacket;

import java.util.concurrent.ConcurrentHashMap;

public class ClientRegistry {
    public static final ConcurrentHashMap<Integer, ClientHandler> activeClients = new ConcurrentHashMap<>();

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

    public static void broadcastUserStatusChange(long userId, String status) {
        try {
            Message statusMsg = new Message(
                    CommandType.STATUS_OK,
                    (int) userId,
                    "STATUS_UPDATE;" + userId + ";" + status
            );
            MessagePacket packet = new MessagePacket((byte) 0, System.currentTimeMillis(), statusMsg);

            for (Integer onlineUserId : activeClients.keySet()) {
                if (onlineUserId != userId) {
                    ClientHandler handler = activeClients.get(onlineUserId);
                    if (handler != null) {
                        handler.sendPacket(packet);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[REGISTRY ERROR] Broadcast status failed: " + e.getMessage());
        }
    }
}
