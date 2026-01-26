package util;

/**
 * Zentrale Konfigurationsklasse fuer alle Simulationsparameter.
 * Diese Klasse enthaelt alle zeitlichen und strategischen Einstellungen,
 * die das Verhalten der Simulation bestimmen.
 *
 * Die Zeitangaben sind fuer 1x Geschwindigkeit definiert und werden
 * zur Laufzeit durch den Geschwindigkeitsfaktor geteilt.
 *
 * Alle Werte sind als Konstanten definiert, um eine einfache
 * Anpassung und Konsistenz im gesamten Projekt zu gewaehrleisten.
 *
 * @author F1 Simulation Team
 * @version 1.0
 */
public final class Konfiguration {

    // ========== ZEITPARAMETER (in Millisekunden, fuer 1x Geschwindigkeit) ==========

    /**
     * Basis-Rundenzeit in Millisekunden.
     * Bei 30 Runden und max. 10 Minuten ergibt sich: 600000ms / 30 = 20000ms pro Runde.
     */
    public static final long BASIS_RUNDENZEIT_MS = 20000;

    /**
     * Durchschnittliche Zeit pro Streckenabschnitt in Millisekunden.
     * Bei 15 Hauptabschnitten: 20000ms / 15 ≈ 1333ms.
     */
    public static final long BASIS_ABSCHNITT_ZEIT_MS = 1300;

    /**
     * Minimale Pitstop-Dauer (Reifenwechsel) in Millisekunden.
     */
    public static final long MIN_PITSTOP_DAUER_MS = 2000;

    /**
     * Maximale Pitstop-Dauer (Reifenwechsel) in Millisekunden.
     */
    public static final long MAX_PITSTOP_DAUER_MS = 4000;

    /**
     * Zeit fuer die Durchfahrt durch die Boxengasse in Millisekunden.
     */
    public static final long BOXENGASSEN_DURCHFAHRT_MS = 3000;

    /**
     * Intervall fuer GUI-Updates in Millisekunden.
     */
    public static final long GUI_UPDATE_INTERVALL_MS = 100;

    /**
     * Intervall fuer Strategie-Ueberpruefungen durch den RennstallThread.
     */
    public static final long STRATEGIE_CHECK_INTERVALL_MS = 1000;

    /**
     * Timeout fuer Lock-Anfragen um potenzielle Deadlocks zu erkennen.
     */
    public static final long LOCK_TIMEOUT_MS = 5000;


    // ========== STRECKENPARAMETER ==========

    /**
     * Anzahl der Hauptstrecken-Abschnitte (ohne Pitstop-Bereich).
     */
    public static final int ANZAHL_HAUPTSTRECKEN_ABSCHNITTE = 15;

    /**
     * ID des Abschnitts, nach dem die Pitstop-Einfahrt abzweigt.
     */
    public static final int PITSTOP_EINFAHRT_NACH_ABSCHNITT = 13;

    /**
     * ID des Abschnitts, vor dem die Pitstop-Ausfahrt einmuendet.
     */
    public static final int PITSTOP_AUSFAHRT_VOR_ABSCHNITT = 2;

    /**
     * ID des Pitstop-Einfahrt-Abschnitts.
     */
    public static final int PITSTOP_EINFAHRT_ID = 15;

    /**
     * ID des Boxengassen-Abschnitts.
     */
    public static final int BOXENGASSE_ID = 16;

    /**
     * ID des Pitstop-Ausfahrt-Abschnitts.
     */
    public static final int PITSTOP_AUSFAHRT_ID = 17;


    // ========== RENNPARAMETER ==========

    /**
     * Standard-Rundenanzahl.
     */
    public static final int DEFAULT_RUNDENANZAHL = 30;

    /**
     * Minimale Rundenanzahl.
     */
    public static final int MIN_RUNDENANZAHL = 20;

    /**
     * Maximale Rundenanzahl.
     */
    public static final int MAX_RUNDENANZAHL = 50;

    /**
     * Anzahl der Teams.
     */
    public static final int ANZAHL_TEAMS = 10;

    /**
     * Anzahl der Autos (2 pro Team).
     */
    public static final int ANZAHL_AUTOS = 20;


    // ========== PITSTOP-STRATEGIE ==========

    /**
     * Frueheste Runde fuer den Pflicht-Pitstop.
     */
    public static final int PFLICHT_PITSTOP_FRUEHESTENS_RUNDE = 8;

    /**
     * Spaeteste Runde fuer den Pflicht-Pitstop (relativ zum Ende).
     * Beispiel: Bei 30 Runden ist die spaeteste Runde 30 - 5 = 25.
     */
    public static final int PFLICHT_PITSTOP_RUNDEN_VOR_ENDE = 5;

    /**
     * Schwellenwert fuer Reifenabnutzung, ab dem ein Pitstop empfohlen wird.
     */
    public static final double REIFEN_KRITISCH_PROZENT = 80.0;

