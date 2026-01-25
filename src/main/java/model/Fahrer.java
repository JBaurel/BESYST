package model;

/**
 * Repraesentiert einen Formel-1-Fahrer mit individuellen Eigenschaften.
 * Jeder Fahrer hat ein Geschicklichkeitsattribut, das die Fahrleistung
 * und Ueberholwahrscheinlichkeit beeinflusst.
 *
 * Die Geschicklichkeit wird als Wert zwischen 0 und 100 gespeichert,
 * wobei hoehere Werte bessere Fahrer repraesentieren.
 *
 * @author F1 Simulation Team
 * @version 1.0
 */
public class Fahrer {

    /** Minimaler Geschicklichkeitswert. */
    public static final int MIN_GESCHICKLICHKEIT = 0;

    /** Maximaler Geschicklichkeitswert. */
    public static final int MAX_GESCHICKLICHKEIT = 100;

    private final String name;
    private final String kuerzel;
    private final int geschicklichkeit;

    /**
     * Erstellt einen neuen Fahrer mit den angegebenen Eigenschaften.
     *
     * @Vorbedingung name darf nicht null oder leer sein
     * @Vorbedingung kuerzel muss genau 3 Zeichen haben
     * @Vorbedingung geschicklichkeit muss zwischen MIN und MAX liegen
     * @Nachbedingung Fahrer-Objekt ist mit den angegebenen Werten erstellt
     *
     * @param name Vollstaendiger Name des Fahrers
     * @param kuerzel Dreistelliges Fahrerkuerzel (z.B. "VER" fuer Verstappen)
     * @param geschicklichkeit Geschicklichkeitswert zwischen 0 und 100
     * @throws IllegalArgumentException wenn Parameter ungueltig sind
     */
    public Fahrer(String name, String kuerzel, int geschicklichkeit) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Fahrername darf nicht leer sein");
        }
        if (kuerzel == null || kuerzel.length() != 3) {
            throw new IllegalArgumentException("Kuerzel muss genau 3 Zeichen haben");
        }
        if (geschicklichkeit < MIN_GESCHICKLICHKEIT || geschicklichkeit > MAX_GESCHICKLICHKEIT) {
            throw new IllegalArgumentException(
                    "Geschicklichkeit muss zwischen " + MIN_GESCHICKLICHKEIT +
                            " und " + MAX_GESCHICKLICHKEIT + " liegen"
            );
        }

        this.name = name;
        this.kuerzel = kuerzel.toUpperCase();
        this.geschicklichkeit = geschicklichkeit;
    }

    /**
     * Berechnet den Geschicklichkeitsfaktor fuer Berechnungen.
     * Der Faktor liegt zwischen 0.0 und 1.0.
     *
     * @Vorbedingung Keine
     * @Nachbedingung Rueckgabewert liegt zwischen 0.0 und 1.0
     *
     * @return Geschicklichkeit als Faktor (0.0 bis 1.0)
     */
    public double getGeschicklichkeitsFaktor() {
        return geschicklichkeit / (double) MAX_GESCHICKLICHKEIT;
    }

    public String getName() {
        return name;
    }

    public String getKuerzel() {
        return kuerzel;
    }

    public int getGeschicklichkeit() {
        return geschicklichkeit;
    }

    @Override
    public String toString() {
        return String.format("%s (%s)", name, kuerzel);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Fahrer fahrer = (Fahrer) obj;
        return kuerzel.equals(fahrer.kuerzel);
    }

    @Override
    public int hashCode() {
        return kuerzel.hashCode();
    }
}
