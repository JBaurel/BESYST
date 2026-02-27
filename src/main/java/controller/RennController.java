package controller;

import model.*;
import model.enumType.StreckenabschnittTyp;
import synchronisation.*;
import threads.AutoThread;
import threads.BoxencrewThread;
import threads.RennleiterThread;
import threads.RennstallThread;
import util.Konfiguration;
import util.RennLogger;

import java.util.*;

/**
 * Zentraler Controller fuer die Rennsimulation.
 * Diese Klasse ist verantwortlich fuer:
 * - Initialisierung aller Synchronisationsobjekte
 * - Erstellung und Start aller Threads
 * - Koordination zwischen View und Model
 * - Steuerung des Rennablaufs
 * Der Controller implementiert das MVC-Pattern und fungiert als
 * Vermittler zwischen der GUI und den Daten.
 *
 */
public class RennController {

    private final RennDaten rennDaten;

    private Map<Integer, StreckenabschnittMonitor> kurvenMonitore;
    private Map<Integer, SchikaneZugriff> schikaneZugriffe;
    private Map<Team, BoxenZugriff> boxenZugriffe;
    private PitstopLaneController pitstopController;
    private UeberholzonenManager ueberholManager;
    private StartFreigabe startFreigabe;

    private List<AutoThread> autoThreads;
    private List<BoxencrewThread> boxencrewThreads;
    private List<RennstallThread> rennstallThreads;
    private RennleiterThread rennleiterThread;

    private RennEventListener eventListener;

    private boolean initialisiert;
    private boolean rennLaeuft;

    /**
     * Interface fuer Callbacks an die GUI.
     */
    public interface RennEventListener {
        void onLogNachricht(String nachricht);
        void onRennstandUpdate();
        void onStartLicht(int nummer);
        void onStartFreigabe();
        void onRennende(List<Rennergebnis> ergebnisse);
    }

    /**
     * Erstellt einen neuen RennController.
     * @Vorbedingung Keine
     * @Nachbedingung Controller ist erstellt, aber noch nicht initialisiert
     */
    public RennController() {
        this.rennDaten = new RennDaten();
        this.initialisiert = false;
        this.rennLaeuft = false;

        RennLogger.setLogCallback(nachricht -> {
            if (eventListener != null) {
                eventListener.onLogNachricht(nachricht);
            }
        });

        RennLogger.info("RennController erstellt");
    }

    /**
     * Setzt den Event-Listener fuer GUI-Callbacks.
     * @param listener Der Listener oder null zum Entfernen
     */
    public void setEventListener(RennEventListener listener) {
        this.eventListener = listener;
    }

    /**
     * Initialisiert alle Komponenten fuer ein neues Rennen.
     * Diese Methode muss vor dem Start aufgerufen werden.
     * @Vorbedingung Kein Rennen laeuft aktuell
     * @Nachbedingung Alle Synchronisationsobjekte und Threads sind erstellt
     */
    public void initialisieren() {
        if (rennLaeuft) {
            RennLogger.warning("Kann nicht initialisieren waehrend ein Rennen laeuft");
            return;
        }

        RennLogger.info("Initialisiere Rennsimulation...");

        rennDaten.zuruecksetzen();
        erstelleZufaelligeStartaufstellung();

        erstelleSynchronisationsobjekte();

        erstelleThreads();

        initialisiert = true;
        RennLogger.info("Initialisierung abgeschlossen");
    }

    /**
     * Erstellt eine zufaellige Startaufstellung im Grid-Format.
     * Die Autos werden zufaellig auf die 20 Startpositionen verteilt.
     */
    private void erstelleZufaelligeStartaufstellung() {
        List<Auto> autos = new ArrayList<>(rennDaten.getAutos());
        Collections.shuffle(autos);

        double startFortschritt = 0.0;
        double abstandProReihe = 0.03;
        double abstandInReihe = 0.01;

        int position = 1;
        for (Auto auto : autos) {
            int reihe = (position - 1) / 2;
            boolean linkeSeite = (position % 2 == 1);

            double fortschritt = startFortschritt - (reihe * abstandProReihe);
            if (!linkeSeite) {
                fortschritt -= abstandInReihe;
            }

            auto.setFortschrittImAbschnitt(Math.max(-0.5, fortschritt));
            auto.setAktuellerAbschnittId(0);

            RennLogger.debug("Startposition " + position + ": " +
                    auto.getFahrer().getKuerzel() + " (Fortschritt: " +
                    String.format("%.2f", fortschritt) + ")");

            position++;
        }

        RennLogger.info("Zufaellige Startaufstellung erstellt");
    }

