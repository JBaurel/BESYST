package threads;

import model.Auto;
import model.Team;
import model.enumType.ReifenTyp;
import synchronisation.BoxenZugriff;
import util.Konfiguration;
import util.RennLogger;

/**
 * Thread-Klasse fuer die Boxencrew eines Teams.
 * Jedes Team hat eine Boxencrew, die auf einfahrende Autos wartet
 * und den Reifenwechsel durchfuehrt.
 *
 * Der BoxencrewThread arbeitet im Producer-Consumer-Muster:
 * Der AutoThread ist der Producer (signalisiert Ankunft),
 * der BoxencrewThread ist der Consumer (fuehrt Service durch).
 *
 * Die Synchronisation erfolgt ueber die BoxenZugriff-Klasse mit
 * ReentrantLock und zwei Condition-Objekten fuer die bidirektionale
 * Kommunikation zwischen Auto und Crew.
 *
 * Der Ablauf eines Pitstops aus Sicht der Crew ist:
 * 1. Warten auf ein Auto (blockiert an Condition autoWartet)
 * 2. Reifenwechsel durchfuehren (simulierte Wartezeit)
 * 3. Service als abgeschlossen melden (signalisiert an Condition serviceAbgeschlossen)
 * 4. Zurueck zu Schritt 1
 *
 * @author F1 Simulation Team
 * @version 1.0
 */
public class BoxencrewThread extends Thread {

    private final Team team;
    private final BoxenZugriff boxenZugriff;

    // Steuerungsflags
    private volatile boolean running;
    private volatile double simulationsGeschwindigkeit;

    // Statistiken
    private int durchgefuehrtePitstops;
    private long gesamtServiceZeit;

    /**
     * Erstellt einen neuen BoxencrewThread fuer das angegebene Team.
     *
     * @Vorbedingung team darf nicht null sein
     * @Vorbedingung boxenZugriff darf nicht null sein
     * @Nachbedingung Thread ist initialisiert aber noch nicht gestartet
     *
     * @param team Das Team dem diese Crew gehoert
     * @param boxenZugriff Die Zugriffskontrolle fuer die Teambox
     * @throws IllegalArgumentException wenn Parameter null sind
     */
    public BoxencrewThread(Team team, BoxenZugriff boxenZugriff) {
        super("BoxencrewThread-" + team.getName());

        if (team == null) {
            throw new IllegalArgumentException("Team darf nicht null sein");
        }
        if (boxenZugriff == null) {
            throw new IllegalArgumentException("BoxenZugriff darf nicht null sein");
        }

        this.team = team;
        this.boxenZugriff = boxenZugriff;
        this.running = true;
        this.simulationsGeschwindigkeit = Konfiguration.DEFAULT_GESCHWINDIGKEIT;
        this.durchgefuehrtePitstops = 0;
        this.gesamtServiceZeit = 0;
    }

    /**
     * Hauptmethode des Threads - wartet kontinuierlich auf Autos und bedient sie.
     * Der Thread laeuft in einer Endlosschleife bis running auf false gesetzt wird.
     */
    @Override
    public void run() {
        RennLogger.logThread(RennLogger.LogLevel.INFO,
                "Boxencrew " + team.getName() + " bereit");

        try {
            while (running) {
                // Mit Timeout warten, um regelmaessig running zu pruefen
                Auto auto = boxenZugriff.warteAufAutoMitTimeout(
                        Konfiguration.STRATEGIE_CHECK_INTERVALL_MS);

                if (auto != null && running) {
                    // Auto ist angekommen - Reifenwechsel durchfuehren
                    fuehreReifenwechselDurch(auto);
                }
            }
        } catch (InterruptedException e) {
            RennLogger.logThread(RennLogger.LogLevel.WARNING,
                    "Boxencrew " + team.getName() + " unterbrochen");
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            // Fehler loggen und ignorieren
            RennLogger.error("Fehler in Boxencrew " + team.getName(), e);
        } finally {
            RennLogger.logThread(RennLogger.LogLevel.INFO,
                    "Boxencrew " + team.getName() + " beendet - " +
                            durchgefuehrtePitstops + " Pitstops durchgefuehrt");
        }
    }

