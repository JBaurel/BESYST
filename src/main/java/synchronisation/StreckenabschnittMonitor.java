package synchronisation;

import model.Auto;
import model.Streckenabschnitt;
import util.RennLogger;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Monitor-Klasse fuer die Synchronisation von Streckenabschnitten mit begrenzter Kapazitaet.
 * Diese Klasse implementiert das klassische Monitor-Pattern mit synchronized, wait und notify
 * fuer den Zugriff auf kritische Bereiche wie enge Kurven.
 *
 * Die Kapazitaet bestimmt, wie viele Autos gleichzeitig den Abschnitt durchfahren duerfen.
 * Bei engen Kurven ist die Kapazitaet typischerweise 1, sodass nur ein Auto gleichzeitig
 * die Kurve passieren kann.
 *
 * Die Warteschlange stellt sicher, dass Autos in der Reihenfolge ihrer Ankunft
 * Zugang zum Abschnitt erhalten (FIFO - First In, First Out).
 *
 * @author F1 Simulation Team
 * @version 1.0
 */
public class StreckenabschnittMonitor {

    private final Streckenabschnitt abschnitt;
    private final int kapazitaet;
    private int aktuelleAnzahl;
    private final Queue<Auto> warteschlange;
    private final Object lock;

    /**
     * Erstellt einen neuen Monitor fuer den angegebenen Streckenabschnitt.
     * Die Kapazitaet wird aus dem Streckenabschnitt uebernommen.
     *
     * @Vorbedingung abschnitt darf nicht null sein
     * @Vorbedingung abschnitt muss ein kritischer Bereich sein (begrenzte Kapazitaet)
     * @Nachbedingung Monitor ist initialisiert mit leerer Warteschlange
     *
     * @param abschnitt Der zu ueberwachende Streckenabschnitt
     * @throws IllegalArgumentException wenn abschnitt null ist
     */
    public StreckenabschnittMonitor(Streckenabschnitt abschnitt) {
        if (abschnitt == null) {
            throw new IllegalArgumentException("Streckenabschnitt darf nicht null sein");
        }

        this.abschnitt = abschnitt;
        this.kapazitaet = abschnitt.getKapazitaet();
        this.aktuelleAnzahl = 0;
        this.warteschlange = new LinkedList<>();
        this.lock = new Object();

        RennLogger.debug("StreckenabschnittMonitor erstellt fuer: " + abschnitt.getName()
                + " (Kapazitaet: " + kapazitaet + ")");
    }

    /**
     * Fordert Zugang zum Streckenabschnitt an.
     * Der aufrufende Thread wird blockiert, bis ein Platz frei ist
     * und das Auto an der Reihe ist (FIFO-Ordnung).
     *
     * Diese Methode implementiert das Monitor-Pattern mit wait/notify.
     * Der Thread gibt den Lock temporaer frei waehrend des Wartens,
     * sodass andere Threads den Abschnitt verlassen koennen.
     *
     * @Vorbedingung auto darf nicht null sein
     * @Nachbedingung Auto befindet sich im kritischen Bereich
     * @Nachbedingung aktuelleAnzahl ist um 1 erhoeht
     *
     * @param auto Das Auto das Zugang anfordert
     * @throws IllegalArgumentException wenn auto null ist
     * @throws InterruptedException wenn der Thread unterbrochen wird
     */
    public void einfahren(Auto auto) throws InterruptedException {
        if (auto == null) {
            throw new IllegalArgumentException("Auto darf nicht null sein");
        }

        synchronized (lock) {
            // In Warteschlange einreihen
            warteschlange.add(auto);
            RennLogger.logSync("WARTESCHLANGE_BEITRITT",
                    abschnitt.getName() + " (Position: " + warteschlange.size() + ")");

            // Warten bis Platz frei UND Auto an der Reihe ist
            while (aktuelleAnzahl >= kapazitaet || warteschlange.peek() != auto) {
                RennLogger.logSync("WARTEN", abschnitt.getName());
                lock.wait();
            }

            // Aus Warteschlange entfernen und einfahren
            warteschlange.poll();
            aktuelleAnzahl++;

            RennLogger.logSync("EINGEFAHREN",
                    abschnitt.getName() + " (Belegt: " + aktuelleAnzahl + "/" + kapazitaet + ")");
        }
    }

