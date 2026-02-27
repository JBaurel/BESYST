package model.enumType;

/**
 * Enumeration fuer den Status des gesamten Rennens.
 * Diese Zustaende bilden den Lebenszyklus eines Rennens ab,
 * von der Vorbereitung bis zum Abschluss.
 *
 * Der Statuswechsel erfolgt ausschliesslich durch den RennController
 * und wird durch den RennleiterThread ueberwacht.
 */
public enum RennStatus {

    /**
     * Rennen wurde erstellt, aber noch nicht gestartet.
     * Alle Autos befinden sich in der Startaufstellung.
     */
    VORBEREITUNG("Vorbereitung"),

    /**
     * Startampel-Sequenz laeuft (Lichter gehen nacheinander an).
     */
    STARTPHASE("Startphase"),

    /**
     * Rennen laeuft normal, alle Autos fahren.
     */
    LAEUFT("Rennen laeuft"),

    /**
     * Rennen ist pausiert (durch Benutzerinteraktion).
     */
    PAUSIERT("Pausiert"),

    /**
     * Rennen wurde abgebrochen (vorzeitiges Ende).
     */
    ABGEBROCHEN("Abgebrochen"),

    /**
     * Rennen ist beendet, Ergebnisse liegen vor.
     */
    BEENDET("Beendet");

    private final String bezeichnung;

    /**
     * Konstruktor fuer einen Rennstatus.
     *
     * @param bezeichnung Anzeigename des Status
     */
    RennStatus(String bezeichnung) {
        this.bezeichnung = bezeichnung;
    }

    public String getBezeichnung() {
        return bezeichnung;
    }

    /**
     * Prueft ob das Rennen in einem aktiven Zustand ist.
     * Ein aktiver Zustand bedeutet, dass die Simulation laeuft
     * und Threads aktiv sind.
     *
     * @Vorbedingung Keine
     * @Nachbedingung Rueckgabewert ist true fuer STARTPHASE und LAEUFT
     *
     * @return true wenn das Rennen aktiv ist
     */
    public boolean istAktiv() {
        return this == STARTPHASE || this == LAEUFT;
    }

    /**
     * Prueft ob das Rennen beendet oder abgebrochen wurde.
     *
     * @Vorbedingung Keine
     * @Nachbedingung Rueckgabewert ist true fuer BEENDET und ABGEBROCHEN
     *
     * @return true wenn das Rennen nicht mehr laeuft
     */
    public boolean istBeendet() {
        return this == BEENDET || this == ABGEBROCHEN;
    }

    @Override
    public String toString() {
        return bezeichnung;
    }
}
