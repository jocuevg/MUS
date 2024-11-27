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

            String[] response={""};

            // Hilo para leer mensajes del servidor
            new Thread(() -> {
                
                try {
                    while ((response[0] = in.readLine()) != null) {
                        if (response[0].equalsIgnoreCase("pasar")){
                            out.println("");
                            out.flush();
                        }
                        else{
                            System.out.println(response[0]);
                            if (response[0].equalsIgnoreCase("FIN")) break;
                        }
                    }
                    System.out.println("Pulsa Enter para cerrar");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();

            while (scaner.hasNextLine()) {
                if(response[0].equalsIgnoreCase("FIN")) break;
                out.println(scaner.nextLine());  // Envía mensajes al servidor
                out.flush();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
