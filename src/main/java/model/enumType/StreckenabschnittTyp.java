package model.enumType;

/**
 * Enumeration fuer die verschiedenen Typen von Streckenabschnitten.
 * Jeder Typ definiert spezifische Eigenschaften wie Kapazitaet,
 * Passierzeit und ob Ueberholen moeglich ist.
 * Die Strecke des Nuerburgring GP-Kurses besteht aus einer Kombination
 * dieser Abschnittstypen, die gemeinsam den Streckenverlauf bilden.
 */
public enum StreckenabschnittTyp {

    /**
     * Start-Ziel-Gerade: Bereich mit Start und Ziel, unbegrenzte Kapazitaet.
     */
    START_ZIEL("Start/Ziel", Integer.MAX_VALUE, 800, false),

    /**
     * Normale Gerade: Schneller Abschnitt ohne Kapazitaetsbegrenzung.
     */
    GERADE("Gerade", Integer.MAX_VALUE, 600, true),

    /**
     * Normale Kurve: Kurve mit begrenzter Kapazitaet, kein Ueberholen.
     */
    NORMALE_KURVE("Kurve", 3, 1000, false),

    /**
     * Enge Kurve: Kritischer Bereich, nur ein Auto gleichzeitig erlaubt.
     */
    ENGE_KURVE("Enge Kurve", 1, 1200, false),

    /**
     * Schikane: Wechselkurven, maximal zwei Autos gleichzeitig.
     */
    SCHIKANE("Schikane", 2, 1500, false),

    /**
     * DRS-Zone: Ueberholzone mit aktiviertem DRS, unbegrenzte Kapazitaet.
     */
    DRS_ZONE("DRS-Zone", Integer.MAX_VALUE, 500, true),

    /**
     * Einfahrt zur Boxengasse.
     */
    PITSTOP_EINFAHRT("Pitstop-Einfahrt", 3, 1000, false),

    /**
     * Boxengasse: Bereich fuer Reifenwechsel, eine Box pro Team.
     */
    BOXENGASSE("Boxengasse", 1, 3000, false),

    /**
     * Ausfahrt aus der Boxengasse.
     */
    PITSTOP_AUSFAHRT("Pitstop-Ausfahrt", 3, 1000, false);

    private final String bezeichnung;
    private final int kapazitaet;
    private final int basisPassierzeitMs;
    private final boolean ueberholenErlaubt;

    /**
     * Konstruktor fuer einen Streckenabschnittstyp.
     * @param bezeichnung Anzeigename des Abschnittstyps
     * @param kapazitaet Maximale Anzahl Autos gleichzeitig
     * @param basisPassierzeitMs Basis-Durchfahrtszeit in Millisekunden
     * @param ueberholenErlaubt True wenn Ueberholen in diesem Abschnitt moeglich
     */
    StreckenabschnittTyp(String bezeichnung, int kapazitaet,
                         int basisPassierzeitMs, boolean ueberholenErlaubt) {
        this.bezeichnung = bezeichnung;
        this.kapazitaet = kapazitaet;
        this.basisPassierzeitMs = basisPassierzeitMs;
        this.ueberholenErlaubt = ueberholenErlaubt;
    }

    public String getBezeichnung() {
        return bezeichnung;
    }

    public int getKapazitaet() {
        return kapazitaet;
    }

    public int getBasisPassierzeitMs() {
        return basisPassierzeitMs;
    }

    public boolean istUeberholenErlaubt() {
        return ueberholenErlaubt;
    }

    /**
     * Prueft ob dieser Abschnittstyp ein kritischer Bereich ist.
     * Kritische Bereiche haben eine begrenzte Kapazitaet und
     * erfordern Synchronisation.
     * @Vorbedingung Keine
     * @Nachbedingung Rueckgabewert ist true wenn Kapazitaet begrenzt
      @return true wenn der Abschnitt eine begrenzte Kapazitaet hat
     */
    public boolean istKritischerBereich() {
        return kapazitaet < Integer.MAX_VALUE;
    }

    @Override
    public String toString() {
        return bezeichnung;
    }
}
