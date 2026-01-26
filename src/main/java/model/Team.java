package model;

import javafx.scene.paint.Color;

/**
 * Repraesentiert ein Formel-1-Rennteam mit zwei Fahrern.
 * Jedes Team hat einen eindeutigen Namen, eine Farbe fuer die
 * visuelle Darstellung und genau zwei Fahrer.
 *
 * Die Teamfarbe wird fuer die Darstellung der Autos und
 * teamspezifischer Elemente in der GUI verwendet.
 *
 * @author F1 Simulation Team
 * @version 1.0
 */
public class Team {

    private final String name;
    private final Color farbe;
    private final Fahrer fahrer1;
    private final Fahrer fahrer2;

    /**
     * Erstellt ein neues Team mit den angegebenen Eigenschaften.
     *
     * @Vorbedingung name darf nicht null oder leer sein
     * @Vorbedingung farbe darf nicht null sein
     * @Vorbedingung fahrer1 und fahrer2 duerfen nicht null sein
     * @Vorbedingung fahrer1 und fahrer2 muessen unterschiedlich sein
     * @Nachbedingung Team-Objekt ist mit den angegebenen Werten erstellt
     *
     * @param name Teamname (z.B. "Red Bull Racing")
     * @param farbe Teamfarbe fuer die Visualisierung
     * @param fahrer1 Erster Fahrer des Teams
     * @param fahrer2 Zweiter Fahrer des Teams
     * @throws IllegalArgumentException wenn Parameter ungueltig sind
     */
    public Team(String name, Color farbe, Fahrer fahrer1, Fahrer fahrer2) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Teamname darf nicht leer sein");
        }
        if (farbe == null) {
            throw new IllegalArgumentException("Teamfarbe darf nicht null sein");
        }
        if (fahrer1 == null || fahrer2 == null) {
            throw new IllegalArgumentException("Fahrer duerfen nicht null sein");
        }
        if (fahrer1.equals(fahrer2)) {
            throw new IllegalArgumentException("Fahrer muessen unterschiedlich sein");
        }

        this.name = name;
        this.farbe = farbe;
        this.fahrer1 = fahrer1;
        this.fahrer2 = fahrer2;
    }

    /**
     * Prueft ob ein bestimmter Fahrer zu diesem Team gehoert.
     *
     * @Vorbedingung fahrer darf nicht null sein
     * @Nachbedingung Rueckgabewert ist true wenn Fahrer zum Team gehoert
     *
     * @param fahrer Der zu pruefende Fahrer
     * @return true wenn der Fahrer zu diesem Team gehoert
     * @throws IllegalArgumentException wenn fahrer null ist
     */
    public boolean hatFahrer(Fahrer fahrer) {
        if (fahrer == null) {
            throw new IllegalArgumentException("Fahrer darf nicht null sein");
        }
        return fahrer1.equals(fahrer) || fahrer2.equals(fahrer);
    }

    /**
     * Gibt den Teamkollegen eines Fahrers zurueck.
     *
     * @Vorbedingung fahrer muss zu diesem Team gehoeren
     * @Nachbedingung Rueckgabewert ist der andere Fahrer des Teams
     *
     * @param fahrer Fahrer dessen Teamkollege gesucht wird
     * @return Der Teamkollege
     * @throws IllegalArgumentException wenn Fahrer nicht zum Team gehoert
     */
    public Fahrer getTeamkollege(Fahrer fahrer) {
        if (fahrer == null) {
            throw new IllegalArgumentException("Fahrer darf nicht null sein");
        }
        if (fahrer.equals(fahrer1)) {
            return fahrer2;
        } else if (fahrer.equals(fahrer2)) {
            return fahrer1;
        } else {
            throw new IllegalArgumentException("Fahrer gehoert nicht zu diesem Team");
        }
    }

    public String getName() {
        return name;
    }

    public Color getFarbe() {
        return farbe;
    }

    public Fahrer getFahrer1() {
        return fahrer1;
    }

    public Fahrer getFahrer2() {
        return fahrer2;
    }

    /**
     * Gibt die Farbe als Hex-String zurueck fuer CSS-Styling.
     *
     * @Vorbedingung Keine
     * @Nachbedingung Rueckgabewert ist ein gueltiger Hex-Farbcode
     *
     * @return Farbe als Hex-String (z.B. "#FF0000")
     */
    public String getFarbeAlsHex() {
        return String.format("#%02X%02X%02X",
                (int) (farbe.getRed() * 255),
                (int) (farbe.getGreen() * 255),
                (int) (farbe.getBlue() * 255)
        );
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Team team = (Team) obj;
        return name.equals(team.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
