package Servidor;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(55555)) {
            ExecutorService executor = Executors.newCachedThreadPool();
            List<Player> currentPlayers = new ArrayList<>();

            while (true) {
                Socket socket = serverSocket.accept();
                Player player = new Player(socket, currentPlayers.size() + 1);
                currentPlayers.add(player);
                new Thread(player).start();

                if (currentPlayers.size() == 4) {
                    executor.execute(new GameRoom(currentPlayers));
                    currentPlayers = new ArrayList<>();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
