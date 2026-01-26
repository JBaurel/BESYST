package threads;

import model.Auto;
import model.RennDaten;
import model.Team;
import model.enumType.ReifenTyp;
import model.enumType.RennStatus;
import util.Konfiguration;
import util.RennLogger;

import java.util.List;

/**
 * Thread-Klasse fuer die Rennstall-Strategie eines Teams.
 * Jedes Team hat einen RennstallThread, der die Pitstop-Strategie
 * fuer beide Teamfahrzeuge ueberwacht und steuert.
 *
 * Der RennstallThread verwendet das Push-Modell zur Kommunikation:
 * Er setzt volatile Flags an den Auto-Objekten, die von den AutoThreads
 * regelmaessig geprueft werden.
 *
 * Die Strategie-Logik beruecksichtigt:
 * - Reifenzustand (kritisch bei > 80% Abnutzung)
 * - Verbleibende Runden (Pflicht-Pitstop zwischen Runde 8 und 5 vor Ende)
 * - Position im Rennen (wichtige Positionen werden geschuetzt)
 * - Reifentyp-Wahl basierend auf verbleibenden Runden
 *
 * @author F1 Simulation Team
 * @version 1.0
 */
public class RennstallThread extends Thread {

    private final Team team;
    private final Auto auto1;
    private final Auto auto2;
    private final RennDaten rennDaten;

    // Steuerungsflags
    private volatile boolean running;
    private volatile boolean rennBeendet;

    /**
     * Erstellt einen neuen RennstallThread fuer das angegebene Team.
     *
     * @Vorbedingung team darf nicht null sein
     * @Vorbedingung rennDaten darf nicht null sein
     * @Nachbedingung Thread ist initialisiert aber noch nicht gestartet
     *
     * @param team Das Team fuer das die Strategie gesteuert wird
     * @param rennDaten Die zentralen Renndaten
     * @throws IllegalArgumentException wenn Parameter null sind
     */
    public RennstallThread(Team team, RennDaten rennDaten) {
        super("RennstallThread-" + team.getName());

        if (team == null) {
            throw new IllegalArgumentException("Team darf nicht null sein");
        }
        if (rennDaten == null) {
            throw new IllegalArgumentException("RennDaten darf nicht null sein");
        }

        this.team = team;
        this.rennDaten = rennDaten;

        // Autos des Teams finden
        Auto gefundenesAuto1 = null;
        Auto gefundenesAuto2 = null;

        for (Auto auto : rennDaten.getAutos()) {
            if (auto.getTeam().equals(team)) {
                if (gefundenesAuto1 == null) {
                    gefundenesAuto1 = auto;
                } else {
                    gefundenesAuto2 = auto;
                    break;
                }
            }
        }

        this.auto1 = gefundenesAuto1;
        this.auto2 = gefundenesAuto2;

        this.running = true;
        this.rennBeendet = false;
    }

    /**
     * Hauptmethode des Threads - ueberwacht kontinuierlich die Strategie.
     * Der Thread prueft in regelmaessigen Intervallen den Zustand
     * beider Teamfahrzeuge und fordert bei Bedarf Pitstops an.
     */
    @Override
    public void run() {
        RennLogger.logThread(RennLogger.LogLevel.INFO,
                "Rennstall " + team.getName() + " Strategie-Thread gestartet");

        try {
            // Warten bis das Rennen gestartet ist
            while (running && rennDaten.getStatus() != RennStatus.LAEUFT) {
                Thread.sleep(100);
            }

            // Strategie-Schleife
            while (running && !rennBeendet) {
                // Strategie fuer beide Autos pruefen
                pruefeStrategieFuerAuto(auto1);
                pruefeStrategieFuerAuto(auto2);

                // Pause zwischen Strategie-Checks
                long checkIntervall = Konfiguration.skaliereZeit(
                        Konfiguration.STRATEGIE_CHECK_INTERVALL_MS,
                        rennDaten.getSimulationsGeschwindigkeit());
                Thread.sleep(checkIntervall);
            }

        } catch (InterruptedException e) {
            RennLogger.logThread(RennLogger.LogLevel.WARNING,
                    "Rennstall " + team.getName() + " Strategie unterbrochen");
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            RennLogger.error("Fehler im RennstallThread " + team.getName(), e);
        } finally {
            RennLogger.logThread(RennLogger.LogLevel.INFO,
                    "Rennstall " + team.getName() + " Strategie-Thread beendet");
        }
    }

