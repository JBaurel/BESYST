package model;

import model.enumType.AutoStatus;
import model.enumType.ReifenTyp;

/**
 * Repraesentiert ein Formel-1-Rennfahrzeug mit allen relevanten Eigenschaften.
 * Diese Klasse ist die zentrale Datenstruktur fuer die Simulation und enthaelt
 * den Zustand eines Autos waehrend des Rennens.
 *
 * Jedes Auto gehoert zu einem Team, hat einen Fahrer und verfuegt ueber
 * Reifen, deren Zustand die Geschwindigkeit beeinflusst.
 *
 * Diese Klasse ist nicht thread-sicher. Der Zugriff muss durch die
 * Synchronisationsschicht geschuetzt werden.
 *
 * @author F1 Simulation Team
 * @version 1.0
 */
public class Auto {

    private final int startnummer;
    private final Team team;
    private final Fahrer fahrer;

    private Reifen aktuelleReifen;
    private AutoStatus status;
    private int aktuelleRunde;
    private int aktuellerAbschnittId;
    private double fortschrittImAbschnitt;
    private int anzahlPitstops;
    private boolean pflichtPitstopErledigt;

    // Position auf der GUI (wird berechnet)
    private double positionX;
    private double positionY;

    // Zeiterfassung
    private long startzeit;
    private long letzteRundenzeit;
    private long besteRundenzeit;
    private long gesamtzeit;

    /**
     * Erstellt ein neues Rennfahrzeug mit den angegebenen Eigenschaften.
     * Das Auto startet mit Medium-Reifen und in der Startaufstellung.
     *
     * @Vorbedingung startnummer muss zwischen 1 und 99 liegen
     * @Vorbedingung team darf nicht null sein
     * @Vorbedingung fahrer darf nicht null sein und muss zum Team gehoeren
     * @Nachbedingung Auto-Objekt ist mit Standardwerten initialisiert
     *
     * @param startnummer Eindeutige Startnummer (1-99)
     * @param team Das Team des Autos
     * @param fahrer Der Fahrer des Autos
     * @throws IllegalArgumentException wenn Parameter ungueltig sind
     */
    public Auto(int startnummer, Team team, Fahrer fahrer) {
        if (startnummer < 1 || startnummer > 99) {
            throw new IllegalArgumentException("Startnummer muss zwischen 1 und 99 liegen");
        }
        if (team == null) {
            throw new IllegalArgumentException("Team darf nicht null sein");
        }
        if (fahrer == null) {
            throw new IllegalArgumentException("Fahrer darf nicht null sein");
        }
        if (!team.hatFahrer(fahrer)) {
            throw new IllegalArgumentException("Fahrer muss zum angegebenen Team gehoeren");
        }

        this.startnummer = startnummer;
        this.team = team;
        this.fahrer = fahrer;

        // Standardwerte setzen
        this.aktuelleReifen = new Reifen(ReifenTyp.MEDIUM);
        this.status = AutoStatus.IN_STARTAUFSTELLUNG;
        this.aktuelleRunde = 0;
        this.aktuellerAbschnittId = 0;
        this.fortschrittImAbschnitt = 0.0;
        this.anzahlPitstops = 0;
        this.pflichtPitstopErledigt = false;

        this.positionX = 0.0;
        this.positionY = 0.0;

        this.startzeit = 0;
        this.letzteRundenzeit = 0;
        this.besteRundenzeit = Long.MAX_VALUE;
        this.gesamtzeit = 0;
    }

    /**
     * Berechnet die aktuelle Geschwindigkeit des Autos.
     * Die Geschwindigkeit haengt vom Reifenzustand und
     * der Fahrergeschicklichkeit ab.
     *
     * @Vorbedingung Keine
     * @Nachbedingung Rueckgabewert liegt zwischen 0.5 und 1.2
     *
     * @return Geschwindigkeitsfaktor (1.0 = Normalgeschwindigkeit)
     */
    public double berechneGeschwindigkeit() {
        double reifenFaktor = aktuelleReifen.berechneGeschwindigkeitsFaktor();
        double fahrerBonus = 0.1 * fahrer.getGeschicklichkeitsFaktor();
        return reifenFaktor + fahrerBonus;
    }

    /**
     * Prueft ob ein Pitstop empfohlen wird.
     * Dies ist der Fall wenn die Reifen kritisch abgenutzt sind
     * oder der Pflicht-Pitstop noch nicht erledigt ist und
     * genuegend Runden verbleiben.
     *
     * @Vorbedingung verbleibendeRunden muss >= 0 sein
     * @Nachbedingung Rueckgabewert ist true wenn Pitstop empfohlen wird
     *
     * @param verbleibendeRunden Anzahl der noch zu fahrenden Runden
     * @return true wenn ein Pitstop empfohlen wird
     */
    public boolean istPitstopEmpfohlen(int verbleibendeRunden) {
        // Pflicht-Pitstop noch nicht erledigt und genuegend Runden uebrig
        if (!pflichtPitstopErledigt && verbleibendeRunden > 3) {
            // Pflicht-Pitstop spätestens wenn 10 Runden uebrig
            if (verbleibendeRunden <= 10) {
                return true;
            }
        }

        // Reifen kritisch abgenutzt
        if (aktuelleReifen.istKritischAbgenutzt()) {
            return verbleibendeRunden > 2; // Nur wenn sich der Stopp noch lohnt
        }

        return false;
    }