    /**
     * Verlaesst den Streckenabschnitt und gibt den Platz frei.
     * Alle wartenden Threads werden benachrichtigt, damit der naechste
     * in der Warteschlange einfahren kann.
     *
     * @Vorbedingung auto muss sich aktuell im Abschnitt befinden
     * @Nachbedingung aktuelleAnzahl ist um 1 verringert
     * @Nachbedingung Wartende Threads sind benachrichtigt
     *
     * @param auto Das Auto das den Abschnitt verlaesst
     * @throws IllegalArgumentException wenn auto null ist
     * @throws IllegalStateException wenn der Abschnitt leer ist
     */
    public void ausfahren(Auto auto) {
        if (auto == null) {
            throw new IllegalArgumentException("Auto darf nicht null sein");
        }

        synchronized (lock) {
            if (aktuelleAnzahl <= 0) {
                throw new IllegalStateException("Abschnitt ist bereits leer");
            }

            aktuelleAnzahl--;

            RennLogger.logSync("AUSGEFAHREN",
                    abschnitt.getName() + " (Belegt: " + aktuelleAnzahl + "/" + kapazitaet + ")");

            // Alle wartenden Threads benachrichtigen
            lock.notifyAll();
        }
    }

    /**
     * Versucht ohne Blockierung in den Abschnitt einzufahren.
     * Diese Methode ist nuetzlich wenn ein Auto alternative Routen waehlen kann.
     *
     * @Vorbedingung auto darf nicht null sein
     * @Nachbedingung Bei Erfolg: aktuelleAnzahl ist um 1 erhoeht
     * @Nachbedingung Bei Misserfolg: Keine Aenderung
     *
     * @param auto Das Auto das Zugang versucht
     * @return true wenn Einfahrt erfolgreich, false wenn Abschnitt voll
     * @throws IllegalArgumentException wenn auto null ist
     */
    public boolean versucheEinfahren(Auto auto) {
        if (auto == null) {
            throw new IllegalArgumentException("Auto darf nicht null sein");
        }

        synchronized (lock) {
            if (aktuelleAnzahl < kapazitaet && warteschlange.isEmpty()) {
                aktuelleAnzahl++;
                RennLogger.logSync("EINGEFAHREN_SOFORT",
                        abschnitt.getName() + " (Belegt: " + aktuelleAnzahl + "/" + kapazitaet + ")");
                return true;
            }
            return false;
        }
    }

    /**
     * Gibt die aktuelle Anzahl der Autos im Abschnitt zurueck.
     * Diese Methode ist thread-sicher.
     *
     * @Vorbedingung Keine
     * @Nachbedingung Rueckgabewert ist die aktuelle Belegung
     *
     * @return Anzahl der Autos im Abschnitt
     */
    public int getAktuelleAnzahl() {
        synchronized (lock) {
            return aktuelleAnzahl;
        }
    }

    /**
     * Gibt die Anzahl der wartenden Autos zurueck.
     *
     * @Vorbedingung Keine
     * @Nachbedingung Rueckgabewert ist die Warteschlangenlaenge
     *
     * @return Anzahl der wartenden Autos
     */
    public int getWartendeAnzahl() {
        synchronized (lock) {
            return warteschlange.size();
        }
    }

    /**
     * Prueft ob der Abschnitt voll ist.
     *
     * @Vorbedingung Keine
     * @Nachbedingung Rueckgabewert ist true wenn aktuelleAnzahl >= kapazitaet
     *
     * @return true wenn der Abschnitt seine maximale Kapazitaet erreicht hat
     */
    public boolean istVoll() {
        synchronized (lock) {
            return aktuelleAnzahl >= kapazitaet;
        }
    }

    /**
     * Gibt den ueberwachten Streckenabschnitt zurueck.
     *
     * @Vorbedingung Keine
     * @Nachbedingung Rueckgabewert ist der zugehoerige Abschnitt
     *
     * @return Der ueberwachte Streckenabschnitt
     */
    public Streckenabschnitt getAbschnitt() {
        return abschnitt;
    }

    /**
     * Gibt die maximale Kapazitaet zurueck.
     *
     * @Vorbedingung Keine
     * @Nachbedingung Rueckgabewert ist die Kapazitaet
     *
     * @return Maximale Anzahl gleichzeitiger Autos
     */
    public int getKapazitaet() {
        return kapazitaet;
    }

    @Override
    public String toString() {
        synchronized (lock) {
            return String.format("Monitor[%s: %d/%d, Wartend: %d]",
                    abschnitt.getName(), aktuelleAnzahl, kapazitaet, warteschlange.size());
        }
    }
}
