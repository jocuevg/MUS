package Servidor;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;


public class Server {
    private static List<Player> players = new ArrayList<>();
    private static List<Card> mazo= new ArrayList<>();
    private static List<List<Card>> manosJugadores=new ArrayList<>();
    private static Map<Integer, List<Card>> conPares = new HashMap<>();
    private static int[] puntos=new int[2];
    private static int ronda=0;
    private static int jugadorMano=0;
    private static int jugadorActual;
    private static int apuestaActual;
    private static boolean envidoActivo;
    private static boolean grandeAlPaso;
    private static boolean pequenaAlPaso;
    private static boolean paresAlPaso;
    private static boolean puntoAlPaso;    
    private static boolean juegoAlPaso;
    
    
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
                if(Math.max(puntos[0], puntos[1])>=25){
                    if(puntos[0]>=25){
                        mandarATodos("Pareja 1 ha ganado");
                    }else{
                        mandarATodos("Pareja 2 ha ganado");
                    }
                }
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
            if(!respuesta.equalsIgnoreCase("mus")) {
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
        grandeAlPaso=false;
        jugadorActual=jugadorMano;
        apuestaActual=1;
        envidoActivo=true;

        mandarATodos("Jugando grande\r\n");

        String respuesta;

        String mensaje0="Que quieres hacer:\r\n";
        mensaje0+="Pasar: \"paso\" \r\n";
        mensaje0+="Envidar: \"envido\" \r\n";
        mensaje0+="Envidar x: \"envido X\" \r\n";
        mensaje0+="Ordago: \"ordago\" \r\n";

        String mensaje1="Que quieres hacer:\r\n";
        mensaje1+="No querer: \"no\" \r\n";
        mensaje1+="Querer: \"quiero\" \r\n";
        mensaje1+="Envidar X más: \"envido X\" \r\n";
        mensaje1+="Ordago: \"ordago\" \r\n";


        while(envidoActivo && !grandeAlPaso){     
            Player jugador= players.get(jugadorActual);
            jugador.mandarMensaje(mensaje0);

            respuesta=jugador.leerMensaje();

            if(respuesta.equalsIgnoreCase("ordago")){
                envidoActivo=resolverOrdagoGrande(jugadorActual,apuestaActual);
                break;
            }else if(respuesta.equalsIgnoreCase("envido")){
                grandeAlPaso=false;
                apuestaActual=2;
                envidoActivo=gestionarRespuestaEnvidoGrande(jugadorActual,apuestaActual,mensaje1);
            }else if(respuesta.startsWith("envido ")){
                try{
                    apuestaActual=Integer.parseInt(respuesta.split(" ")[1]);
                    grandeAlPaso=false;
                    envidoActivo=gestionarRespuestaEnvidoGrande(jugadorActual, apuestaActual, mensaje1);
                }
                catch(Exception e){
                    grandeAlPaso = todosPasaron(jugadorActual);
                }                
            }else{
                grandeAlPaso = todosPasaron(jugadorActual);
            }
            jugadorActual = (jugadorActual + 1) % 4;
        }
        if (grandeAlPaso) {
            mandarATodos("Todos han pasado");
        }        
    }

    private static void pequena(){
        pequenaAlPaso=false;
        jugadorActual=jugadorMano;
        apuestaActual=1;
        envidoActivo=true;

        mandarATodos("Jugando pequeña\r\n");

        String respuesta;

        String mensaje0="Que quieres hacer:\r\n";
        mensaje0+="Pasar: \"paso\" \r\n";
        mensaje0+="Envidar: \"envido\" \r\n";
        mensaje0+="Envidar x: \"envido X\" \r\n";
        mensaje0+="Ordago: \"ordago\" \r\n";

        String mensaje1="Que quieres hacer:\r\n";
        mensaje1+="No querer: \"no\" \r\n";
        mensaje1+="Querer: \"quiero\" \r\n";
        mensaje1+="Envidar X más: \"envido X\" \r\n";
        mensaje1+="Ordago: \"ordago\" \r\n";


        while(envidoActivo && !pequenaAlPaso){     
            Player jugador= players.get(jugadorActual);
            jugador.mandarMensaje(mensaje0);

            respuesta=jugador.leerMensaje();

            if(respuesta.equalsIgnoreCase("ordago")){
                envidoActivo=resolverOrdagoPequena(jugadorActual,apuestaActual);
                break;
            }else if(respuesta.equalsIgnoreCase("envido")){
                pequenaAlPaso=false;
                apuestaActual=2;
                envidoActivo=gestionarRespuestaEnvidoPequena(jugadorActual,apuestaActual,mensaje1);
            }else if(respuesta.startsWith("envido ")){
                try{
                    apuestaActual=Integer.parseInt(respuesta.split(" ")[1]);
                    pequenaAlPaso=false;
                    envidoActivo=gestionarRespuestaEnvidoPequena(jugadorActual, apuestaActual, mensaje1);
                }
                catch(Exception e){
                    pequenaAlPaso = todosPasaron(jugadorActual);
                }                
            }else{
                pequenaAlPaso = todosPasaron(jugadorActual);
            }
            jugadorActual = (jugadorActual + 1) % 4;
        }
        if (pequenaAlPaso) {
            mandarATodos("Todos han pasado");
        }        
    }    