    /**
     * Fuehrt einen Reifenwechsel durch.
     * Die alten Reifen werden durch neue Reifen des angegebenen Typs ersetzt.
     *
     * @Vorbedingung neuerTyp darf nicht null sein
     * @Nachbedingung aktuelleReifen sind neue Reifen des angegebenen Typs
     * @Nachbedingung anzahlPitstops ist um 1 erhoeht
     * @Nachbedingung pflichtPitstopErledigt ist true
     *
     * @param neuerTyp Der Typ der neuen Reifen
     * @throws IllegalArgumentException wenn neuerTyp null ist
     */
    public void wechsleReifen(ReifenTyp neuerTyp) {
        if (neuerTyp == null) {
            throw new IllegalArgumentException("Reifentyp darf nicht null sein");
        }
        this.aktuelleReifen = new Reifen(neuerTyp);
        this.anzahlPitstops++;
        this.pflichtPitstopErledigt = true;
    }

    /**
     * Setzt das Auto in den Startzustand zurueck.
     * Wird verwendet beim Neustart eines Rennens.
     *
     * @Vorbedingung Keine
     * @Nachbedingung Auto ist im Startzustand mit frischen Medium-Reifen
     */
    public void zuruecksetzen() {
        this.aktuelleReifen = new Reifen(ReifenTyp.MEDIUM);
        this.status = AutoStatus.IN_STARTAUFSTELLUNG;
        this.aktuelleRunde = 0;
        this.aktuellerAbschnittId = 0;
        this.fortschrittImAbschnitt = 0.0;
        this.anzahlPitstops = 0;
        this.pflichtPitstopErledigt = false;
        this.startzeit = 0;
        this.letzteRundenzeit = 0;
        this.besteRundenzeit = Long.MAX_VALUE;
        this.gesamtzeit = 0;
    }

    /**
     * Registriert den Start einer neuen Runde.
     *
     * @Vorbedingung aktuelleZeit muss > 0 sein
     * @Nachbedingung aktuelleRunde ist um 1 erhoeht
     * @Nachbedingung Rundenzeit ist berechnet (ausser erste Runde)
     *
     * @param aktuelleZeit Aktuelle Systemzeit in Millisekunden
     */
    public void starteNeueRunde(long aktuelleZeit) {
        if (aktuelleRunde > 0 && startzeit > 0) {
            letzteRundenzeit = aktuelleZeit - startzeit;
            if (letzteRundenzeit < besteRundenzeit) {
                besteRundenzeit = letzteRundenzeit;
            }
            gesamtzeit += letzteRundenzeit;
        }
        aktuelleRunde++;
        startzeit = aktuelleZeit;
    }

    /**
     * Aktualisiert die GUI-Position basierend auf dem aktuellen Abschnitt
     * und dem Fortschritt.
     *
     * @Vorbedingung abschnitt darf nicht null sein
     * @Nachbedingung positionX und positionY sind aktualisiert
     *
     * @param abschnitt Der aktuelle Streckenabschnitt
     */
    public void aktualisierePosition(Streckenabschnitt abschnitt) {
        if (abschnitt == null) {
            throw new IllegalArgumentException("Abschnitt darf nicht null sein");
        }
        double[] pos = abschnitt.berechnePosition(fortschrittImAbschnitt);
        this.positionX = pos[0];
        this.positionY = pos[1];
    }

    // Getter und Setter

    public int getStartnummer() {
        return startnummer;
    }

    public Team getTeam() {
        return team;
    }

    public Fahrer getFahrer() {
        return fahrer;
    }

    public Reifen getAktuelleReifen() {
        return aktuelleReifen;
    }

    public AutoStatus getStatus() {
        return status;
    }

    public void setStatus(AutoStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("Status darf nicht null sein");
        }
        this.status = status;
    }

    public int getAktuelleRunde() {
        return aktuelleRunde;
    }

    public void setAktuelleRunde(int runde) {
        this.aktuelleRunde = runde;
    }

    public int getAktuellerAbschnittId() {
        return aktuellerAbschnittId;
    }

    public void setAktuellerAbschnittId(int id) {
        this.aktuellerAbschnittId = id;
    }

    public double getFortschrittImAbschnitt() {
        return fortschrittImAbschnitt;
    }

    public void setFortschrittImAbschnitt(double fortschritt) {
        this.fortschrittImAbschnitt = Math.max(0.0, Math.min(1.0, fortschritt));
    }

    public int getAnzahlPitstops() {
        return anzahlPitstops;
    }

    public boolean isPflichtPitstopErledigt() {
        return pflichtPitstopErledigt;
    }

    public double getPositionX() {
        return positionX;
    }

    public double getPositionY() {
        return positionY;
    }

    public void setPositionX(double x) {
        this.positionX = x;
    }

    public void setPositionY(double y) {
        this.positionY = y;
    }

    public long getLetzteRundenzeit() {
        return letzteRundenzeit;
    }

    public long getBesteRundenzeit() {
        return besteRundenzeit == Long.MAX_VALUE ? 0 : besteRundenzeit;
    }

    public long getGesamtzeit() {
        return gesamtzeit;
    }

    @Override
    public String toString() {
        return String.format("#%d %s (%s)", startnummer, fahrer.getName(), team.getName());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Auto auto = (Auto) obj;
        return startnummer == auto.startnummer;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(startnummer);
    }
}
