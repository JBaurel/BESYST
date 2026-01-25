package synchronisation;

import model.Auto;
import util.RennLogger;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Controller fuer die Zugriffskontrolle auf die Boxengasse.
 * Diese Klasse verwaltet sowohl die Einfahrt als auch die Ausfahrt
 * der Boxengasse mit separaten Semaphoren.
 *
 * Die Boxengasse hat an beiden Enden eine begrenzte Kapazitaet,
 * um Staus und gefaehrliche Situationen zu vermeiden. Typischerweise
 * koennen maximal 3 Autos gleichzeitig ein- oder ausfahren.
 *
 * Die Klasse koordiniert auch die Geschwindigkeitsbegrenzung
 * in der Boxengasse (Pit Lane Speed Limit).
 *
 * @author F1 Simulation Team
 * @version 1.0
 */
public class PitstopLaneController {

    /** Maximale Anzahl Autos in der Einfahrt. */
    public static final int EINFAHRT_KAPAZITAET = 3;

    /** Maximale Anzahl Autos in der Ausfahrt. */
    public static final int AUSFAHRT_KAPAZITAET = 3;

    /** Geschwindigkeitslimit in der Boxengasse (simulierte Verzoegerung in ms). */
    public static final long BOXENGASSEN_DURCHFAHRT_MS = 3000;

    private final Semaphore einfahrtSemaphore;
    private final Semaphore ausfahrtSemaphore;
    private final AtomicInteger autosInBoxengasse;

    /**
     * Erstellt einen neuen Controller fuer die Boxengasse.
     * Beide Semaphoren werden mit ihren jeweiligen Kapazitaeten
     * und fairem Scheduling initialisiert.
     *
     * @Vorbedingung Keine
     * @Nachbedingung Semaphoren sind mit Standardkapazitaeten initialisiert
     */
    public PitstopLaneController() {
        this.einfahrtSemaphore = new Semaphore(EINFAHRT_KAPAZITAET, true);
        this.ausfahrtSemaphore = new Semaphore(AUSFAHRT_KAPAZITAET, true);
        this.autosInBoxengasse = new AtomicInteger(0);

        RennLogger.debug("PitstopLaneController initialisiert");
    }

    /**
     * Erstellt einen Controller mit benutzerdefinierten Kapazitaeten.
     * Diese Variante ermoeglicht flexible Konfiguration fuer Tests.
     *
     * @Vorbedingung einfahrtKapazitaet muss > 0 sein
     * @Vorbedingung ausfahrtKapazitaet muss > 0 sein
     * @Nachbedingung Semaphoren sind mit angegebenen Kapazitaeten initialisiert
     *
     * @param einfahrtKapazitaet Kapazitaet der Einfahrt
     * @param ausfahrtKapazitaet Kapazitaet der Ausfahrt
     * @throws IllegalArgumentException wenn Kapazitaeten <= 0
     */
    public PitstopLaneController(int einfahrtKapazitaet, int ausfahrtKapazitaet) {
        if (einfahrtKapazitaet <= 0 || ausfahrtKapazitaet <= 0) {
            throw new IllegalArgumentException("Kapazitaeten muessen > 0 sein");
        }

        this.einfahrtSemaphore = new Semaphore(einfahrtKapazitaet, true);
        this.ausfahrtSemaphore = new Semaphore(ausfahrtKapazitaet, true);
        this.autosInBoxengasse = new AtomicInteger(0);

        RennLogger.debug("PitstopLaneController initialisiert (Ein: "
                + einfahrtKapazitaet + ", Aus: " + ausfahrtKapazitaet + ")");
    }

    /**
     * Fordert die Einfahrt in die Boxengasse an.
     * Der Thread blockiert bis ein Einfahrtsplatz frei ist.
     * Nach erfolgreicher Einfahrt wird der Zaehler erhoeht.
     *
     * @Vorbedingung auto darf nicht null sein
     * @Nachbedingung Einfahrt-Permit ist erworben
     * @Nachbedingung autosInBoxengasse ist um 1 erhoeht
     *
     * @param auto Das einfahrende Auto
     * @throws IllegalArgumentException wenn auto null ist
     * @throws InterruptedException wenn der Thread unterbrochen wird
     */
    public void einfahrtAnfordern(Auto auto) throws InterruptedException {
        if (auto == null) {
            throw new IllegalArgumentException("Auto darf nicht null sein");
        }

        RennLogger.logSync("EINFAHRT_ANGEFORDERT",
                "Boxengasse - " + auto.getFahrer().getKuerzel());

        einfahrtSemaphore.acquire();
        autosInBoxengasse.incrementAndGet();

        RennLogger.logSync("EINFAHRT_GEWAEHRT",
                "Boxengasse - Autos drin: " + autosInBoxengasse.get());
    }

