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
    private static List<Card> mazo = new ArrayList<>();
    private static List<List<Card>> manosJugadores = new ArrayList<>();
    private static Map<Integer, List<Card>> conPares = new HashMap<>();
    private static Map<Integer, List<Card>> conJuego = new HashMap<>();
    private static int[] puntos = new int[2];
    private static int ronda = 0;
    private static int jugadorMano = 0;
    private static int jugadorActual;
    private static int apuestaActual;
    private static int apuestaLanzada;
    private static boolean envidoActivo;
    private static boolean grandeAlPaso;
    private static boolean pequenaAlPaso;
    private static boolean paresAlPaso;
    private static boolean puntoAlPaso;
    private static boolean juegoAlPaso;

    public static void main(String[] args) {
        try (ServerSocket ss = new ServerSocket(55555)) {
            while (players.size() < 4) {
                Socket socket = ss.accept();
                Player player = new Player(socket, players.size() + 1);
                players.add(player);
                new Thread(player).start();
            }

            puntos[0] = 0;
            puntos[1] = 0;
            for (int i = 0; i < 4; i++) {
                manosJugadores.add(new ArrayList<Card>());
            }

            while (Math.max(puntos[0], puntos[1]) < 25) {
                reiniciarCartas();
                jugadorMano = ronda;
                mandarATodos("Ronda " + (ronda + 1) + ", jugador " + (ronda + 1) + " va de mano");
                repartir();
                mus();
                ordenarManos();
                grande();
                if (Math.max(puntos[0], puntos[1]) >= 25) break;
                pequena();
                if (Math.max(puntos[0], puntos[1]) >= 25) break;
                pares();
                if (Math.max(puntos[0], puntos[1]) >= 25) break;
                
                juego();
                if (Math.max(puntos[0], puntos[1]) >= 25) break;
                
                sumarPaso();
                if (Math.max(puntos[0], puntos[1]) >= 25) break;
                
                mostrarPuntos();

                ronda++;
            }
            if (puntos[0] >= 25) {
                mandarATodos("Pareja 1 ha ganado");
            } else {
                mandarATodos("Pareja 2 ha ganado");
            }          
            mostrarPuntos();

            mandarATodos("FIN");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void reiniciarCartas() {
        mazo.clear();
        for (int i = 0; i < 4; i++) {
            manosJugadores.get(i).clear();
        }
        String[] palos = { "Oros", "Copas", "Espadas", "Bastos" };
        String[] valores = { "As", "Tres", "Cuatro", "Cinco", "Seis", "Siete", "Sota", "Caballo", "Rey" };

        for (String palo : palos) {
            for (String valor : valores) {
                mazo.add(new Card(palo, valor));
            }
        }
        Collections.shuffle(mazo);
    }

    private static void repartir() {
        int aux = ronda;
        for (int i = 0; i < 4; i++) {
            for (int j = manosJugadores.get(aux % 4).size(); j < 4; j++) {
                Card c = mazo.remove(0);
                manosJugadores.get(aux % 4).add(c);
            }
            mandarMano(aux % 4);
            aux++;
        }
    }

    private static void mandarMano(int idPlayer) {
        String mensaje = "Mano: \r\n";
        for (Card c : manosJugadores.get(idPlayer)) {
            mensaje += c.toString() + "\r\n";
        }
        players.get(idPlayer).mandarMensaje(mensaje);
    }

    private static void mus() {
        String respuesta;
        for (int i = 0; i < 4; i++) {
            players.get((jugadorMano + i) % 4).mandarMensaje("Quieres mus? (mus/no)");
            respuesta = players.get((jugadorMano + i) % 4).leerMensaje();
            if (!respuesta.equalsIgnoreCase("mus")) {
                mandarATodos("Jugador " + ((jugadorMano + i) % 4 + 1) + " ha cortado el mus");
                return;
            }
        }
        mandarATodos(
                "Hay mus, indica en orden el indice de las cartas que tiras separadas por comas (ej: \"0,1,3\" -> te quedas solo con la segunda carta)");
        for (int i = 0; i < 4; i++) {
            respuesta = players.get((jugadorMano + i) % 4).leerMensaje();
            String[] tiradas = respuesta.split(",");
            for (int j = 0; j < tiradas.length; j++) {
                try {
                    Card c = manosJugadores.get((jugadorMano + i) % 4).remove(Integer.parseInt(tiradas[j]));
                    mazo.add(c);
                    c = mazo.remove(0);
                    manosJugadores.get((jugadorMano + i) % 4).add(Integer.parseInt(tiradas[j]), c);
                    ;
                } catch (Exception e) {
                }
            }
            mandarMano((jugadorMano + i) % 4);
        }
        mus();
    }

    private static void ordenarManos() {
        for (int i = 0; i < 4; i++) {
            manosJugadores.get(i).sort(Comparator.comparingInt(Card::getValorNumerico).reversed());
        }
    }

    private static void grande() {
        grandeAlPaso = false;
        jugadorActual = jugadorMano;
        apuestaActual = 1;
        apuestaLanzada=1;
        envidoActivo = true;

        mandarATodos("Jugando grande\r\n");

        String respuesta;

        String mensaje0 = "Que quieres hacer:\r\n";
        mensaje0 += "Pasar: \"paso\" \r\n";
        mensaje0 += "Envidar: \"envido\" \r\n";
        mensaje0 += "Envidar x: \"envido X\" \r\n";
        mensaje0 += "Ordago: \"ordago\" \r\n";

        String mensaje1 = "Que quieres hacer:\r\n";
        mensaje1 += "No querer: \"no\" \r\n";
        mensaje1 += "Querer: \"quiero\" \r\n";
        mensaje1 += "Envidar X mas: \"envido X\" \r\n";
        mensaje1 += "Ordago: \"ordago\" \r\n";

        while (envidoActivo && !grandeAlPaso) {
            Player jugador = players.get(jugadorActual);
            mandarMano(jugadorActual);
            jugador.mandarMensaje(mensaje0);

            respuesta = jugador.leerMensaje();

            if (respuesta.equalsIgnoreCase("ordago")) {
                envidoActivo = resolverOrdagoGrande(jugadorActual, apuestaActual);
                break;
            } else if (respuesta.equalsIgnoreCase("envido")) {
                grandeAlPaso = false;
                apuestaLanzada = 2;
                envidoActivo = gestionarRespuestaEnvidoGrande(jugadorActual, apuestaActual,apuestaLanzada, mensaje1);
            } else if (respuesta.startsWith("envido ")) {
                try {
                    apuestaLanzada = Integer.parseInt(respuesta.split(" ")[1]);
                    grandeAlPaso = false;
                    envidoActivo = gestionarRespuestaEnvidoGrande(jugadorActual, apuestaActual,apuestaLanzada, mensaje1);
                } catch (Exception e) {
                    grandeAlPaso = todosPasaron(jugadorActual);
                }
            } else {
                grandeAlPaso = todosPasaron(jugadorActual);
            }
            jugadorActual = (jugadorActual + 1) % 4;
        }
        if (grandeAlPaso) {
            mandarATodos("Todos han pasado");
        }
    }

    private static void pequena() {
        pequenaAlPaso = false;
        jugadorActual = jugadorMano;
        apuestaActual = 1;
        apuestaLanzada=1;
        envidoActivo = true;

        mandarATodos("Jugando pequena\r\n");

        String respuesta;

        String mensaje0 = "Que quieres hacer:\r\n";
        mensaje0 += "Pasar: \"paso\" \r\n";
        mensaje0 += "Envidar: \"envido\" \r\n";
        mensaje0 += "Envidar x: \"envido X\" \r\n";
        mensaje0 += "Ordago: \"ordago\" \r\n";

        String mensaje1 = "Que quieres hacer:\r\n";
        mensaje1 += "No querer: \"no\" \r\n";
        mensaje1 += "Querer: \"quiero\" \r\n";
        mensaje1 += "Envidar X mas: \"envido X\" \r\n";
        mensaje1 += "Ordago: \"ordago\" \r\n";

        while (envidoActivo && !pequenaAlPaso) {
            Player jugador = players.get(jugadorActual);
            mandarMano(jugadorActual);
            jugador.mandarMensaje(mensaje0);

            respuesta = jugador.leerMensaje();

            if (respuesta.equalsIgnoreCase("ordago")) {
                envidoActivo = resolverOrdagoPequena(jugadorActual, apuestaActual);
                break;
            } else if (respuesta.equalsIgnoreCase("envido")) {
                pequenaAlPaso = false;
                apuestaLanzada = 2;
                envidoActivo = gestionarRespuestaEnvidoPequena(jugadorActual, apuestaActual,apuestaLanzada, mensaje1);
            } else if (respuesta.startsWith("envido ")) {
                try {
                    apuestaLanzada = Integer.parseInt(respuesta.split(" ")[1]);
                    pequenaAlPaso = false;
                    envidoActivo = gestionarRespuestaEnvidoPequena(jugadorActual, apuestaActual,apuestaLanzada, mensaje1);
                } catch (Exception e) {
                    pequenaAlPaso = todosPasaron(jugadorActual);
                }
            } else {
                pequenaAlPaso = todosPasaron(jugadorActual);
            }
            jugadorActual = (jugadorActual + 1) % 4;
        }
        if (pequenaAlPaso) {
            mandarATodos("Todos han pasado");
        }
    }

    private static void pares() {

        jugadorActual = jugadorMano;
        Player player;
        conPares = new HashMap<>();
        apuestaActual = 1;
        apuestaLanzada=1;
        envidoActivo = true;
        paresAlPaso = false;

        String respuesta;
        String mesajePares = "Tienes pares (si/no)?:\r\n";

        mandarATodos("Jugando pares\r\n");

        for (int i = 0; i < 4; i++) {
            player = players.get(jugadorActual);
            mandarMano(jugadorActual);
            player.mandarMensaje(mesajePares);
            respuesta = player.leerMensaje();
            if (respuesta.equalsIgnoreCase("si")) {
                mandarATodos("Jugador "+(jugadorActual+1)+" tiene pares");
                conPares.put(jugadorActual, manosJugadores.get(jugadorActual));
            }else{
                mandarATodos("Jugador "+(jugadorActual+1)+" no tiene pares");
            }
            jugadorActual = (jugadorActual + 1) % 4;
        }

        // Verificar si hay un jugador con pares en cada pareja
        boolean equipo1TienePares = conPares.containsKey(0) || conPares.containsKey(2);
        boolean equipo2TienePares = conPares.containsKey(1) || conPares.containsKey(3);

        if (conPares.size() == 0) {
            mandarATodos("Nadie tiene pares\r\n");
            return;
        }
        if (!equipo1TienePares || !equipo2TienePares) {
            if(equipo1TienePares){
                mandarATodos("Solo tiene pares la pareja 1\r\n");
                puntos[0]+=getGanadorPares()[1];
            }else{
                mandarATodos("Solo tiene pares la pareja 2\r\n");   
                puntos[1]+=getGanadorPares()[1];
            }
            return;
        }
        
        mandarATodos("Se juegan pares");

        String mensaje0 = "Que quieres hacer:\r\n";
        mensaje0 += "Pasar: \"paso\" \r\n";
        mensaje0 += "Envidar: \"envido\" \r\n";
        mensaje0 += "Envidar x: \"envido X\" \r\n";
        mensaje0 += "Orgdago: \"ordago\" \r\n";

        String mensaje1 = "Que quieres hacer:\r\n";
        mensaje1 += "No querer: \"no\" \r\n";
        mensaje1 += "Querer: \"quiero\" \r\n";
        mensaje1 += "Envidar x mas: \"envido X\" \r\n";
        mensaje1 += "Orgdago: \"ordago\" \r\n";

        jugadorActual=jugadorMano;

        while (envidoActivo && !paresAlPaso) {
            if (!conPares.containsKey(jugadorActual)){
                jugadorActual = (jugadorActual + 1) % 4;
                continue; // Si no tiene pares jugadorActual
            }
            Player jugador = players.get(jugadorActual);
            mandarMano(jugadorActual);
            jugador.mandarMensaje(mensaje0);

            respuesta = jugador.leerMensaje();

            if (respuesta.equalsIgnoreCase("ordago")) {
                envidoActivo = resolverOrdagoPares(jugadorActual, apuestaActual);
                break;
            } else if (respuesta.equalsIgnoreCase("envido")) {
                paresAlPaso = false;
                apuestaLanzada = 2;
                envidoActivo = gestionarRespuestaEnvidoPares(jugadorActual, apuestaActual,apuestaLanzada, mensaje1);
            } else if (respuesta.startsWith("envido ")) {
                try {
                    apuestaLanzada = Integer.parseInt(respuesta.split(" ")[1]);
                    paresAlPaso = false;
                    envidoActivo = gestionarRespuestaEnvidoPares(jugadorActual, apuestaActual,apuestaLanzada, mensaje1);
                } catch (Exception e) {
                    paresAlPaso = todosPasaron(jugadorActual);
                }
            } else {
                paresAlPaso = todosPasaron(jugadorActual);
            }
            jugadorActual = (jugadorActual + 1) % 4;
        }
        if (paresAlPaso) {
            mandarATodos("Todos han pasado");
        }
    }

    private static void juego() {

        jugadorActual = jugadorMano;
        Player player;
        conJuego = new HashMap<>();
        apuestaActual = 1;
        apuestaLanzada=1;
        envidoActivo = true;
        puntoAlPaso = false;
        juegoAlPaso = false;


        String respuesta;
        String mesajeJuego = "Tienes juego (si/no)?:\r\n";

        mandarATodos("Jugando juego\r\n");

        for (int i = 0; i < 4; i++) {
            player = players.get(jugadorActual);
            mandarMano(jugadorActual);
            player.mandarMensaje(mesajeJuego);
            respuesta = player.leerMensaje();
            if (respuesta.equalsIgnoreCase("si")) {
                conJuego.put(jugadorActual, manosJugadores.get(jugadorActual));
                mandarATodos("Jugador "+(jugadorActual+1)+" tiene juego");
            }else{
                mandarATodos("Jugador "+(jugadorActual+1)+" no tiene juego");
            }
            jugadorActual = (jugadorActual + 1) % 4;
        }

        // Verificar si hay un jugador con juego en cada pareja
        boolean equipo1TieneJuego = conJuego.containsKey(0) || conJuego.containsKey(2);
        boolean equipo2TieneJuego = conJuego.containsKey(1) || conJuego.containsKey(3);

        if (conJuego.size() == 0) {
            mandarATodos("Nadie tiene juego\r\n");
            punto();
            return;
        }
        if (!equipo1TieneJuego || !equipo2TieneJuego) {
            if(equipo1TieneJuego){
                mandarATodos("Solo tiene juego la pareja 1\r\n");
                puntos[0]+=getGanadorJuego()[1];
            }else{
                mandarATodos("Solo tiene juego la pareja 2\r\n");   
                puntos[1]+=getGanadorJuego()[1];
            }
            return;
        }
        
        mandarATodos("Se juega juego");

        String mensaje0 = "Que quieres hacer:\r\n";
        mensaje0 += "Pasar: \"paso\" \r\n";
        mensaje0 += "Envidar: \"envido\" \r\n";
        mensaje0 += "Envidar x: \"envido X\" \r\n";
        mensaje0 += "Orgdago: \"ordago\" \r\n";

        String mensaje1 = "Que quieres hacer:\r\n";
        mensaje1 += "No querer: \"no\" \r\n";
        mensaje1 += "Querer: \"quiero\" \r\n";
        mensaje1 += "Envidar x mas: \"envido X\" \r\n";
        mensaje1 += "Orgdago: \"ordago\" \r\n";

        jugadorActual=jugadorMano;

        while (envidoActivo && !juegoAlPaso) {
            if (!conJuego.containsKey(jugadorActual)){
                jugadorActual = (jugadorActual + 1) % 4;
                continue; // Si no tiene juego jugadorActual
            }
            Player jugador = players.get(jugadorActual);
            mandarMano(jugadorActual);
            jugador.mandarMensaje(mensaje0);

            respuesta = jugador.leerMensaje();

            if (respuesta.equalsIgnoreCase("ordago")) {
                envidoActivo = resolverOrdagoJuego(jugadorActual, apuestaActual);
                break;
            } else if (respuesta.equalsIgnoreCase("envido")) {
                juegoAlPaso = false;
                apuestaLanzada = 2;
                envidoActivo = gestionarRespuestaEnvidoJuego(jugadorActual, apuestaActual,apuestaLanzada, mensaje1);
            } else if (respuesta.startsWith("envido ")) {
                try {
                    apuestaLanzada = Integer.parseInt(respuesta.split(" ")[1]);
                    juegoAlPaso = false;
                    envidoActivo = gestionarRespuestaEnvidoJuego(jugadorActual, apuestaActual,apuestaLanzada, mensaje1);
                } catch (Exception e) {
                    juegoAlPaso = todosPasaron(jugadorActual);
                }
            } else {
                juegoAlPaso = todosPasaron(jugadorActual);
            }
            jugadorActual = (jugadorActual + 1) % 4;
        }
        if (juegoAlPaso) {
            mandarATodos("Todos han pasado");
        }
    }

    private static void punto() {
        puntoAlPaso = false;
        jugadorActual = jugadorMano;
        apuestaActual = 1;
        apuestaLanzada=1;
        envidoActivo = true;

        mandarATodos("Jugando punto\r\n");

        String respuesta;

        String mensaje0 = "Que quieres hacer:\r\n";
        mensaje0 += "Pasar: \"paso\" \r\n";
        mensaje0 += "Envidar: \"envido\" \r\n";
        mensaje0 += "Envidar x: \"envido X\" \r\n";
        mensaje0 += "Ordago: \"ordago\" \r\n";

        String mensaje1 = "Que quieres hacer:\r\n";
        mensaje1 += "No querer: \"no\" \r\n";
        mensaje1 += "Querer: \"quiero\" \r\n";
        mensaje1 += "Envidar X mas: \"envido X\" \r\n";
        mensaje1 += "Ordago: \"ordago\" \r\n";

        while (envidoActivo && !puntoAlPaso) {
            Player jugador = players.get(jugadorActual);
            mandarMano(jugadorActual);
            jugador.mandarMensaje(mensaje0);

            respuesta = jugador.leerMensaje();

            if (respuesta.equalsIgnoreCase("ordago")) {
                envidoActivo = resolverOrdagoPunto(jugadorActual, apuestaActual);
                break;
            } else if (respuesta.equalsIgnoreCase("envido")) {
                puntoAlPaso = false;
                apuestaLanzada = 2;
                envidoActivo = gestionarRespuestaEnvidoPunto(jugadorActual, apuestaActual,apuestaLanzada, mensaje1);
            } else if (respuesta.startsWith("envido ")) {
                try {
                    apuestaLanzada = Integer.parseInt(respuesta.split(" ")[1]);
                    puntoAlPaso = false;
                    envidoActivo = gestionarRespuestaEnvidoPunto(jugadorActual, apuestaActual,apuestaLanzada, mensaje1);
                } catch (Exception e) {
                    puntoAlPaso = todosPasaron(jugadorActual);
                }
            } else {
                puntoAlPaso = todosPasaron(jugadorActual);
            }
            jugadorActual = (jugadorActual + 1) % 4;
        }
        if (puntoAlPaso) {
            mandarATodos("Todos han pasado");
        }
    }

    private static boolean gestionarRespuestaEnvidoGrande(int jugador, int apuestaActual,int apuestaLanzada, String mensaje1) {
        mandarATodos("Jugador " + (jugador % 4 + 1) + " ha envidado " + apuestaLanzada + " puntos.");
        
        mandarMano((jugador + 1) % 4);
        players.get((jugador + 1) % 4).mandarMensaje(mensaje1);
        String respuesta = players.get((jugador + 1) % 4).leerMensaje();
        if (respuesta.equalsIgnoreCase("quiero")) {
            mandarATodos("Jugador " + ((jugador + 1) % 4 + 1) + " ha aceptado el envido.");
            mandarATodos("Jugador "+(getGanadorGrande()+1)+" ha ganado la grande");
            puntos[getGanadorGrande() % 2] += apuestaLanzada; // Se suman los puntos al equipo contrario
            return false; // Termina el envido
        } else if (respuesta.startsWith("envido ")) {
            try {
                int nuevaApuesta = apuestaLanzada + Integer.parseInt(respuesta.split(" ")[1]);
                mandarATodos("Jugador " + ((jugador + 1) % 4 + 1) + " ha aumentado el envido a " + nuevaApuesta
                        + " puntos.");
                apuestaActual = apuestaLanzada;
                apuestaLanzada=nuevaApuesta;
                gestionarRespuestaEnvidoGrande((jugador + 1) % 4, apuestaActual,apuestaLanzada, mensaje1);
                return false; // Continúa el envido con la nueva apuesta
            } catch (NumberFormatException e) {
                mandarATodos("Jugador " + ((jugador + 1) % 4 + 1) + " ha aceptado el envido.");
                mandarATodos("Jugador "+(getGanadorGrande()+1)+" ha ganado la grande");
                puntos[getGanadorGrande() % 2] += apuestaLanzada; // Se suman los puntos al equipo contrario
                return false; // Termina el envido
            }
        } else if (respuesta.equalsIgnoreCase("ordago")) {
            return resolverOrdagoGrande((jugador + 1) % 4, apuestaLanzada);
        } else {
            mandarATodos("Jugador " + ((jugador + 1) % 4 + 1) + " ha rechazado el envido.");
            
            mandarMano((jugador + 3) % 4);
            players.get((jugador + 3) % 4).mandarMensaje(mensaje1);
            respuesta = players.get((jugador + 3) % 4).leerMensaje();

            if (respuesta.equalsIgnoreCase("quiero")) {
                mandarATodos("Jugador " + ((jugador + 3) % 4 + 1) + " ha aceptado el envido.");
                mandarATodos("Jugador "+(getGanadorGrande()+1)+" ha ganado la grande");
                puntos[getGanadorGrande() % 2] += apuestaLanzada; // Se suman los puntos al equipo contrario
                return false; // Termina el envido
            } else if (respuesta.startsWith("envido ")) {
                try {
                    int nuevaApuesta = apuestaLanzada + Integer.parseInt(respuesta.split(" ")[1]);
                    mandarATodos("Jugador " + ((jugador + 3) % 4 + 1) + " ha aumentado el envido a " + nuevaApuesta
                            + " puntos.");
                    apuestaActual = apuestaLanzada;
                    apuestaLanzada=nuevaApuesta;
                    gestionarRespuestaEnvidoGrande((jugador + 3) % 4, apuestaActual,apuestaLanzada, mensaje1);
                    return false; // Continúa el envido con la nueva apuesta
                } catch (NumberFormatException e) {
                    mandarATodos("Jugador " + ((jugador + 3) % 4 + 1) + " ha aceptado el envido.");
                    mandarATodos("Jugador "+(getGanadorGrande()+1)+" ha ganado la grande");
                    puntos[getGanadorGrande() % 2] += apuestaLanzada; // Se suman los puntos al equipo contrario
                    return false; // Termina el envido
                }
            } else if (respuesta.equalsIgnoreCase("ordago")) {
                return resolverOrdagoGrande((jugador + 3) % 4, apuestaLanzada);
            } else {
                mandarATodos("El envite fue rechazado.");
                mandarATodos("Pareja "+ ((jugador%2)+1)+" ha ganado la grande");
                puntos[jugador % 2] += apuestaActual;
                return false;
            }
        }
    }

    private static boolean gestionarRespuestaEnvidoPequena(int jugador, int apuestaActual,int apuestaLanzada, String mensaje1) {
        mandarATodos("Jugador " + (jugador % 4 + 1) + " ha envidado " + apuestaLanzada + " puntos.");

        mandarMano((jugador + 1) % 4);
        players.get((jugador + 1) % 4).mandarMensaje(mensaje1);
        String respuesta = players.get((jugador + 1) % 4).leerMensaje();
        if (respuesta.equalsIgnoreCase("quiero")) {
            mandarATodos("Jugador " + ((jugador + 1) % 4 + 1) + " ha aceptado el envido.");
            mandarATodos("Jugador "+(getGanadorPequena()+1)+" ha ganado la pequena");
            puntos[getGanadorPequena() % 2] += apuestaLanzada; // Se suman los puntos al equipo contrario
            return false; // Termina el envido
        } else if (respuesta.startsWith("envido ")) {
            try {
                int nuevaApuesta = apuestaLanzada + Integer.parseInt(respuesta.split(" ")[1]);
                mandarATodos("Jugador " + ((jugador + 1) % 4 + 1) + " ha aumentado el envido a " + nuevaApuesta
                        + " puntos.");
                apuestaActual = apuestaLanzada;
                apuestaLanzada=nuevaApuesta;
                gestionarRespuestaEnvidoPequena((jugador + 1) % 4, apuestaActual, apuestaLanzada, mensaje1);
                return false; // Continúa el envido con la nueva apuesta
            } catch (NumberFormatException e) {
                mandarATodos("Jugador " + ((jugador + 1) % 4 + 1) + " ha aceptado el envido.");
                mandarATodos("Jugador "+(getGanadorPequena()+1)+" ha ganado la pequena");
                puntos[getGanadorPequena() % 2] += apuestaLanzada; // Se suman los puntos al equipo contrario
                return false; // Termina el envido
            }
        } else if (respuesta.equalsIgnoreCase("ordago")) {
            return resolverOrdagoPequena((jugador + 1) % 4, apuestaLanzada);
        } else {
            mandarATodos("Jugador " + ((jugador + 1) % 4 + 1) + " ha rechazado el envido.");

            mandarMano((jugador + 3) % 4);
            players.get((jugador + 3) % 4).mandarMensaje(mensaje1);
            respuesta = players.get((jugador + 3) % 4).leerMensaje();

            if (respuesta.equalsIgnoreCase("quiero")) {
                mandarATodos("Jugador " + ((jugador + 3) % 4 + 1) + " ha aceptado el envido.");
                mandarATodos("Jugador "+(getGanadorPequena()+1)+" ha ganado la pequena");
                puntos[getGanadorPequena() % 2] += apuestaLanzada; // Se suman los puntos al equipo contrario
                return false; // Termina el envido
            } else if (respuesta.startsWith("envido ")) {
                try {
                    int nuevaApuesta = apuestaLanzada + Integer.parseInt(respuesta.split(" ")[1]);
                    mandarATodos("Jugador " + ((jugador + 3) % 4 + 1) + " ha aumentado el envido a " + nuevaApuesta
                            + " puntos.");
                    apuestaActual = apuestaLanzada;
                    apuestaLanzada=nuevaApuesta;
                    gestionarRespuestaEnvidoPequena((jugador + 3) % 4, apuestaActual,apuestaLanzada, mensaje1);
                    return false; // Continúa el envido con la nueva apuesta
                } catch (NumberFormatException e) {
                    mandarATodos("Jugador " + ((jugador + 3) % 4 + 1) + " ha aceptado el envido.");
                    mandarATodos("Jugador "+(getGanadorPequena()+1)+" ha ganado la pequena");
                    puntos[getGanadorPequena() % 2] += apuestaLanzada; // Se suman los puntos al equipo contrario
                    return false; // Termina el envido
                }
            } else if (respuesta.equalsIgnoreCase("ordago")) {
                return resolverOrdagoPequena((jugador + 3) % 4, apuestaLanzada);
            } else {
                mandarATodos("El envite fue rechazado.");
                mandarATodos("Pareja "+((jugador%2)+1)+" ha ganado la pequena");
                puntos[jugador % 2] += apuestaActual;
                return false;
            }
        }
    }

    private static boolean gestionarRespuestaEnvidoPares(int jugador, int apuestaActual,int apuestaLanzada, String mensaje1) {
        mandarATodos("Jugador " + (jugador % 4 + 1) + " ha envidado " + apuestaLanzada + " puntos.");
        String respuesta;

        if (conPares.containsKey((jugador + 1) % 4)) {
            mandarMano((jugador + 1) % 4);
            players.get((jugador + 1) % 4).mandarMensaje(mensaje1);
            respuesta = players.get((jugador + 1) % 4).leerMensaje();
            if (respuesta.equalsIgnoreCase("quiero")) {
                mandarATodos("Jugador " + ((jugador + 1) % 4 + 1) + " ha aceptado el envido.");
                mandarATodos("Jugador "+(getGanadorPares()[0]+1)+" ha ganado pares");
                puntos[getGanadorPares()[0] % 2] += apuestaLanzada+getGanadorPares()[1]; // Se suman los puntos al equipo contrario
                return false; // Termina el envido
            } else if (respuesta.startsWith("envido ")) {
                try {
                    int nuevaApuesta = apuestaLanzada + Integer.parseInt(respuesta.split(" ")[1]);
                    mandarATodos("Jugador " + ((jugador + 1) % 4 + 1) + " ha aumentado el envido a " + nuevaApuesta
                            + " puntos.");
                    apuestaActual = apuestaLanzada;
                    apuestaLanzada=nuevaApuesta;
                    gestionarRespuestaEnvidoPares((jugador + 1) % 4, apuestaActual,apuestaLanzada, mensaje1);
                    return false; // Continúa el envido con la nueva apuesta
                } catch (NumberFormatException e) {
                    mandarATodos("Jugador " + ((jugador + 1) % 4 + 1) + " ha aceptado el envido.");
                    mandarATodos("Jugador "+(getGanadorPares()[0]+1)+" ha ganado pares");
                    puntos[getGanadorPares()[0] % 2] += apuestaLanzada+getGanadorPares()[1]; // Se suman los puntos al equipo contrario
                    return false; // Termina el envido
                }
            } else if (respuesta.equalsIgnoreCase("ordago")) {
                return resolverOrdagoPares((jugador + 1) % 4, apuestaLanzada);
            } else {
                mandarATodos("Jugador " + ((jugador + 1) % 4 + 1) + " ha rechazado el envido.");
            }
        }
        if (conPares.containsKey((jugador + 3) % 4)) {
            mandarMano((jugador + 3) % 4);
            players.get((jugador + 3) % 4).mandarMensaje(mensaje1);
            respuesta = players.get((jugador + 3) % 4).leerMensaje();

            if (respuesta.equalsIgnoreCase("quiero")) {
                mandarATodos("Jugador " + ((jugador + 3) % 4 + 1) + " ha aceptado el envido.");
                mandarATodos("Jugador "+(getGanadorPares()[0]+1)+" ha ganado pares");
                puntos[getGanadorPares()[0] % 2] += apuestaLanzada+getGanadorPares()[1]; // Se suman los puntos al equipo contrario
                return false; // Termina el envido
            } else if (respuesta.startsWith("envido ")) {
                try {
                    int nuevaApuesta = apuestaLanzada + Integer.parseInt(respuesta.split(" ")[1]);
                    mandarATodos("Jugador " + ((jugador + 3) % 4 + 1) + " ha aumentado el envido a " + nuevaApuesta
                            + " puntos.");
                    apuestaActual = apuestaLanzada;
                    apuestaLanzada=nuevaApuesta;
                    gestionarRespuestaEnvidoPares((jugador + 3) % 4, apuestaActual,apuestaLanzada, mensaje1);
                    return false; // Continúa el envido con la nueva apuesta
                } catch (NumberFormatException e) {
                    mandarATodos("Jugador " + ((jugador + 3) % 4 + 1) + " ha aceptado el envido.");
                    mandarATodos("Jugador "+(getGanadorPares()[0]+1)+" ha ganado pares");
                    puntos[getGanadorPares()[0] % 2] += apuestaLanzada+getGanadorPares()[1]; // Se suman los puntos al equipo contrario
                    return false; // Termina el envido
                }
            } else if (respuesta.equalsIgnoreCase("ordago")) {
                return resolverOrdagoPares((jugador + 3) % 4, apuestaLanzada);
            } else {
                mandarATodos("Jugador " + ((jugador + 3) % 4 + 1) + " ha rechazado el envido.");
                mandarATodos("El envite fue rechazado.");
                int compaJugador=(jugador+2)%4;
                if(conPares.get(compaJugador)==null) {
                    puntos[jugador%2]+=apuestaActual+evaluarTipoPares(manosJugadores.get(jugador));
                }else{
                    puntos[jugador%2]+=apuestaActual+evaluarTipoPares(manosJugadores.get(jugador))+evaluarTipoPares(manosJugadores.get(compaJugador));
                }
                mandarATodos("Pareja "+((jugador%2)+1)+" ha ganado pares");
                return false;
            }
        }else{
            mandarATodos("El envite fue rechazado.");
            int compaJugador=(jugador+2)%4;
            if(conPares.get(compaJugador)==null) {
                puntos[jugador%2]+=apuestaActual+evaluarTipoPares(manosJugadores.get(jugador));
            }else{
                puntos[jugador%2]+=apuestaActual+evaluarTipoPares(manosJugadores.get(jugador))+evaluarTipoPares(manosJugadores.get(compaJugador));
            }
            mandarATodos("Jugador "+((jugador%2)+1)+" ha ganado pares");
            return false;
        }
    }

    private static boolean gestionarRespuestaEnvidoJuego(int jugador, int apuestaActual,int apuestaLanzada, String mensaje1) {
        mandarATodos("Jugador " + (jugador % 4 + 1) + " ha envidado " + apuestaLanzada + " puntos.");
        String respuesta;

        if (conJuego.containsKey((jugador + 1) % 4)) {
            mandarMano((jugador + 1) % 4);
            players.get((jugador + 1) % 4).mandarMensaje(mensaje1);
            respuesta = players.get((jugador + 1) % 4).leerMensaje();
            if (respuesta.equalsIgnoreCase("quiero")) {
                mandarATodos("Jugador " + ((jugador + 1) % 4 + 1) + " ha aceptado el envido.");
                mandarATodos("Jugador "+(getGanadorJuego()[0]+1)+" ha ganado el juego");
                puntos[getGanadorJuego()[0] % 2] += apuestaLanzada+getGanadorJuego()[1]; // Se suman los puntos al equipo contrario
                return false; // Termina el envido
            } else if (respuesta.startsWith("envido ")) {
                try {
                    int nuevaApuesta = apuestaLanzada + Integer.parseInt(respuesta.split(" ")[1]);
                    mandarATodos("Jugador " + ((jugador + 1) % 4 + 1) + " ha aumentado el envido a " + nuevaApuesta
                            + " puntos.");
                    apuestaActual = apuestaLanzada;
                    apuestaLanzada=nuevaApuesta;
                    gestionarRespuestaEnvidoJuego((jugador + 1) % 4, apuestaActual,apuestaLanzada, mensaje1);
                    return false; // Continúa el envido con la nueva apuesta
                } catch (NumberFormatException e) {
                    mandarATodos("Jugador " + ((jugador + 1) % 4 + 1) + " ha aceptado el envido.");
                    mandarATodos("Jugador "+(getGanadorJuego()[0]+1)+" ha ganado el juego");
                    puntos[getGanadorJuego()[0] % 2] += apuestaLanzada+getGanadorJuego()[1]; // Se suman los puntos al equipo contrario
                    return false; // Termina el envido
                }
            } else if (respuesta.equalsIgnoreCase("ordago")) {
                return resolverOrdagoJuego((jugador + 1) % 4, apuestaLanzada);
            } else {
                mandarATodos("Jugador " + ((jugador + 1) % 4 + 1) + " ha rechazado el envido.");
            }
        }
        if (conJuego.containsKey((jugador + 3) % 4)) {
            mandarMano((jugador + 3) % 4);
            players.get((jugador + 3) % 4).mandarMensaje(mensaje1);
            respuesta = players.get((jugador + 3) % 4).leerMensaje();

            if (respuesta.equalsIgnoreCase("quiero")) {
                mandarATodos("Jugador " + ((jugador + 3) % 4 + 1) + " ha aceptado el envido.");
                mandarATodos("Jugador "+(getGanadorJuego()[0]+1)+" ha ganado el juego");
                puntos[getGanadorJuego()[0] % 2] += apuestaLanzada+getGanadorJuego()[1]; // Se suman los puntos al equipo contrario
                return false; // Termina el envido
            } else if (respuesta.startsWith("envido ")) {
                try {
                    int nuevaApuesta = apuestaLanzada + Integer.parseInt(respuesta.split(" ")[1]);
                    mandarATodos("Jugador " + ((jugador + 3) % 4 + 1) + " ha aumentado el envido a " + nuevaApuesta
                            + " puntos.");
                    apuestaActual = apuestaLanzada;
                    apuestaLanzada=nuevaApuesta;
                    gestionarRespuestaEnvidoJuego((jugador + 3) % 4, apuestaActual,apuestaLanzada, mensaje1);
                    return false; // Continúa el envido con la nueva apuesta
                } catch (NumberFormatException e) {
                    mandarATodos("Jugador " + ((jugador + 3) % 4 + 1) + " ha aceptado el envido.");
                    mandarATodos("Jugador "+(getGanadorJuego()[0]+1)+" ha ganado el juego");
                    puntos[getGanadorJuego()[0] % 2] += apuestaLanzada+getGanadorJuego()[1]; // Se suman los puntos al equipo contrario
                    return false; // Termina el envido
                }
            } else if (respuesta.equalsIgnoreCase("ordago")) {
                return resolverOrdagoJuego((jugador + 3) % 4, apuestaLanzada);
            } else {
                mandarATodos("Jugador " + ((jugador + 3) % 4 + 1) + " ha rechazado el envido.");
                mandarATodos("El envite fue rechazado.");
                int compaJugador=(jugador+2)%4;
                int cantidadGanada=getPuntosMano(conJuego.get(jugador)) ==31 ? 3 :2;
                if(conJuego.get(compaJugador)!=null) {
                    cantidadGanada+=getPuntosMano(conJuego.get(compaJugador)) ==31 ? 3 :2;
                }
                puntos[jugador%2]+=apuestaActual+cantidadGanada;
                mandarATodos("Pareja "+((jugador%2)+1)+" ha ganado el juego");
                return false;
            }
        }else{
            mandarATodos("El envite fue rechazado.");
            int compaJugador=(jugador+2)%4;
            int cantidadGanada=getPuntosMano(conJuego.get(jugador)) ==31 ? 3 :2;
            if(conJuego.get(compaJugador)!=null) {
                cantidadGanada+=getPuntosMano(conJuego.get(compaJugador)) ==31 ? 3 :2;
            }
            puntos[jugador%2]+=apuestaActual+cantidadGanada;
            mandarATodos("Pareja "+((jugador%2)+1)+" ha ganado el juego");
            return false;
        }
    }

    private static boolean gestionarRespuestaEnvidoPunto(int jugador, int apuestaActual,int apuestaLanzada, String mensaje1) {
        mandarATodos("Jugador " + (jugador % 4 + 1) + " ha envidado " + apuestaLanzada + " puntos.");

        mandarMano((jugador + 1) % 4);
        players.get((jugador + 1) % 4).mandarMensaje(mensaje1);
        String respuesta = players.get((jugador + 1) % 4).leerMensaje();
        if (respuesta.equalsIgnoreCase("quiero")) {
            mandarATodos("Jugador " + ((jugador + 1) % 4 + 1) + " ha aceptado el envido.");
            mandarATodos("Jugador "+(getGanadorPunto()+1)+" ha ganado el punto");
            puntos[getGanadorPunto() % 2] += apuestaLanzada; // Se suman los puntos al equipo contrario
            return false; // Termina el envido
        } else if (respuesta.startsWith("envido ")) {
            try {
                int nuevaApuesta = apuestaLanzada + Integer.parseInt(respuesta.split(" ")[1]);
                mandarATodos("Jugador " + ((jugador + 1) % 4 + 1) + " ha aumentado el envido a " + nuevaApuesta
                        + " puntos.");
                apuestaActual = apuestaLanzada;
                apuestaLanzada=nuevaApuesta;
                gestionarRespuestaEnvidoPunto((jugador + 1) % 4, apuestaActual,apuestaLanzada, mensaje1);
                return false; // Continúa el envido con la nueva apuesta
            } catch (NumberFormatException e) {
                mandarATodos("Jugador " + ((jugador + 1) % 4 + 1) + " ha aceptado el envido.");
                mandarATodos("Jugador "+(getGanadorPunto()+1)+" ha ganado el punto");
                puntos[getGanadorPunto() % 2] += apuestaLanzada; // Se suman los puntos al equipo contrario
                return false; // Termina el envido
            }
        } else if (respuesta.equalsIgnoreCase("ordago")) {
            return resolverOrdagoPunto((jugador + 1) % 4, apuestaLanzada);
        } else {
            mandarATodos("Jugador " + ((jugador + 1) % 4 + 1) + " ha rechazado el envido.");

            mandarMano((jugador + 3) % 4);
            players.get((jugador + 3) % 4).mandarMensaje(mensaje1);
            respuesta = players.get((jugador + 3) % 4).leerMensaje();

            if (respuesta.equalsIgnoreCase("quiero")) {
                mandarATodos("Jugador " + ((jugador + 3) % 4 + 1) + " ha aceptado el envido.");
                mandarATodos("Jugador "+(getGanadorPunto()+1)+" ha ganado el punto");
                puntos[getGanadorPunto() % 2] += apuestaLanzada; // Se suman los puntos al equipo contrario
                return false; // Termina el envido
            } else if (respuesta.startsWith("envido ")) {
                try {
                    int nuevaApuesta = apuestaLanzada + Integer.parseInt(respuesta.split(" ")[1]);
                    mandarATodos("Jugador " + ((jugador + 3) % 4 + 1) + " ha aumentado el envido a " + nuevaApuesta
                            + " puntos.");
                    apuestaActual = apuestaLanzada;
                    apuestaLanzada=nuevaApuesta;
                    gestionarRespuestaEnvidoPunto((jugador + 3) % 4, apuestaActual,apuestaLanzada, mensaje1);
                    return false; // Continúa el envido con la nueva apuesta
                } catch (NumberFormatException e) {
                    mandarATodos("Jugador " + ((jugador + 3) % 4 + 1) + " ha aceptado el envido.");
                    mandarATodos("Jugador "+(getGanadorPunto()+1)+" ha ganado el punto");
                    puntos[getGanadorPunto() % 2] += apuestaLanzada; // Se suman los puntos al equipo contrario
                    return false; // Termina el envido
                }
            } else if (respuesta.equalsIgnoreCase("ordago")) {
                return resolverOrdagoPunto((jugador + 3) % 4, apuestaLanzada);
            } else {
                mandarATodos("El envite fue rechazado.");
                mandarATodos("Pareja "+((jugador%2)+1)+" ha ganado el punto");
                puntos[jugador % 2] += apuestaActual;
                return false;
            }
        }
    }

    private static boolean todosPasaron(int jugadorActual) {
        mandarATodos("Jugador " + (jugadorActual + 1) % 4 + " ha pasado.");
        return jugadorActual == (jugadorMano + 3) % 4;
    }

    private static boolean resolverOrdagoGrande(int jugador, int apuestaActual) {
        mandarATodos("Jugador " + (jugador + 1) + " ha lanzado un ordago!");

        mandarMano((jugador + 1) % 4);
        players.get((jugador + 1) % 4).mandarMensaje("Quieres el ordago? (si/no)");
        String respuesta = players.get((jugador + 1) % 4).leerMensaje();
        if (respuesta.equalsIgnoreCase("si")) {
            mandarATodos("Ordago aceptado! Fin del juego.");
            puntos[getGanadorGrande() % 2] = 25; // Asigna los puntos al equipo ganador
            return false;
        } else {
            mandarATodos("Jugador " + ((jugador + 1) % 4 +1)+ " no ha querido el ordago");
            mandarMano((jugador + 3) % 4);
            players.get((jugador + 3) % 4).mandarMensaje("Quieres el ordago? (si/no)");
            respuesta = players.get((jugador + 3) % 4).leerMensaje();
            if (respuesta.equalsIgnoreCase("si")) {
                mandarATodos("Ordago aceptado! Fin del juego.");
                puntos[getGanadorGrande() % 2] = 25; // Asigna los puntos al equipo ganador
                return false;
            } else {
                mandarATodos("El ordago fue rechazado.");
                mandarATodos("Pareja "+((jugador%2)+1)+" ha ganado grande");
                puntos[jugador % 2] += apuestaActual;
                return false;
            }
        }
    }

    private static boolean resolverOrdagoPequena(int jugador, int apuestaActual) {
        mandarATodos("Jugador " + (jugador + 1) + " ha lanzado un ordago!");

        mandarMano((jugador + 1) % 4);
        players.get((jugador + 1) % 4).mandarMensaje("Quieres el ordago? (si/no)");
        String respuesta = players.get((jugador + 1) % 4).leerMensaje();
        if (respuesta.equalsIgnoreCase("si")) {
            mandarATodos("Ordago aceptado! Fin del juego.");
            puntos[getGanadorPequena() % 2] = 25; // Asigna los puntos al equipo ganador
            return false;
        } else {
            mandarATodos("Jugador " + ((jugador + 1) % 4+1) + " no ha querido el ordago");
            mandarMano((jugador + 3) % 4);
            players.get((jugador + 3) % 4).mandarMensaje("Quieres el ordago? (si/no)");
            respuesta = players.get((jugador + 3) % 4).leerMensaje();
            if (respuesta.equalsIgnoreCase("si")) {
                mandarATodos("Ordago aceptado! Fin del juego.");
                puntos[getGanadorPequena() % 2] = 25; // Asigna los puntos al equipo ganador
                return false;
            } else {
                mandarATodos("El ordago fue rechazado.");
                mandarATodos("Pareja "+((jugador%2)+1)+" ha ganado pequena");
                puntos[jugador % 2] += apuestaActual;
                return false;
            }
        }
    }

    private static boolean resolverOrdagoPares(int jugador, int apuestaActual) {
        mandarATodos("Jugador " + (jugador + 1) + " ha lanzado un ordago!");
        String respuesta;

        if (conPares.containsKey((jugador + 1) % 4)) {
            mandarMano((jugador + 1) % 4);
            players.get((jugador + 1) % 4).mandarMensaje("Quieres el ordago? (si/no)");
            respuesta = players.get((jugador + 1) % 4).leerMensaje();
            if (respuesta.equalsIgnoreCase("si")) {
                mandarATodos("Ordago aceptado! Fin del juego.");
                puntos[getGanadorPares()[0] % 2] = 25; // Asigna los puntos al equipo ganador
                return false;
            } else{
                mandarATodos("Jugador " + ((jugador + 1) % 4 + 1) + " no ha querido el ordago");
            }
        }
        if (conPares.containsKey((jugador + 3) % 4)) {
            mandarMano((jugador + 3) % 4);
            players.get((jugador + 3) % 4).mandarMensaje("Quieres el ordago? (si/no)");
            respuesta = players.get((jugador + 3) % 4).leerMensaje();
            if (respuesta.equalsIgnoreCase("si")) {
                mandarATodos("Ordago aceptado! Fin del juego.");
                puntos[getGanadorPares()[0] % 2] = 25; // Asigna los puntos al equipo ganador
                return false;
            } else {
                mandarATodos("Jugador " + ((jugador + 3) % 4 + 1) + " no ha querido el ordago");
                mandarATodos("El ordago fue rechazado.");
                int compaJugador=(jugador+2)%4;
                if(conPares.get(compaJugador)==null) {
                    puntos[jugador%2]+=apuestaActual+evaluarTipoPares(manosJugadores.get(jugador));
                }else{
                    puntos[jugador%2]+=apuestaActual+evaluarTipoPares(manosJugadores.get(jugador))+evaluarTipoPares(manosJugadores.get(compaJugador));
                }
                mandarATodos("Pareja "+((jugador%2)+1)+" ha ganado pares");
                return false;
            }
        } else {
            mandarATodos("El ordago fue rechazado.");
            int compaJugador=(jugador+2)%4;
            if(conPares.get(compaJugador)==null) {
                puntos[jugador%2]+=apuestaActual+evaluarTipoPares(manosJugadores.get(jugador));
            }else{
                puntos[jugador%2]+=apuestaActual+evaluarTipoPares(manosJugadores.get(jugador))+evaluarTipoPares(manosJugadores.get(compaJugador));
            }
            mandarATodos("Pareja "+((jugador%2)+1)+" ha ganado pares");
            return false;
        }
    }

    private static boolean resolverOrdagoJuego(int jugador, int apuestaActual) {
        mandarATodos("Jugador " + (jugador + 1) + " ha lanzado un ordago!");
        String respuesta;

        if (conJuego.containsKey((jugador + 1) % 4)) {
            mandarMano((jugador + 1) % 4);
            players.get((jugador + 1) % 4).mandarMensaje("Quieres el ordago? (si/no)");
            respuesta = players.get((jugador + 1) % 4).leerMensaje();
            if (respuesta.equalsIgnoreCase("si")) {
                mandarATodos("Ordago aceptado! Fin del juego.");
                puntos[getGanadorJuego()[0] % 2] = 25; // Asigna los puntos al equipo ganador
                return false;
            } else{
                mandarATodos("Jugador " + ((jugador + 1) % 4 + 1) + " no ha querido el ordago");
            }
        }
        if (conJuego.containsKey((jugador + 3) % 4)) {
            mandarMano((jugador + 3) % 4);
            players.get((jugador + 3) % 4).mandarMensaje("Quieres el ordago? (si/no)");
            respuesta = players.get((jugador + 3) % 4).leerMensaje();
            if (respuesta.equalsIgnoreCase("si")) {
                mandarATodos("Ordago aceptado! Fin del juego.");
                puntos[getGanadorJuego()[0] % 2] = 25; // Asigna los puntos al equipo ganador
                return false;
            } else {
                mandarATodos("Jugador " + ((jugador + 3) % 4 + 1) + " no ha querido el ordago");
                mandarATodos("El ordago fue rechazado.");
                int compaJugador=(jugador+2)%4;
                int cantidadGanada=getPuntosMano(conJuego.get(jugador)) ==31 ? 3 :2;
                if(conJuego.get(compaJugador)!=null) {
                    cantidadGanada+=getPuntosMano(conJuego.get(compaJugador)) ==31 ? 3 :2;
                }
                puntos[jugador%2]+=apuestaActual+cantidadGanada;
                mandarATodos("Pareja "+((jugador%2)+1)+" ha ganado el juego");
                return false;
            }
        } else {
            mandarATodos("El ordago fue rechazado.");
            int compaJugador=(jugador+2)%4;
            int cantidadGanada=getPuntosMano(conJuego.get(jugador)) ==31 ? 3 :2;
            if(conJuego.get(compaJugador)!=null) {
                cantidadGanada+=getPuntosMano(conJuego.get(compaJugador)) ==31 ? 3 :2;
            }
            puntos[jugador%2]+=apuestaActual+cantidadGanada;
            mandarATodos("Pareja "+((jugador%2)+1)+" ha ganado el juego");
            return false;
        }
    }

    private static boolean resolverOrdagoPunto(int jugador, int apuestaActual) {
        mandarATodos("Jugador " + (jugador + 1) + " ha lanzado un ordago!");

        mandarMano((jugador + 1) % 4);
        players.get((jugador + 1) % 4).mandarMensaje("Quieres el ordago? (si/no)");
        String respuesta = players.get((jugador + 1) % 4).leerMensaje();
        if (respuesta.equalsIgnoreCase("si")) {
            mandarATodos("Ordago aceptado! Fin del juego.");
            puntos[getGanadorPunto() % 2] = 25; // Asigna los puntos al equipo ganador
            return false;
        } else {
            mandarATodos("Jugador " + ((jugador + 1) % 4 +1)+ " no ha querido el ordago");
            mandarMano((jugador + 3) % 4);
            players.get((jugador + 3) % 4).mandarMensaje("Quieres el ordago? (si/no)");
            respuesta = players.get((jugador + 3) % 4).leerMensaje();
            if (respuesta.equalsIgnoreCase("si")) {
                mandarATodos("Ordago aceptado! Fin del juego.");
                puntos[getGanadorPunto() % 2] = 25; // Asigna los puntos al equipo ganador
                return false;
            } else {
                mandarATodos("El ordago fue rechazado.");
                mandarATodos("Pareja "+((jugador%2)+1)+" ha ganado el punto");
                puntos[jugador % 2] += apuestaActual;
                return false;
            }
        }
    }

    private static int getGanadorGrande() {
        for (int i = 0; i < 4; i++) {
            int manoGanadora = -1;
            int valorMayor = -1;
            jugadorActual = jugadorMano;
            // Buscar la carta más alta en la posición i
            for (int j = 0; j < 4; j++) {

                int valorCarta = manosJugadores.get(jugadorActual).get(i).getValorNumerico();
                if (valorCarta > valorMayor) {
                    valorMayor = valorCarta;
                    manoGanadora = jugadorActual;
                } else if (valorCarta == valorMayor) {
                    manoGanadora = -1; // Si hay empate en esta posición, seguimos comparando
                }
                jugadorActual = (jugadorActual + 1) % 4;
            }

            // Si hemos encontrado una mano ganadora en esta posición, retornamos
            if (manoGanadora != -1) {
                return manoGanadora;
            }
        }

        return jugadorMano;
    }

    private static int getGanadorPequena() {
        for (int i = 3; i >= 0; i--) {
            int manoGanadora = -1;
            int valorMenor = 11;
            jugadorActual = jugadorMano;
            // Buscar la carta más alta en la posición i
            for (int j = 0; j < 4; j++) {

                int valorCarta = manosJugadores.get(jugadorActual).get(i).getValorNumerico();
                if (valorCarta < valorMenor) {
                    valorMenor = valorCarta;
                    manoGanadora = jugadorActual;
                } else if (valorCarta == valorMenor) {
                    manoGanadora = 11; // Si hay empate en esta posición, seguimos comparando
                }
                jugadorActual = (jugadorActual + 1) % 4;
            }

            // Si hemos encontrado una mano ganadora en esta posición, retornamos
            if (manoGanadora != 11) {
                return manoGanadora;
            }
        }

        return jugadorMano;
    }

    private static int[] getGanadorPares() {
        int jugadorGanador = -1;
        int mejorTipoPares = -1;

        for (Map.Entry<Integer, List<Card>> entry : conPares.entrySet()) {
            int jugador = entry.getKey();
            List<Card> mano = entry.getValue();
            int tipoPares = evaluarTipoPares(mano);

            // Comparar el tipo de pares del jugador actual con el mejor encontrado hasta ahora
            if (tipoPares > mejorTipoPares) {
                mejorTipoPares = tipoPares;
                jugadorGanador = jugador;
            } else if (tipoPares == mejorTipoPares) {
                jugadorGanador = compararValoresPares(jugadorGanador, jugador,tipoPares);
            }
        }
        int compaGanador=(jugadorGanador+2)%4;
        if(conPares.get(compaGanador)==null) return new int[]{jugadorGanador,mejorTipoPares};
        mejorTipoPares+=evaluarTipoPares(conPares.get(compaGanador));
        return new int[]{jugadorGanador,mejorTipoPares};        
    }

    private static int getGanadorPunto() {
        int manoGanadora = -1;
        int valorMayor = -1;
        jugadorActual = jugadorMano;

        for (int i = 0; i < 4; i++) {
            int valorMano = getPuntosMano(manosJugadores.get(jugadorActual));
            if (valorMano > valorMayor) {
                valorMayor = valorMano;
                manoGanadora = jugadorActual;
            }
            jugadorActual = (jugadorActual + 1) % 4;
        }

        return manoGanadora;
    }

    private static int[] getGanadorJuego() {
        int jugadorGanador = -1;
        int mejorvalorJuego = -1;
        int mejorJuego=-1;

        for (Map.Entry<Integer, List<Card>> entry : conJuego.entrySet()) {
            int jugador = entry.getKey();
            List<Card> mano = entry.getValue();
            int juego=getPuntosMano(mano);
            int valorJuego = getValorJuego(mano);

            // Comparar el valor de juego del jugador actual con el mejor encontrado hasta ahora
            if (valorJuego > mejorvalorJuego) {
                mejorvalorJuego = valorJuego;
                mejorJuego=juego;
                jugadorGanador = jugador;
            }
        }
        int cantidadGanada = mejorJuego==31 ? 3 : 2;

        int compaGanador=(jugadorGanador+2)%4;
        if(conJuego.get(compaGanador)==null) return new int[]{jugadorGanador,cantidadGanada};

        cantidadGanada += getPuntosMano(conJuego.get(compaGanador)) ==31 ? 3 :2;
        return new int[]{jugadorGanador,cantidadGanada};  
    }

    // Método para clasificar el tipo de pares en la mano: 3 = duples, 2 = medias, 1 = pares
    private static int evaluarTipoPares(List<Card> mano) {
        Map<Integer, Integer> contadorValores = new HashMap<>();
        for (Card carta : mano) {
            int valor = carta.getValorNumerico();
            contadorValores.put(valor, contadorValores.getOrDefault(valor, 0) + 1);
        }

        boolean tieneDuples = contadorValores.containsValue(2) && contadorValores.size() == 2;
        boolean tieneMedias = contadorValores.containsValue(3);

        if (tieneDuples) {
            return 3; // Duples
        }
        if (tieneMedias) {
            return 2; // Medias
        }
        return 1; // Pares
    }

    // Método para comparar las manos de jugadores con el mismo tipo de pares
    private static int compararValoresPares(int jugador1, int jugador2,int tipoPares) {
        List<Card> mano1 = conPares.get(jugador1);
        List<Card> mano2 = conPares.get(jugador2);

        switch (tipoPares) {
            case 3:
                if(mano1.get(0).getValorNumerico()>mano2.get(0).getValorNumerico()){
                    return jugador1;
                }else if(mano1.get(0).getValorNumerico()<mano2.get(0).getValorNumerico()){
                    return jugador2;
                }else{
                    if(mano1.get(2).getValorNumerico()<mano2.get(2).getValorNumerico()){
                        return jugador2;
                    }else {
                        return jugador1;
                    }
                }
            case 2:
                if(mano1.get(1).getValorNumerico()<mano2.get(1).getValorNumerico()){
                    return jugador2;
                }else {
                    return jugador1;
                }
            case 1:
                if(mano1.get(getPosicionPareja(mano1)).getValorNumerico()<mano2.get(getPosicionPareja(mano2)).getValorNumerico()){
                    return jugador2;
                }else{
                    return jugador1;
                }
            default: return jugador1;
        }
    }

    private static int getPosicionPareja(List<Card> mano){
        for (int i = 0; i < mano.size()-1; i++) {
            int valor1 = mano.get(i).getValorNumerico();
            int valor2 = mano.get(i+1).getValorNumerico();
            if (valor1 == valor2) {
                return i;
            }
        }
        return 0;
    }

    private static int getPuntosMano(List<Card> mano) {
        int totalValue = 0;
        for (Card card : mano) {
            totalValue += card.getPuntos();
        }
        return totalValue;
    }

    private static int getValorJuego(List<Card> mano){
        Map<Integer, Integer> valorMap = new HashMap<>();
        valorMap.put(33, 1);
        valorMap.put(34, 3);
        valorMap.put(35, 4);
        valorMap.put(36, 5);
        valorMap.put(37, 6);
        valorMap.put(38, 7);
        valorMap.put(39, 8);
        valorMap.put(40,9);
        valorMap.put(32, 10);
        valorMap.put(31, 11);

        return valorMap.getOrDefault(getPuntosMano(mano), 0);
    }

    private static void sumarPaso() {
        if (grandeAlPaso) {
            mandarATodos("Jugador "+(getGanadorGrande()+1)+" ha ganado grande al paso");
            puntos[getGanadorGrande() % 2]++;
        }
        if (pequenaAlPaso) {
            mandarATodos("Jugador "+(getGanadorPequena()+1)+" ha ganado pequena al paso");
            puntos[getGanadorPequena() % 2]++;
        }
        if (paresAlPaso) {
            mandarATodos("Jugador "+(getGanadorPares()[0]+1)+" ha ganado pares al paso");
            puntos[getGanadorPares()[0] % 2]+=getGanadorPares()[1];
        }
        if (puntoAlPaso) {
            mandarATodos("Jugador "+(getGanadorPunto()+1)+" ha ganado el punto al paso");
            puntos[getGanadorPunto() %2]++;
        }
        if (juegoAlPaso) {
            mandarATodos("Jugador "+(getGanadorJuego()[0]+1)+" ha ganado el juego al paso");
            puntos[getGanadorJuego()[0]%2]+=getGanadorJuego()[1];
        }
    }

    private static void mandarATodos(String mensaje) {
        for (int i = 0; i < 4; i++) {
            players.get(i).mandarMensaje(mensaje);
        }
    }

    private static void mostrarPuntos(){
        String mensaje ="Pareja 1: "+puntos[0]+" puntos \r\n";
        mensaje +="Pareja 2: "+puntos[1]+" puntos \r\n";
        mandarATodos(mensaje);
    }
}
