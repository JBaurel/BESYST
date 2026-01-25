package util;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Zentrale Logging-Klasse fuer die Rennsimulation.
 * Diese Klasse stellt thread-sichere Logging-Methoden bereit,
 * die sowohl auf der Konsole als auch in einer Datei ausgeben koennen.
 *
 * Das Logging ist in verschiedene Level unterteilt (DEBUG, INFO, WARNING, ERROR)
 * und kann zur Laufzeit aktiviert oder deaktiviert werden.
 *
 * Die Klasse verwendet eine BlockingQueue fuer thread-sicheres Logging
 * aus mehreren Threads ohne Synchronisationsprobleme.
 *
 * @author F1 Simulation Team
 * @version 1.0
 */
public final class RennLogger {

    /**
     * Aufzaehlung der verfuegbaren Log-Level.
     */
    public enum LogLevel {
        DEBUG(0, "DEBUG"),
        INFO(1, "INFO"),
        WARNING(2, "WARNING"),
        ERROR(3, "ERROR");

        private final int priority;
        private final String bezeichnung;

        LogLevel(int priority, String bezeichnung) {
            this.priority = priority;
            this.bezeichnung = bezeichnung;
        }

        public int getPriority() {
            return priority;
        }

        public String getBezeichnung() {
            return bezeichnung;
        }
    }

    private static final DateTimeFormatter ZEITFORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private static final BlockingQueue<String> logQueue = new LinkedBlockingQueue<>();

    private static LogLevel aktuellesLevel = LogLevel.INFO;
    private static boolean konsolenAusgabe = true;
    private static boolean dateiAusgabe = false;
    private static String logDateiPfad = "rennsimulation.log";
    private static PrintWriter dateiWriter = null;
    private static boolean debugAktiv = false;

    // Privater Konstruktor verhindert Instanziierung
    private RennLogger() {
        // Utility-Klasse
    }

    /**
     * Initialisiert das File-Logging mit dem angegebenen Dateipfad.
     *
     * @Vorbedingung pfad darf nicht null oder leer sein
     * @Nachbedingung Datei-Logging ist aktiviert zum angegebenen Pfad
     *
     * @param pfad Pfad zur Log-Datei
     * @throws IllegalArgumentException wenn pfad null oder leer ist
     */
    public static void initDateiLogging(String pfad) {
        if (pfad == null || pfad.trim().isEmpty()) {
            throw new IllegalArgumentException("Dateipfad darf nicht leer sein");
        }

        try {
            logDateiPfad = pfad;
            dateiWriter = new PrintWriter(new FileWriter(pfad, true), true);
            dateiAusgabe = true;
            info("Datei-Logging initialisiert: " + pfad);
        } catch (IOException e) {
            System.err.println("Fehler beim Initialisieren des Datei-Loggings: " + e.getMessage());
        }
    }

    /**
     * Beendet das Datei-Logging und schliesst die Datei.
     *
     * @Vorbedingung Keine
     * @Nachbedingung Datei-Logging ist deaktiviert und Datei geschlossen
     */
    public static void beendeDateiLogging() {
        if (dateiWriter != null) {
            dateiWriter.close();
            dateiWriter = null;
        }
        dateiAusgabe = false;
    }

    /**
     * Loggt eine DEBUG-Nachricht.
     * DEBUG-Nachrichten werden nur ausgegeben wenn Debug aktiviert ist.
     *
     * @Vorbedingung Keine
     * @Nachbedingung Nachricht ist geloggt wenn Debug aktiv und Level passt
     *
     * @param nachricht Die zu loggende Nachricht
     */
    public static void debug(String nachricht) {
        if (debugAktiv) {
            log(LogLevel.DEBUG, nachricht);
        }
    }

    /**
     * Loggt eine INFO-Nachricht.
     *
     * @Vorbedingung Keine
     * @Nachbedingung Nachricht ist geloggt wenn Level passt
     *
     * @param nachricht Die zu loggende Nachricht
     */
    public static void info(String nachricht) {
        log(LogLevel.INFO, nachricht);
    }

    /**
     * Loggt eine WARNING-Nachricht.
     *
     * @Vorbedingung Keine
     * @Nachbedingung Nachricht ist geloggt wenn Level passt
     *
     * @param nachricht Die zu loggende Nachricht
     */
    public static void warning(String nachricht) {
        log(LogLevel.WARNING, nachricht);
    }

    /**
     * Loggt eine ERROR-Nachricht.
     *
     * @Vorbedingung Keine
     * @Nachbedingung Nachricht ist geloggt
     *
     * @param nachricht Die zu loggende Nachricht
     */
    public static void error(String nachricht) {
        log(LogLevel.ERROR, nachricht);
    }

