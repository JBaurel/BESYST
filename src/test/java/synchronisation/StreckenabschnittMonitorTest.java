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
 * Testklasse fuer den StreckenabschnittMonitor.
 *
 * Diese Tests pruefen das klassische Monitor-Pattern mit synchronized, wait() und notifyAll().
 * Der Monitor stellt sicher, dass nur ein Auto gleichzeitig in einer engen Kurve ist
 * (Kapazitaet = 1) und dass wartende Autos in FIFO-Reihenfolge bedient werden.
 *
 * - synchronized: Nur ein Thread kann gleichzeitig den kritischen Bereich betreten
 * - wait(): Thread gibt den Lock frei und wartet auf Benachrichtigung
 * - notifyAll(): Weckt alle wartenden Threads auf, die dann um den Lock konkurrieren
 *
 * @author F1 Simulation Team
 * @version 1.0
 */
@DisplayName("StreckenabschnittMonitor Tests")

class StreckenabschnittMonitorTest {
    private StreckenabschnittMonitor monitor;
    private Streckenabschnitt engeKurve;
    private Team testTeam;
    private Fahrer fahrer1;
    private Fahrer fahrer2;
    private Fahrer fahrer3;
    private Auto auto1;
    private Auto auto2;
    private Auto auto3;

    /**
     * Vor jedem Test wird ein frischer Monitor und Test-Autos erstellt.
     * Die enge Kurve hat Kapazitaet 1 (nur ein Auto gleichzeitig).
     */
    @BeforeEach
    void setUp() {
        // Enge Kurve mit Kapazitaet 1 erstellen
        engeKurve = new Streckenabschnitt(
                2, "Yokohama-S", StreckenabschnittTyp.ENGE_KURVE,
                15, 0.25, 0.20, 0.35, 1  // Kapazitaet = 1
        );

        monitor = new StreckenabschnittMonitor(engeKurve);

        // Test-Team und Fahrer erstellen
        fahrer1 = new Fahrer("Test Fahrer 1", "TF1", 90);
        fahrer2 = new Fahrer("Test Fahrer 2", "TF2", 85);
        fahrer3 = new Fahrer("Test Fahrer 3", "TF3", 80);
        testTeam = new Team("Test Team", javafx.scene.paint.Color.RED, fahrer1, fahrer2);

        // Test-Autos erstellen
        auto1 = new Auto(1, testTeam, fahrer1);
        auto2 = new Auto(2, testTeam, fahrer2);

        // Drittes Auto benoetigt ein zweites Team
        Fahrer fahrer3a = new Fahrer("Test Fahrer 3", "TF3", 80);
        Fahrer fahrer3b = new Fahrer("Test Fahrer 3b", "T3B", 75);
        Team testTeam2 = new Team("Test Team 2", javafx.scene.paint.Color.BLUE, fahrer3a, fahrer3b);
        auto3 = new Auto(3, testTeam2, fahrer3a);
    }

