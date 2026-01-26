package synchronisation;

import model.Auto;
import model.Fahrer;
import model.Streckenabschnitt;
import model.Team;
import model.enumType.StreckenabschnittTyp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
/**
 * Testklasse fuer den SchikaneZugriff.
 *
 * Diese Tests pruefen die Semaphore-basierte Zugriffskontrolle fuer Schikanen.
 * Eine Semaphore ist ein Zaehler, der die Anzahl der gleichzeitigen Zugriffe begrenzt.
 * Im Gegensatz zum Monitor (Kapazitaet 1) erlaubt eine Semaphore mehrere gleichzeitige Zugriffe.
 *
 * - Semaphore(N): Erlaubt maximal N gleichzeitige Zugriffe
 * - acquire(): Dekrementiert den Zaehler, blockiert wenn Zaehler = 0
 * - release(): Inkrementiert den Zaehler, weckt wartende Threads
 * - Fair-Parameter: Garantiert FIFO-Reihenfolge bei wartenden Threads
 *
 * UNTERSCHIED ZUM MONITOR:
 * - Monitor: Exklusiver Zugriff (nur 1 Thread)
 * - Semaphore: Kontrollierter Mehrfachzugriff (bis zu N Threads)
 *
 * @author F1 Simulation Team
 * @version 1.0
 */

@DisplayName("SchikaneZugriff Tests (Semaphore)")
class SchikaneZugriffTest {
    private SchikaneZugriff schikaneZugriff;
    private Streckenabschnitt schikane;
    private List<Auto> testAutos;