    /**
     * Versucht mit Timeout in die Boxengasse einzufahren.
     *
     * @Vorbedingung auto darf nicht null sein
     * @Vorbedingung timeoutMs muss >= 0 sein
     * @Nachbedingung Bei Erfolg: Einfahrt-Permit erworben
     * @Nachbedingung Bei Timeout: Keine Aenderung
     *
     * @param auto Das einfahrende Auto
     * @param timeoutMs Maximale Wartezeit in Millisekunden
     * @return true wenn Einfahrt erfolgreich
     * @throws IllegalArgumentException wenn auto null ist
     * @throws InterruptedException wenn der Thread unterbrochen wird
     */
    public boolean einfahrtMitTimeout(Auto auto, long timeoutMs) throws InterruptedException {
        if (auto == null) {
            throw new IllegalArgumentException("Auto darf nicht null sein");
        }

        boolean erfolg = einfahrtSemaphore.tryAcquire(timeoutMs, TimeUnit.MILLISECONDS);

        if (erfolg) {
            autosInBoxengasse.incrementAndGet();
            RennLogger.logSync("EINFAHRT_GEWAEHRT",
                    "Boxengasse - Autos drin: " + autosInBoxengasse.get());
        } else {
            RennLogger.logSync("EINFAHRT_TIMEOUT",
                    "Boxengasse - " + auto.getFahrer().getKuerzel());
        }

        return erfolg;
    }

    /**
     * Beendet die Einfahrt und gibt das Einfahrt-Permit frei.
     * Diese Methode wird aufgerufen nachdem das Auto die Einfahrtszone
     * verlassen hat und sich in der eigentlichen Boxengasse befindet.
     *
     * @Vorbedingung auto muss ein Einfahrt-Permit besitzen
     * @Nachbedingung Einfahrt-Permit ist freigegeben
     *
     * @param auto Das Auto das die Einfahrt abgeschlossen hat
     * @throws IllegalArgumentException wenn auto null ist
     */
    public void einfahrtAbschliessen(Auto auto) {
        if (auto == null) {
            throw new IllegalArgumentException("Auto darf nicht null sein");
        }

        einfahrtSemaphore.release();

        RennLogger.logSync("EINFAHRT_ABGESCHLOSSEN",
                "Boxengasse - " + auto.getFahrer().getKuerzel());
    }

    /**
     * Fordert die Ausfahrt aus der Boxengasse an.
     * Der Thread blockiert bis ein Ausfahrtsplatz frei ist.
     *
     * @Vorbedingung auto darf nicht null sein
     * @Nachbedingung Ausfahrt-Permit ist erworben
     *
     * @param auto Das ausfahrende Auto
     * @throws IllegalArgumentException wenn auto null ist
     * @throws InterruptedException wenn der Thread unterbrochen wird
     */
    public void ausfahrtAnfordern(Auto auto) throws InterruptedException {
        if (auto == null) {
            throw new IllegalArgumentException("Auto darf nicht null sein");
        }

        RennLogger.logSync("AUSFAHRT_ANGEFORDERT",
                "Boxengasse - " + auto.getFahrer().getKuerzel());

        ausfahrtSemaphore.acquire();

        RennLogger.logSync("AUSFAHRT_GEWAEHRT",
                "Boxengasse - " + auto.getFahrer().getKuerzel());
    }

    /**
     * Beendet die Ausfahrt und gibt das Ausfahrt-Permit frei.
     * Der Zaehler der Autos in der Boxengasse wird verringert.
     *
     * @Vorbedingung auto muss ein Ausfahrt-Permit besitzen
     * @Nachbedingung Ausfahrt-Permit ist freigegeben
     * @Nachbedingung autosInBoxengasse ist um 1 verringert
     *
     * @param auto Das Auto das die Boxengasse verlassen hat
     * @throws IllegalArgumentException wenn auto null ist
     */
    public void ausfahrtAbschliessen(Auto auto) {
        if (auto == null) {
            throw new IllegalArgumentException("Auto darf nicht null sein");
        }

        ausfahrtSemaphore.release();
        autosInBoxengasse.decrementAndGet();

        RennLogger.logSync("AUSFAHRT_ABGESCHLOSSEN",
                "Boxengasse - Autos verbleibend: " + autosInBoxengasse.get());
    }

