package synchronisation;

import model.Auto;
import model.Box;
import model.Team;
import model.enumType.ReifenTyp;
import util.RennLogger;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Synchronisationsklasse fuer den exklusiven Zugriff auf eine Teambox.
 * Diese Klasse verwendet ReentrantLock mit Condition fuer die Koordination
 * zwischen Autos und der Boxencrew.
 *
 * Jedes Team hat genau eine Box, die von beiden Teamfahrzeugen genutzt wird.
 * Wenn beide Autos gleichzeitig einen Pitstop benoetigen, muss eines warten.
 *
 * Der ReentrantLock ermoeglicht:
 * - Exklusiven Zugriff auf die Box
 * - Fairness-Garantie (FIFO-Ordnung bei Wartezeiten)
 * - Condition fuer die Koordination zwischen Auto und Crew
 *
 * Die Boxencrew wird durch eine separate Condition benachrichtigt,
 * wenn ein Auto eingetroffen ist und auf Service wartet.
 *
 * @author F1 Simulation Team
 * @version 1.0
 */
public class BoxenZugriff {

    /** Minimale Pitstop-Dauer in Millisekunden. */
    public static final long MIN_PITSTOP_DAUER_MS = 2000;

    /** Maximale Pitstop-Dauer in Millisekunden. */
    public static final long MAX_PITSTOP_DAUER_MS = 4000;

    private final Box box;
    private final Team team;
    private final ReentrantLock lock;
    private final Condition autoWartet;
    private final Condition serviceAbgeschlossen;

    private Auto aktuellesAuto;
    private boolean serviceAngefordert;
    private boolean serviceLaeuft;
    private boolean serviceBeendet;
    private ReifenTyp gewaehlterReifenTyp;

    /**
     * Erstellt eine neue Boxen-Zugriffskontrolle fuer die angegebene Box.
     * Der Lock ist als "fair" konfiguriert fuer gerechte Wartezeiten.
     *
     * @Vorbedingung box darf nicht null sein
     * @Nachbedingung Lock und Conditions sind initialisiert
     * @Nachbedingung Box ist im Leerlauf-Zustand
     *
     * @param box Die zu verwaltende Teambox
     * @throws IllegalArgumentException wenn box null ist
     */
    public BoxenZugriff(Box box) {
        if (box == null) {
            throw new IllegalArgumentException("Box darf nicht null sein");
        }

        this.box = box;
        this.team = box.getTeam();
        this.lock = new ReentrantLock(true); // Fair lock
        this.autoWartet = lock.newCondition();
        this.serviceAbgeschlossen = lock.newCondition();

        this.aktuellesAuto = null;
        this.serviceAngefordert = false;
        this.serviceLaeuft = false;
        this.serviceBeendet = false;
        this.gewaehlterReifenTyp = null;

        RennLogger.debug("BoxenZugriff erstellt fuer Team: " + team.getName());
    }

    /**
     * Faehrt in die Box ein und wartet auf den Reifenwechsel.
     * Diese Methode wird vom AutoThread aufgerufen und blockiert
     * bis der Service abgeschlossen ist.
     *
     * Der Ablauf ist:
     * 1. Lock erwerben (warten falls Box belegt)
     * 2. Service anfordern und Boxencrew signalisieren
     * 3. Warten bis Service abgeschlossen ist
     * 4. Lock freigeben
     *
     * @Vorbedingung auto darf nicht null sein
     * @Vorbedingung auto muss zum Team dieser Box gehoeren
     * @Vorbedingung reifenTyp darf nicht null sein
     * @Nachbedingung Reifenwechsel ist durchgefuehrt
     * @Nachbedingung Box ist wieder frei
     *
     * @param auto Das einfahrende Auto
     * @param reifenTyp Der gewuenschte Reifentyp fuer den Wechsel
     * @throws IllegalArgumentException wenn Parameter ungueltig sind
     * @throws InterruptedException wenn der Thread unterbrochen wird
     */
    public void pitstopDurchfuehren(Auto auto, ReifenTyp reifenTyp) throws InterruptedException {
        if (auto == null) {
            throw new IllegalArgumentException("Auto darf nicht null sein");
        }
        if (!auto.getTeam().equals(team)) {
            throw new IllegalArgumentException("Auto gehoert nicht zu diesem Team");
        }
        if (reifenTyp == null) {
            throw new IllegalArgumentException("Reifentyp darf nicht null sein");
        }

        lock.lock();
        try {
            RennLogger.logSync("BOX_EINFAHRT",
                    team.getName() + " - " + auto.getFahrer().getKuerzel());

            /
            aktuellesAuto = auto;
            gewaehlterReifenTyp = reifenTyp;
            serviceAngefordert = true;
            serviceBeendet = false;

            autoWartet.signalAll();


            while (!serviceBeendet) {
                RennLogger.logSync("WARTEN_AUF_SERVICE", team.getName());
                serviceAbgeschlossen.await();
            }

            RennLogger.logSync("SERVICE_ERHALTEN",
                    team.getName() + " - Neue Reifen: " + reifenTyp);


            aktuellesAuto = null;
            serviceAngefordert = false;
            serviceLaeuft = false;
            serviceBeendet = false;
            gewaehlterReifenTyp = null;

        } finally {
            lock.unlock();
        }
    }

