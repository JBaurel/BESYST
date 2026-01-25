package synchronisation;

import model.Auto;
import util.RennLogger;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Synchronisationsklasse fuer den Rennstart.
 * Diese Klasse koordiniert den gleichzeitigen Start aller Autos
 * nach der Startampel-Sequenz.
 *
 * Der Ablauf eines Rennstarts ist:
 * 1. Alle Autos melden sich bereit (warteAufStartfreigabe)
 * 2. Die Startampel-Sequenz laeuft (5 rote Lichter nacheinander)
 * 3. Alle Lichter gehen aus (Freigabe)
 * 4. Alle Autos starten gleichzeitig
 *
 * Die Klasse verwendet:
 * - CountDownLatch fuer die gleichzeitige Freigabe aller Autos
 * - AtomicInteger fuer die Zaehlung der bereiten Autos
 * - AtomicBoolean fuer den Fehlstart-Status
 *
 * @author F1 Simulation Team
 * @version 1.0
 */
public class StartFreigabe {

    /** Anzahl der Startampel-Lichter. */
    public static final int ANZAHL_LICHTER = 5;

    /** Verzögerung zwischen dem Aufleuchten der einzelnen Lichter in Millisekunden. */
    public static final long LICHT_INTERVALL_MS = 1000;

    /** Minimale Verzögerung nach dem letzten Licht bis zur Freigabe in Millisekunden. */
    public static final long MIN_FREIGABE_VERZOEGERUNG_MS = 500;

    /** Maximale Verzögerung nach dem letzten Licht bis zur Freigabe in Millisekunden. */
    public static final long MAX_FREIGABE_VERZOEGERUNG_MS = 3000;

    private final int anzahlAutos;
    private CountDownLatch startLatch;
    private final AtomicInteger bereiteAutos;
    private final AtomicBoolean startFreigegeben;
    private final AtomicBoolean fehlstartErkannt;
    private int aktiveLichter;

    // Callback fuer GUI-Updates
    private StartSequenzListener listener;

    /**
     * Interface fuer Callbacks waehrend der Startsequenz.
     * Ermoeglicht der GUI die Darstellung der Ampel-Lichter.
     */
    public interface StartSequenzListener {
        /**
         * Wird aufgerufen wenn ein Licht aufleuchtet.
         * @param lichtNummer Nummer des Lichts (1-5)
         */
        void lichtAn(int lichtNummer);

        /**
         * Wird aufgerufen wenn alle Lichter ausgehen (Freigabe).
         */
        void lichterAus();

        /**
         * Wird aufgerufen wenn ein Fehlstart erkannt wird.
         * @param auto Das Auto das den Fehlstart verursacht hat
         */
        void fehlstart(Auto auto);
    }

    /**
     * Erstellt eine neue StartFreigabe fuer die angegebene Anzahl Autos.
     *
     * @Vorbedingung anzahlAutos muss > 0 sein
     * @Nachbedingung StartFreigabe ist initialisiert und bereit
     *
     * @param anzahlAutos Anzahl der startenden Autos
     * @throws IllegalArgumentException wenn anzahlAutos <= 0
     */
    public StartFreigabe(int anzahlAutos) {
        if (anzahlAutos <= 0) {
            throw new IllegalArgumentException("Anzahl der Autos muss > 0 sein");
        }

        this.anzahlAutos = anzahlAutos;
        this.startLatch = new CountDownLatch(1);
        this.bereiteAutos = new AtomicInteger(0);
        this.startFreigegeben = new AtomicBoolean(false);
        this.fehlstartErkannt = new AtomicBoolean(false);
        this.aktiveLichter = 0;
        this.listener = null;

        RennLogger.debug("StartFreigabe initialisiert fuer " + anzahlAutos + " Autos");
    }

    /**
     * Setzt den Listener fuer Startsequenz-Events.
     *
     * @Vorbedingung Keine
     * @Nachbedingung listener ist gesetzt
     *
     * @param listener Der Listener oder null zum Entfernen
     */
    public void setStartSequenzListener(StartSequenzListener listener) {
        this.listener = listener;
    }

