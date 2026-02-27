package threads;

import model.Auto;
import model.RennDaten;
import model.Rennergebnis;
import model.enumType.AutoStatus;
import model.enumType.RennStatus;
import synchronisation.StartFreigabe;
import util.Konfiguration;
import util.RennLogger;

import java.util.List;

/**
 * Thread-Klasse fuer den Rennleiter.
 * Der RennleiterThread ist fuer die uebergeordnete Steuerung des Rennens
 * verantwortlich und existiert nur einmal pro Rennen.
 *
 * Die Aufgaben des Rennleiters sind:
 * 1. Durchfuehrung der Startampel-Sequenz und Freigabe
 * 2. Ueberwachung des Rennfortschritts
 * 3. Erkennung des Rennendes (wenn der Fuehrende im Ziel ist)
 * 4. Benachrichtigung aller anderen Threads ueber das Rennende
 * 5. Erstellung der Endergebnisse
 *
 * Der RennleiterThread verwendet die StartFreigabe-Klasse mit CountDownLatch
 * fuer die gleichzeitige Freigabe aller Autos beim Rennstart.
 *
 * @author F1 Simulation Team
 * @version 1.0
 */
public class RennleiterThread extends Thread {

    private final RennDaten rennDaten;
    private final StartFreigabe startFreigabe;
    private final List<AutoThread> autoThreads;


    private volatile boolean running;
    private volatile boolean rennGestartet;
    private volatile boolean rennBeendet;

    private RennleiterListener listener;

    /**
     * Interface fuer Callbacks vom RennleiterThread.
     * Ermoeglicht der GUI, auf Rennereignisse zu reagieren.
     */
    public interface RennleiterListener {
        /**
         * Wird aufgerufen wenn die Startsequenz beginnt.
         */
        void onStartSequenzBeginn();

        /**
         * Wird aufgerufen wenn ein Startampel-Licht angeht.
         * @param lichtNummer Nummer des Lichts (1-5)
         */
        void onStartLicht(int lichtNummer);

        /**
         * Wird aufgerufen wenn der Start freigegeben wird.
         */
        void onStartFreigabe();

        /**
         * Wird aufgerufen wenn ein Auto ins Ziel kommt.
         * @param auto Das Auto das ins Ziel gekommen ist
         * @param position Die Zielposition
         */
        void onAutoImZiel(Auto auto, int position);

        /**
         * Wird aufgerufen wenn das Rennen beendet ist.
         * @param ergebnisse Die Endergebnisse
         */
        void onRennende(List<Rennergebnis> ergebnisse);
    }

    /**
     * Erstellt einen neuen RennleiterThread.
     *
     * @Vorbedingung rennDaten darf nicht null sein
     * @Vorbedingung startFreigabe darf nicht null sein
     * @Vorbedingung autoThreads darf nicht null oder leer sein
     * @Nachbedingung Thread ist initialisiert aber noch nicht gestartet
     *
     * @param rennDaten Die zentralen Renndaten
     * @param startFreigabe Die Startfreigabe-Synchronisation
     * @param autoThreads Liste aller AutoThreads
     * @throws IllegalArgumentException wenn Parameter ungueltig sind
     */
    public RennleiterThread(RennDaten rennDaten, StartFreigabe startFreigabe,
                            List<AutoThread> autoThreads) {
        super("RennleiterThread");

        if (rennDaten == null) {
            throw new IllegalArgumentException("RennDaten darf nicht null sein");
        }
        if (startFreigabe == null) {
            throw new IllegalArgumentException("StartFreigabe darf nicht null sein");
        }
        if (autoThreads == null || autoThreads.isEmpty()) {
            throw new IllegalArgumentException("AutoThreads darf nicht null oder leer sein");
        }

        this.rennDaten = rennDaten;
        this.startFreigabe = startFreigabe;
        this.autoThreads = autoThreads;

        this.running = true;
        this.rennGestartet = false;
        this.rennBeendet = false;
        this.listener = null;
    }

    /**
     * Setzt den Listener fuer Rennereignisse.
     *
     * @param listener Der Listener oder null zum Entfernen
     */
    public void setRennleiterListener(RennleiterListener listener) {
        this.listener = listener;
    }

