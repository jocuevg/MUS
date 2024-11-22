package Servidor;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;

public class Player implements Runnable{
    private DataInputStream in;
    private PrintStream out;
    private int playerId;

    public Player(Socket socket, int playerId) {
        this.playerId = playerId;
        try {
            in = new DataInputStream(socket.getInputStream());
            out = new PrintStream(socket.getOutputStream(), true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        out.println("Bienvenido, Jugador " + playerId + "/4");
    }

    public void mandarMensaje(String mensaje) {
        out.println(mensaje);
    }
    @SuppressWarnings("deprecation")
    public String leerMensaje() {
        String mensaje="";
        try {
            if ((mensaje = in.readLine()) != null) {
                System.out.println("Jugador " + playerId + ": " + mensaje);
                return mensaje;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return mensaje;
    }
    public int getId(){
        return playerId;
    }

}