    /**
     * Vor jedem Test wird ein frischer SchikaneZugriff erstellt.
     * Die Schikane hat Kapazitaet 2 (zwei Autos gleichzeitig erlaubt).
     */
    @BeforeEach
    void setUp() {
        // Schikane mit Kapazitaet 2 erstellen
        schikane = new Streckenabschnitt(
                7, "Schumacher-S", StreckenabschnittTyp.SCHIKANE,
                45, 0.40, 0.50, 0.50, 2  // Kapazitaet = 2
        );

        schikaneZugriff = new SchikaneZugriff(schikane);

        // Test-Autos erstellen
        testAutos = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Fahrer f1 = new Fahrer("Fahrer " + i + "a", "F" + i + "A", 80);
            Fahrer f2 = new Fahrer("Fahrer " + i + "b", "F" + i + "B", 75);
            Team team = new Team("Team " + i, javafx.scene.paint.Color.BLUE, f1, f2);
            testAutos.add(new Auto(10 + i, team, f1));
        }
    }

    /**
     * TEST 1: Kapazitaet 2 - Genau zwei Autos gleichzeitig erlaubt
     *
     * SZENARIO:
     * Drei Threads versuchen gleichzeitig in die Schikane einzufahren.
     * Da die Kapazitaet 2 ist, duerfen maximal 2 Autos gleichzeitig drin sein.
     *
     * ERWARTETES VERHALTEN:
     * - Die ersten beiden Threads fahren sofort ein (acquire() erfolgreich)
     * - Der dritte Thread muss warten (blockiert in acquire())
     * - Erst wenn einer ausfahrt (release()), kann der dritte einfahren
     *
     * KONZEPT: Semaphore(2) erlaubt 2 gleichzeitige acquire()-Aufrufe
     */
    @Test
    @DisplayName("Test 1: Maximal 2 Autos gleichzeitig in der Schikane (Semaphore-Kapazitaet)")
    void testMaximalZweiAutosGleichzeitig() throws InterruptedException {
        final int ANZAHL_THREADS = 3;

        AtomicInteger gleichzeitigInSchikane = new AtomicInteger(0);
        AtomicInteger maxGleichzeitig = new AtomicInteger(0);

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch fertigLatch = new CountDownLatch(ANZAHL_THREADS);

        // Drei Threads erstellen
        for (int i = 0; i < ANZAHL_THREADS; i++) {
            final Auto auto = testAutos.get(i);

            Thread thread = new Thread(() -> {
                try {
                    startLatch.await();

                    schikaneZugriff.einfahren(auto);

                    // Zaehler aktualisieren
                    int aktuell = gleichzeitigInSchikane.incrementAndGet();
                    maxGleichzeitig.updateAndGet(max -> Math.max(max, aktuell));

                    // In der Schikane bleiben
                    Thread.sleep(150);

                    gleichzeitigInSchikane.decrementAndGet();
                    schikaneZugriff.ausfahren(auto);

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    fertigLatch.countDown();
                }
            }, "AutoThread-" + i);

            thread.start();
        }

        // Alle Threads gleichzeitig starten
        startLatch.countDown();

        // Auf alle Threads warten
        boolean fertig = fertigLatch.await(5, TimeUnit.SECONDS);

        // ASSERTIONS
        assertTrue(fertig, "Alle Threads sollten fertig werden");
        assertEquals(2, maxGleichzeitig.get(),
                "Die Semaphore hat Kapazitaet 2, also duerfen maximal 2 Autos gleichzeitig " +
                        "in der Schikane sein. Der dritte Thread muss warten bis einer ausfahrt. " +
                        "Dies ist der Unterschied zum Monitor: Semaphore erlaubt kontrollierten Mehrfachzugriff.");
    }

    /**
     * TEST 2: Dritter Thread blockiert bis einer ausfahrt
     *
     * SZENARIO:
     * Zwei Autos sind in der Schikane. Ein drittes versucht einzufahren.
     * Es muss warten bis eines der ersten beiden ausfahrt.
     *
     * ERWARTETES VERHALTEN:
     * - Auto 1 und 2 fahren ein (Semaphore-Zaehler: 2 -> 1 -> 0)
     * - Auto 3 blockiert (Zaehler ist 0)
     * - Wenn Auto 1 ausfahrt (Zaehler: 0 -> 1), kann Auto 3 einfahren
     *
     * KONZEPT: acquire() blockiert wenn der Semaphore-Zaehler 0 ist
     */
    @Test
    @DisplayName("Test 2: Dritter Thread blockiert und wird durch release() freigegeben")
    void testDritterThreadBlockiert() throws InterruptedException {
        List<String> ereignisse = Collections.synchronizedList(new ArrayList<>());

        CountDownLatch beideEingefahren = new CountDownLatch(2);
        CountDownLatch fertigLatch = new CountDownLatch(3);

        Auto auto1 = testAutos.get(0);
        Auto auto2 = testAutos.get(1);
        Auto auto3 = testAutos.get(2);

        // Thread 1: Faehrt ein und bleibt lange
        Thread thread1 = new Thread(() -> {
            try {
                schikaneZugriff.einfahren(auto1);
                ereignisse.add("Auto1 eingefahren");
                beideEingefahren.countDown();

                Thread.sleep(300);  // Lange in der Schikane bleiben

                ereignisse.add("Auto1 faehrt aus");
                schikaneZugriff.ausfahren(auto1);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                fertigLatch.countDown();
            }
        }, "AutoThread-1");

        // Thread 2: Faehrt ein und bleibt lange
        Thread thread2 = new Thread(() -> {
            try {
                schikaneZugriff.einfahren(auto2);
                ereignisse.add("Auto2 eingefahren");
                beideEingefahren.countDown();

                Thread.sleep(500);  // Noch laenger bleiben

                ereignisse.add("Auto2 faehrt aus");
                schikaneZugriff.ausfahren(auto2);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                fertigLatch.countDown();
            }
        }, "AutoThread-2");

        // Thread 3: Wartet bis beide drin sind, dann versucht einzufahren
        Thread thread3 = new Thread(() -> {
            try {
                beideEingefahren.await();  // Warten bis beide drin sind
                Thread.sleep(50);

                ereignisse.add("Auto3 versucht einzufahren");
                schikaneZugriff.einfahren(auto3);  // Sollte blockieren!
                ereignisse.add("Auto3 eingefahren");

                schikaneZugriff.ausfahren(auto3);
                ereignisse.add("Auto3 ausgefahren");

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                fertigLatch.countDown();
            }
        }, "AutoThread-3");

        thread1.start();
        thread2.start();
        thread3.start();

        boolean fertig = fertigLatch.await(5, TimeUnit.SECONDS);

        // ASSERTIONS
        assertTrue(fertig, "Alle Threads sollten fertig werden");

        // Auto 3 darf erst einfahren NACHDEM Auto 1 oder Auto 2 ausgefahren ist
        int auto3EingefahrenIndex = ereignisse.indexOf("Auto3 eingefahren");
        int auto1AusgefahrenIndex = ereignisse.indexOf("Auto1 faehrt aus");
        int auto2AusgefahrenIndex = ereignisse.indexOf("Auto2 faehrt aus");

        // Mindestens eines der ersten Autos muss vor Auto 3 ausgefahren sein
        boolean korrekteReihenfolge =
                (auto1AusgefahrenIndex < auto3EingefahrenIndex) ||
                        (auto2AusgefahrenIndex < auto3EingefahrenIndex);

        assertTrue(korrekteReihenfolge,
                "Auto 3 darf erst einfahren NACHDEM Auto 1 oder Auto 2 ausgefahren ist. " +
                        "Dies zeigt, dass acquire() blockiert wenn der Semaphore-Zaehler 0 ist, " +
                        "und release() einen wartenden Thread aufweckt. Ereignisse: " + ereignisse);
    }

    /**
     * TEST 3: Timeout-Funktion (tryAcquire mit Timeout)
     *
     * SZENARIO:
     * Die Schikane ist voll (2 Autos). Ein drittes Auto versucht mit Timeout einzufahren.
     * Wenn das Timeout ablaeuft bevor ein Platz frei wird, kehrt es ohne Einfahrt zurueck.
     *
     * ERWARTETES VERHALTEN:
     * - einfahrenMitTimeout() gibt false zurueck wenn Timeout ablaeuft
     * - Das Auto wurde nicht in die Schikane aufgenommen
     *
     * KONZEPT: tryAcquire(timeout) ist die nicht-blockierende Alternative zu acquire()
     */
    @Test
    @DisplayName("Test 3: Timeout bei vollem Abschnitt (tryAcquire)")
    void testTimeoutBeiVollemAbschnitt() throws InterruptedException {
        Auto auto1 = testAutos.get(0);
        Auto auto2 = testAutos.get(1);
        Auto auto3 = testAutos.get(2);

        // Schikane fuellen
        schikaneZugriff.einfahren(auto1);
        schikaneZugriff.einfahren(auto2);

        // Jetzt ist die Schikane voll (Zaehler = 0)

        // Thread der die Schikane nach einer Weile freigibt
        Thread freigeberThread = new Thread(() -> {
            try {
                Thread.sleep(300);  // Warten
                schikaneZugriff.ausfahren(auto1);  // Erst dann freigeben
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        freigeberThread.start();

        // Auto 3 versucht mit kurzem Timeout einzufahren
        long startZeit = System.currentTimeMillis();
        boolean erfolgreich = schikaneZugriff.einfahrenMitTimeout(auto3, 100);
        long dauer = System.currentTimeMillis() - startZeit;

        // ASSERTIONS
        assertFalse(erfolgreich,
                "einfahrenMitTimeout() sollte false zurueckgeben, da das Timeout (100ms) " +
                        "ablaeuft bevor ein Platz frei wird (300ms). " +
                        "Dies ist nuetzlich um Deadlocks zu vermeiden.");

        assertTrue(dauer >= 90 && dauer < 200,
                "Die Methode sollte etwa 100ms warten bevor sie aufgibt. Tatsaechlich: " + dauer + "ms");

        // Aufraeumen
        freigeberThread.join();
        schikaneZugriff.ausfahren(auto2);
    }
}