    /**
     * Hauptmethode des Threads - fuehrt den kompletten Rennablauf durch.
     */
    @Override
    public void run() {
        RennLogger.logThread(RennLogger.LogLevel.INFO, "Rennleiter gestartet");

        try {
            fuehreStartsequenzDurch();

            if (!running) return;
            ueberwacheRennen();


            if (rennBeendet) {
                erstelleErgebnisse();
            }

        } catch (InterruptedException e) {
            RennLogger.logThread(RennLogger.LogLevel.WARNING, "Rennleiter unterbrochen");
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            RennLogger.error("Fehler im RennleiterThread", e);
        } finally {
            RennLogger.logThread(RennLogger.LogLevel.INFO, "Rennleiter beendet");
        }
    }

    /**
     * Fuehrt die komplette Startsequenz durch.
     * Wartet bis alle Autos bereit sind, zeigt dann die Ampelsequenz
     * und gibt schliesslich den Start frei.
     *
     * @throws InterruptedException wenn der Thread unterbrochen wird
     */
    private void fuehreStartsequenzDurch() throws InterruptedException {
        RennLogger.logThread(RennLogger.LogLevel.INFO, "Startsequenz beginnt...");

        if (listener != null) {
            listener.onStartSequenzBeginn();
        }

        rennDaten.setStatus(RennStatus.STARTPHASE);

        RennLogger.logThread(RennLogger.LogLevel.INFO,
                "Warte auf Autos... (" + startFreigabe.getBereiteAutos() + "/" +
                        startFreigabe.getAnzahlAutos() + ")");

        while (!startFreigabe.sindAlleAutosBereit() && running) {
            Thread.sleep(100);
            RennLogger.logThread(RennLogger.LogLevel.DEBUG,
                    "Bereite Autos: " + startFreigabe.getBereiteAutos());
        }

        if (!running) return;

        RennLogger.logThread(RennLogger.LogLevel.INFO, "Alle Autos bereit!");


        StartFreigabe.StartSequenzListener ampelListener = new StartFreigabe.StartSequenzListener() {
            @Override
            public void lichtAn(int lichtNummer) {
                RennLogger.logThread(RennLogger.LogLevel.INFO,
                        "LICHT " + lichtNummer + " AN");
                if (listener != null) {
                    listener.onStartLicht(lichtNummer);
                }
            }

            @Override
            public void lichterAus() {
                RennLogger.logThread(RennLogger.LogLevel.INFO,
                        "LICHTER AUS - GO GO GO!");
                if (listener != null) {
                    listener.onStartFreigabe();
                }
            }

            @Override
            public void fehlstart(Auto auto) {
                RennLogger.logThread(RennLogger.LogLevel.WARNING,
                        "FEHLSTART: " + auto.getFahrer().getKuerzel());
            }
        };

        startFreigabe.setStartSequenzListener(ampelListener);

        startFreigabe.starteStartsequenz();
        rennGestartet = true;
        rennDaten.setStatus(RennStatus.LAEUFT);
        rennDaten.setRennStartzeit(System.currentTimeMillis());

        RennLogger.logThread(RennLogger.LogLevel.INFO,
                "RENNEN GESTARTET! " + rennDaten.getStrecke().getAnzahlRunden() + " Runden");
    }

    /**
     * Ueberwacht das laufende Rennen bis es beendet ist.
     * Prueft regelmaessig, ob ein Auto ins Ziel gekommen ist.
     * Sobald der Fuehrende das Ziel erreicht, wird das Rennen fuer alle beendet.
     *
     * @throws InterruptedException wenn der Thread unterbrochen wird
     */
    private void ueberwacheRennen() throws InterruptedException {
        RennLogger.logThread(RennLogger.LogLevel.INFO, "Ueberwache Rennen...");

        int zielPosition = 0;
        Auto fuehrenderImZiel = null;

        while (running && !rennBeendet) {

            Thread.sleep(Konfiguration.skaliereZeit(
                    Konfiguration.GUI_UPDATE_INTERVALL_MS,
                    rennDaten.getSimulationsGeschwindigkeit()));


            List<Auto> reihenfolge = rennDaten.getRennreihenfolge();

            for (Auto auto : reihenfolge) {
                if (auto.istImZiel() && auto.getStatus() == AutoStatus.IM_ZIEL) {

                    if (fuehrenderImZiel == null) {

                        fuehrenderImZiel = auto;
                        zielPosition = 1;

                        RennLogger.logThread(RennLogger.LogLevel.INFO,
                                "FUEHRENDER IM ZIEL: " + auto.getFahrer().getKuerzel());

                        if (listener != null) {
                            listener.onAutoImZiel(auto, zielPosition);
                        }


                        beendeRennenFuerAlle();
                    }
                }
            }


            boolean alleBeendet = true;
            for (AutoThread autoThread : autoThreads) {
                if (autoThread.isRunning() && !autoThread.getAuto().istImZiel()) {
                    alleBeendet = false;
                    break;
                }
            }

            if (alleBeendet || fuehrenderImZiel != null) {
                // Kurze Wartezeit damit alle Threads reagieren koennen
                Thread.sleep(500);
                rennBeendet = true;
            }
        }

        RennLogger.logThread(RennLogger.LogLevel.INFO, "Rennueberwachung beendet");
    }

