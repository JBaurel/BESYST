package synchronisation;

import model.Auto;
import model.Streckenabschnitt;
import util.RennLogger;

import java.util.Random;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Manager fuer Ueberholmanoever in DRS-Zonen.
 * Diese Klasse berechnet die Erfolgswahrscheinlichkeit eines Ueberholversuchs
 * basierend auf verschiedenen Faktoren wie Reifenzustand, DRS, Windschatten
 * und Fahrergeschicklichkeit.
 *
 * Die Ueberhollogik verwendet gewichtete Faktoren:
 * - Reifenzustand: 25% (bessere Reifen = hoehere Chance)
 * - Reifentyp: 15% (weichere Reifen = hoehere Chance)
 * - DRS: 20% (aktiviertes DRS erhoeht Chance deutlich)
 * - Windschatten: 15% (nah hinter Vordermann = Vorteil)
 * - Fahrzeugschaden: 15% (nicht implementiert - immer 0)
 * - Fahrergeschicklichkeit: 10% (besserer Fahrer = hoehere Chance)
 *
 * Diese Klasse verwendet ein ReadWriteLock fuer thread-sichere Zugriffe
 * auf die Statistiken, waehrend Berechnungen nicht blockieren.
 *
 * @author F1 Simulation Team
 * @version 1.0
 */
public class UeberholzonenManager {

    // Gewichtung der einzelnen Faktoren (Summe = 1.0)
    private static final double GEWICHT_REIFENZUSTAND = 0.25;
    private static final double GEWICHT_REIFENTYP = 0.15;
    private static final double GEWICHT_DRS = 0.20;
    private static final double GEWICHT_WINDSCHATTEN = 0.15;
    private static final double GEWICHT_FAHRZEUGSCHADEN = 0.15;
    private static final double GEWICHT_FAHRERGESCHICK = 0.10;

    // Basiswahrscheinlichkeit fuer einen erfolgreichen Ueberholversuch
    private static final double BASIS_ERFOLGSWAHRSCHEINLICHKEIT = 0.3;

    // Maximale Wahrscheinlichkeit (um unrealistische Werte zu vermeiden)
    private static final double MAX_WAHRSCHEINLICHKEIT = 0.85;

    private final ReadWriteLock statistikLock;
    private final Random random;

    // Statistiken
    private int gesamtVersuche;
    private int erfolgreicheUeberholungen;
    private int fehlgeschlageneVersuche;

    /**
     * Erstellt einen neuen Ueberholzonen-Manager.
     * Statistiken werden auf 0 initialisiert.
     *
     * @Vorbedingung Keine
     * @Nachbedingung Manager ist initialisiert mit leeren Statistiken
     */
    public UeberholzonenManager() {
        this.statistikLock = new ReentrantReadWriteLock();
        this.random = new Random();
        this.gesamtVersuche = 0;
        this.erfolgreicheUeberholungen = 0;
        this.fehlgeschlageneVersuche = 0;

        RennLogger.debug("UeberholzonenManager initialisiert");
    }

    /**
     * Fuehrt einen Ueberholversuch durch und bestimmt ob er erfolgreich ist.
     * Die Methode berechnet die Erfolgswahrscheinlichkeit und wuerfelt dann
     * ob das Manoever gelingt.
     *
     * @Vorbedingung ueberholer und verteidiger duerfen nicht null sein
     * @Vorbedingung zone darf nicht null sein und muss eine Ueberholzone sein
     * @Nachbedingung Statistiken sind aktualisiert
     * @Nachbedingung Rueckgabewert gibt Erfolg des Manoevers an
     *
     * @param ueberholer Das ueberholende Auto
     * @param verteidiger Das zu ueberholende Auto
     * @param zone Der Streckenabschnitt (Ueberholzone)
     * @param abstandInMs Zeitabstand zum Vordermann in Millisekunden
     * @return true wenn das Ueberholmanoever erfolgreich ist
     * @throws IllegalArgumentException wenn Parameter ungueltig sind
     */
    public boolean versucheUeberholung(Auto ueberholer, Auto verteidiger,
                                       Streckenabschnitt zone, long abstandInMs) {
        if (ueberholer == null || verteidiger == null) {
            throw new IllegalArgumentException("Autos duerfen nicht null sein");
        }
        if (zone == null) {
            throw new IllegalArgumentException("Zone darf nicht null sein");
        }

        // Berechne Erfolgswahrscheinlichkeit
        double wahrscheinlichkeit = berechneErfolgswahrscheinlichkeit(
                ueberholer, verteidiger, zone, abstandInMs);

        // Wuerfeln ob Ueberholung gelingt
        double wurf = random.nextDouble();
        boolean erfolg = wurf < wahrscheinlichkeit;

        // Statistiken aktualisieren
        aktualisiereStatistiken(erfolg);

        // Logging
        String meldung = String.format(
                "%s vs %s: Chance=%.1f%%, Wurf=%.1f%%, Ergebnis=%s",
                ueberholer.getFahrer().getKuerzel(),
                verteidiger.getFahrer().getKuerzel(),
                wahrscheinlichkeit * 100,
                wurf * 100,
                erfolg ? "ERFOLG" : "MISSERFOLG"
        );

        if (erfolg) {
            RennLogger.info("Ueberholung: " + meldung);
        } else {
            RennLogger.debug("Ueberholversuch: " + meldung);
        }

        return erfolg;
    }

