package threads;

import model.*;
import model.enumType.AutoStatus;
import model.enumType.ReifenTyp;
import synchronisation.*;
import util.Konfiguration;
import util.RennLogger;

import java.util.List;
import java.util.Map;

/**
 * Thread-Klasse fuer ein einzelnes Rennfahrzeug.
 * Jedes Auto im Rennen wird von einem eigenen AutoThread gesteuert,
 * der die Fahrt durch alle Streckenabschnitte, Pitstops und
 * Ueberholmanoever koordiniert.
 *
 * Der AutoThread durchlaeuft folgende Phasen:
 * 1. Warten auf Startfreigabe
 * 2. Fahren durch die Streckenabschnitte
 * 3. Bei Pitstop-Anforderung: Abzweig zur Boxengasse
 * 4. Nach letzter Runde: Ins Ziel fahren
 *
 * Die Synchronisation erfolgt ueber die Klassen der Synchronisationsschicht:
 * - StreckenabschnittMonitor fuer enge Kurven
 * - SchikaneZugriff fuer Schikanen
 * - PitstopLaneController fuer die Boxengasse
 * - BoxenZugriff fuer den Reifenwechsel
 *
 * @author F1 Simulation Team
 * @version 1.0
 */
public class AutoThread extends Thread {

    private final Auto auto;
    private final Rennstrecke strecke;
    private final RennDaten rennDaten;


    private final Map<Integer, StreckenabschnittMonitor> kurvenMonitore;
    private final Map<Integer, SchikaneZugriff> schikaneZugriffe;
    private final PitstopLaneController pitstopController;
    private final BoxenZugriff boxenZugriff;
    private final UeberholzonenManager ueberholManager;
    private final StartFreigabe startFreigabe;


    private volatile boolean running;
    private volatile boolean rennBeendet;
    private double simulationsGeschwindigkeit;

    /**
     * Erstellt einen neuen AutoThread fuer das angegebene Fahrzeug.
     *
     * @Vorbedingung Alle Parameter duerfen nicht null sein
     * @Nachbedingung Thread ist initialisiert aber noch nicht gestartet
     *
     * @param auto Das zu steuernde Fahrzeug
     * @param rennDaten Die zentralen Renndaten
     * @param kurvenMonitore Map der Monitore fuer enge Kurven (AbschnittID -> Monitor)
     * @param schikaneZugriffe Map der Schikanen-Zugriffe (AbschnittID -> Zugriff)
     * @param pitstopController Controller fuer die Boxengasse
     * @param boxenZugriff Zugriffskontrolle fuer die Teambox
     * @param ueberholManager Manager fuer Ueberholmanoever
     * @param startFreigabe Synchronisation fuer den Rennstart
     */
    public AutoThread(Auto auto, RennDaten rennDaten,
                      Map<Integer, StreckenabschnittMonitor> kurvenMonitore,
                      Map<Integer, SchikaneZugriff> schikaneZugriffe,
                      PitstopLaneController pitstopController,
                      BoxenZugriff boxenZugriff,
                      UeberholzonenManager ueberholManager,
                      StartFreigabe startFreigabe) {

        super("AutoThread-" + auto.getStartnummer() + "-" + auto.getFahrer().getKuerzel());

        this.auto = auto;
        this.rennDaten = rennDaten;
        this.strecke = rennDaten.getStrecke();

        this.kurvenMonitore = kurvenMonitore;
        this.schikaneZugriffe = schikaneZugriffe;
        this.pitstopController = pitstopController;
        this.boxenZugriff = boxenZugriff;
        this.ueberholManager = ueberholManager;
        this.startFreigabe = startFreigabe;

        this.running = true;
        this.rennBeendet = false;
        this.simulationsGeschwindigkeit = Konfiguration.DEFAULT_GESCHWINDIGKEIT;
    }

