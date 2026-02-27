package synchronisation;

import model.Auto;
import model.Fahrer;
import model.Team;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testklasse fuer den PitstopLaneController.
 *
 * Diese Tests pruefen die Koordination der Boxengassen-Ein- und Ausfahrt
 * mit zwei separaten Semaphoren. Dies ist ein Beispiel fuer die Verwendung
 * mehrerer Semaphoren zur Kontrolle verschiedener Engpaesse.
 *
 * - einfahrtSemaphore: Begrenzt gleichzeitige Einfahrten
 * - ausfahrtSemaphore: Begrenzt gleichzeitige Ausfahrten
 * - AtomicInteger: Thread-sicherer Zaehler fuer Autos in der Boxengasse
 *
 * ARCHITEKTUR-KONZEPT:
 * Die Boxengasse hat zwei Engpaesse (Einfahrt und Ausfahrt) die unabhaengig
 * voneinander kontrolliert werden muessen. Dies verhindert Staus und
 * ermoeglicht fluessigen Verkehr.
 *
 * @author F1 Simulation Team
 * @version 1.0
 */
@DisplayName("PitstopLaneController Tests (Zwei Semaphoren)")
class PitstopLaneControllerTest {
    private PitstopLaneController controller;
    private List<Auto> testAutos;

    /**
     * Vor jedem Test wird ein frischer Controller erstellt.
     * Einfahrt-Kapazitaet: 3, Ausfahrt-Kapazitaet: 3
     */
    @BeforeEach
    void setUp() {
        controller = new PitstopLaneController();


        testAutos = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            Fahrer f1 = new Fahrer("Fahrer " + i + "a", "F" + i + "A", 80);
            Fahrer f2 = new Fahrer("Fahrer " + i + "b", "F" + i + "B", 75);
            Team team = new Team("Team " + i, javafx.scene.paint.Color.BLUE, f1, f2);
            testAutos.add(new Auto(10 + i, team, f1));
        }
    }

    /**
     * TEST 1: Einfahrt-Semaphore begrenzt gleichzeitige Einfahrten auf 3
     *
     * SZENARIO:
     * 5 Autos versuchen gleichzeitig in die Boxengasse einzufahren.
     * Die Einfahrt-Semaphore hat Kapazitaet 3.
     *
     * ERWARTETES VERHALTEN:
     * - Maximal 3 Autos sind gleichzeitig in der Einfahrt-Phase
     * - Die anderen 2 muessen warten
     *
     * KONZEPT: Erste Semaphore kontrolliert den Einfahrt-Engpass
     */
    @Test
    @DisplayName("Test 1: Maximal 3 Autos gleichzeitig in der Einfahrt")
    void testMaximalDreiAutosInEinfahrt() throws InterruptedException {
        final int ANZAHL_AUTOS = 5;

        AtomicInteger gleichzeitigInEinfahrt = new AtomicInteger(0);
        AtomicInteger maxGleichzeitig = new AtomicInteger(0);

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch fertigLatch = new CountDownLatch(ANZAHL_AUTOS);

        for (int i = 0; i < ANZAHL_AUTOS; i++) {
            final Auto auto = testAutos.get(i);

            Thread thread = new Thread(() -> {
                try {
                    startLatch.await();


                    controller.einfahrtAnfordern(auto);


                    int aktuell = gleichzeitigInEinfahrt.incrementAndGet();
                    maxGleichzeitig.updateAndGet(max -> Math.max(max, aktuell));


                    Thread.sleep(100);

                    gleichzeitigInEinfahrt.decrementAndGet();
                    controller.einfahrtAbschliessen(auto);

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    fertigLatch.countDown();
                }
            }, "AutoThread-" + i);

            thread.start();
        }

        startLatch.countDown();
        boolean fertig = fertigLatch.await(5, TimeUnit.SECONDS);


        assertTrue(fertig, "Alle Threads sollten fertig werden");
        assertEquals(3, maxGleichzeitig.get(),
                "Die Einfahrt-Semaphore hat Kapazitaet 3. " +
                        "Maximal 3 Autos duerfen gleichzeitig in der Einfahrt-Phase sein. " +
                        "Dies verhindert Staus am Boxengassen-Eingang.");
    }

    /**
     * TEST 2: Ausfahrt-Semaphore arbeitet unabhaengig von Einfahrt
     *
     * SZENARIO:
     * Mehrere Autos sind bereits in der Boxengasse und wollen ausfahren.
     * Die Ausfahrt wird separat von der Einfahrt kontrolliert.
     *
     * ERWARTETES VERHALTEN:
     * - Maximal 3 Autos sind gleichzeitig in der Ausfahrt-Phase
     * - Einfahrt und Ausfahrt sind unabhaengig
     *
     * KONZEPT: Zweite Semaphore kontrolliert den Ausfahrt-Engpass
     */
    @Test
    @DisplayName("Test 2: Maximal 3 Autos gleichzeitig in der Ausfahrt")
    void testMaximalDreiAutosInAusfahrt() throws InterruptedException {
        final int ANZAHL_AUTOS = 5;

        AtomicInteger gleichzeitigInAusfahrt = new AtomicInteger(0);
        AtomicInteger maxGleichzeitig = new AtomicInteger(0);

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch fertigLatch = new CountDownLatch(ANZAHL_AUTOS);


        for (int i = 0; i < ANZAHL_AUTOS; i++) {
            controller.einfahrtAnfordern(testAutos.get(i));
            controller.einfahrtAbschliessen(testAutos.get(i));
        }

        for (int i = 0; i < ANZAHL_AUTOS; i++) {
            final Auto auto = testAutos.get(i);

            Thread thread = new Thread(() -> {
                try {
                    startLatch.await();


                    controller.ausfahrtAnfordern(auto);


                    int aktuell = gleichzeitigInAusfahrt.incrementAndGet();
                    maxGleichzeitig.updateAndGet(max -> Math.max(max, aktuell));


                    Thread.sleep(100);

                    gleichzeitigInAusfahrt.decrementAndGet();
                    controller.ausfahrtAbschliessen(auto);

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    fertigLatch.countDown();
                }
            }, "AutoThread-" + i);

            thread.start();
        }

        startLatch.countDown();
        boolean fertig = fertigLatch.await(5, TimeUnit.SECONDS);


        assertTrue(fertig, "Alle Threads sollten fertig werden");
        assertEquals(3, maxGleichzeitig.get(),
                "Die Ausfahrt-Semaphore hat Kapazitaet 3. " +
                        "Maximal 3 Autos duerfen gleichzeitig in der Ausfahrt-Phase sein.");
    }

    /**
     * TEST 3: Komplette Boxengassen-Durchfahrt (Einfahrt -> Durchfahrt -> Ausfahrt)
     *
     * SZENARIO:
     * Mehrere Autos fahren durch die komplette Boxengasse.
     * Dies testet die Koordination beider Semaphoren zusammen.
     *
     * ERWARTETES VERHALTEN:
     * - Alle Autos kommen erfolgreich durch
     * - Beide Engpaesse werden korrekt kontrolliert
     * - Kein Deadlock entsteht
     *
     * KONZEPT: Beide Semaphoren arbeiten zusammen ohne Deadlock
     */
    @Test
    @DisplayName("Test 3: Komplette Boxengassen-Durchfahrt (Integration)")
    void testKompletteBoxengassenDurchfahrt() throws InterruptedException {
        final int ANZAHL_AUTOS = 4;

        AtomicInteger erfolgreichDurchgefahren = new AtomicInteger(0);

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch fertigLatch = new CountDownLatch(ANZAHL_AUTOS);

        for (int i = 0; i < ANZAHL_AUTOS; i++) {
            final Auto auto = testAutos.get(i);

            Thread thread = new Thread(() -> {
                try {
                    startLatch.await();


                    controller.einfahrtAnfordern(auto);
                    Thread.sleep(30);
                    controller.einfahrtAbschliessen(auto);


                    controller.boxengassenDurchfahrt(50);


                    controller.ausfahrtAnfordern(auto);
                    Thread.sleep(30);
                    controller.ausfahrtAbschliessen(auto);

                    erfolgreichDurchgefahren.incrementAndGet();

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    fertigLatch.countDown();
                }
            }, "AutoThread-" + i);

            thread.start();
        }

        startLatch.countDown();
        boolean fertig = fertigLatch.await(10, TimeUnit.SECONDS);


        assertTrue(fertig,
                "Alle Threads sollten innerhalb von 10 Sekunden fertig werden. " +
                        "Wenn nicht, koennte ein Deadlock vorliegen.");
        assertEquals(ANZAHL_AUTOS, erfolgreichDurchgefahren.get(),
                "Alle " + ANZAHL_AUTOS + " Autos sollten erfolgreich durch die Boxengasse fahren. " +
                        "Dies zeigt, dass beide Semaphoren korrekt zusammenarbeiten.");
    }
}