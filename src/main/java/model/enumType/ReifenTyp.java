package model.enumType;

/**
 * Enumeration fuer die verschiedenen Reifentypen in der Formel 1.
 * Jeder Reifentyp hat unterschiedliche Eigenschaften bezueglich
 * Geschwindigkeit, Haltbarkeit und Abnutzungsrate.
 *
 * SOFT: Schnellster Reifen, aber hoechste Abnutzung
 * MEDIUM: Ausgewogener Reifen fuer mittlere Stints
 * HARD: Langsamster Reifen, aber geringste Abnutzung
 *
 * @author F1 Simulation Team
 * @version 1.0
 */
public enum ReifenTyp {

    /**
     * Soft-Reifen: Hoechste Geschwindigkeit, schnellste Abnutzung.
     */
    SOFT("Soft", 1.0, 3.0, "Rot"),

    /**
     * Medium-Reifen: Ausgewogene Eigenschaften.
     */
    MEDIUM("Medium", 0.85, 2.0, "Gelb"),

    /**
     * Hard-Reifen: Geringste Geschwindigkeit, langsamste Abnutzung.
     */
    HARD("Hard", 0.7, 1.0, "Weiss");

    private final String bezeichnung;
    private final double geschwindigkeitsFaktor;
    private final double abnutzungsRate;
    private final String farbe;

    /**
     * Konstruktor fuer einen Reifentyp.
     *
     * @param bezeichnung Anzeigename des Reifentyps
     * @param geschwindigkeitsFaktor Faktor fuer die Geschwindigkeit (1.0 = Basis)
     * @param abnutzungsRate Rate der Abnutzung pro Runde
     * @param farbe Farbe zur visuellen Kennzeichnung
     */
    ReifenTyp(String bezeichnung, double geschwindigkeitsFaktor,
              double abnutzungsRate, String farbe) {
        this.bezeichnung = bezeichnung;
        this.geschwindigkeitsFaktor = geschwindigkeitsFaktor;
        this.abnutzungsRate = abnutzungsRate;
        this.farbe = farbe;
    }

    public String getBezeichnung() {
        return bezeichnung;
    }

    public double getGeschwindigkeitsFaktor() {
        return geschwindigkeitsFaktor;
    }

    public double getAbnutzungsRate() {
        return abnutzungsRate;
    }

    public String getFarbe() {
        return farbe;
    }

    @Override
    public String toString() {
        return bezeichnung;
    }
}