    /**
     * Meldet ein Auto als bereit fuer den Start.
     * Diese Methode wird von jedem AutoThread aufgerufen.
     * Der Thread blockiert dann bis die Startfreigabe erfolgt.
     *
     * @Vorbedingung auto darf nicht null sein
     * @Nachbedingung Auto ist registriert und wartet
     * @Nachbedingung Methode kehrt erst nach Startfreigabe zurueck
     *
     * @param auto Das startbereite Auto
     * @throws IllegalArgumentException wenn auto null ist
     * @throws InterruptedException wenn der Thread unterbrochen wird
     */
    public void warteAufStartfreigabe(Auto auto) throws InterruptedException {
        if (auto == null) {
            throw new IllegalArgumentException("Auto darf nicht null sein");
        }

        int nummer = bereiteAutos.incrementAndGet();
        RennLogger.logSync("START_BEREIT",
                auto.getFahrer().getKuerzel() + " (" + nummer + "/" + anzahlAutos + ")");

        // Warte auf die Startfreigabe
        startLatch.await();

        RennLogger.logSync("GESTARTET", auto.getFahrer().getKuerzel());
    }

    /**
     * Meldet ein Auto als bereit mit Timeout.
     * Nuetzlich um haengende Threads zu erkennen.
     *
     * @Vorbedingung auto darf nicht null sein
     * @Vorbedingung timeoutMs muss > 0 sein
     * @Nachbedingung Bei Erfolg: Auto ist gestartet
     * @Nachbedingung Bei Timeout: Rueckgabe false
     *
     * @param auto Das startbereite Auto
     * @param timeoutMs Maximale Wartezeit in Millisekunden
     * @return true wenn Start erfolgt ist, false bei Timeout
     * @throws InterruptedException wenn der Thread unterbrochen wird
     */
    public boolean warteAufStartfreigabeMitTimeout(Auto auto, long timeoutMs)
            throws InterruptedException {
        if (auto == null) {
            throw new IllegalArgumentException("Auto darf nicht null sein");
        }

        int nummer = bereiteAutos.incrementAndGet();
        RennLogger.logSync("START_BEREIT",
                auto.getFahrer().getKuerzel() + " (" + nummer + "/" + anzahlAutos + ")");

        boolean erfolg = startLatch.await(timeoutMs, TimeUnit.MILLISECONDS);

        if (erfolg) {
            RennLogger.logSync("GESTARTET", auto.getFahrer().getKuerzel());
        } else {
            RennLogger.warning("Start-Timeout fuer " + auto.getFahrer().getKuerzel());
        }

        return erfolg;
    }

    /**
     * Fuehrt die komplette Startampel-Sequenz durch und gibt dann alle Autos frei.
     * Diese Methode wird vom RennleiterThread aufgerufen.
     *
     * Die Sequenz ist:
     * 1. Warte bis alle Autos bereit sind
     * 2. Schalte die 5 Lichter nacheinander ein
     * 3. Warte eine zufaellige Zeit
     * 4. Schalte alle Lichter aus (Freigabe)
     *
     * @Vorbedingung Keine
     * @Nachbedingung Alle wartenden Autos sind freigegeben
     *
     * @throws InterruptedException wenn der Thread unterbrochen wird
     */
    public void starteStartsequenz() throws InterruptedException {
        RennLogger.info("Startsequenz beginnt...");

        // Warte bis genuegend Autos bereit sind (mindestens 50%)
        while (bereiteAutos.get() < anzahlAutos / 2) {
            RennLogger.debug("Warte auf Autos: " + bereiteAutos.get() + "/" + anzahlAutos);
            Thread.sleep(100);
        }

        RennLogger.info("Startampel-Sequenz startet");

        // Lichter nacheinander einschalten
        for (int i = 1; i <= ANZAHL_LICHTER; i++) {
            aktiveLichter = i;
            RennLogger.info("Startampel: Licht " + i + " AN");

            if (listener != null) {
                listener.lichtAn(i);
            }

            Thread.sleep(LICHT_INTERVALL_MS);
        }

        // Zufaellige Verzoegerung vor der Freigabe (wie in der echten F1)
        long verzoegerung = MIN_FREIGABE_VERZOEGERUNG_MS +
                (long) (Math.random() * (MAX_FREIGABE_VERZOEGERUNG_MS - MIN_FREIGABE_VERZOEGERUNG_MS));

        RennLogger.debug("Verzoegerung vor Freigabe: " + verzoegerung + "ms");
        Thread.sleep(verzoegerung);

        // Freigabe!
        freigeben();
    }

    /**
     * Gibt alle wartenden Autos sofort frei.
     * Diese Methode kann auch direkt aufgerufen werden um die
     * Ampelsequenz zu ueberspringen (z.B. fuer Tests).
     *
     * @Vorbedingung Keine
     * @Nachbedingung startFreigegeben ist true
     * @Nachbedingung Alle wartenden Threads sind freigegeben
     */
    public void freigeben() {
        if (startFreigegeben.compareAndSet(false, true)) {
            aktiveLichter = 0;
            RennLogger.info("LICHTER AUS - START FREIGEGEBEN!");

            if (listener != null) {
                listener.lichterAus();
            }

            startLatch.countDown();
        }
    }

