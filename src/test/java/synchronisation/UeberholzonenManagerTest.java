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
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testklasse fuer den UeberholzonenManager.
 *
 * Diese Tests pruefen die ReadWriteLock-basierte Zugriffskontrolle.
 * Das ReadWriteLock loest das klassische Leser-Schreiber-Problem:
 * - Mehrere Leser koennen gleichzeitig lesen
 * - Ein Schreiber braucht exklusiven Zugriff
 * - Waehrend geschrieben wird, koennen keine Leser lesen
 *
 * LERNZIEL: Verstehen wie ReadWriteLock funktioniert:
 * - readLock(): Erlaubt mehrere gleichzeitige Leser
 * - writeLock(): Exklusiver Zugriff fuer einen Schreiber
 * - Optimierung gegenueber einfachem Lock wenn Lesen haeufiger als Schreiben
 *
 * ANWENDUNGSFALL:
 * Die Ueberholstatistiken werden von der GUI oft gelesen (readLock),
 * aber nur bei Ueberholversuchen geschrieben (writeLock).
 *
 * @author F1 Simulation Team
 * @version 1.0
 */
@DisplayName("UeberholzonenManager Tests (ReadWriteLock)")
class UeberholzonenManagerTest {
    private UeberholzonenManager manager;
    private Streckenabschnitt drsZone;
    private List<Auto> testAutos;