    /**
     * Hauptmethode des Threads - fuehrt das Rennen durch.
     * Der Thread laeuft bis das Rennen beendet ist oder running auf false gesetzt wird.
     */
    @Override
    public void run() {
        RennLogger.logThread(RennLogger.LogLevel.INFO,
                "AutoThread gestartet fuer " + auto);

        try {

            warteAufStart();

            if (!running || rennBeendet) return;


            fahreRennen();

        } catch (InterruptedException e) {
            RennLogger.logThread(RennLogger.LogLevel.WARNING,
                    "AutoThread unterbrochen: " + auto.getFahrer().getKuerzel());
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            // Fehler loggen und ignorieren (wie spezifiziert)
            RennLogger.error("Fehler im AutoThread " + auto.getFahrer().getKuerzel(), e);
        } finally {
            RennLogger.logThread(RennLogger.LogLevel.INFO,
                    "AutoThread beendet fuer " + auto.getFahrer().getKuerzel());
        }
    }

    /**
     * Wartet auf die Startfreigabe durch den Rennleiter.
     *
     * @throws InterruptedException wenn der Thread unterbrochen wird
     */
    private void warteAufStart() throws InterruptedException {
        RennLogger.logThread(RennLogger.LogLevel.DEBUG,
                auto.getFahrer().getKuerzel() + " wartet auf Start");

        startFreigabe.warteAufStartfreigabe(auto);

        // Nach Startfreigabe: Status auf FAEHRT setzen und erste Runde starten
        auto.setStatus(AutoStatus.FAEHRT);
        auto.starteNeueRunde(System.currentTimeMillis());

        RennLogger.logThread(RennLogger.LogLevel.INFO,
                auto.getFahrer().getKuerzel() + " GESTARTET - Runde 1");
    }

    /**
     * Hauptschleife: Faehrt alle Runden des Rennens.
     *
     * @throws InterruptedException wenn der Thread unterbrochen wird
     */
    private void fahreRennen() throws InterruptedException {
        int gesamtRunden = strecke.getAnzahlRunden();

        while (running && !rennBeendet && auto.getAktuelleRunde() <= gesamtRunden) {

            durchfahreAktuellenAbschnitt();

            if (!running || rennBeendet) break;


            wechsleZumNaechstenAbschnitt();
        }


        if (running && !rennBeendet) {
            auto.setzeImZiel();
            RennLogger.logThread(RennLogger.LogLevel.INFO,
                    auto.getFahrer().getKuerzel() + " IM ZIEL nach " +
                            auto.getAktuelleRunde() + " Runden!");
        }
    }

    /**
     * Durchfaehrt den aktuellen Streckenabschnitt.
     * Je nach Abschnittstyp werden unterschiedliche Synchronisationsmechanismen verwendet.
     *
     * @throws InterruptedException wenn der Thread unterbrochen wird
     */
    private void durchfahreAktuellenAbschnitt() throws InterruptedException {
        int abschnittId = auto.getAktuellerAbschnittId();
        Streckenabschnitt abschnitt = strecke.getAbschnitt(abschnittId);

        RennLogger.logThread(RennLogger.LogLevel.DEBUG,
                auto.getFahrer().getKuerzel() + " betritt Abschnitt " + abschnitt.getName());


        switch (abschnitt.getTyp()) {
            case ENGE_KURVE:
                durchfahreEngeKurve(abschnitt);
                break;

            case SCHIKANE:
                durchfahreSchikane(abschnitt);
                break;

            case DRS_ZONE:
            case GERADE:
                durchfahreGeradeOderDRS(abschnitt);
                break;

            default:
                durchfahreNormalenAbschnitt(abschnitt);
                break;
        }
    }

