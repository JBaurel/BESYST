package synchronisation;

import model.Auto;
import model.Box;
import model.Fahrer;
import model.Team;
import model.enumType.ReifenTyp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
/**
 * Testklasse fuer den BoxenZugriff.
 *
 * Diese Tests pruefen die Kombination aus ReentrantLock und Conditions.
 * Der BoxenZugriff koordiniert die Kommunikation zwischen Auto-Thread (Producer)
 * und Boxencrew-Thread (Consumer) nach dem Producer-Consumer-Pattern.
 *
 * - ReentrantLock: Explizites Locking mit mehr Kontrolle als synchronized
 * - Condition: Ersetzt wait()/notify() bei ReentrantLock
 * - await(): Wartet auf Signal (gibt Lock frei wie wait())
 * - signal()/signalAll(): Weckt wartende Threads (wie notify()/notifyAll())
 *
 * UNTERSCHIED ZU MONITOR:
 * - Monitor: Ein implizites Lock, eine Warteliste
 * - ReentrantLock + Conditions: Explizites Lock, MEHRERE Wartelisten moeglich
 *
 * VORTEIL: Zwei separate Conditions erlauben gezielte Kommunikation:
 * - autoWartet: Crew wartet hier bis ein Auto ankommt
 * - serviceAbgeschlossen: Auto wartet hier bis der Service fertig ist
 *
 * @author F1 Simulation Team
 * @version 1.0
 */
@DisplayName("BoxenZugriff Tests (ReentrantLock + Conditions)")
class BoxenZugriffTest {
    private BoxenZugriff boxenZugriff;
    private Box testBox;
    private Team testTeam;
    private Auto testAuto;

    /**
     * Vor jedem Test wird ein frischer BoxenZugriff erstellt.
     */
    @BeforeEach
    void setUp() {
        Fahrer fahrer1 = new Fahrer("Test Fahrer 1", "TF1", 90);
        Fahrer fahrer2 = new Fahrer("Test Fahrer 2", "TF2", 85);
        testTeam = new Team("Test Team", javafx.scene.paint.Color.RED, fahrer1, fahrer2);
        testBox = new Box(testTeam, 0);
        boxenZugriff = new BoxenZugriff(testBox);
        testAuto = new Auto(1, testTeam, fahrer1);
    }

    /**
     * TEST 1: Kompletter Pitstop-Ablauf (Auto und Crew Koordination)
     *
     * SZENARIO:
     * Ein Auto faehrt in die Box und wartet auf den Reifenwechsel.
     * Die Boxencrew wird benachrichtigt, fuehrt den Service durch,
     * und signalisiert dem Auto, dass es weiterfahren kann.
     *
     * ERWARTETES VERHALTEN:
     * 1. Auto ruft pitstopDurchfuehren() auf -> signalisiert Ankunft, wartet auf Service
     * 2. Crew ruft warteAufAuto() auf -> wird geweckt weil Auto da ist
     * 3. Crew fuehrt Service durch
     * 4. Crew ruft serviceAbschliessen() auf -> signalisiert dem Auto
     * 5. Auto wird geweckt und kann weiterfahren
     *
     * KONZEPT: Bidirektionale Kommunikation mit zwei Conditions
     */
    @Test
    @DisplayName("Test 1: Kompletter Pitstop-Ablauf mit bidirektionaler Kommunikation")
    void testKompletterPitstopAblauf() throws InterruptedException {
        List<String> ereignisse = Collections.synchronizedList(new ArrayList<>());
        AtomicBoolean pitstopErfolgreich = new AtomicBoolean(false);

        CountDownLatch fertigLatch = new CountDownLatch(2);


        Thread autoThread = new Thread(() -> {
            try {
                ereignisse.add("Auto faehrt in Box");


                boxenZugriff.pitstopDurchfuehren(testAuto, ReifenTyp.HARD);

                ereignisse.add("Auto faehrt weiter");
                pitstopErfolgreich.set(true);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                fertigLatch.countDown();
            }
        }, "AutoThread");


        Thread crewThread = new Thread(() -> {
            try {
                ereignisse.add("Crew wartet auf Auto");


                Auto angekommenesAuto = boxenZugriff.warteAufAuto();

                ereignisse.add("Crew hat Auto empfangen: " + angekommenesAuto.getFahrer().getKuerzel());


                Thread.sleep(100);
                ereignisse.add("Crew Service abgeschlossen");


                boxenZugriff.serviceAbschliessen();

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                fertigLatch.countDown();
            }
        }, "CrewThread");


        crewThread.start();
        Thread.sleep(50);


        autoThread.start();

        boolean fertig = fertigLatch.await(5, TimeUnit.SECONDS);


        assertTrue(fertig, "Beide Threads sollten fertig werden");
        assertTrue(pitstopErfolgreich.get(), "Der Pitstop sollte erfolgreich sein");


        int crewEmpfangenIndex = -1;
        for (int i = 0; i < ereignisse.size(); i++) {
            if (ereignisse.get(i).contains("Crew hat Auto empfangen")) {
                crewEmpfangenIndex = i;
                break;
            }
        }
        int autoFaehrtWeiterIndex = ereignisse.indexOf("Auto faehrt weiter");
        int crewServiceIndex = ereignisse.indexOf("Crew Service abgeschlossen");

        assertTrue(crewEmpfangenIndex < crewServiceIndex,
                "Crew muss Auto empfangen bevor Service abgeschlossen ist");
        assertTrue(crewServiceIndex < autoFaehrtWeiterIndex,
                "Service muss abgeschlossen sein bevor Auto weiterfaehrt. " +
                        "Dies zeigt die korrekte Synchronisation mit zwei Conditions. " +
                        "Ereignisse: " + ereignisse);
    }