    /**
     * Berechnet die Erfolgswahrscheinlichkeit eines Ueberholmanoevers.
     * Diese Methode ist public fuer Testzwecke und Anzeige in der GUI.
     *
     * Die Berechnung erfolgt additiv: Jeder Faktor traegt entsprechend
     * seiner Gewichtung zur Gesamtwahrscheinlichkeit bei.
     *
     * @Vorbedingung ueberholer und verteidiger duerfen nicht null sein
     * @Nachbedingung Rueckgabewert liegt zwischen 0.0 und MAX_WAHRSCHEINLICHKEIT
     *
     * @param ueberholer Das ueberholende Auto
     * @param verteidiger Das zu ueberholende Auto
     * @param zone Der Streckenabschnitt
     * @param abstandInMs Zeitabstand in Millisekunden
     * @return Erfolgswahrscheinlichkeit zwischen 0.0 und 1.0
     */
    public double berechneErfolgswahrscheinlichkeit(Auto ueberholer, Auto verteidiger,
                                                    Streckenabschnitt zone, long abstandInMs) {
        double wahrscheinlichkeit = BASIS_ERFOLGSWAHRSCHEINLICHKEIT;

        // Faktor 1: Reifenzustand (25%)
        // Besser erhaltene Reifen des Ueberholers = Vorteil
        double reifenDifferenz = verteidiger.getAktuelleReifen().getAbnutzungProzent()
                - ueberholer.getAktuelleReifen().getAbnutzungProzent();
        double reifenFaktor = (reifenDifferenz / 100.0) * GEWICHT_REIFENZUSTAND;
        wahrscheinlichkeit += reifenFaktor;

        // Faktor 2: Reifentyp (15%)
        // Weichere Reifen = schneller = Vorteil
        double typUeberholer = ueberholer.getAktuelleReifen().getTyp().getGeschwindigkeitsFaktor();
        double typVerteidiger = verteidiger.getAktuelleReifen().getTyp().getGeschwindigkeitsFaktor();
        double typDifferenz = typUeberholer - typVerteidiger;
        wahrscheinlichkeit += typDifferenz * GEWICHT_REIFENTYP;

        // Faktor 3: DRS (20%)
        // In einer DRS-Zone erhaelt der Ueberholer einen festen Bonus
        if (zone.istUeberholzone()) {
            wahrscheinlichkeit += GEWICHT_DRS;
        }

        // Faktor 4: Windschatten (15%)
        // Je naeher dran, desto groesser der Vorteil (max bei < 1 Sekunde)
        double windschattenBonus = 0.0;
        if (abstandInMs < 1000) {
            // Voller Bonus bei < 1 Sekunde
            windschattenBonus = GEWICHT_WINDSCHATTEN;
        } else if (abstandInMs < 2000) {
            // Linearer Abfall zwischen 1-2 Sekunden
            windschattenBonus = GEWICHT_WINDSCHATTEN * (2000 - abstandInMs) / 1000.0;
        }
        wahrscheinlichkeit += windschattenBonus;

        // Faktor 5: Fahrzeugschaden (15%)
        // Nicht implementiert in dieser Version - immer 0
        // In einer Erweiterung koennte hier der Zustand des Autos einfliessen

        // Faktor 6: Fahrergeschicklichkeit (10%)
        double geschickUeberholer = ueberholer.getFahrer().getGeschicklichkeitsFaktor();
        double geschickVerteidiger = verteidiger.getFahrer().getGeschicklichkeitsFaktor();
        double geschickDifferenz = geschickUeberholer - geschickVerteidiger;
        wahrscheinlichkeit += geschickDifferenz * GEWICHT_FAHRERGESCHICK;

        // Begrenze auf sinnvollen Bereich
        wahrscheinlichkeit = Math.max(0.05, Math.min(MAX_WAHRSCHEINLICHKEIT, wahrscheinlichkeit));

        return wahrscheinlichkeit;
    }