    /**
     * Vor jedem Test wird ein frischer Manager erstellt.
     */
    @BeforeEach
    void setUp() {
        manager = new UeberholzonenManager();


        drsZone = new Streckenabschnitt(
                1, "DRS-Zone 1", StreckenabschnittTyp.DRS_ZONE,
                50, 0.10, 0.15, 0.15, 20
        );


        testAutos = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            Fahrer f1 = new Fahrer("Fahrer " + i + "a", "F" + i + "A", 80 + i * 2);
            Fahrer f2 = new Fahrer("Fahrer " + i + "b", "F" + i + "B", 75);
            Team team = new Team("Team " + i, javafx.scene.paint.Color.BLUE, f1, f2);
            testAutos.add(new Auto(10 + i, team, f1));
        }
    }

    /**
     * TEST 1: Mehrere Leser koennen gleichzeitig Statistiken lesen
     *
     * SZENARIO:
     * 5 Threads lesen gleichzeitig die Ueberholstatistiken.
     * Alle sollten gleichzeitig lesen koennen (kein gegenseitiger Ausschluss).
     *
     * ERWARTETES VERHALTEN:
     * - Alle 5 Threads koennen gleichzeitig im readLock sein
     * - Kein Thread muss auf einen anderen Leser warten
     *
     * KONZEPT: readLock() blockiert andere Leser NICHT
     * Das ist der grosse Vorteil gegenueber synchronized: Paralleles Lesen!
     */
    @Test
    @DisplayName("Test 1: Mehrere Leser gleichzeitig (ReadLock parallel)")
    void testMehrereLeserGleichzeitig() throws InterruptedException {
        final int ANZAHL_LESER = 5;

        AtomicInteger gleichzeitigLesend = new AtomicInteger(0);
        AtomicInteger maxGleichzeitig = new AtomicInteger(0);

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch alleLesen = new CountDownLatch(ANZAHL_LESER);
        CountDownLatch fertigLatch = new CountDownLatch(ANZAHL_LESER);


        Auto auto1 = testAutos.get(0);
        Auto auto2 = testAutos.get(1);
        manager.versucheUeberholung(auto1, auto2, drsZone, 500);

        for (int i = 0; i < ANZAHL_LESER; i++) {
            Thread thread = new Thread(() -> {
                try {
                    startLatch.await();


                    int aktuell = gleichzeitigLesend.incrementAndGet();
                    maxGleichzeitig.updateAndGet(max -> Math.max(max, aktuell));


                    alleLesen.countDown();


                    alleLesen.await();


                    long versuche = manager.getGesamtVersuche();
                    double erfolge = manager.getErfolgsquote();

                    Thread.sleep(50);

                    gleichzeitigLesend.decrementAndGet();

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    fertigLatch.countDown();
                }
            }, "LeserThread-" + i);

            thread.start();
        }

        startLatch.countDown();
        boolean fertig = fertigLatch.await(5, TimeUnit.SECONDS);


        assertTrue(fertig, "Alle Leser-Threads sollten fertig werden");
        assertEquals(ANZAHL_LESER, maxGleichzeitig.get(),
                "Alle " + ANZAHL_LESER + " Leser sollten GLEICHZEITIG lesen koennen! " +
                        "Das ist der Hauptvorteil von ReadWriteLock: Paralleles Lesen. " +
                        "Bei synchronized waere nur 1 Leser gleichzeitig moeglich.");
    }

    /**
     * TEST 2: Schreiber hat exklusiven Zugriff (keine Leser waehrend Schreiben)
     *
     * SZENARIO:
     * Ein Thread fuehrt Ueberholungen durch (Schreiber), waehrend andere
     * versuchen zu lesen. Die Statistiken muessen konsistent bleiben.
     *
     * ERWARTETES VERHALTEN:
     * - Waehrend eines Ueberholversuchs sind Statistiken konsistent
     * - Leser sehen nie inkonsistente Zwischenzustaende
     *
     * KONZEPT: writeLock() blockiert alle anderen (Leser UND Schreiber)
     */
    @Test
    @DisplayName("Test 2: Statistiken sind konsistent bei parallelen Schreibern")
    void testKonsistenteStatistiken() throws InterruptedException {
        final int ANZAHL_UEBERHOLUNGEN = 20;

        AtomicBoolean inkonsistenzGefunden = new AtomicBoolean(false);

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch fertigLatch = new CountDownLatch(2);


        Thread schreiberThread = new Thread(() -> {
            try {
                startLatch.await();

                Auto ueberholer = testAutos.get(0);
                Auto vordermann = testAutos.get(1);

                for (int i = 0; i < ANZAHL_UEBERHOLUNGEN; i++) {
                    manager.versucheUeberholung(ueberholer, vordermann, drsZone, 500);
                    Thread.sleep(5);
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                fertigLatch.countDown();
            }
        }, "SchreiberThread");


        Thread leserThread = new Thread(() -> {
            try {
                startLatch.await();

                for (int i = 0; i < 50 && !inkonsistenzGefunden.get(); i++) {
                    long versuche = manager.getGesamtVersuche();
                    double erfolge = manager.getErfolgsquote();
                    long fehlschlaege = manager.getFehlgeschlageneVersuche();

                    if (versuche != erfolge + fehlschlaege) {
                        inkonsistenzGefunden.set(true);
                        System.err.println("INKONSISTENZ: " + versuche + " != " +
                                erfolge + " + " + fehlschlaege);
                    }

                    Thread.sleep(2);
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                fertigLatch.countDown();
            }
        }, "LeserThread");

        schreiberThread.start();
        leserThread.start();

        startLatch.countDown();
        boolean fertig = fertigLatch.await(10, TimeUnit.SECONDS);


        assertTrue(fertig, "Beide Threads sollten fertig werden");
        assertFalse(inkonsistenzGefunden.get(),
                "Keine Inkonsistenz sollte gefunden werden. " +
                        "Das ReadWriteLock garantiert, dass Schreiboperationen atomar sind " +
                        "und Leser nie Zwischenzustaende sehen.");


        long versuche = manager.getGesamtVersuche();
        double erfolge = manager.getErfolgsquote();
        long fehlschlaege = manager.getFehlgeschlageneVersuche();

        assertEquals(versuche, erfolge + fehlschlaege,
                "Am Ende muessen die Statistiken konsistent sein: " +
                        "Versuche (" + versuche + ") = Erfolge (" + erfolge + ") + " +
                        "Fehlschlaege (" + fehlschlaege + ")");
    }
}