    /**
     * Durchfaehrt eine enge Kurve mit Monitor-Synchronisation.
     *
     * @param abschnitt Der Kurven-Abschnitt
     * @throws InterruptedException wenn der Thread unterbrochen wird
     */
    private void durchfahreEngeKurve(Streckenabschnitt abschnitt) throws InterruptedException {
        StreckenabschnittMonitor monitor = kurvenMonitore.get(abschnitt.getId());

        if (monitor != null) {
            auto.setStatus(AutoStatus.WARTET_AUF_ABSCHNITT);
            RennLogger.logThread(RennLogger.LogLevel.DEBUG,
                    auto.getFahrer().getKuerzel() + " wartet auf Kurve " + abschnitt.getName());


            monitor.einfahren(auto);

            auto.setStatus(AutoStatus.IN_KRITISCHEM_BEREICH);
            RennLogger.logThread(RennLogger.LogLevel.DEBUG,
                    auto.getFahrer().getKuerzel() + " in Kurve " + abschnitt.getName());

            try {

                simuliereDurchfahrt(abschnitt);
            } finally {

                monitor.ausfahren(auto);
                auto.setStatus(AutoStatus.FAEHRT);
            }
        } else {

            simuliereDurchfahrt(abschnitt);
        }
    }

    /**
     * Durchfaehrt eine Schikane mit Semaphore-Synchronisation.
     *
     * @param abschnitt Der Schikanen-Abschnitt
     * @throws InterruptedException wenn der Thread unterbrochen wird
     */
    private void durchfahreSchikane(Streckenabschnitt abschnitt) throws InterruptedException {
        SchikaneZugriff zugriff = schikaneZugriffe.get(abschnitt.getId());

        if (zugriff != null) {
            auto.setStatus(AutoStatus.WARTET_AUF_ABSCHNITT);


            zugriff.einfahren(auto);

            auto.setStatus(AutoStatus.IN_KRITISCHEM_BEREICH);

            try {
                simuliereDurchfahrt(abschnitt);
            } finally {
                zugriff.ausfahren(auto);
                auto.setStatus(AutoStatus.FAEHRT);
            }
        } else {
            simuliereDurchfahrt(abschnitt);
        }
    }

    /**
     * Durchfaehrt eine Gerade oder DRS-Zone mit Ueberholmoeglichkeit.
     *
     * @param abschnitt Der Geraden-Abschnitt
     * @throws InterruptedException wenn der Thread unterbrochen wird
     */
    private void durchfahreGeradeOderDRS(Streckenabschnitt abschnitt) throws InterruptedException {
        auto.setStatus(AutoStatus.IN_UEBERHOLZONE);


        if (abschnitt.istUeberholzone()) {
            versucheUeberholung(abschnitt);
        }

        simuliereDurchfahrt(abschnitt);
        auto.setStatus(AutoStatus.FAEHRT);
    }

    /**
     * Durchfaehrt einen normalen Abschnitt ohne spezielle Synchronisation.
     *
     * @param abschnitt Der Abschnitt
     * @throws InterruptedException wenn der Thread unterbrochen wird
     */
    private void durchfahreNormalenAbschnitt(Streckenabschnitt abschnitt) throws InterruptedException {
        simuliereDurchfahrt(abschnitt);
    }

    /**
     * Simuliert die physische Durchfahrt durch einen Abschnitt.
     * Die Dauer haengt von der Abschnittslaenge, Reifenzustand und
     * Simulationsgeschwindigkeit ab.
     *
     * @param abschnitt Der zu durchfahrende Abschnitt
     * @throws InterruptedException wenn der Thread unterbrochen wird
     */
    private void simuliereDurchfahrt(Streckenabschnitt abschnitt) throws InterruptedException {

        long basisZeit = Konfiguration.BASIS_ABSCHNITT_ZEIT_MS;


        double geschwindigkeit = auto.berechneGeschwindigkeit();
        long angepassteZeit = (long) (basisZeit / geschwindigkeit);


        long skalierteZeit = Konfiguration.skaliereZeit(angepassteZeit, simulationsGeschwindigkeit);


        int schritte = 10;
        long zeitProSchritt = skalierteZeit / schritte;
        double fortschrittProSchritt = 1.0 / schritte;

        for (int i = 0; i < schritte && running && !rennBeendet; i++) {
            Thread.sleep(Math.max(1, zeitProSchritt));

            double neuerFortschritt = auto.getFortschrittImAbschnitt() + fortschrittProSchritt;
            auto.setFortschrittImAbschnitt(Math.min(1.0, neuerFortschritt));
            auto.aktualisierePosition(abschnitt);
        }

        auto.getAktuelleReifen().abnutzen();
    }