    /**
     * Fuehrt den Reifenwechsel fuer das angegebene Auto durch.
     * Diese Methode simuliert die Arbeit der Boxencrew und meldet
     * dann den Abschluss an den wartenden AutoThread.
     *
     * @Vorbedingung auto darf nicht null sein
     * @Nachbedingung Reifenwechsel ist abgeschlossen
     * @Nachbedingung AutoThread wurde benachrichtigt
     *
     * @param auto Das zu bedienende Auto
     */
    private void fuehreReifenwechselDurch(Auto auto) {
        RennLogger.logThread(RennLogger.LogLevel.INFO,
                "Boxencrew " + team.getName() + " beginnt Reifenwechsel fuer " +
                        auto.getFahrer().getKuerzel());

        long startZeit = System.currentTimeMillis();

        try {
            // Gewuenschten Reifentyp ermitteln
            ReifenTyp neuerTyp = boxenZugriff.getGewaehlterReifenTyp();
            if (neuerTyp == null) {
                neuerTyp = ReifenTyp.MEDIUM;
            }

            // Reifenwechsel simulieren (zufaellige Dauer zwischen MIN und MAX)
            long pitstopDauer = Konfiguration.berechneZufaelligePitstopDauer();
            long skalierteZeit = Konfiguration.skaliereZeit(pitstopDauer, simulationsGeschwindigkeit);

            RennLogger.logThread(RennLogger.LogLevel.DEBUG,
                    "Pitstop-Dauer: " + pitstopDauer + "ms (skaliert: " + skalierteZeit + "ms)");

            // Arbeit simulieren
            Thread.sleep(skalierteZeit);

            // Statistiken aktualisieren
            durchgefuehrtePitstops++;
            gesamtServiceZeit += pitstopDauer;

            long verstricheneZeit = System.currentTimeMillis() - startZeit;

            RennLogger.logThread(RennLogger.LogLevel.INFO,
                    "Boxencrew " + team.getName() + " Reifenwechsel FERTIG fuer " +
                            auto.getFahrer().getKuerzel() + " - Neue Reifen: " + neuerTyp +
                            " (Dauer: " + verstricheneZeit + "ms)");

        } catch (InterruptedException e) {
            RennLogger.logThread(RennLogger.LogLevel.WARNING,
                    "Reifenwechsel unterbrochen fuer " + auto.getFahrer().getKuerzel());
            Thread.currentThread().interrupt();
        } finally {
            // Service als abgeschlossen melden - AutoThread kann weiterfahren
            boxenZugriff.serviceAbschliessen();
        }
    }

    // ========== Steuerungsmethoden ==========

    /**
     * Stoppt den Thread sicher.
     * Der Thread wird bei der naechsten Gelegenheit beendet
     * (nach Ablauf des Timeouts in warteAufAutoMitTimeout).
     *
     * @Vorbedingung Keine
     * @Nachbedingung running ist false
     */
    public void stoppen() {
        this.running = false;
        this.interrupt();
    }

    /**
     * Setzt die Simulationsgeschwindigkeit.
     * Beeinflusst die Dauer des Reifenwechsels.
     *
     * @Vorbedingung geschwindigkeit muss > 0 sein
     * @Nachbedingung simulationsGeschwindigkeit ist aktualisiert
     *
     * @param geschwindigkeit Neue Geschwindigkeit (z.B. 2.0 fuer doppelte Geschwindigkeit)
     */
    public void setSimulationsGeschwindigkeit(double geschwindigkeit) {
        if (geschwindigkeit > 0) {
            this.simulationsGeschwindigkeit = geschwindigkeit;
        }
    }

    // ========== Getter ==========

    /**
     * Gibt das Team dieser Boxencrew zurueck.
     *
     * @return Das Team
     */
    public Team getTeam() {
        return team;
    }

    /**
     * Gibt die Anzahl der durchgefuehrten Pitstops zurueck.
     *
     * @return Anzahl der Pitstops
     */
    public int getDurchgefuehrtePitstops() {
        return durchgefuehrtePitstops;
    }

    /**
     * Gibt die gesamte Service-Zeit zurueck.
     *
     * @return Gesamte Service-Zeit in Millisekunden
     */
    public long getGesamtServiceZeit() {
        return gesamtServiceZeit;
    }

    /**
     * Berechnet die durchschnittliche Pitstop-Dauer.
     *
     * @return Durchschnittliche Dauer in Millisekunden, 0 wenn keine Pitstops
     */
    public long getDurchschnittlichePitstopDauer() {
        if (durchgefuehrtePitstops == 0) {
            return 0;
        }
        return gesamtServiceZeit / durchgefuehrtePitstops;
    }

    /**
     * Prueft ob der Thread noch laeuft.
     *
     * @return true wenn der Thread aktiv ist
     */
    public boolean isRunning() {
        return running;
    }
}