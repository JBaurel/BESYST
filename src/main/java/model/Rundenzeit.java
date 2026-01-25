package model;

import model.enumType.ReifenTyp;

/**
 * Repraesentiert eine einzelne Rundenzeit eines Fahrzeugs.
 * Diese Klasse speichert alle relevanten Informationen zu einer
 * gefahrenen Runde, einschliesslich der Zeit und des Reifenzustands.
 *
 * Rundenzeiten werden vom RennleiterThread erfasst und koennen
 * fuer Statistiken und Anzeigen verwendet werden.
 *
 * @author F1 Simulation Team
 * @version 1.0
 */
public class Rundenzeit implements Comparable<Rundenzeit> {

    private final int startnummer;
    private final int rundenNummer;
    private final long zeitInMillis;
    private final ReifenTyp reifenTyp;
    private final double reifenAbnutzung;
    private final long zeitstempel;

    /**
     * Erstellt eine neue Rundenzeit-Aufzeichnung.
     *
     * @Vorbedingung startnummer muss > 0 sein
     * @Vorbedingung rundenNummer muss > 0 sein
     * @Vorbedingung zeitInMillis muss > 0 sein
     * @Vorbedingung reifenTyp darf nicht null sein
     * @Vorbedingung reifenAbnutzung muss zwischen 0 und 100 liegen
     * @Nachbedingung Rundenzeit-Objekt ist mit den Werten initialisiert
     *
     * @param startnummer Startnummer des Autos
     * @param rundenNummer Nummer der gefahrenen Runde
     * @param zeitInMillis Rundenzeit in Millisekunden
     * @param reifenTyp Typ der verwendeten Reifen
     * @param reifenAbnutzung Abnutzung der Reifen bei Rundenbeginn
     * @throws IllegalArgumentException wenn Parameter ungueltig sind
     */
    public Rundenzeit(int startnummer, int rundenNummer, long zeitInMillis,
                      ReifenTyp reifenTyp, double reifenAbnutzung) {
        if (startnummer <= 0) {
            throw new IllegalArgumentException("Startnummer muss > 0 sein");
        }
        if (rundenNummer <= 0) {
            throw new IllegalArgumentException("Rundennummer muss > 0 sein");
        }
        if (zeitInMillis <= 0) {
            throw new IllegalArgumentException("Zeit muss > 0 sein");
        }
        if (reifenTyp == null) {
            throw new IllegalArgumentException("Reifentyp darf nicht null sein");
        }
        if (reifenAbnutzung < 0 || reifenAbnutzung > 100) {
            throw new IllegalArgumentException("Reifenabnutzung muss zwischen 0 und 100 liegen");
        }

        this.startnummer = startnummer;
        this.rundenNummer = rundenNummer;
        this.zeitInMillis = zeitInMillis;
        this.reifenTyp = reifenTyp;
        this.reifenAbnutzung = reifenAbnutzung;
        this.zeitstempel = System.currentTimeMillis();
    }

    /**
     * Formatiert die Rundenzeit als lesbaren String.
     * Das Format ist M:SS.mmm (Minuten:Sekunden.Millisekunden).
     *
     * @Vorbedingung Keine
     * @Nachbedingung Rueckgabewert ist formatierter Zeitstring
     *
     * @return Formatierte Zeitangabe
     */
    public String getFormattierteZeit() {
        long minuten = zeitInMillis / 60000;
        long sekunden = (zeitInMillis % 60000) / 1000;
        long millis = zeitInMillis % 1000;
        return String.format("%d:%02d.%03d", minuten, sekunden, millis);
    }

    /**
     * Vergleicht diese Rundenzeit mit einer anderen.
     * Kuerzere Zeiten sind "kleiner" (besser).
     *
     * @Vorbedingung other darf nicht null sein
     * @Nachbedingung Rueckgabewert ist negativ wenn diese Zeit schneller ist
     *
     * @param other Die zu vergleichende Rundenzeit
     * @return Vergleichswert gemaess Comparable-Konvention
     */
    @Override
    public int compareTo(Rundenzeit other) {
        return Long.compare(this.zeitInMillis, other.zeitInMillis);
    }

    public int getStartnummer() {
        return startnummer;
    }

    public int getRundenNummer() {
        return rundenNummer;
    }

    public long getZeitInMillis() {
        return zeitInMillis;
    }

    public ReifenTyp getReifenTyp() {
        return reifenTyp;
    }

    public double getReifenAbnutzung() {
        return reifenAbnutzung;
    }

    public long getZeitstempel() {
        return zeitstempel;
    }

    @Override
    public String toString() {
        return String.format("Runde %d: %s (Reifen: %s, %.0f%% Abnutzung)",
                rundenNummer, getFormattierteZeit(), reifenTyp, reifenAbnutzung);
    }
}