    /**
     * TEST 1: Exklusiver Zugriff - Nur ein Auto gleichzeitig im kritischen Bereich
     *
     * SZENARIO:
     * Zwei Threads versuchen gleichzeitig, in die enge Kurve einzufahren.
     * Da die Kapazitaet 1 ist, darf zu jedem Zeitpunkt nur ein Auto drin sein.
     *
     * ERWARTETES VERHALTEN:
     * - Der erste Thread faehrt sofort ein
     * - Der zweite Thread muss warten (blockiert in wait())
     * - Erst wenn der erste Thread ausfahrt (notifyAll()), kann der zweite einfahren
     *
     * KONZEPT: Das ist der Kern des Monitor-Patterns - gegenseitiger Ausschluss (Mutual Exclusion)
     */
    @Test
    @DisplayName("Test 1: Nur ein Auto gleichzeitig in der Kurve (Mutual Exclusion)")
    void testNurEinAutoGleichzeitigInKurve() throws InterruptedException {
        // Zaehler fuer die maximale Anzahl gleichzeitiger Autos
        AtomicInteger gleichzeitigInKurve = new AtomicInteger(0);
        AtomicInteger maxGleichzeitig = new AtomicInteger(0);

        // Latch damit beide Threads gleichzeitig starten
        CountDownLatch startLatch = new CountDownLatch(1);
        // Latch um auf beide Threads zu warten
        CountDownLatch fertigLatch = new CountDownLatch(2);

        // Thread 1: Auto 1 faehrt in die Kurve
        Thread thread1 = new Thread(() -> {
            try {
                startLatch.await();  // Warten auf Startsignal

                monitor.einfahren(auto1);

                // Zaehler erhoehen und Maximum aktualisieren
                int aktuell = gleichzeitigInKurve.incrementAndGet();
                maxGleichzeitig.updateAndGet(max -> Math.max(max, aktuell));

                // Kurze Zeit in der Kurve bleiben
                Thread.sleep(100);

                gleichzeitigInKurve.decrementAndGet();
                monitor.ausfahren(auto1);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                fertigLatch.countDown();
            }
        }, "AutoThread-1");

        // Thread 2: Auto 2 versucht auch einzufahren
        Thread thread2 = new Thread(() -> {
            try {
                startLatch.await();  // Warten auf Startsignal

                monitor.einfahren(auto2);

                // Zaehler erhoehen und Maximum aktualisieren
                int aktuell = gleichzeitigInKurve.incrementAndGet();
                maxGleichzeitig.updateAndGet(max -> Math.max(max, aktuell));

                // Kurze Zeit in der Kurve bleiben
                Thread.sleep(100);

                gleichzeitigInKurve.decrementAndGet();
                monitor.ausfahren(auto2);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                fertigLatch.countDown();
            }
        }, "AutoThread-2");

        // Threads starten
        thread1.start();
        thread2.start();

        // Beide Threads gleichzeitig loslassen
        startLatch.countDown();

        // Auf beide Threads warten (max 5 Sekunden)
        boolean fertig = fertigLatch.await(5, TimeUnit.SECONDS);

        // ASSERTIONS
        assertTrue(fertig, "Beide Threads sollten innerhalb von 5 Sekunden fertig sein");
        assertEquals(1, maxGleichzeitig.get(),
                "Es darf nie mehr als 1 Auto gleichzeitig in der Kurve sein! " +
                        "Das ist der Kern des Monitor-Patterns: Mutual Exclusion.");
    }

    /**
     * TEST 2: Wait und NotifyAll - Wartender Thread wird korrekt aufgeweckt
     *
     * SZENARIO:
     * Auto 1 ist in der Kurve. Auto 2 versucht einzufahren und muss warten.
     * Wenn Auto 1 ausfahrt, wird notifyAll() aufgerufen und Auto 2 wird geweckt.
     *
     * ERWARTETES VERHALTEN:
     * - Auto 2 blockiert in wait() waehrend Auto 1 in der Kurve ist
     * - Nach ausfahren() von Auto 1 wird Auto 2 aufgeweckt
     * - Auto 2 kann dann einfahren
     *
     * KONZEPT: wait() gibt den Lock frei, notifyAll() weckt wartende Threads auf
     */
    @Test
    @DisplayName("Test 2: Wartender Thread wird durch notifyAll() aufgeweckt")
    void testWaitUndNotifyAll() throws InterruptedException {
        // Liste zum Protokollieren der Reihenfolge
        List<String> ereignisse = Collections.synchronizedList(new ArrayList<>());

        CountDownLatch auto1Drin = new CountDownLatch(1);
        CountDownLatch fertigLatch = new CountDownLatch(2);

        // Thread 1: Auto 1 faehrt ein und bleibt eine Weile
        Thread thread1 = new Thread(() -> {
            try {
                monitor.einfahren(auto1);
                ereignisse.add("Auto1 eingefahren");
                auto1Drin.countDown();  // Signal dass Auto 1 drin ist

                // Warten damit Auto 2 Zeit hat zu versuchen einzufahren
                Thread.sleep(200);

                ereignisse.add("Auto1 faehrt aus");
                monitor.ausfahren(auto1);
                ereignisse.add("Auto1 ausgefahren");

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                fertigLatch.countDown();
            }
        }, "AutoThread-1");

        // Thread 2: Auto 2 wartet bis Auto 1 drin ist, dann versucht es einzufahren
        Thread thread2 = new Thread(() -> {
            try {
                // Warten bis Auto 1 sicher drin ist
                auto1Drin.await();
                Thread.sleep(50);  // Kleine Verzoegerung

                ereignisse.add("Auto2 versucht einzufahren");
                monitor.einfahren(auto2);  // Hier sollte wait() aufgerufen werden
                ereignisse.add("Auto2 eingefahren");

                monitor.ausfahren(auto2);
                ereignisse.add("Auto2 ausgefahren");

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                fertigLatch.countDown();
            }
        }, "AutoThread-2");

        thread1.start();
        thread2.start();

        boolean fertig = fertigLatch.await(5, TimeUnit.SECONDS);

        // ASSERTIONS
        assertTrue(fertig, "Beide Threads sollten fertig werden");

        // Die Reihenfolge pruefen: Auto2 darf erst einfahren NACHDEM Auto1 ausgefahren ist
        int auto1AusgefahrenIndex = ereignisse.indexOf("Auto1 ausgefahren");
        int auto2EingefahrenIndex = ereignisse.indexOf("Auto2 eingefahren");

        assertTrue(auto1AusgefahrenIndex < auto2EingefahrenIndex,
                "Auto 2 darf erst einfahren NACHDEM Auto 1 ausgefahren ist. " +
                        "Dies zeigt, dass wait() den Thread blockiert hat und notifyAll() ihn aufgeweckt hat. " +
                        "Ereignisse: " + ereignisse);
    }

