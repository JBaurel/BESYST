package synchronisation;

import model.Auto;
import model.Streckenabschnitt;
import util.RennLogger;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Synchronisationsklasse fuer Schikanen mit Semaphore-basierter Zugriffskontrolle.
 * Eine Schikane ist ein Streckenabschnitt mit Wechselkurven, der maximal
 * zwei Autos gleichzeitig durchfahren laesst.
 *
 * Die Klasse verwendet java.util.concurrent.Semaphore fuer die Kapazitaetskontrolle.
 * Im Gegensatz zum Monitor-Pattern ermoeglicht der Semaphore einen einfacheren
 * Zugriff ohne explizite Warteschlangenverwaltung.
 *
 * Der Semaphore ist als "fair" konfiguriert, sodass Autos in der Reihenfolge
 * ihrer Ankunft Zugang erhalten (FIFO-Garantie).
 *
 * @author F1 Simulation Team
 * @version 1.0
 */
public class SchikaneZugriff {

    /** Standardkapazitaet fuer Schikanen. */
    public static final int SCHIKANE_KAPAZITAET = 2;

    /** Timeout fuer Einfahrversuche in Millisekunden. */
    public static final long TIMEOUT_MS = 5000;

    private final Streckenabschnitt abschnitt;
    private final Semaphore semaphore;

    /**
     * Erstellt eine neue Schikanen-Zugriffskontrolle fuer den angegebenen Abschnitt.
     * Der Semaphore wird mit der Kapazitaet 2 und fairem Scheduling initialisiert.
     *
     * @Vorbedingung abschnitt darf nicht null sein
     * @Nachbedingung Semaphore ist mit SCHIKANE_KAPAZITAET Permits initialisiert
     *
     * @param abschnitt Der Schikanen-Streckenabschnitt
     * @throws IllegalArgumentException wenn abschnitt null ist
     */
    public SchikaneZugriff(Streckenabschnitt abschnitt) {
        if (abschnitt == null) {
            throw new IllegalArgumentException("Streckenabschnitt darf nicht null sein");
        }

        this.abschnitt = abschnitt;
        // Fair = true stellt FIFO-Ordnung sicher
        this.semaphore = new Semaphore(SCHIKANE_KAPAZITAET, true);

        RennLogger.debug("SchikaneZugriff erstellt fuer: " + abschnitt.getName());
    }

    /**
     * Erstellt eine Schikanen-Zugriffskontrolle mit benutzerdefinierter Kapazitaet.
     * Diese Variante ermoeglicht flexible Konfiguration fuer Testzwecke.
     *
     * @Vorbedingung abschnitt darf nicht null sein
     * @Vorbedingung kapazitaet muss > 0 sein
     * @Nachbedingung Semaphore ist mit angegebener Kapazitaet initialisiert
     *
     * @param abschnitt Der Streckenabschnitt
     * @param kapazitaet Maximale Anzahl gleichzeitiger Autos
     * @throws IllegalArgumentException wenn Parameter ungueltig sind
     */
    public SchikaneZugriff(Streckenabschnitt abschnitt, int kapazitaet) {
        if (abschnitt == null) {
            throw new IllegalArgumentException("Streckenabschnitt darf nicht null sein");
        }
        if (kapazitaet <= 0) {
            throw new IllegalArgumentException("Kapazitaet muss > 0 sein");
        }

        this.abschnitt = abschnitt;
        this.semaphore = new Semaphore(kapazitaet, true);

        RennLogger.debug("SchikaneZugriff erstellt fuer: " + abschnitt.getName()
                + " (Kapazitaet: " + kapazitaet + ")");
    }

    /**
     * Fordert Zugang zur Schikane an und blockiert bis ein Platz frei ist.
     * Der Semaphore stellt sicher, dass maximal SCHIKANE_KAPAZITAET Autos
     * gleichzeitig in der Schikane sind.
     *
     * @Vorbedingung auto darf nicht null sein
     * @Nachbedingung Ein Permit wurde erfolgreich erworben
     * @Nachbedingung Auto darf in die Schikane einfahren
     *
     * @param auto Das einfahrende Auto
     * @throws IllegalArgumentException wenn auto null ist
     * @throws InterruptedException wenn der Thread unterbrochen wird
     */
    public void einfahren(Auto auto) throws InterruptedException {
        if (auto == null) {
            throw new IllegalArgumentException("Auto darf nicht null sein");
        }

        RennLogger.logSync("WARTEN_AUF_PERMIT", abschnitt.getName());
        semaphore.acquire();
        RennLogger.logSync("PERMIT_ERHALTEN",
                abschnitt.getName() + " (Verfuegbar: " + semaphore.availablePermits() + ")");
    }