    /**
     * Grenzwert verbleibende Runden fuer Hard-Reifen Wahl.
     */
    public static final int REIFEN_HARD_AB_RUNDEN = 15;

    /**
     * Grenzwert verbleibende Runden fuer Medium-Reifen Wahl.
     */
    public static final int REIFEN_MEDIUM_AB_RUNDEN = 8;


    // ========== UEBERHOLPARAMETER ==========

    /**
     * Maximaler Zeitabstand fuer einen Ueberholversuch in Millisekunden.
     * Nur wenn der Abstand kleiner als dieser Wert ist, wird ueberholt.
     */
    public static final long MAX_ABSTAND_FUER_UEBERHOLUNG_MS = 1000;

    /**
     * Fortschritts-Bonus bei erfolgreicher Ueberholung.
     * Der Ueberholer erhaelt diesen Bonus auf seinen Fortschritt.
     */
    public static final double UEBERHOLUNG_FORTSCHRITT_BONUS = 0.05;


    // ========== GESCHWINDIGKEITSFAKTOREN ==========

    /**
     * Verfuegbare Simulationsgeschwindigkeiten.
     */
    public static final double[] VERFUEGBARE_GESCHWINDIGKEITEN = {1.0, 2.0, 5.0, 10.0};

    /**
     * Standard-Simulationsgeschwindigkeit.
     */
    public static final double DEFAULT_GESCHWINDIGKEIT = 1.0;


    // ========== HILFSMETHODEN ==========

    /**
     * Berechnet die skalierte Zeit basierend auf der Simulationsgeschwindigkeit.
     *
     * @Vorbedingung basisZeitMs muss >= 0 sein
     * @Vorbedingung geschwindigkeit muss > 0 sein
     * @Nachbedingung Rueckgabewert ist basisZeitMs / geschwindigkeit
     *
     * @param basisZeitMs Die Basiszeit in Millisekunden
     * @param geschwindigkeit Der Geschwindigkeitsfaktor (z.B. 2.0 fuer doppelte Geschwindigkeit)
     * @return Die skalierte Zeit in Millisekunden
     */
    public static long skaliereZeit(long basisZeitMs, double geschwindigkeit) {
        if (geschwindigkeit <= 0) {
            throw new IllegalArgumentException("Geschwindigkeit muss > 0 sein");
        }
        return Math.max(1, (long) (basisZeitMs / geschwindigkeit));
    }

    /**
     * Berechnet die spaeteste Runde fuer den Pflicht-Pitstop.
     *
     * @Vorbedingung gesamtRunden muss > PFLICHT_PITSTOP_RUNDEN_VOR_ENDE sein
     * @Nachbedingung Rueckgabewert ist gesamtRunden - PFLICHT_PITSTOP_RUNDEN_VOR_ENDE
     *
     * @param gesamtRunden Die Gesamtanzahl der Runden im Rennen
     * @return Die letzte Runde, in der ein Pflicht-Pitstop moeglich ist
     */
    public static int berechneSpaetestesPitstopRunde(int gesamtRunden) {
        return gesamtRunden - PFLICHT_PITSTOP_RUNDEN_VOR_ENDE;
    }

    /**
     * Prueft ob eine Runde im gueltigen Pitstop-Fenster liegt.
     *
     * @Vorbedingung aktuelleRunde muss > 0 sein
     * @Vorbedingung gesamtRunden muss > 0 sein
     * @Nachbedingung Rueckgabewert ist true wenn Runde im Fenster
     *
     * @param aktuelleRunde Die aktuelle Runde
     * @param gesamtRunden Die Gesamtanzahl der Runden
     * @return true wenn ein Pitstop in dieser Runde erlaubt ist
     */
    public static boolean istImPitstopFenster(int aktuelleRunde, int gesamtRunden) {
        return aktuelleRunde >= PFLICHT_PITSTOP_FRUEHESTENS_RUNDE
                && aktuelleRunde <= berechneSpaetestesPitstopRunde(gesamtRunden);
    }

    /**
     * Berechnet eine zufaellige Pitstop-Dauer zwischen MIN und MAX.
     *
     * @Vorbedingung Keine
     * @Nachbedingung Rueckgabewert liegt zwischen MIN_PITSTOP_DAUER_MS und MAX_PITSTOP_DAUER_MS
     *
     * @return Zufaellige Pitstop-Dauer in Millisekunden
     */
    public static long berechneZufaelligePitstopDauer() {
        return MIN_PITSTOP_DAUER_MS +
                (long) (Math.random() * (MAX_PITSTOP_DAUER_MS - MIN_PITSTOP_DAUER_MS));
    }

    // Privater Konstruktor verhindert Instanziierung
    private Konfiguration() {
        // Utility-Klasse
    }
}