    /**
     * Versucht ein Ueberholmanoever gegen ein vorausfahrendes Auto.
     *
     * @param abschnitt Der aktuelle DRS-Zonen-Abschnitt
     */
    private void versucheUeberholung(Streckenabschnitt abschnitt) {

        List<Auto> reihenfolge = rennDaten.getRennreihenfolge();
        int meinePosition = -1;

        for (int i = 0; i < reihenfolge.size(); i++) {
            if (reihenfolge.get(i).equals(auto)) {
                meinePosition = i;
                break;
            }
        }


        if (meinePosition > 0) {
            Auto vordermann = reihenfolge.get(meinePosition - 1);


            if (vordermann.getAktuellerAbschnittId() == auto.getAktuellerAbschnittId()) {

                double abstand = vordermann.getFortschrittImAbschnitt() - auto.getFortschrittImAbschnitt();
                long abstandMs = (long) (abstand * Konfiguration.BASIS_ABSCHNITT_ZEIT_MS);

                if (abstandMs < Konfiguration.MAX_ABSTAND_FUER_UEBERHOLUNG_MS && abstandMs > 0) {

                    boolean erfolg = ueberholManager.versucheUeberholung(
                            auto, vordermann, abschnitt, abstandMs);

                    if (erfolg) {

                        double neuerFortschritt = vordermann.getFortschrittImAbschnitt()
                                + Konfiguration.UEBERHOLUNG_FORTSCHRITT_BONUS;
                        auto.setFortschrittImAbschnitt(Math.min(0.99, neuerFortschritt));

                        RennLogger.logThread(RennLogger.LogLevel.INFO,
                                auto.getFahrer().getKuerzel() + " UEBERHOLT " +
                                        vordermann.getFahrer().getKuerzel() + "!");
                    }
                }
            }
        }
    }

    /**
     * Wechselt zum naechsten Streckenabschnitt.
     * Prueft dabei, ob ein Pitstop angefordert wurde oder eine neue Runde beginnt.
     *
     * @throws InterruptedException wenn der Thread unterbrochen wird
     */
    private void wechsleZumNaechstenAbschnitt() throws InterruptedException {
        int aktuelleId = auto.getAktuellerAbschnittId();


        if (auto.istPitstopAngefordert() &&
                aktuelleId == Konfiguration.PITSTOP_EINFAHRT_NACH_ABSCHNITT) {

            fuehrePitstopDurch();
            return;
        }


        int naechsteId;

        if (aktuelleId == Konfiguration.ANZAHL_HAUPTSTRECKEN_ABSCHNITTE - 1) {

            naechsteId = 0;
            beendeRunde();
        } else if (aktuelleId == Konfiguration.PITSTOP_AUSFAHRT_ID) {

            naechsteId = Konfiguration.PITSTOP_AUSFAHRT_VOR_ABSCHNITT;
        } else {

            naechsteId = aktuelleId + 1;
        }

        auto.setAktuellerAbschnittId(naechsteId);
        auto.setFortschrittImAbschnitt(0.0);

        RennLogger.logThread(RennLogger.LogLevel.DEBUG,
                auto.getFahrer().getKuerzel() + " -> Abschnitt " + naechsteId);
    }