    /**
     * Simuliert die Durchfahrt durch die Boxengasse mit Geschwindigkeitslimit.
     * Diese Methode kann aufgerufen werden nachdem ein Auto die Box verlassen hat
     * und zur Ausfahrt faehrt.
     *
     * @Vorbedingung dauer muss > 0 sein
     * @Nachbedingung Thread hat fuer die angegebene Dauer geschlafen
     *
     * @param dauer Durchfahrtsdauer in Millisekunden
     * @throws InterruptedException wenn der Thread unterbrochen wird
     */
    public void boxengassenDurchfahrt(long dauer) throws InterruptedException {
        if (dauer <= 0) {
            dauer = BOXENGASSEN_DURCHFAHRT_MS;
        }

        RennLogger.logSync("BOXENGASSE_DURCHFAHRT",
                "Dauer: " + dauer + "ms");

        Thread.sleep(dauer);
    }

    /**
     * Gibt die Anzahl der Autos in der Boxengasse zurueck.
     *
     * @Vorbedingung Keine
     * @Nachbedingung Rueckgabewert ist aktuelle Anzahl
     *
     * @return Anzahl der Autos in der Boxengasse
     */
    public int getAutosInBoxengasse() {
        return autosInBoxengasse.get();
    }

    /**
     * Gibt die verfuegbaren Einfahrtsplaetze zurueck.
     *
     * @Vorbedingung Keine
     * @Nachbedingung Rueckgabewert ist Anzahl freier Einfahrts-Permits
     *
     * @return Anzahl freier Einfahrtsplaetze
     */
    public int getFreieEinfahrtPlaetze() {
        return einfahrtSemaphore.availablePermits();
    }

    /**
     * Gibt die verfuegbaren Ausfahrtsplaetze zurueck.
     *
     * @Vorbedingung Keine
     * @Nachbedingung Rueckgabewert ist Anzahl freier Ausfahrts-Permits
     *
     * @return Anzahl freier Ausfahrtsplaetze
     */
    public int getFreieAusfahrtPlaetze() {
        return ausfahrtSemaphore.availablePermits();
    }

    /**
     * Gibt die Anzahl der auf Einfahrt wartenden Autos zurueck.
     *
     * @Vorbedingung Keine
     * @Nachbedingung Rueckgabewert ist geschaetzte Warteschlangenlaenge
     *
     * @return Anzahl wartender Autos an der Einfahrt
     */
    public int getWartendeEinfahrt() {
        return einfahrtSemaphore.getQueueLength();
    }

    /**
     * Gibt die Anzahl der auf Ausfahrt wartenden Autos zurueck.
     *
     * @Vorbedingung Keine
     * @Nachbedingung Rueckgabewert ist geschaetzte Warteschlangenlaenge
     *
     * @return Anzahl wartender Autos an der Ausfahrt
     */
    public int getWartendeAusfahrt() {
        return ausfahrtSemaphore.getQueueLength();
    }

    /**
     * Prueft ob die Einfahrt blockiert ist.
     *
     * @Vorbedingung Keine
     * @Nachbedingung Rueckgabewert ist true wenn keine Permits verfuegbar
     *
     * @return true wenn die Einfahrt voll ist
     */
    public boolean istEinfahrtVoll() {
        return einfahrtSemaphore.availablePermits() == 0;
    }

    /**
     * Prueft ob die Ausfahrt blockiert ist.
     *
     * @Vorbedingung Keine
     * @Nachbedingung Rueckgabewert ist true wenn keine Permits verfuegbar
     *
     * @return true wenn die Ausfahrt voll ist
     */
    public boolean istAusfahrtVoll() {
        return ausfahrtSemaphore.availablePermits() == 0;
    }

    @Override
    public String toString() {
        return String.format(
                "PitstopLane[Einfahrt: %d/%d frei, Ausfahrt: %d/%d frei, Drin: %d]",
                einfahrtSemaphore.availablePermits(), EINFAHRT_KAPAZITAET,
                ausfahrtSemaphore.availablePermits(), AUSFAHRT_KAPAZITAET,
                autosInBoxengasse.get());
    }
}
