package model;

import model.enumType.StreckenabschnittTyp;

/**
 * Repraesentiert einen einzelnen Abschnitt der Rennstrecke.
 * Jeder Abschnitt hat einen Typ, eine Laenge und Koordinaten
 * fuer die visuelle Darstellung.
 *
 * Die Kapazitaet bestimmt, wie viele Autos gleichzeitig
 * diesen Abschnitt durchfahren duerfen. Bei kritischen Bereichen
 * wie engen Kurven ist die Kapazitaet auf 1 begrenzt.
 *
 * @author F1 Simulation Team
 * @version 1.0
 */
public class Streckenabschnitt {

    private final int id;
    private final String name;
    private final StreckenabschnittTyp typ;
    private final int laengeInMetern;
    private final int kapazitaet;
    private final boolean istUeberholzone;


    private final double startX;
    private final double startY;
    private final double endeX;
    private final double endeY;

    /**
     * Erstellt einen neuen Streckenabschnitt mit allen Eigenschaften.
     *
     * @Vorbedingung id muss >= 0 sein
     * @Vorbedingung name darf nicht null oder leer sein
     * @Vorbedingung typ darf nicht null sein
     * @Vorbedingung laengeInMetern muss > 0 sein
     * @Vorbedingung kapazitaet muss > 0 sein
     * @Vorbedingung Koordinaten muessen zwischen 0.0 und 1.0 liegen
     * @Nachbedingung Streckenabschnitt-Objekt ist erstellt
     *
     * @param id Eindeutige ID des Abschnitts
     * @param name Name des Abschnitts (z.B. "Yokohama-S")
     * @param typ Typ des Abschnitts
     * @param laengeInMetern Laenge in Metern
     * @param kapazitaet Maximale Anzahl gleichzeitiger Fahrzeuge
     * @param istUeberholzone True wenn Ueberholen erlaubt
     * @param startX Start-X-Koordinate (0.0 bis 1.0)
     * @param startY Start-Y-Koordinate (0.0 bis 1.0)
     * @param endeX Ende-X-Koordinate (0.0 bis 1.0)
     * @param endeY Ende-Y-Koordinate (0.0 bis 1.0)
     * @throws IllegalArgumentException wenn Parameter ungueltig sind
     */
    public Streckenabschnitt(int id, String name, StreckenabschnittTyp typ,
                             int laengeInMetern, int kapazitaet, boolean istUeberholzone,
                             double startX, double startY, double endeX, double endeY) {
        if (id < 0) {
            throw new IllegalArgumentException("ID muss >= 0 sein");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name darf nicht leer sein");
        }
        if (typ == null) {
            throw new IllegalArgumentException("Typ darf nicht null sein");
        }
        if (laengeInMetern <= 0) {
            throw new IllegalArgumentException("Laenge muss > 0 sein");
        }
        if (kapazitaet <= 0) {
            throw new IllegalArgumentException("Kapazitaet muss > 0 sein");
        }

        this.id = id;
        this.name = name;
        this.typ = typ;
        this.laengeInMetern = laengeInMetern;
        this.kapazitaet = kapazitaet;
        this.istUeberholzone = istUeberholzone;
        this.startX = startX;
        this.startY = startY;
        this.endeX = endeX;
        this.endeY = endeY;
    }

    /**
     * Erstellt einen Streckenabschnitt mit Standardkapazitaet aus dem Typ.
     *
     * @Vorbedingung id muss >= 0 sein
     * @Vorbedingung name darf nicht null oder leer sein
     * @Vorbedingung typ darf nicht null sein
     * @Vorbedingung laengeInMetern muss > 0 sein
     * @Nachbedingung Streckenabschnitt-Objekt ist erstellt mit Typ-Kapazitaet
     *
     * @param id Eindeutige ID des Abschnitts
     * @param name Name des Abschnitts
     * @param typ Typ des Abschnitts (bestimmt Kapazitaet)
     * @param laengeInMetern Laenge in Metern
     * @param startX Start-X-Koordinate
     * @param startY Start-Y-Koordinate
     * @param endeX Ende-X-Koordinate
     * @param endeY Ende-Y-Koordinate
     * @throws IllegalArgumentException wenn Parameter ungueltig sind
     */
    public Streckenabschnitt(int id, String name, StreckenabschnittTyp typ,
                             int laengeInMetern,
                             double startX, double startY, double endeX, double endeY) {
        this(id, name, typ, laengeInMetern,
                typ.getKapazitaet() == Integer.MAX_VALUE ? 100 : typ.getKapazitaet(),
                typ.istUeberholenErlaubt(),
                startX, startY, endeX, endeY);
    }

    /**
     * Berechnet die Basis-Passierzeit fuer diesen Abschnitt.
     * Die tatsaechliche Zeit haengt von Reifenzustand und
     * Fahrzeuggeschwindigkeit ab.
     *
     * @Vorbedingung Keine
     * @Nachbedingung Rueckgabewert ist die Basiszeit in Millisekunden
     *
     * @return Basis-Passierzeit in Millisekunden
     */
    public int getBasisPassierzeitMs() {
        return typ.getBasisPassierzeitMs();
    }

    /**
     * Prueft ob dieser Abschnitt ein kritischer Bereich ist,
     * der synchronisierten Zugriff erfordert.
     *
     * @Vorbedingung Keine
     * @Nachbedingung Rueckgabewert ist true bei begrenzter Kapazitaet
     *
     * @return true wenn der Abschnitt kritisch ist
     */
    public boolean istKritischerBereich() {
        return kapazitaet < 100;
    }

    /**
     * Berechnet einen Punkt auf diesem Abschnitt basierend auf dem Fortschritt.
     * Der Fortschritt ist ein Wert zwischen 0.0 (Start) und 1.0 (Ende).
     *
     * @Vorbedingung fortschritt muss zwischen 0.0 und 1.0 liegen
     * @Nachbedingung Rueckgabewert enthaelt interpolierte X- und Y-Koordinate
     *
     * @param fortschritt Fortschritt auf dem Abschnitt (0.0 bis 1.0)
     * @return Array mit [x, y] Koordinaten
     * @throws IllegalArgumentException wenn fortschritt ausserhalb des Bereichs
     */
    public double[] berechnePosition(double fortschritt) {
        if (fortschritt < 0.0 || fortschritt > 1.0) {
            throw new IllegalArgumentException("Fortschritt muss zwischen 0.0 und 1.0 liegen");
        }

        double x = startX + (endeX - startX) * fortschritt;
        double y = startY + (endeY - startY) * fortschritt;

        return new double[] {x, y};
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public StreckenabschnittTyp getTyp() {
        return typ;
    }

    public int getLaengeInMetern() {
        return laengeInMetern;
    }

    public int getKapazitaet() {
        return kapazitaet;
    }

    public boolean istUeberholzone() {
        return istUeberholzone;
    }

    public double getStartX() {
        return startX;
    }

    public double getStartY() {
        return startY;
    }

    public double getEndeX() {
        return endeX;
    }

    public double getEndeY() {
        return endeY;
    }

    @Override
    public String toString() {
        return String.format("%d: %s (%s)", id, name, typ.getBezeichnung());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Streckenabschnitt that = (Streckenabschnitt) obj;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}