    /**
     * Loggt eine ERROR-Nachricht mit Exception.
     *
     * @Vorbedingung Keine
     * @Nachbedingung Nachricht und Stacktrace sind geloggt
     *
     * @param nachricht Die zu loggende Nachricht
     * @param exception Die aufgetretene Exception
     */
    public static void error(String nachricht, Throwable exception) {
        log(LogLevel.ERROR, nachricht + " - " + exception.getMessage());
        if (debugAktiv && exception != null) {
            exception.printStackTrace();
        }
    }

    /**
     * Loggt eine Thread-bezogene Nachricht mit Thread-Name.
     * Nuetzlich fuer das Debugging von Nebenlaeufigkeitsproblemen.
     *
     * @Vorbedingung Keine
     * @Nachbedingung Nachricht mit Thread-Info ist geloggt
     *
     * @param level Das Log-Level
     * @param nachricht Die zu loggende Nachricht
     */
    public static void logThread(LogLevel level, String nachricht) {
        String threadInfo = "[" + Thread.currentThread().getName() + "] ";
        log(level, threadInfo + nachricht);
    }

    /**
     * Loggt eine Synchronisations-Ereignis-Nachricht.
     * Speziell fuer Lock-Acquire, Lock-Release und Wait-Ereignisse.
     *
     * @Vorbedingung Keine
     * @Nachbedingung Synchronisations-Nachricht ist geloggt
     *
     * @param ereignis Beschreibung des Ereignisses (z.B. "LOCK_ACQUIRED")
     * @param ressource Name der betroffenen Ressource
     */
    public static void logSync(String ereignis, String ressource) {
        if (debugAktiv) {
            String threadName = Thread.currentThread().getName();
            debug(String.format("[SYNC] %s: %s auf %s", threadName, ereignis, ressource));
        }
    }

    /**
     * Interne Log-Methode die alle Ausgaben durchfuehrt.
     *
     * @Vorbedingung level und nachricht duerfen nicht null sein
     * @Nachbedingung Nachricht ist auf aktiven Kanaelen ausgegeben
     *
     * @param level Das Log-Level
     * @param nachricht Die zu loggende Nachricht
     */
    private static void log(LogLevel level, String nachricht) {
        if (level == null || nachricht == null) {
            return;
        }

        if (level.getPriority() < aktuellesLevel.getPriority()) {
            return;
        }

        String zeitstempel = LocalDateTime.now().format(ZEITFORMAT);
        String formatierteNachricht = String.format("[%s] [%s] %s",
                zeitstempel, level.getBezeichnung(), nachricht);

        if (konsolenAusgabe) {
            if (level == LogLevel.ERROR) {
                System.err.println(formatierteNachricht);
            } else {
                System.out.println(formatierteNachricht);
            }
        }

        if (dateiAusgabe && dateiWriter != null) {
            dateiWriter.println(formatierteNachricht);
        }
    }

    /**
     * Setzt das minimale Log-Level.
     * Nachrichten unter diesem Level werden nicht ausgegeben.
     *
     * @Vorbedingung level darf nicht null sein
     * @Nachbedingung aktuellesLevel ist gesetzt
     *
     * @param level Das neue minimale Log-Level
     */
    public static void setLogLevel(LogLevel level) {
        if (level != null) {
            aktuellesLevel = level;
        }
    }

    /**
     * Aktiviert oder deaktiviert die Konsolenausgabe.
     *
     * @Vorbedingung Keine
     * @Nachbedingung konsolenAusgabe ist entsprechend gesetzt
     *
     * @param aktiv true zum Aktivieren, false zum Deaktivieren
     */
    public static void setKonsolenAusgabe(boolean aktiv) {
        konsolenAusgabe = aktiv;
    }

    /**
     * Aktiviert oder deaktiviert den Debug-Modus.
     * Im Debug-Modus werden auch DEBUG-Nachrichten ausgegeben.
     *
     * @Vorbedingung Keine
     * @Nachbedingung debugAktiv ist entsprechend gesetzt
     *
     * @param aktiv true zum Aktivieren, false zum Deaktivieren
     */
    public static void setDebugAktiv(boolean aktiv) {
        debugAktiv = aktiv;
        if (aktiv) {
            info("Debug-Modus aktiviert");
        }
    }

    /**
     * Prueft ob der Debug-Modus aktiv ist.
     *
     * @Vorbedingung Keine
     * @Nachbedingung Rueckgabewert entspricht debugAktiv
     *
     * @return true wenn Debug-Modus aktiv
     */
    public static boolean isDebugAktiv() {
        return debugAktiv;
    }

    /**
     * Gibt das aktuelle Log-Level zurueck.
     *
     * @Vorbedingung Keine
     * @Nachbedingung Rueckgabewert ist das aktuelle Level
     *
     * @return Das aktuelle Log-Level
     */
    public static LogLevel getLogLevel() {
        return aktuellesLevel;
    }
}
