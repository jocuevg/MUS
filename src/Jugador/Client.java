package Jugador;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    @SuppressWarnings("deprecation")
    public static void main(String[] args) {
        try (Socket socket = new Socket("localhost", 55555);
            DataInputStream in = new DataInputStream(socket.getInputStream());
            PrintStream out = new PrintStream(socket.getOutputStream(), true);
            Scanner scaner = new Scanner(System.in)) {

            System.out.println("Conectado al servidor de Mus.");

            // Hilo para leer mensajes del servidor
            new Thread(() -> {
                String response;
                try {
                    while ((response = in.readLine()) != null) {
                        System.out.println(response);
                        if (response.equalsIgnoreCase("FIN")) break;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();

            while (scaner.hasNextLine()) {
                out.println(scaner.nextLine());  // Envía mensajes al servidor
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