    /**
     * Fuehrt einen kompletten Pitstop durch.
     *
     * @throws InterruptedException wenn der Thread unterbrochen wird
     */
    private void fuehrePitstopDurch() throws InterruptedException {
        RennLogger.logThread(RennLogger.LogLevel.INFO,
                auto.getFahrer().getKuerzel() + " faehrt in die Box!");

        ReifenTyp neuerTyp = auto.holeAngefordertReifenTypUndReset();
        if (neuerTyp == null) {
            neuerTyp = ReifenTyp.MEDIUM; // Fallback
        }


        auto.setStatus(AutoStatus.FAEHRT_IN_BOX);
        auto.setAktuellerAbschnittId(Konfiguration.PITSTOP_EINFAHRT_ID);
        pitstopController.einfahrtAnfordern(auto);

        try {

            simuliereDurchfahrt(strecke.getAbschnitt(Konfiguration.PITSTOP_EINFAHRT_ID));
            pitstopController.einfahrtAbschliessen(auto);


            auto.setAktuellerAbschnittId(Konfiguration.BOXENGASSE_ID);
            auto.setStatus(AutoStatus.IN_BOX);

            boxenZugriff.pitstopDurchfuehren(auto, neuerTyp);


            auto.wechsleReifen(neuerTyp);

            RennLogger.logThread(RennLogger.LogLevel.INFO,
                    auto.getFahrer().getKuerzel() + " Reifenwechsel: " + neuerTyp);


            auto.setStatus(AutoStatus.VERLAESST_BOX);
            auto.setAktuellerAbschnittId(Konfiguration.PITSTOP_AUSFAHRT_ID);
            pitstopController.ausfahrtAnfordern(auto);

            simuliereDurchfahrt(strecke.getAbschnitt(Konfiguration.PITSTOP_AUSFAHRT_ID));
            pitstopController.ausfahrtAbschliessen(auto);

        } finally {
            auto.setStatus(AutoStatus.FAEHRT);
        }


        auto.setAktuellerAbschnittId(Konfiguration.PITSTOP_AUSFAHRT_VOR_ABSCHNITT);
        auto.setFortschrittImAbschnitt(0.0);
    }

    /**
     * Beendet die aktuelle Runde und startet ggf. die naechste.
     */
    private void beendeRunde() {
        int abgeschlosseneRunde = auto.getAktuelleRunde();
        long aktuelleZeit = System.currentTimeMillis();


        Rundenzeit rundenzeit = new Rundenzeit(
                auto.getStartnummer(),
                abgeschlosseneRunde,
                auto.getLetzteRundenzeit() > 0 ? auto.getLetzteRundenzeit() :
                        aktuelleZeit - rennDaten.getRennStartzeit(),
                auto.getAktuelleReifen().getTyp(),
                auto.getAktuelleReifen().getAbnutzungProzent()
        );
        rennDaten.addRundenzeit(rundenzeit);

        RennLogger.logThread(RennLogger.LogLevel.INFO,
                auto.getFahrer().getKuerzel() + " beendet Runde " + abgeschlosseneRunde +
                        " - Zeit: " + rundenzeit.getFormattierteZeit());


        if (abgeschlosseneRunde >= strecke.getAnzahlRunden()) {
            auto.setzeImZiel();
            RennLogger.logThread(RennLogger.LogLevel.INFO,
                    auto.getFahrer().getKuerzel() + " HAT DAS RENNEN BEENDET!");
        } else {
            // Naechste Runde starten
            auto.starteNeueRunde(aktuelleZeit);
            RennLogger.logThread(RennLogger.LogLevel.INFO,
                    auto.getFahrer().getKuerzel() + " startet Runde " + auto.getAktuelleRunde());
        }
    }


    /**
     * Stoppt den Thread sicher.
     *
     * @Vorbedingung Keine
     * @Nachbedingung running ist false, Thread wird bei naechster Gelegenheit beendet
     */
    public void stoppen() {
        this.running = false;
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

    /**
     * Setzt die Simulationsgeschwindigkeit.
     *
     * @param geschwindigkeit Neue Geschwindigkeit (z.B. 2.0 fuer doppelte Geschwindigkeit)
     */
    public void setSimulationsGeschwindigkeit(double geschwindigkeit) {
        this.simulationsGeschwindigkeit = geschwindigkeit;
    }

    /**
     * Gibt das gesteuerte Auto zurueck.
     *
     * @return Das Auto dieses Threads
     */
    public Auto getAuto() {
        return auto;
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
