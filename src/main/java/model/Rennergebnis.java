package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Repraesentiert das Endergebnis eines Fahrzeugs nach Abschluss des Rennens.
 * Diese Klasse fasst alle relevanten Statistiken zusammen, die am
 * Rennende vorliegen.
 *
 * Die Rennergebnisse werden nach Abschluss aller Runden erstellt und
 * fuer die Ergebnisanzeige verwendet.
 *
 * @author F1 Simulation Team
 * @version 1.0
 */
public class Rennergebnis implements Comparable<Rennergebnis> {

    private final Auto auto;
    private final int position;
    private final long gesamtzeit;
    private final int absolvierteRunden;
    private final long besteRundenzeit;
    private final int anzahlPitstops;
    private final List<Rundenzeit> alleRundenzeiten;
    private final long rueckstandZumErsten;

    /**
     * Erstellt ein neues Rennergebnis fuer ein Auto.
     *
     * @Vorbedingung auto darf nicht null sein
     * @Vorbedingung position muss > 0 sein
     * @Vorbedingung gesamtzeit muss >= 0 sein
     * @Vorbedingung absolvierteRunden muss >= 0 sein
     * @Nachbedingung Rennergebnis-Objekt ist mit den Werten initialisiert
     *
     * @param auto Das Auto zu dem das Ergebnis gehoert
     * @param position Zielposition (1 = Sieger)
     * @param gesamtzeit Gesamte Rennzeit in Millisekunden
     * @param absolvierteRunden Anzahl der gefahrenen Runden
     * @param besteRundenzeit Schnellste Rundenzeit in Millisekunden
     * @param anzahlPitstops Anzahl der durchgefuehrten Pitstops
     * @param alleRundenzeiten Liste aller Rundenzeiten
     * @param rueckstandZumErsten Zeitrueckstand zum Sieger in Millisekunden
     * @throws IllegalArgumentException wenn Parameter ungueltig sind
     */
    public Rennergebnis(Auto auto, int position, long gesamtzeit, int absolvierteRunden,
                        long besteRundenzeit, int anzahlPitstops,
                        List<Rundenzeit> alleRundenzeiten, long rueckstandZumErsten) {
        if (auto == null) {
            throw new IllegalArgumentException("Auto darf nicht null sein");
        }
        if (position <= 0) {
            throw new IllegalArgumentException("Position muss > 0 sein");
        }
        if (gesamtzeit < 0) {
            throw new IllegalArgumentException("Gesamtzeit muss >= 0 sein");
        }
        if (absolvierteRunden < 0) {
            throw new IllegalArgumentException("Absolvierte Runden muss >= 0 sein");
        }

        this.auto = auto;
        this.position = position;
        this.gesamtzeit = gesamtzeit;
        this.absolvierteRunden = absolvierteRunden;
        this.besteRundenzeit = besteRundenzeit;
        this.anzahlPitstops = anzahlPitstops;
        this.alleRundenzeiten = alleRundenzeiten != null
                ? new ArrayList<>(alleRundenzeiten)
                : new ArrayList<>();
        this.rueckstandZumErsten = rueckstandZumErsten;
    }

    /**
     * Formatiert die Gesamtzeit als lesbaren String.
     * Das Format ist H:MM:SS.mmm.
     *
     * @Vorbedingung Keine
     * @Nachbedingung Rueckgabewert ist formatierter Zeitstring
     *
     * @return Formatierte Gesamtzeit
     */
    public String getFormattierteGesamtzeit() {
        long stunden = gesamtzeit / 3600000;
        long minuten = (gesamtzeit % 3600000) / 60000;
        long sekunden = (gesamtzeit % 60000) / 1000;
        long millis = gesamtzeit % 1000;

        if (stunden > 0) {
            return String.format("%d:%02d:%02d.%03d", stunden, minuten, sekunden, millis);
        } else {
            return String.format("%d:%02d.%03d", minuten, sekunden, millis);
        }
    }

    /**
     * Formatiert die beste Rundenzeit als lesbaren String.
     *
     * @Vorbedingung Keine
     * @Nachbedingung Rueckgabewert ist formatierter Zeitstring
     *
     * @return Formatierte beste Rundenzeit
     */
    public String getFormattierteBesteRundenzeit() {
        if (besteRundenzeit <= 0) {
            return "-:--.---";
        }
        long minuten = besteRundenzeit / 60000;
        long sekunden = (besteRundenzeit % 60000) / 1000;
        long millis = besteRundenzeit % 1000;
        return String.format("%d:%02d.%03d", minuten, sekunden, millis);
    }

    /**
     * Formatiert den Rueckstand zum Ersten als lesbaren String.
     *
     * @Vorbedingung Keine
     * @Nachbedingung Rueckgabewert ist formatierter Rueckstandsstring
     *
     * @return Formatierter Rueckstand oder "Sieger" bei Position 1
     */
    public String getFormattierterRueckstand() {
        if (position == 1) {
            return "Sieger";
        }

        double sekunden = rueckstandZumErsten / 1000.0;
        return String.format("+%.3f s", sekunden);
    }

    /**
     * Berechnet die durchschnittliche Rundenzeit.
     *
     * @Vorbedingung Mindestens eine Runde muss gefahren sein
     * @Nachbedingung Rueckgabewert ist Durchschnitt in Millisekunden
     *
     * @return Durchschnittliche Rundenzeit in Millisekunden
     */
    public long getDurchschnittlicheRundenzeit() {
        if (absolvierteRunden == 0) {
            return 0;
        }
        return gesamtzeit / absolvierteRunden;
    }

    /**
     * Vergleicht dieses Ergebnis mit einem anderen nach Position.
     * Bessere Positionen (niedrigere Zahlen) kommen zuerst.
     *
     * @Vorbedingung other darf nicht null sein
     * @Nachbedingung Rueckgabewert ist negativ wenn diese Position besser ist
     *
     * @param other Das zu vergleichende Ergebnis
     * @return Vergleichswert gemaess Comparable-Konvention
     */
    @Override
    public int compareTo(Rennergebnis other) {
        return Integer.compare(this.position, other.position);
    }

    public Auto getAuto() {
        return auto;
    }

    public int getPosition() {
        return position;
    }

    public long getGesamtzeit() {
        return gesamtzeit;
    }

    public int getAbsolvierteRunden() {
        return absolvierteRunden;
    }

    public long getBesteRundenzeit() {
        return besteRundenzeit;
    }

    public int getAnzahlPitstops() {
        return anzahlPitstops;
    }

    public List<Rundenzeit> getAlleRundenzeiten() {
        return Collections.unmodifiableList(alleRundenzeiten);
    }

    public long getRueckstandZumErsten() {
        return rueckstandZumErsten;
    }

    @Override
    public String toString() {
        return String.format("P%d: %s - %s (%d Pitstops, Beste: %s)",
                position,
                auto.getFahrer().getName(),
                getFormattierterRueckstand(),
                anzahlPitstops,
                getFormattierteBesteRundenzeit());
    }
}