    private static void pares(){

        jugadorActual=jugadorMano;
        Player player; 
        conPares=new HashMap<>();  
        apuestaActual=1;
        envidoActivo=true;
        paresAlPaso=true;

        String respuesta;
        String mesajePares="¿Tienes pares (si/no)?:\r\n";

        mandarATodos("Jugando pares\r\n");

        for(int i=0;i<3;i++){ 
            player=players.get(jugadorActual);
            player.mandarMensaje(mesajePares);
            respuesta=player.leerMensaje();
            if(respuesta.equalsIgnoreCase("si")){
                conPares.put(jugadorActual, manosJugadores.get(jugadorActual));
            }
            jugadorActual=(jugadorActual+1)%4;
        }

        if(conPares.size()==0) return;
        
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
    }

    private static boolean gestionarRespuestaEnvidoGrande(int jugador, int apuestaActual, String mensaje1) {
        mandarATodos("Jugador " + (jugador%4 + 1) + " ha envidado " + apuestaActual + " puntos.");

        players.get((jugador+1)%4).mandarMensaje(mensaje1);
        String respuesta = players.get((jugador+1)%4).leerMensaje();
        if (respuesta.equalsIgnoreCase("quiero")) {
            mandarATodos("Jugador " + ((jugador+1)%4+1) + " ha aceptado el envido.");
            puntos[getGanadorGrande()%2] += apuestaActual; // Se suman los puntos al equipo contrario
            return false; // Termina el envido
        } else if (respuesta.startsWith("envido ")) {
            try {
                int nuevaApuesta = apuestaActual+Integer.parseInt(respuesta.split(" ")[1]);
                mandarATodos("Jugador " + ((jugador+1)%4+1) + " ha aumentado el envido a " + nuevaApuesta + " puntos.");
                apuestaActual = nuevaApuesta;
                gestionarRespuestaEnvidoGrande((jugador+1)%4, apuestaActual, mensaje1);
                return false; // Continúa el envido con la nueva apuesta
            } catch (NumberFormatException e) {
                mandarATodos("Jugador " + ((jugador+1)%4+1) + " ha aceptado el envido.");
                puntos[getGanadorGrande()%2] += apuestaActual; // Se suman los puntos al equipo contrario
                return false; // Termina el envido
            }
        } else if (respuesta.equalsIgnoreCase("ordago")){
            return resolverOrdagoGrande((jugador+1)%4,apuestaActual);
        }else {
            mandarATodos("Jugador " + ((jugador+1)%4+1) + " ha rechazado el envido.");

            players.get((jugador+3)%4).mandarMensaje(mensaje1);
            respuesta = players.get((jugador+3)%4).leerMensaje();

            if (respuesta.equalsIgnoreCase("quiero")) {
                mandarATodos("Jugador " + ((jugador+3)%4+1) + " ha aceptado el envido.");
                puntos[getGanadorGrande()%2] += apuestaActual; // Se suman los puntos al equipo contrario
                return false; // Termina el envido
            } else if (respuesta.startsWith("envido ")) {
                try {
                    int nuevaApuesta = apuestaActual+Integer.parseInt(respuesta.split(" ")[1]);
                    mandarATodos("Jugador " + ((jugador+3)%4+1) + " ha aumentado el envido a " + nuevaApuesta + " puntos.");
                    apuestaActual = nuevaApuesta;
                    gestionarRespuestaEnvidoGrande((jugador+3)%4, apuestaActual, mensaje1);
                    return false; // Continúa el envido con la nueva apuesta
                } catch (NumberFormatException e) {
                    mandarATodos("Jugador " + ((jugador+3)%4+1) + " ha aceptado el envido.");
                    puntos[getGanadorGrande()%2] += apuestaActual; // Se suman los puntos al equipo contrario
                    return false; // Termina el envido
                }
            } else if (respuesta.equalsIgnoreCase("ordago")){
                return resolverOrdagoGrande((jugador+3)%4,apuestaActual);
            }else{
                mandarATodos("El envite fue rechazado.");
                puntos[jugador%2]+=apuestaActual;
                return false;
            }
        }
    }