    /**
     * TEST 3: Nebenläufigkeitstest mit 5 Threads
     *
     * SZENARIO:
     * 5 Threads versuchen gleichzeitig durch die enge Kurve zu fahren.
     * Jeder Thread faehrt mehrmals durch (3 Durchgaenge).
     *
     * ERWARTETES VERHALTEN:
     * - Zu keinem Zeitpunkt mehr als 1 Auto in der Kurve
     * - Alle Threads werden irgendwann bedient (kein Starvation)
     * - Kein Deadlock (alle Threads werden fertig)
     *
     * KONZEPT: Stress-Test des Monitor-Patterns unter hoher Last
     */
    @Test
    @DisplayName("Test 3: Nebenläufigkeitstest mit 5 Threads (Stress-Test)")
    void testNebenlaeufigkeitMitVielenThreads() throws InterruptedException {
        final int ANZAHL_THREADS = 5;
        final int DURCHGAENGE_PRO_THREAD = 3;

        AtomicInteger gleichzeitigInKurve = new AtomicInteger(0);
        AtomicInteger maxGleichzeitig = new AtomicInteger(0);
        AtomicInteger erfolgreicheDurchgaenge = new AtomicInteger(0);

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch fertigLatch = new CountDownLatch(ANZAHL_THREADS);

        // Mehrere Test-Autos erstellen
        List<Auto> testAutos = new ArrayList<>();
        for (int i = 0; i < ANZAHL_THREADS; i++) {
            Fahrer f1 = new Fahrer("Fahrer " + i + "a", "F" + i + "A", 80);
            Fahrer f2 = new Fahrer("Fahrer " + i + "b", "F" + i + "B", 75);
            Team team = new Team("Team " + i, javafx.scene.paint.Color.BLUE, f1, f2);
            testAutos.add(new Auto(10 + i, team, f1));
        }

        // Threads erstellen und starten
        for (int i = 0; i < ANZAHL_THREADS; i++) {
            final Auto auto = testAutos.get(i);

            Thread thread = new Thread(() -> {
                try {
                    startLatch.await();

                    for (int d = 0; d < DURCHGAENGE_PRO_THREAD; d++) {
                        monitor.einfahren(auto);

                        // Zaehler aktualisieren
                        int aktuell = gleichzeitigInKurve.incrementAndGet();
                        maxGleichzeitig.updateAndGet(max -> Math.max(max, aktuell));

                        // Kurze Zeit in der Kurve
                        Thread.sleep(10 + (int)(Math.random() * 20));

                        gleichzeitigInKurve.decrementAndGet();
                        monitor.ausfahren(auto);

                        erfolgreicheDurchgaenge.incrementAndGet();
                    }
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
        boolean fertig = fertigLatch.await(30, TimeUnit.SECONDS);

        // ASSERTIONS
        assertTrue(fertig,
                "Alle Threads sollten innerhalb von 30 Sekunden fertig werden. " +
                        "Wenn nicht, liegt moeglicherweise ein Deadlock oder Starvation vor.");

        assertEquals(1, maxGleichzeitig.get(),
                "Auch unter hoher Last darf nie mehr als 1 Auto gleichzeitig in der Kurve sein!");

        assertEquals(ANZAHL_THREADS * DURCHGAENGE_PRO_THREAD, erfolgreicheDurchgaenge.get(),
                "Alle Durchgaenge muessen erfolgreich sein. " +
                        "Dies zeigt, dass kein Thread verhungert (Starvation).");
    }
}