    /**
     * TEST 2: Crew wartet korrekt auf Auto (Condition await)
     *
     * SZENARIO:
     * Die Boxencrew wartet auf ein Auto. Ohne Auto sollte die Crew
     * in await() blockiert bleiben (oder bei Timeout zurueckkehren).
     *
     * ERWARTETES VERHALTEN:
     * - warteAufAutoMitTimeout() gibt null zurueck wenn kein Auto kommt
     * - Die Crew blockiert nicht ewig, sondern kehrt nach Timeout zurueck
     *
     * KONZEPT: await() mit Timeout verhindert ewiges Warten
     */
    @Test
    @DisplayName("Test 2: Crew blockiert in await() bis Auto kommt oder Timeout")
    void testCrewWartetAufAuto() throws InterruptedException {
        // Crew wartet mit Timeout - kein Auto kommt
        long startZeit = System.currentTimeMillis();
        Auto ergebnis = boxenZugriff.warteAufAutoMitTimeout(200);
        long dauer = System.currentTimeMillis() - startZeit;


        assertNull(ergebnis,
                "warteAufAutoMitTimeout() sollte null zurueckgeben wenn kein Auto kommt. " +
                        "Dies zeigt, dass await() mit Timeout funktioniert.");

        assertTrue(dauer >= 180 && dauer < 400,
                "Die Methode sollte etwa 200ms warten. Tatsaechlich: " + dauer + "ms");
    }

    /**
     * TEST 3: Mehrere Pitstops hintereinander (Wiederverwendbarkeit)
     *
     * SZENARIO:
     * Mehrere Autos fahren nacheinander in die Box.
     * Der BoxenZugriff muss fuer jeden Pitstop korrekt funktionieren.
     *
     * ERWARTETES VERHALTEN:
     * - Jeder Pitstop wird korrekt durchgefuehrt
     * - Die Conditions werden korrekt zurueckgesetzt
     * - Kein Zustand bleibt haengen
     *
     * KONZEPT: Die Lock/Condition-Logik muss wiederverwendbar sein
     */
    @Test
    @DisplayName("Test 3: Mehrere Pitstops hintereinander (Wiederverwendbarkeit)")
    void testMehrerePitstopsHintereinander() throws InterruptedException {
        final int ANZAHL_PITSTOPS = 3;


        List<Auto> testAutos = new ArrayList<>();
        for (int i = 0; i < ANZAHL_PITSTOPS; i++) {
            Fahrer f1 = new Fahrer("Fahrer " + i + "a", "F" + i + "A", 80);
            Fahrer f2 = new Fahrer("Fahrer " + i + "b", "F" + i + "B", 75);
            Team team = new Team("Team " + i, javafx.scene.paint.Color.BLUE, f1, f2);
            testAutos.add(new Auto(10 + i, team, f1));
        }

        AtomicBoolean fehlerAufgetreten = new AtomicBoolean(false);
        List<String> bearbeitet = Collections.synchronizedList(new ArrayList<>());


        Thread crewThread = new Thread(() -> {
            try {
                for (int i = 0; i < ANZAHL_PITSTOPS; i++) {
                    Auto auto = boxenZugriff.warteAufAuto();
                    if (auto != null) {
                        bearbeitet.add(auto.getFahrer().getKuerzel());
                        Thread.sleep(50);
                        boxenZugriff.serviceAbschliessen();
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                fehlerAufgetreten.set(true);
            }
        }, "CrewThread");

        crewThread.start();
        Thread.sleep(50);


        for (Auto auto : testAutos) {
            try {
                boxenZugriff.pitstopDurchfuehren(auto, ReifenTyp.MEDIUM);
            } catch (Exception e) {
                fehlerAufgetreten.set(true);
            }
            Thread.sleep(20);
        }

        crewThread.join(5000);


        assertFalse(fehlerAufgetreten.get(),
                "Kein Fehler sollte auftreten bei mehreren Pitstops");
        assertEquals(ANZAHL_PITSTOPS, bearbeitet.size(),
                "Alle " + ANZAHL_PITSTOPS + " Pitstops sollten erfolgreich sein. " +
                        "Dies zeigt, dass der Lock/Condition-Mechanismus wiederverwendbar ist. " +
                        "Bearbeitet: " + bearbeitet);
    }
}