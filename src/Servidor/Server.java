package Servidor;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;


public class Server {
    private static List<Player> players = new ArrayList<>();
    private static List<Card> mazo= new ArrayList<>();
    private static List<List<Card>> manosJugadores=new ArrayList<>();
    private static int[] puntos=new int[2];
    private static int ronda=0;
    private static int jugadorTurno=0;
    private static  int apuesta;           // Apuesta actual en la ronda
    private static int ganador;         // ID del jugador que inició la última apuesta
    
    public static void main(String[] args){
        try(ServerSocket ss= new ServerSocket(55555)){
            while (players.size() < 4) {  
                Socket socket = ss.accept();
                Player player = new Player(socket, players.size() + 1);
                players.add(player);
                new Thread(player).start();
            }

            while(Math.max(puntos[0], puntos[1])<25){
                reiniciarCartas();
                jugadorTurno = 0;
                for (int i=0;i<4;i++){
                    players.get(i).mandarMensaje("Ronda "+(ronda+1)+", jugador "+(ronda+1)+" va de mano");
                }
                repartir();

                ronda++;
            }

            
        }catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void reiniciarCartas() {
        mazo.clear();
        for (List<Card> mano : manosJugadores) {
            mano.clear();            
        }
        String[] palos = {"Oros", "Copas", "Espadas", "Bastos"};
        String[] valores = {"As", "Dos", "Tres", "Cuatro", "Cinco", "Seis", "Sota", "Caballo", "Rey"};
        
        for (String palo : palos) {
            for (String valor : valores) {
                mazo.add(new Card(palo, valor));
            }
        }
        Collections.shuffle(mazo);
    }

    private static void repartir() {
        int aux=ronda;
        for (int i = 0; i < 4; i++) {
            for (int j = manosJugadores.get(aux%4).size(); j < 4; j++) {
                Card c=mazo.remove(0);
                manosJugadores.get(i).add(c);
                players.get(aux%4).mandarMensaje(c.toString());
            }
            aux++;
        }
    }
}