    /**
     * Erstellt alle Synchronisationsobjekte basierend auf der Strecke.
     */
    private void erstelleSynchronisationsobjekte() {
        Rennstrecke strecke = rennDaten.getStrecke();

        kurvenMonitore = new HashMap<>();
        schikaneZugriffe = new HashMap<>();

        for (Streckenabschnitt abschnitt : strecke.getAbschnitte()) {
            if (abschnitt.getTyp() == StreckenabschnittTyp.ENGE_KURVE) {
                kurvenMonitore.put(abschnitt.getId(),
                        new StreckenabschnittMonitor(abschnitt));
                RennLogger.debug("Monitor erstellt fuer: " + abschnitt.getName());
            } else if (abschnitt.getTyp() == StreckenabschnittTyp.SCHIKANE) {
                schikaneZugriffe.put(abschnitt.getId(),
                        new SchikaneZugriff(abschnitt));
                RennLogger.debug("SchikaneZugriff erstellt fuer: " + abschnitt.getName());
            }
        }


        boxenZugriffe = new HashMap<>();
        for (Box box : rennDaten.getBoxen()) {
            boxenZugriffe.put(box.getTeam(), new BoxenZugriff(box));
            RennLogger.debug("BoxenZugriff erstellt fuer: " + box.getTeam().getName());
        }

        pitstopController = new PitstopLaneController();

        ueberholManager = new UeberholzonenManager();

        startFreigabe = new StartFreigabe(Konfiguration.ANZAHL_AUTOS);

        RennLogger.info("Synchronisationsobjekte erstellt: " +
                kurvenMonitore.size() + " Kurvenmonitore, " +
                schikaneZugriffe.size() + " Schikanen, " +
                boxenZugriffe.size() + " Boxen");
    }

    /**
     * Erstellt alle Thread-Objekte.
     */
    private void erstelleThreads() {
        autoThreads = new ArrayList<>();
        for (Auto auto : rennDaten.getAutos()) {
            BoxenZugriff boxZugriff = boxenZugriffe.get(auto.getTeam());

            AutoThread autoThread = new AutoThread(
                    auto, rennDaten,
                    kurvenMonitore, schikaneZugriffe,
                    pitstopController, boxZugriff,
                    ueberholManager, startFreigabe
            );
            autoThreads.add(autoThread);
        }
        RennLogger.debug(autoThreads.size() + " AutoThreads erstellt");

        boxencrewThreads = new ArrayList<>();
        for (Team team : rennDaten.getTeams()) {
            BoxenZugriff boxZugriff = boxenZugriffe.get(team);
            BoxencrewThread crewThread = new BoxencrewThread(team, boxZugriff);
            boxencrewThreads.add(crewThread);
        }
        RennLogger.debug(boxencrewThreads.size() + " BoxencrewThreads erstellt");

        rennstallThreads = new ArrayList<>();
        for (Team team : rennDaten.getTeams()) {
            RennstallThread stallThread = new RennstallThread(team, rennDaten);
            rennstallThreads.add(stallThread);
        }
        RennLogger.debug(rennstallThreads.size() + " RennstallThreads erstellt");

        rennleiterThread = new RennleiterThread(rennDaten, startFreigabe, autoThreads);

        rennleiterThread.setRennleiterListener(new RennleiterThread.RennleiterListener() {
            @Override
            public void onStartSequenzBeginn() {
                RennLogger.info("=== STARTSEQUENZ BEGINNT ===");
            }

            @Override
            public void onStartLicht(int lichtNummer) {
                if (eventListener != null) {
                    eventListener.onStartLicht(lichtNummer);
                }
            }

            @Override
            public void onStartFreigabe() {
                if (eventListener != null) {
                    eventListener.onStartFreigabe();
                }
            }

            @Override
            public void onAutoImZiel(Auto auto, int position) {
                RennLogger.info("P" + position + ": " + auto.getFahrer().getName() + " im Ziel!");
            }

            @Override
            public void onRennende(List<Rennergebnis> ergebnisse) {
                rennLaeuft = false;
                if (eventListener != null) {
                    eventListener.onRennende(ergebnisse);
                }
            }
        });

        RennLogger.debug("RennleiterThread erstellt");
    }