    /**
     * Prueft ob ein Fehlstart stattgefunden hat.
     * Ein Fehlstart liegt vor wenn ein Auto vor der Freigabe losfaehrt.
     *
     * In dieser Simulation werden Fehlstarts nur erkannt aber nicht bestraft,
     * da die Threads ohnehin auf den Latch warten muessen.
     *
     * @Vorbedingung auto darf nicht null sein
     * @Nachbedingung Bei Fehlstart: fehlstartErkannt ist true
     *
     * @param auto Das zu pruefende Auto
     * @return true wenn ein Fehlstart erkannt wurde
     */
    public boolean pruefeFehlstart(Auto auto) {
        if (auto == null) {
            throw new IllegalArgumentException("Auto darf nicht null sein");
        }

        // Ein Fehlstart liegt vor wenn das Auto sich bewegt bevor die Freigabe erfolgt
        // In dieser Implementierung ist das technisch nicht moeglich, da die Threads
        // auf den Latch warten. Die Methode ist fuer zukuenftige Erweiterungen vorgesehen.

        if (!startFreigegeben.get() && aktiveLichter == ANZAHL_LICHTER) {
            // Theoretisch koennte hier Bewegung vor Freigabe erkannt werden
            fehlstartErkannt.set(true);
            RennLogger.warning("Fehlstart erkannt: " + auto.getFahrer().getKuerzel());

            if (listener != null) {
                listener.fehlstart(auto);
            }

            return true;
        }

        return false;
    }

    /**
     * Setzt die StartFreigabe fuer ein neues Rennen zurueck.
     *
     * @Vorbedingung Keine
     * @Nachbedingung Alle Zustaende sind zurueckgesetzt
     */
    public void zuruecksetzen() {
        startLatch = new CountDownLatch(1);
        bereiteAutos.set(0);
        startFreigegeben.set(false);
        fehlstartErkannt.set(false);
        aktiveLichter = 0;

        RennLogger.debug("StartFreigabe zurueckgesetzt");
    }

    /**
     * Gibt die Anzahl der bereiten Autos zurueck.
     *
     * @Vorbedingung Keine
     * @Nachbedingung Rueckgabewert ist aktuelle Anzahl
     *
     * @return Anzahl der Autos die auf Start warten
     */
    public int getBereiteAutos() {
        return bereiteAutos.get();
    }

    /**
     * Gibt die Gesamtanzahl der erwarteten Autos zurueck.
     *
     * @Vorbedingung Keine
     * @Nachbedingung Rueckgabewert ist die konfigurierte Anzahl
     *
     * @return Gesamtanzahl der Autos
     */
    public int getAnzahlAutos() {
        return anzahlAutos;
    }

    /**
     * Prueft ob die Startfreigabe bereits erfolgt ist.
     *
     * @Vorbedingung Keine
     * @Nachbedingung Rueckgabewert zeigt Freigabe-Status
     *
     * @return true wenn der Start freigegeben wurde
     */
    public boolean istFreigegeben() {
        return startFreigegeben.get();
    }

    /**
     * Prueft ob ein Fehlstart erkannt wurde.
     *
     * @Vorbedingung Keine
     * @Nachbedingung Rueckgabewert zeigt Fehlstart-Status
     *
     * @return true wenn ein Fehlstart erkannt wurde
     */
    public boolean wurdeFehlstartErkannt() {
        return fehlstartErkannt.get();
    }

    /**
     * Gibt die Anzahl der aktuell aktiven Lichter zurueck.
     * Fuer die GUI-Anzeige der Startampel.
     *
     * @Vorbedingung Keine
     * @Nachbedingung Rueckgabewert liegt zwischen 0 und ANZAHL_LICHTER
     *
     * @return Anzahl der leuchtenden Lichter (0 = alle aus)
     */
    public int getAktiveLichter() {
        return aktiveLichter;
    }

    /**
     * Prueft ob alle Autos bereit sind.
     *
     * @Vorbedingung Keine
     * @Nachbedingung Rueckgabewert ist true wenn alle bereit sind
     *
     * @return true wenn alle Autos auf Start warten
     */
    public boolean sindAlleAutosBereit() {
        return bereiteAutos.get() >= anzahlAutos;
    }

    @Override
    public String toString() {
        return String.format(
                "StartFreigabe[Bereit: %d/%d, Lichter: %d/%d, Freigegeben: %s]",
                bereiteAutos.get(), anzahlAutos,
                aktiveLichter, ANZAHL_LICHTER,
                startFreigegeben.get() ? "Ja" : "Nein");
    }
}