    /**
     * Wartet auf ein einfahrendes Auto.
     * Diese Methode wird vom BoxencrewThread aufgerufen und blockiert
     * bis ein Auto Service anfordert.
     *
     * @Vorbedingung Keine
     * @Nachbedingung Ein Auto wartet auf Service
     * @Nachbedingung Rueckgabewert ist das wartende Auto
     *
     * @return Das Auto das auf Service wartet
     * @throws InterruptedException wenn der Thread unterbrochen wird
     */
    public Auto warteAufAuto() throws InterruptedException {
        lock.lock();
        try {
            // Warten bis ein Auto Service anfordert
            while (!serviceAngefordert) {
                autoWartet.await();
            }

            RennLogger.logSync("AUTO_ERKANNT",
                    team.getName() + " - " + aktuellesAuto.getFahrer().getKuerzel());

            serviceLaeuft = true;
            return aktuellesAuto;

        } finally {
            lock.unlock();
        }
    }

    /**
     * Wartet auf ein Auto mit Timeout.
     * Nuetzlich um den BoxencrewThread regelmaessig auf Abbruchbedingungen
     * pruefen zu lassen.
     *
     * @Vorbedingung timeoutMs muss > 0 sein
     * @Nachbedingung Bei Erfolg: Auto wartet auf Service
     * @Nachbedingung Bei Timeout: Rueckgabe null
     *
     * @param timeoutMs Maximale Wartezeit in Millisekunden
     * @return Das Auto oder null bei Timeout
     * @throws InterruptedException wenn der Thread unterbrochen wird
     */
    public Auto warteAufAutoMitTimeout(long timeoutMs) throws InterruptedException {
        lock.lock();
        try {
            long verbleibend = timeoutMs;
            long start = System.currentTimeMillis();

            while (!serviceAngefordert && verbleibend > 0) {
                autoWartet.await(verbleibend, TimeUnit.MILLISECONDS);
                verbleibend = timeoutMs - (System.currentTimeMillis() - start);
            }

            if (serviceAngefordert) {
                serviceLaeuft = true;
                return aktuellesAuto;
            }
            return null;

        } finally {
            lock.unlock();
        }
    }

    /**
     * Meldet den Abschluss des Reifenwechsels.
     * Diese Methode wird vom BoxencrewThread aufgerufen nachdem
     * der Reifenwechsel durchgefuehrt wurde.
     *
     * @Vorbedingung Ein Auto muss aktuell bedient werden
     * @Nachbedingung serviceBeendet ist true
     * @Nachbedingung Wartendes Auto ist benachrichtigt
     *
     * @throws IllegalStateException wenn kein Auto bedient wird
     */
    public void serviceAbschliessen() {
        lock.lock();
        try {
            if (!serviceLaeuft) {
                throw new IllegalStateException("Kein Service aktiv");
            }

            serviceBeendet = true;
            serviceLaeuft = false;

            RennLogger.logSync("SERVICE_ABGESCHLOSSEN", team.getName());


            serviceAbgeschlossen.signalAll();

        } finally {
            lock.unlock();
        }
    }

    /**
     * Gibt den aktuell gewaehlten Reifentyp zurueck.
     * Diese Information wird vom BoxencrewThread benoetigt
     * um die richtigen Reifen vorzubereiten.
     *
     * @Vorbedingung Keine
     * @Nachbedingung Rueckgabewert ist der gewaehlte Typ oder null
     *
     * @return Der angeforderte Reifentyp oder null
     */
    public ReifenTyp getGewaehlterReifenTyp() {
        lock.lock();
        try {
            return gewaehlterReifenTyp;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Prueft ob die Box aktuell belegt ist.
     *
     * @Vorbedingung Keine
     * @Nachbedingung Rueckgabewert ist true wenn ein Auto in der Box ist
     *
     * @return true wenn die Box belegt ist
     */
    public boolean istBelegt() {
        lock.lock();
        try {
            return aktuellesAuto != null;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Prueft ob ein Service aktiv laeuft.
     *
     * @Vorbedingung Keine
     * @Nachbedingung Rueckgabewert ist true wenn Reifenwechsel aktiv
     *
     * @return true wenn gerade ein Reifenwechsel durchgefuehrt wird
     */
    public boolean istServiceAktiv() {
        lock.lock();
        try {
            return serviceLaeuft;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Gibt die Anzahl wartender Threads zurueck.
     *
     * @Vorbedingung Keine
     * @Nachbedingung Rueckgabewert ist Anzahl wartender Threads
     *
     * @return Anzahl der Threads die auf den Lock warten
     */
    public int getWartendeAnzahl() {
        return lock.getQueueLength();
    }

    /**
     * Gibt das aktuell in der Box befindliche Auto zurueck.
     *
     * @Vorbedingung Keine
     * @Nachbedingung Rueckgabewert ist das Auto oder null
     *
     * @return Das aktuelle Auto oder null wenn Box leer
     */
    public Auto getAktuellesAuto() {
        lock.lock();
        try {
            return aktuellesAuto;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Gibt die verwaltete Box zurueck.
     *
     * @Vorbedingung Keine
     * @Nachbedingung Rueckgabewert ist die Box
     *
     * @return Die verwaltete Box
     */
    public Box getBox() {
        return box;
    }

    /**
     * Gibt das zugehoerige Team zurueck.
     *
     * @Vorbedingung Keine
     * @Nachbedingung Rueckgabewert ist das Team
     *
     * @return Das Team dieser Box
     */
    public Team getTeam() {
        return team;
    }

    @Override
    public String toString() {
        lock.lock();
        try {
            String status = aktuellesAuto != null
                    ? "Belegt: " + aktuellesAuto.getFahrer().getKuerzel()
                    : "Frei";
            return String.format("BoxenZugriff[%s: %s, Wartend: %d]",
                    team.getName(), status, lock.getQueueLength());
        } finally {
            lock.unlock();
        }
    }
}