    /**
     * Startet das Rennen.
     * Alle Threads werden in der richtigen Reihenfolge gestartet.
     *
     * @Vorbedingung initialisieren() wurde aufgerufen
     * @Nachbedingung Alle Threads laufen, Rennen hat begonnen
     */
    public void starteRennen() {
        if (!initialisiert) {
            RennLogger.warning("Rennen nicht initialisiert - rufe zuerst initialisieren() auf");
            return;
        }

        if (rennLaeuft) {
            RennLogger.warning("Rennen laeuft bereits");
            return;
        }

        RennLogger.info("=== STARTE RENNEN ===");
        RennLogger.info("Strecke: " + rennDaten.getStrecke().getName());
        RennLogger.info("Runden: " + rennDaten.getAnzahlRunden());
        RennLogger.info("Autos: " + rennDaten.getAutos().size());

        rennLaeuft = true;


        for (BoxencrewThread crew : boxencrewThreads) {
            crew.start();
        }
        RennLogger.debug("BoxencrewThreads gestartet");


        for (RennstallThread stall : rennstallThreads) {
            stall.start();
        }
        RennLogger.debug("RennstallThreads gestartet");


        for (AutoThread auto : autoThreads) {
            auto.start();
        }
        RennLogger.debug("AutoThreads gestartet");


        rennleiterThread.start();
        RennLogger.debug("RennleiterThread gestartet");

        RennLogger.info("Alle Threads gestartet - warte auf Startsequenz...");
    }

    /**
     * Pausiert das Rennen.
     *
     * @Vorbedingung Rennen laeuft
     * @Nachbedingung Rennen ist pausiert
     */
    public void pausieren() {
        if (!rennLaeuft) {
            return;
        }

        RennLogger.info("=== RENNEN PAUSIERT ===");
        rennleiterThread.pausieren();
    }

    /**
     * Setzt ein pausiertes Rennen fort.
     *
     * @Vorbedingung Rennen ist pausiert
     * @Nachbedingung Rennen laeuft weiter
     */
    public void fortsetzen() {
        if (!rennLaeuft) {
            return;
        }

        RennLogger.info("=== RENNEN FORTGESETZT ===");
        rennleiterThread.fortsetzen();
    }

    /**
     * Stoppt das Rennen vorzeitig.
     * Alle Threads werden sicher beendet.
     * @Vorbedingung Rennen laeuft
     * @Nachbedingung Alle Threads sind beendet, Rennen ist gestoppt
     */
    public void stoppeRennen() {
        if (!rennLaeuft && !initialisiert) {
            return;
        }

        RennLogger.info("=== STOPPE RENNEN ===");

        if (rennleiterThread != null) {
            rennleiterThread.stoppen();
        }

        for (RennstallThread stall : rennstallThreads) {
            stall.stoppen();
        }

        for (AutoThread auto : autoThreads) {
            auto.stoppen();
        }

        for (BoxencrewThread crew : boxencrewThreads) {
            crew.stoppen();
        }

        rennLaeuft = false;
        initialisiert = false;

        RennLogger.info("Alle Threads gestoppt");
    }

    /**
     * Setzt die Simulationsgeschwindigkeit.
     * @param geschwindigkeit Neue Geschwindigkeit (1.0, 2.0, 5.0 oder 10.0)
     */
    public void setSimulationsGeschwindigkeit(double geschwindigkeit) {
        rennDaten.setSimulationsGeschwindigkeit(geschwindigkeit);

        // Geschwindigkeit weitergeben
        for (AutoThread auto : autoThreads) {
            auto.setSimulationsGeschwindigkeit(geschwindigkeit);
        }
        for (BoxencrewThread crew : boxencrewThreads) {
            crew.setSimulationsGeschwindigkeit(geschwindigkeit);
        }

        RennLogger.info("Simulationsgeschwindigkeit: " + geschwindigkeit + "x");
    }

    /**
     * Setzt die Rundenanzahl.
     * Nur vor dem Start moeglich.
     * @param anzahl Neue Rundenanzahl (20-50)
     */
    public void setRundenanzahl(int anzahl) {
        if (rennLaeuft) {
            RennLogger.warning("Rundenanzahl kann waehrend des Rennens nicht geaendert werden");
            return;
        }

        if (anzahl < Konfiguration.MIN_RUNDENANZAHL || anzahl > Konfiguration.MAX_RUNDENANZAHL) {
            RennLogger.warning("Rundenanzahl muss zwischen " +
                    Konfiguration.MIN_RUNDENANZAHL + " und " +
                    Konfiguration.MAX_RUNDENANZAHL + " liegen");
            return;
        }

        rennDaten.setAnzahlRunden(anzahl);
        RennLogger.info("Rundenanzahl: " + anzahl);
    }


    public RennDaten getRennDaten() {
        return rennDaten;
    }

    public boolean isInitialisiert() {
        return initialisiert;
    }

    public boolean isRennLaeuft() {
        return rennLaeuft;
    }

    public UeberholzonenManager getUeberholManager() {
        return ueberholManager;
    }

    public List<AutoThread> getAutoThreads() {
        return autoThreads;
    }
}