    /**
     * Versucht mit Timeout in die Schikane einzufahren.
     * Diese Methode ist nuetzlich um Deadlocks zu vermeiden oder
     * alternative Strategien zu ermoeglichen.
     *
     * @Vorbedingung auto darf nicht null sein
     * @Vorbedingung timeoutMs muss >= 0 sein
     * @Nachbedingung Bei Erfolg: Permit erworben, Rueckgabe true
     * @Nachbedingung Bei Timeout: Kein Permit, Rueckgabe false
     *
     * @param auto Das einfahrende Auto
     * @param timeoutMs Maximale Wartezeit in Millisekunden
     * @return true wenn Einfahrt erfolgreich, false bei Timeout
     * @throws IllegalArgumentException wenn auto null ist
     * @throws InterruptedException wenn der Thread unterbrochen wird
     */
    public boolean einfahrenMitTimeout(Auto auto, long timeoutMs) throws InterruptedException {
        if (auto == null) {
            throw new IllegalArgumentException("Auto darf nicht null sein");
        }

        RennLogger.logSync("WARTEN_MIT_TIMEOUT",
                abschnitt.getName() + " (max " + timeoutMs + "ms)");

        boolean erfolg = semaphore.tryAcquire(timeoutMs, TimeUnit.MILLISECONDS);

        if (erfolg) {
            RennLogger.logSync("PERMIT_ERHALTEN",
                    abschnitt.getName() + " (Verfuegbar: " + semaphore.availablePermits() + ")");
        } else {
            RennLogger.logSync("TIMEOUT", abschnitt.getName());
        }

        return erfolg;
    }

    /**
     * Versucht sofort ohne Warten in die Schikane einzufahren.
     * Diese Methode blockiert nicht und gibt sofort zurueck.
     *
     * @Vorbedingung auto darf nicht null sein
     * @Nachbedingung Bei Erfolg: Permit erworben
     * @Nachbedingung Bei Misserfolg: Keine Aenderung
     *
     * @param auto Das einfahrende Auto
     * @return true wenn Einfahrt sofort moeglich war
     * @throws IllegalArgumentException wenn auto null ist
     */
    public boolean versucheEinfahren(Auto auto) {
        if (auto == null) {
            throw new IllegalArgumentException("Auto darf nicht null sein");
        }

        boolean erfolg = semaphore.tryAcquire();

        if (erfolg) {
            RennLogger.logSync("SOFORT_EINGEFAHREN",
                    abschnitt.getName() + " (Verfuegbar: " + semaphore.availablePermits() + ")");
        }

        return erfolg;
    }

    /**
     * Verlaesst die Schikane und gibt das Permit zurueck.
     * Ein wartender Thread kann dadurch einfahren.
     *
     * @Vorbedingung auto muss ein Permit besitzen
     * @Nachbedingung Permit ist freigegeben
     * @Nachbedingung Verfuegbare Permits ist um 1 erhoeht
     *
     * @param auto Das ausfahrende Auto
     * @throws IllegalArgumentException wenn auto null ist
     */
    public void ausfahren(Auto auto) {
        if (auto == null) {
            throw new IllegalArgumentException("Auto darf nicht null sein");
        }

        semaphore.release();
        RennLogger.logSync("PERMIT_FREIGEGEBEN",
                abschnitt.getName() + " (Verfuegbar: " + semaphore.availablePermits() + ")");
    }

    /**
     * Gibt die Anzahl der aktuell verfuegbaren Plaetze zurueck.
     *
     * @Vorbedingung Keine
     * @Nachbedingung Rueckgabewert ist Anzahl freier Permits
     *
     * @return Anzahl freier Plaetze in der Schikane
     */
    public int getVerfuegbarePlaetze() {
        return semaphore.availablePermits();
    }

    /**
     * Gibt die Anzahl der wartenden Autos zurueck.
     * Diese Methode gibt eine Schaetzung zurueck, da sich die Zahl
     * waehrend der Abfrage aendern kann.
     *
     * @Vorbedingung Keine
     * @Nachbedingung Rueckgabewert ist geschaetzte Warteschlangenlaenge
     *
     * @return Geschaetzte Anzahl wartender Threads
     */
    public int getWartendeAnzahl() {
        return semaphore.getQueueLength();
    }

    /**
     * Prueft ob Autos auf Einfahrt warten.
     *
     * @Vorbedingung Keine
     * @Nachbedingung Rueckgabewert ist true wenn Warteschlange nicht leer
     *
     * @return true wenn mindestens ein Auto wartet
     */
    public boolean hatWartende() {
        return semaphore.hasQueuedThreads();
    }

    /**
     * Gibt den zugehoerigen Streckenabschnitt zurueck.
     *
     * @Vorbedingung Keine
     * @Nachbedingung Rueckgabewert ist der Schikanen-Abschnitt
     *
     * @return Der Streckenabschnitt
     */
    public Streckenabschnitt getAbschnitt() {
        return abschnitt;
    }

    @Override
    public String toString() {
        return String.format("Schikane[%s: %d/%d frei, Wartend: %d]",
                abschnitt.getName(),
                semaphore.availablePermits(),
                SCHIKANE_KAPAZITAET,
                semaphore.getQueueLength());
    }
}
