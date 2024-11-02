package Servidor;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.Comparator;


public class Server {
    private static List<Player> players = new ArrayList<>();
    private static List<Card> mazo= new ArrayList<>();
    private static List<List<Card>> manosJugadores=new ArrayList<>();
    private static int[] puntos=new int[2];
    private static int ronda=0;
    private static int jugadorMano=0;
    private static int apuesta=0;
    private static int envido=0;            
    private static int ganador;         
    private static boolean ordago=false;            
    private static boolean grandeAlPaso=true;
    private static boolean pequenaAlPaso=true;
    private static boolean paresAlPaso=true;
    private static boolean puntoAlPaso=true;    
    private static boolean juegoAlPaso=true;
    
    
    public static void main(String[] args){
        try(ServerSocket ss= new ServerSocket(55555)){
            while (players.size() < 4) {  
                Socket socket = ss.accept();
                Player player = new Player(socket, players.size() + 1);
                players.add(player);
                new Thread(player).start();
            }

            puntos[0]=0;
            puntos[1]=0;
            for (int i=0;i<4;i++) {
                manosJugadores.add(new ArrayList<Card>());            
            }

            while(Math.max(puntos[0], puntos[1])<25){
                reiniciarCartas();
                jugadorMano = ronda;
                mandarATodos("Ronda "+(ronda+1)+", jugador "+(ronda+1)+" va de mano");
                repartir();
                mus();
                ordenarManos();
                grande();
                pequena(); 
                pares();
                /*
                juego();*/
                sumarPaso();
                /*mostrarPuntos();*/

                puntos[0]=25;
                ronda++;
            }

            
        }catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void reiniciarCartas() {
        mazo.clear();
        for (int i=0;i<4;i++) {
            manosJugadores.get(i).clear();            
        }
        String[] palos = {"Oros", "Copas", "Espadas", "Bastos"};
        String[] valores = {"As", "Tres", "Cuatro", "Cinco", "Seis", "Siete", "Sota", "Caballo", "Rey"};
        
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
                manosJugadores.get(aux%4).add(c);
            }
            mandarMano(aux%4);
            aux++;
        }
    }

    private static void mandarMano(int idPlayer){
        String mensaje="Mano: \r\n";
        for (Card c : manosJugadores.get(idPlayer)) {
            mensaje+=c.toString()+"\r\n";
        }
        players.get(idPlayer).mandarMensaje(mensaje);
    }

    private static void mus(){
        String respuesta;
        for(int i=0;i<4;i++){
            players.get((jugadorMano+i)%4).mandarMensaje("Quieres mus? (mus/no)");
            respuesta=players.get((jugadorMano+i)%4).leerMensaje();
            if(respuesta!="mus") {
                mandarATodos("Jugador "+((jugadorMano+i)%4+1)+" ha cortado el mus");
                return;
            }
        }
        mandarATodos("Hay mus, indica en orden el indice de las cartas que tiras separadas por comas (ej: \"0,1,3\" -> te quedas solo con la segunda carta)");
        for(int i=0;i<4;i++){
            respuesta=players.get((jugadorMano+i)%4).leerMensaje();
            String[] tiradas=respuesta.split(",");
            for(int j=0;j<tiradas.length;j++){
                try {
                    Card c=manosJugadores.get((jugadorMano+i)%4).remove(Integer.parseInt(tiradas[j])); 
                    mazo.add(c);
                    c=mazo.remove(0);
                    manosJugadores.get((jugadorMano+i)%4).add(Integer.parseInt(tiradas[j]), c);; 
                } catch (Exception e) {}
            }
            mandarMano((jugadorMano+i)%4);
        }
        mus();        
    }

    private static void ordenarManos(){
        for(int i=0;i<4;i++){
            manosJugadores.get(i).sort(Comparator.comparingInt(Card::getValorNumerico).reversed());
        }
    }

    private static void grande(){
        grandeAlPaso=true;
        apuesta=1;
        envido=0;
        ordago=false;
        int i=0;
        String respuesta;
        String mensaje0="Que quieres hacer:\r\n";
        mensaje0+="Pasar: \"paso\" \r\n";
        mensaje0+="Envidar: \"envido\" \r\n";
        mensaje0+="Envidar x: \"envido X\" \r\n";
        mensaje0+="Órgdago: \"ordago\" \r\n";

        String mensaje1="Que quieres hacer:\r\n";
        mensaje1+="No querer: \"no\" \r\n";
        mensaje1+="Querer: \"quiero\" \r\n";
        mensaje1+="Envidar x más: \"envido X\" \r\n";
        mensaje1+="Órgdago: \"ordago\" \r\n";

        mandarATodos("Jugando grande\r\n");

        while(grandeAlPaso && i<4){            
            players.get((jugadorMano+i)%4).mandarMensaje(mensaje0);
            respuesta=players.get((jugadorMano+i)%4).leerMensaje();
            i++;
            if(respuesta=="ordago"){
                grandeAlPaso=false;
                apuesta=25;
                ordago=true;
            }else if(respuesta=="envido"){
                grandeAlPaso=false;
                envido=2;
            }else if(respuesta.startsWith("envido ")){
                grandeAlPaso=false;
                try{envido=Integer.parseInt(respuesta.split(" ")[1]);}
                catch(Exception e){mandarATodos("Jugador "+ ((jugadorMano+i)%4) +" ha pasado\r\n");}                
            }else{
                mandarATodos("Jugador "+ ((jugadorMano+i)%4) +" ha pasado\r\n");
            }
        }
        if (grandeAlPaso) return;

        
    }

    private static void pequena(){
        pequenaAlPaso=true;
        apuesta=1;
        envido=0;
        ordago=false;
        int i=0;
        String respuesta;
        String mensaje0="Que quieres hacer:\r\n";
        mensaje0+="Pasar: \"paso\" \r\n";
        mensaje0+="Envidar: \"envido\" \r\n";
        mensaje0+="Envidar x: \"envido X\" \r\n";
        mensaje0+="Órgdago: \"ordago\" \r\n";

        String mensaje1="Que quieres hacer:\r\n";
        mensaje1+="No querer: \"no\" \r\n";
        mensaje1+="Querer: \"quiero\" \r\n";
        mensaje1+="Envidar x más: \"envido X\" \r\n";
        mensaje1+="Órgdago: \"ordago\" \r\n";

        mandarATodos("Jugando pequena\r\n");

        while(pequenaAlPaso && i<4){            
            players.get((jugadorMano+i)%4).mandarMensaje(mensaje0);
            respuesta=players.get((jugadorMano+i)%4).leerMensaje();
            i++;
            if(respuesta=="ordago"){
                pequenaAlPaso=false;
                apuesta=25;
                ordago=true;
            }else if(respuesta=="envido"){
                pequenaAlPaso=false;
                envido=2;
            }else if(respuesta.startsWith("envido ")){
                pequenaAlPaso=false;
                try{envido=Integer.parseInt(respuesta.split(" ")[1]);}
                catch(Exception e){mandarATodos("Jugador "+ ((jugadorMano+i)%4) +" ha pasado\r\n");}                
            }else{
                mandarATodos("Jugador "+ ((jugadorMano+i)%4) +" ha pasado\r\n");
            }
        }
        if (pequenaAlPaso) return;

        
    }

    private static void pares(){
        paresAlPaso=true;
        apuesta=1;
        envido=0;
        ordago=false;
        int i=0;
        String respuesta;
        String mesajePares="¿Tienes pares?:\r\n";
        String mensaje0="Que quieres hacer:\r\n";
        mensaje0+="Pasar: \"paso\" \r\n";
        mensaje0+="Envidar: \"envido\" \r\n";
        mensaje0+="Envidar x: \"envido X\" \r\n";
        mensaje0+="Órgdago: \"ordago\" \r\n";

        String mensaje1="Que quieres hacer:\r\n";
        mensaje1+="No querer: \"no\" \r\n";
        mensaje1+="Querer: \"quiero\" \r\n";
        mensaje1+="Envidar x más: \"envido X\" \r\n";
        mensaje1+="Órgdago: \"ordago\" \r\n";

        mandarATodos("Jugando pares\r\n");

        while(paresAlPaso && i<4){            
            players.get((jugadorMano+i)%4).mandarMensaje(mensaje0);
            respuesta=players.get((jugadorMano+i)%4).leerMensaje();
            i++;
            if(respuesta=="ordago"){
                paresAlPaso=false;
                apuesta=25;
                ordago=true;
            }else if(respuesta=="envido"){
                paresAlPaso=false;
                envido=2;
            }else if(respuesta.startsWith("envido ")){
                paresAlPaso=false;
                try{envido=Integer.parseInt(respuesta.split(" ")[1]);}
                catch(Exception e){mandarATodos("Jugador "+ ((jugadorMano+i)%4) +" ha pasado\r\n");}                
            }else{
                mandarATodos("Jugador "+ ((jugadorMano+i)%4) +" ha pasado\r\n");
            }
        }
        if (paresAlPaso) return;
    }

    private static int getValorCarta(Card card) {
        switch (card.getValor()) {
            case "As": return 1;
            case "Tres": return 3;
            case "Cuatro": return 4;
            case "Cinco": return 5;
            case "Seis": return 6;
            case "Siete": return 7;
            case "Sota": return 8;
            case "Caballo": return 9;
            case "Rey": return 10;
            default: return 0;
        }
    }

    private static int getPuntosMano(List<Card> mano) {
        int totalValue = 0;
        for (Card card : mano) {
            totalValue += getPuntosCarta(card);
        }
        return totalValue;
    }

    private static int getPuntosCarta(Card card) {
        switch (card.getValor()) {
            case "As": return 1;
            case "Tres": return 3;
            case "Cuatro": return 4;
            case "Cinco": return 5;
            case "Seis": return 6;
            case "Siete": return 7;
            case "Sota": return 10;
            case "Caballo": return 10;
            case "Rey": return 10;
            default: return 0;
        }
    }

    private static void sumarPaso(){
        if(grandeAlPaso){
            int[] todos=new int[4];
            for (int i=0;i<4;i++){todos[i]=i;}
        }
    }

    private static void mandarATodos(String mensaje){
        for (int i=0;i<4;i++){
            players.get(i).mandarMensaje(mensaje);
        }
    }
}