    /**
     * Aktualisiert die Statistiken thread-sicher mit einem Write-Lock.
     *
     * @Vorbedingung Keine
     * @Nachbedingung Statistiken sind aktualisiert
     *
     * @param erfolg true wenn Ueberholung erfolgreich war
     */
    private void aktualisiereStatistiken(boolean erfolg) {
        statistikLock.writeLock().lock();
        try {
            gesamtVersuche++;
            if (erfolg) {
                erfolgreicheUeberholungen++;
            } else {
                fehlgeschlageneVersuche++;
            }
        } finally {
            statistikLock.writeLock().unlock();
        }
    }

    /**
     * Gibt die Gesamtanzahl der Ueberholversuche zurueck.
     *
     * @Vorbedingung Keine
     * @Nachbedingung Rueckgabewert ist aktuelle Anzahl
     *
     * @return Anzahl aller Ueberholversuche
     */
    public int getGesamtVersuche() {
        statistikLock.readLock().lock();
        try {
            return gesamtVersuche;
        } finally {
            statistikLock.readLock().unlock();
        }
    }

    /**
     * Gibt die Anzahl der erfolgreichen Ueberholungen zurueck.
     *
     * @Vorbedingung Keine
     * @Nachbedingung Rueckgabewert ist aktuelle Anzahl
     *
     * @return Anzahl erfolgreicher Ueberholungen
     */
    public int getErfolgreicheUeberholungen() {
        statistikLock.readLock().lock();
        try {
            return erfolgreicheUeberholungen;
        } finally {
            statistikLock.readLock().unlock();
        }
    }

    /**
     * Gibt die Anzahl der fehlgeschlagenen Versuche zurueck.
     *
     * @Vorbedingung Keine
     * @Nachbedingung Rueckgabewert ist aktuelle Anzahl
     *
     * @return Anzahl fehlgeschlagener Versuche
     */
    public int getFehlgeschlageneVersuche() {
        statistikLock.readLock().lock();
        try {
            return fehlgeschlageneVersuche;
        } finally {
            statistikLock.readLock().unlock();
        }
    }

    /**
     * Berechnet die Erfolgsquote aller Ueberholversuche.
     *
     * @Vorbedingung Keine
     * @Nachbedingung Rueckgabewert liegt zwischen 0.0 und 1.0
     *
     * @return Erfolgsquote als Wert zwischen 0.0 und 1.0
     */
    public double getErfolgsquote() {
        statistikLock.readLock().lock();
        try {
            if (gesamtVersuche == 0) {
                return 0.0;
            }
            return (double) erfolgreicheUeberholungen / gesamtVersuche;
        } finally {
            statistikLock.readLock().unlock();
        }
    }

    /**
     * Setzt alle Statistiken auf 0 zurueck.
     * Wird beim Start eines neuen Rennens aufgerufen.
     *
     * @Vorbedingung Keine
     * @Nachbedingung Alle Statistiken sind 0
     */
    public void resetStatistiken() {
        statistikLock.writeLock().lock();
        try {
            gesamtVersuche = 0;
            erfolgreicheUeberholungen = 0;
            fehlgeschlageneVersuche = 0;
            RennLogger.debug("Ueberholstatistiken zurueckgesetzt");
        } finally {
            statistikLock.writeLock().unlock();
        }
    }

    @Override
    public String toString() {
        statistikLock.readLock().lock();
        try {
            return String.format(
                    "Ueberholungen[Versuche: %d, Erfolge: %d (%.1f%%), Fehlschlaege: %d]",
                    gesamtVersuche,
                    erfolgreicheUeberholungen,
                    getErfolgsquote() * 100,
                    fehlgeschlageneVersuche);
        } finally {
            statistikLock.readLock().unlock();
        }
    }
}