    private static boolean gestionarRespuestaEnvidoPequena(int jugador, int apuestaActual, String mensaje1) {
        mandarATodos("Jugador " + (jugador%4 + 1) + " ha envidado " + apuestaActual + " puntos.");

        players.get((jugador+1)%4).mandarMensaje(mensaje1);
        String respuesta = players.get((jugador+1)%4).leerMensaje();
        if (respuesta.equalsIgnoreCase("quiero")) {
            mandarATodos("Jugador " + ((jugador+1)%4+1) + " ha aceptado el envido.");
            puntos[getGanadorPequeña()%2] += apuestaActual; // Se suman los puntos al equipo contrario
            return false; // Termina el envido
        } else if (respuesta.startsWith("envido ")) {
            try {
                int nuevaApuesta = apuestaActual+Integer.parseInt(respuesta.split(" ")[1]);
                mandarATodos("Jugador " + ((jugador+1)%4+1) + " ha aumentado el envido a " + nuevaApuesta + " puntos.");
                apuestaActual = nuevaApuesta;
                gestionarRespuestaEnvidoPequena((jugador+1)%4, apuestaActual, mensaje1);
                return false; // Continúa el envido con la nueva apuesta
            } catch (NumberFormatException e) {
                mandarATodos("Jugador " + ((jugador+1)%4+1) + " ha aceptado el envido.");
                puntos[getGanadorPequeña()%2] += apuestaActual; // Se suman los puntos al equipo contrario
                return false; // Termina el envido
            }
        } else if (respuesta.equalsIgnoreCase("ordago")){
            return resolverOrdagoPequena((jugador+1)%4,apuestaActual);
        }else {
            mandarATodos("Jugador " + ((jugador+1)%4+1) + " ha rechazado el envido.");

            players.get((jugador+3)%4).mandarMensaje(mensaje1);
            respuesta = players.get((jugador+3)%4).leerMensaje();

            if (respuesta.equalsIgnoreCase("quiero")) {
                mandarATodos("Jugador " + ((jugador+3)%4+1) + " ha aceptado el envido.");
                puntos[getGanadorPequeña()%2] += apuestaActual; // Se suman los puntos al equipo contrario
                return false; // Termina el envido
            } else if (respuesta.startsWith("envido ")) {
                try {
                    int nuevaApuesta = apuestaActual+Integer.parseInt(respuesta.split(" ")[1]);
                    mandarATodos("Jugador " + ((jugador+3)%4+1) + " ha aumentado el envido a " + nuevaApuesta + " puntos.");
                    apuestaActual = nuevaApuesta;
                    gestionarRespuestaEnvidoPequena((jugador+3)%4, apuestaActual, mensaje1);
                    return false; // Continúa el envido con la nueva apuesta
                } catch (NumberFormatException e) {
                    mandarATodos("Jugador " + ((jugador+3)%4+1) + " ha aceptado el envido.");
                    puntos[getGanadorPequeña()%2] += apuestaActual; // Se suman los puntos al equipo contrario
                    return false; // Termina el envido
                }
            } else if (respuesta.equalsIgnoreCase("ordago")){
                return resolverOrdagoPequena((jugador+3)%4,apuestaActual);
            }else{
                mandarATodos("El envite fue rechazado.");
                puntos[jugador%2]+=apuestaActual;
                return false;
            }
        }
    }

    private static boolean todosPasaron(int jugadorActual) {
        mandarATodos("Jugador " + (jugadorActual + 1)%4 + " ha pasado.");
        return jugadorActual == (jugadorMano + 3) % 4;
    }

