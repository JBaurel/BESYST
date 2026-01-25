package model.enumType;

/**
 * Enumeration fuer den Status eines einzelnen Rennfahrzeugs.
 * Diese Zustaende bilden ab, wo sich ein Auto gerade befindet
 * und welche Aktion es ausfuehrt.
 *
 * Der Statuswechsel wird durch den zugehoerigen AutoThread gesteuert
 * und beeinflusst die Visualisierung in der GUI.
 *
 * @author F1 Simulation Team
 * @version 1.0
 */
public enum AutoStatus {

    /**
     * Auto steht in der Startaufstellung und wartet auf den Rennstart.
     */
    IN_STARTAUFSTELLUNG("In Startaufstellung"),

    /**
     * Auto faehrt normal auf der Rennstrecke.
     */
    FAEHRT("Faehrt"),

    /**
     * Auto wartet vor einem kritischen Bereich auf Einfahrt.
     */
    WARTET_AUF_ABSCHNITT("Wartet auf Abschnitt"),

    /**
     * Auto befindet sich in einem kritischen Bereich (enge Kurve, Schikane).
     */
    IN_KRITISCHEM_BEREICH("In kritischem Bereich"),

    /**
     * Auto befindet sich in einer Ueberholzone und versucht zu ueberholen.
     */
    IN_UEBERHOLZONE("In Ueberholzone"),

    /**
     * Auto faehrt in die Boxengasse ein.
     */
    FAEHRT_IN_BOX("Faehrt in Box"),

    /**
     * Auto steht in der Box und wird bearbeitet (Reifenwechsel).
     */
    IN_BOX("In Box"),

    /**
     * Auto verlaesst die Boxengasse.
     */
    VERLAESST_BOX("Verlaesst Box"),

    /**
     * Auto hat das Rennen beendet (alle Runden absolviert).
     */
    IM_ZIEL("Im Ziel"),

    /**
     * Auto ist ausgefallen (nur fuer zukuenftige Erweiterungen).
     */
    AUSGEFALLEN("Ausgefallen");

    private final String bezeichnung;

    /**
     * Konstruktor fuer einen Autostatus.
     *
     * @param bezeichnung Anzeigename des Status
     */
    AutoStatus(String bezeichnung) {
        this.bezeichnung = bezeichnung;
    }

    public String getBezeichnung() {
        return bezeichnung;
    }

    /**
     * Prueft ob das Auto aktiv am Rennen teilnimmt.
     * Ein aktives Auto kann Positionen wechseln und Runden absolvieren.
     *
     * @Vorbedingung Keine
     * @Nachbedingung Rueckgabewert ist true wenn Auto aktiv faehrt
     *
     * @return true wenn das Auto aktiv im Rennen ist
     */
    public boolean istAktiv() {
        return this != IN_STARTAUFSTELLUNG
                && this != IM_ZIEL
                && this != AUSGEFALLEN;
    }

    /**
     * Prueft ob das Auto sich gerade in der Boxengasse befindet.
     *
     * @Vorbedingung Keine
     * @Nachbedingung Rueckgabewert ist true bei Box-bezogenen Status
     *
     * @return true wenn das Auto in der Boxengasse ist
     */
    public boolean istInBoxengasse() {
        return this == FAEHRT_IN_BOX
                || this == IN_BOX
                || this == VERLAESST_BOX;
    }

    @Override
    public String toString() {
        return bezeichnung;
    }
}
