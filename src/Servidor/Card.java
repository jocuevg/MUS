package Servidor;

import java.util.HashMap;
import java.util.Map;

public class Card {
    private String palo;
    private String valor;

    public Card(String palo, String valor) {
        this.palo = palo;
        this.valor = valor;
    }

    public String getPalo() {
        return palo;
    }

    public String getValor() {
        return valor;
    }

    public int getValorNumerico() {
        Map<String, Integer> valorMap = new HashMap<>();
        valorMap.put("As", 1);
        valorMap.put("Tres", 3);
        valorMap.put("Cuatro", 4);
        valorMap.put("Cinco", 5);
        valorMap.put("Seis", 6);
        valorMap.put("Siete", 7);
        valorMap.put("Sota", 8);
        valorMap.put("Caballo", 9);
        valorMap.put("Rey", 10);

        return valorMap.getOrDefault(valor, 0);
    }

    @Override
    public String toString() {
        return valor + " de " + palo;
    }
}