    /**
     * Prueft die Strategie fuer ein einzelnes Auto und fordert ggf. einen Pitstop an.
     * Die Entscheidungslogik beruecksichtigt mehrere Faktoren.
     *
     * @param auto Das zu pruefende Auto (kann null sein)
     */
    private void pruefeStrategieFuerAuto(Auto auto) {
        if (auto == null || auto.istImZiel()) {
            return;
        }

        // Wenn bereits ein Pitstop angefordert ist, nichts tun
        if (auto.istPitstopAngefordert()) {
            return;
        }

        int aktuelleRunde = auto.getAktuelleRunde();
        int gesamtRunden = rennDaten.getStrecke().getAnzahlRunden();
        int verbleibendeRunden = gesamtRunden - aktuelleRunde;

        // Pruefen ob ein Pitstop noetig/sinnvoll ist
        boolean pitstopNoetig = false;
        String grund = "";

        // Grund 1: Pflicht-Pitstop noch nicht erledigt
        if (!auto.isPflichtPitstopErledigt()) {
            // Pruefen ob wir im Pitstop-Fenster sind
            if (Konfiguration.istImPitstopFenster(aktuelleRunde, gesamtRunden)) {
                // Spätestens bei 5 Runden vor Ende den Pflicht-Pitstop machen
                if (verbleibendeRunden <= Konfiguration.PFLICHT_PITSTOP_RUNDEN_VOR_ENDE) {
                    pitstopNoetig = true;
                    grund = "Pflicht-Pitstop (letzte Chance)";
                }
                // Oder wenn die Reifen schlecht genug sind
                else if (auto.getAktuelleReifen().getAbnutzungProzent() >= 60) {
                    pitstopNoetig = true;
                    grund = "Pflicht-Pitstop (Reifen bei " +
                            String.format("%.0f", auto.getAktuelleReifen().getAbnutzungProzent()) + "%)";
                }
            }
        }

        // Grund 2: Reifen kritisch abgenutzt (auch nach Pflicht-Pitstop)
        if (!pitstopNoetig && auto.getAktuelleReifen().istKritischAbgenutzt()) {
            // Nur wenn sich der Stopp noch lohnt (mehr als 2 Runden uebrig)
            if (verbleibendeRunden > 2) {
                pitstopNoetig = true;
                grund = "Kritische Reifenabnutzung (" +
                        String.format("%.0f", auto.getAktuelleReifen().getAbnutzungProzent()) + "%)";
            }
        }

        // Wenn Pitstop noetig, den passenden Reifentyp waehlen und anfordern
        if (pitstopNoetig) {
            ReifenTyp neuerTyp = waehleReifenTyp(verbleibendeRunden);

            RennLogger.logThread(RennLogger.LogLevel.INFO,
                    "STRATEGIE " + team.getName() + ": " + auto.getFahrer().getKuerzel() +
                            " -> Pitstop angefordert! Grund: " + grund +
                            ", Neue Reifen: " + neuerTyp);

            // Pitstop anfordern (volatile Write)
            auto.fordertPitstopAn(neuerTyp);
        }
    }

    /**
     * Waehlt den optimalen Reifentyp basierend auf den verbleibenden Runden.
     * Die Strategie ist:
     * - Mehr als 15 Runden: Hard (lange Haltbarkeit)
     * - 8 bis 15 Runden: Medium (ausgewogen)
     * - Weniger als 8 Runden: Soft (maximale Geschwindigkeit)
     *
     * @param verbleibendeRunden Anzahl der noch zu fahrenden Runden
     * @return Der empfohlene Reifentyp
     */
    private ReifenTyp waehleReifenTyp(int verbleibendeRunden) {
        if (verbleibendeRunden > Konfiguration.REIFEN_HARD_AB_RUNDEN) {
            return ReifenTyp.HARD;
        } else if (verbleibendeRunden >= Konfiguration.REIFEN_MEDIUM_AB_RUNDEN) {
            return ReifenTyp.MEDIUM;
        } else {
            return ReifenTyp.SOFT;
        }
    }

    /**
     * Berechnet eine Strategie-Empfehlung fuer ein Auto.
     * Diese Methode kann von der GUI aufgerufen werden, um dem Benutzer
     * Informationen zur aktuellen Strategie anzuzeigen.
     *
     * @param auto Das Auto fuer das die Empfehlung erstellt wird
     * @return String mit der Strategie-Empfehlung
     */
    public String getStrategieEmpfehlung(Auto auto) {
        if (auto == null || !auto.getTeam().equals(team)) {
            return "Kein Auto des Teams";
        }

        if (auto.istImZiel()) {
            return "Rennen beendet";
        }

        int verbleibendeRunden = rennDaten.getStrecke().getAnzahlRunden() - auto.getAktuelleRunde();
        double reifenZustand = auto.getAktuelleReifen().getAbnutzungProzent();

        StringBuilder sb = new StringBuilder();
        sb.append("Runde ").append(auto.getAktuelleRunde());
        sb.append("/").append(rennDaten.getStrecke().getAnzahlRunden());
        sb.append(" | Reifen: ").append(auto.getAktuelleReifen().getTyp());
        sb.append(" (").append(String.format("%.0f", reifenZustand)).append("% abgenutzt)");
        sb.append(" | Pitstops: ").append(auto.getAnzahlPitstops());

        if (!auto.isPflichtPitstopErledigt()) {
            sb.append(" | PFLICHT-PITSTOP FEHLT!");
        }

        if (auto.istPitstopAngefordert()) {
            sb.append(" | Pitstop angefordert: ").append(auto.getAngefordertReifenTyp());
        }

        return sb.toString();
    }

    // ========== Steuerungsmethoden ==========

    /**
     * Stoppt den Thread sicher.
     *
     * @Vorbedingung Keine
     * @Nachbedingung running ist false
     */
    public void stoppen() {
        this.running = false;
        this.rennBeendet = true;
        this.interrupt();
    }

    /**
     * Signalisiert dass das Rennen beendet ist.
     *
     * @Vorbedingung Keine
     * @Nachbedingung rennBeendet ist true
     */
    public void setzeRennBeendet() {
        this.rennBeendet = true;
    }

    // ========== Getter ==========

    /**
     * Gibt das Team dieses Strategie-Threads zurueck.
     *
     * @return Das Team
     */
    public Team getTeam() {
        return team;
    }

    /**
     * Gibt das erste Auto des Teams zurueck.
     *
     * @return Das erste Auto
     */
    public Auto getAuto1() {
        return auto1;
    }

    /**
     * Gibt das zweite Auto des Teams zurueck.
     *
     * @return Das zweite Auto
     */
    public Auto getAuto2() {
        return auto2;
    }

    /**
     * Prueft ob der Thread noch laeuft.
     *
     * @return true wenn der Thread aktiv ist
     */
    public boolean isRunning() {
        return running && !rennBeendet;
    }
}