    /**
     * Signalisiert allen AutoThreads, dass das Rennen beendet ist.
     * Dies geschieht, wenn der fuehrende Fahrer das Ziel erreicht hat.
     */
    private void beendeRennenFuerAlle() {
        RennLogger.logThread(RennLogger.LogLevel.INFO,
                "RENNENDE - Alle Autos werden gestoppt");

        rennDaten.setStatus(RennStatus.BEENDET);

        for (AutoThread autoThread : autoThreads) {
            autoThread.setzeRennBeendet();
        }
    }

    /**
     * Erstellt die Endergebnisse basierend auf der finalen Reihenfolge.
     */
    private void erstelleErgebnisse() {
        RennLogger.logThread(RennLogger.LogLevel.INFO, "Erstelle Endergebnisse...");

        List<Auto> reihenfolge = rennDaten.getRennreihenfolge();


        long siegerZeit = 0;
        if (!reihenfolge.isEmpty()) {
            siegerZeit = reihenfolge.get(0).getGesamtzeit();
        }

        int position = 1;
        for (Auto auto : reihenfolge) {
            long rueckstand = auto.getGesamtzeit() - siegerZeit;

            Rennergebnis ergebnis = new Rennergebnis(
                    auto,
                    position,
                    auto.getGesamtzeit(),
                    auto.getAktuelleRunde(),
                    auto.getBesteRundenzeit(),
                    auto.getAnzahlPitstops(),
                    null,
                    rueckstand
            );

            rennDaten.addErgebnis(ergebnis);

            RennLogger.logThread(RennLogger.LogLevel.INFO,
                    "P" + position + ": " + auto.getFahrer().getName() +
                            " (" + auto.getTeam().getName() + ") - " +
                            ergebnis.getFormattierterRueckstand());

            if (listener != null) {
                listener.onAutoImZiel(auto, position);
            }

            position++;
        }

        RennLogger.logThread(RennLogger.LogLevel.INFO,
                "=== RENNEN BEENDET ===");
        RennLogger.logThread(RennLogger.LogLevel.INFO,
                "Sieger: " + reihenfolge.get(0).getFahrer().getName());

        if (listener != null) {
            listener.onRennende(rennDaten.getErgebnisse());
        }
    }


    /**
     * Stoppt den Thread und das Rennen sicher.
     *
     * @Vorbedingung Keine
     * @Nachbedingung running ist false, Rennen wird beendet
     */
    public void stoppen() {
        this.running = false;
        this.rennBeendet = true;
        rennDaten.setStatus(RennStatus.ABGEBROCHEN);
        beendeRennenFuerAlle();
        this.interrupt();
    }

    /**
     * Pausiert das Rennen.
     * In der aktuellen Implementierung wird die Pause durch Aenderung
     * des RennStatus signalisiert. Die AutoThreads muessen diese
     * Statusaenderung beachten.
     */
    public void pausieren() {
        if (rennGestartet && !rennBeendet) {
            rennDaten.setStatus(RennStatus.PAUSIERT);
            RennLogger.logThread(RennLogger.LogLevel.INFO, "Rennen PAUSIERT");
        }
    }

    /**
     * Setzt ein pausiertes Rennen fort.
     */
    public void fortsetzen() {
        if (rennDaten.getStatus() == RennStatus.PAUSIERT) {
            rennDaten.setStatus(RennStatus.LAEUFT);
            RennLogger.logThread(RennLogger.LogLevel.INFO, "Rennen FORTGESETZT");
        }
    }

    /**
     * Prueft ob das Rennen gestartet wurde.
     *
     * @return true wenn das Rennen laeuft oder beendet ist
     */
    public boolean isRennGestartet() {
        return rennGestartet;
    }

    /**
     * Prueft ob das Rennen beendet ist.
     *
     * @return true wenn das Rennen beendet ist
     */
    public boolean isRennBeendet() {
        return rennBeendet;
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
