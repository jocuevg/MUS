package Servidor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Logic {
    private List<Card> mazo;
    private List<List<Card>> manosJugadores;
    private int[] puntos;
    private int ronda;
    private int jugadorTurno;

    private int apuesta;           // Apuesta actual en la ronda
    private int ganador;         // ID del jugador que inició la última apuesta
    private boolean ordago;            // Si se ha echado un órdago
    private int[] teamPoints;          // Puntos de cada equipo
    private boolean isRoundActive;     // Estado de la ronda

    public Logic(int numPlayers) {
        this.mazo = new ArrayList<>();
        this.manosJugadores = new ArrayList<>();
        this.puntos = new int[2]; // Dos equipos
        this.ronda = 0;
        this.jugadorTurno = 0;

        crearMazo();
        repartir(numPlayers);
    }

    private void crearMazo() {
        mazo.clear();
        String[] palos = {"Oros", "Copas", "Espadas", "Bastos"};
        String[] valores = {"As", "Dos", "Tres", "Cuatro", "Cinco", "Seis", "Sota", "Caballo", "Rey"};
        
        for (String palo : palos) {
            for (String valor : valores) {
                mazo.add(new Card(palo, valor));
            }
        }
        Collections.shuffle(mazo);
    }

    private void repartir(int numPlayers) {
        for (int i = 0; i < numPlayers; i++) {
            List<Card> mano = new ArrayList<>();
            for (int j = 0; j < 4; j++) {
                mano.add(mazo.remove(0));
            }
            manosJugadores.add(mano);
        }
    }

    public List<Card> getManoJugador(int playerId) {
        return manosJugadores.get(playerId);
    }

    public void empazarRonda() {
        ronda++;
        jugadorTurno = 0; // El primer jugador comienza
        // Iniciar categorías de apuestas (Grande, Chica, Pares, Juego)
        Grande();
        Chica();
        Pares();
        Juego();
    }

    private void Grande() {
        // Lógica para Grande
        System.out.println("Jugando Grande...");
        int mayorValor = -1;
        int ganador = -1;

        for (int i = 0; i < manosJugadores.size(); i++) {
            int value = calculateHandValue(manosJugadores.get(i)); // True para valor alto
            if (value > mayorValor) {
                mayorValor = value;
                ganador = i;
            }
        }
        System.out.println("Ganador de Grande: Jugador " + (ganador + 1));
        sumarPuntos(ganador % 2, 1);  // Sumar puntos al equipo del ganador
    }

    private void Chica() {
        // Lógica para Chica
        System.out.println("Jugando Chica...");

        int lowestValue = Integer.MAX_VALUE;
        int winner = -1;

        for (int i = 0; i < manosJugadores.size(); i++) {
            int value = calculateHandValue(manosJugadores.get(i)); // False para valor bajo
            if (value < lowestValue) {
                lowestValue = value;
                winner = i;
            }
        }
        System.out.println("Ganador de Chica: Jugador " + (winner + 1));
        sumarPuntos(winner % 2, 1);  // Sumar puntos al equipo del ganador
    }

    private void Pares() {
        // Lógica para Pares
        System.out.println("Jugando Pares...");
        for (int i = 0; i < manosJugadores.size(); i++) {
            if (tienePares(manosJugadores.get(i))) {
                System.out.println("Jugador " + (i + 1) + " tiene pares.");
                sumarPuntos(i % 2, 1);
            }
        }
    }

    private void Juego() {
        // Lógica para Juego o Punto
        System.out.println("Jugando Juego...");
        for (int i = 0; i < manosJugadores.size(); i++) {
            int total = calculateHandValue(manosJugadores.get(i));
            if (total >= 31) {
                System.out.println("Jugador " + (i + 1) + " tiene Juego.");
                sumarPuntos(i % 2, 2); // Juego vale 2 puntos
            } else {
                System.out.println("Jugador " + (i + 1) + " tiene Punto.");
                sumarPuntos(i % 2, 1); // Punto vale 1 punto
            }
        }
    }

    public void sumarPuntos(int pareja, int puntos) {
        this.puntos[pareja] += puntos;
    }

    public int[] getPuntos() {
        return this.puntos;
    }

    private int calculateHandValue(List<Card> hand) {
        int totalValue = 0;
        for (Card card : hand) {
            totalValue += getCardValue(card);
        }
        return totalValue;
    }
    
    private int getCardValue(Card card) {
        switch (card.getValor()) {
            case "As": return 1;
            case "Dos": return 2;
            case "Tres": return 3;
            case "Cuatro": return 4;
            case "Cinco": return 5;
            case "Seis": return 6;
            case "Sota": return 7;
            case "Caballo": return 8;
            case "Rey": return 9;
            default: return 0;
        }
    }
    
}