    private static boolean resolverOrdagoGrande(int jugador,int apuestaActual) {
        mandarATodos("Jugador " + (jugador + 1) + " ha lanzado un ordago!");
       
        players.get((jugador+1)%4).mandarMensaje("¿Quieres el órdago? (si/no)");
        String respuesta = players.get((jugador+1)%4).leerMensaje();
        if (respuesta.equalsIgnoreCase("si")) {
            mandarATodos("¡Órdago aceptado! Fin del juego.");
            puntos[getGanadorGrande()%2] = 25; // Asigna los puntos al equipo ganador
            return false;
        }else{
            mandarATodos("Jugador "+(jugador+2)%4+" no ha querido el ordago");
            players.get((jugador+3)%4).mandarMensaje("¿Quieres el órdago? (si/no)");
            respuesta = players.get((jugador+3)%4).leerMensaje();
            if (respuesta.equalsIgnoreCase("si")) {
                mandarATodos("¡Órdago aceptado! Fin del juego.");
                puntos[getGanadorGrande()%2] = 25; // Asigna los puntos al equipo ganador
                return false;
            }else{
                mandarATodos("El órdago fue rechazado.");
                puntos[jugador%2]+=apuestaActual;
                return false;
            }
        }
    }

    private static boolean resolverOrdagoPequena(int jugador,int apuestaActual) {
        mandarATodos("Jugador " + (jugador + 1) + " ha lanzado un ordago!");
       
        players.get((jugador+1)%4).mandarMensaje("¿Quieres el órdago? (si/no)");
        String respuesta = players.get((jugador+1)%4).leerMensaje();
        if (respuesta.equalsIgnoreCase("si")) {
            mandarATodos("¡Órdago aceptado! Fin del juego.");
            puntos[getGanadorPequeña()%2] = 25; // Asigna los puntos al equipo ganador
            return false;
        }else{
            mandarATodos("Jugador "+(jugador+2)%4+" no ha querido el ordago");
            players.get((jugador+3)%4).mandarMensaje("¿Quieres el órdago? (si/no)");
            respuesta = players.get((jugador+3)%4).leerMensaje();
            if (respuesta.equalsIgnoreCase("si")) {
                mandarATodos("¡Órdago aceptado! Fin del juego.");
                puntos[getGanadorPequeña()%2] = 25; // Asigna los puntos al equipo ganador
                return false;
            }else{
                mandarATodos("El órdago fue rechazado.");
                puntos[jugador%2]+=apuestaActual;
                return false;
            }
        }
    }

    private static int getGanadorGrande() {
        for (int i = 0; i < 4; i++) {
            int manoGanadora = -1;
            int valorMayor = -1;
            jugadorActual=jugadorMano;
            // Buscar la carta más alta en la posición i
            for (int j = 0; j < 4; j++) {

                int valorCarta = manosJugadores.get(jugadorActual).get(i).getValorNumerico();
                if (valorCarta > valorMayor) {
                    valorMayor = valorCarta;
                    manoGanadora = jugadorActual;
                } else if (valorCarta == valorMayor) {
                    manoGanadora = -1; // Si hay empate en esta posición, seguimos comparando
                }
                jugadorActual=(jugadorActual+1)%4;
            }

            // Si hemos encontrado una mano ganadora en esta posición, retornamos
            if (manoGanadora != -1) {
                return manoGanadora;
            }
        }

        return jugadorMano;
    }

    private static int getGanadorPequeña() {
        for (int i = 3; i >=0; i--) {
            int manoGanadora = -1;
            int valorMenor = 11;
            jugadorActual=jugadorMano;
            // Buscar la carta más alta en la posición i
            for (int j = 0; j < 4; j++) {

                int valorCarta = manosJugadores.get(jugadorActual).get(i).getValorNumerico();
                if (valorCarta < valorMenor) {
                    valorMenor = valorCarta;
                    manoGanadora = jugadorActual;
                } else if (valorCarta == valorMenor) {
                    manoGanadora = 11; // Si hay empate en esta posición, seguimos comparando
                }
                jugadorActual=(jugadorActual+1)%4;
            }

            // Si hemos encontrado una mano ganadora en esta posición, retornamos
            if (manoGanadora != 11) {
                return manoGanadora;
            }
        }

        return jugadorMano;
    }

    private static int getPuntosMano(List<Card> mano) {
        int totalValue = 0;
        for (Card card : mano) {
            totalValue += card.getPuntos();
        }
        return totalValue;
    }

    private static void sumarPaso(){
        if(grandeAlPaso){
            puntos[getGanadorGrande()%2]++;
        }if(pequenaAlPaso){
            puntos[getGanadorPequeña()%2]++;
        }if(paresAlPaso){
            
        }if(puntoAlPaso){
            
        }if(juegoAlPaso){
            
        }
    }

    private static void mandarATodos(String mensaje){
        for (int i=0;i<4;i++){
            players.get(i).mandarMensaje(mensaje);
        }
    }
}
