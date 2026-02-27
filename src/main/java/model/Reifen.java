package model;

import model.enumType.ReifenTyp;

/**
 * Repraesentiert einen Reifensatz eines Rennfahrzeugs.
 * Die Reifen nutzen sich waehrend des Rennens ab und beeinflussen
 * die Geschwindigkeit des Fahrzeugs. Bei zu hoher Abnutzung
 * ist ein Reifenwechsel erforderlich.
 *
 * Die Abnutzung wird als Prozentwert zwischen 0 (neu) und 100 (verschlissen)
 * gespeichert. Der kritische Bereich beginnt bei 80 Prozent.
 *
 * @author F1 Simulation Team
 * @version 1.0
 */
public class Reifen {

    public static final double KRITISCHE_ABNUTZUNG = 80.0;

    public static final double MAXIMALE_ABNUTZUNG = 100.0;

    private final ReifenTyp typ;
    private double abnutzungProzent;

    /**
     * Erstellt einen neuen Reifensatz des angegebenen Typs.
     * Neue Reifen haben eine Abnutzung von 0 Prozent.
     *
     * @Vorbedingung typ darf nicht null sein
     * @Nachbedingung Reifen ist erstellt mit Abnutzung 0
     *
     * @param typ Der Reifentyp (SOFT, MEDIUM oder HARD)
     * @throws IllegalArgumentException wenn typ null ist
     */
    public Reifen(ReifenTyp typ) {
        if (typ == null) {
            throw new IllegalArgumentException("Reifentyp darf nicht null sein");
        }
        this.typ = typ;
        this.abnutzungProzent = 0.0;
    }

    /**
     * Simuliert die Abnutzung der Reifen fuer eine gefahrene Runde.
     * Die Abnutzungsrate haengt vom Reifentyp ab.
     * Soft-Reifen nutzen sich schneller ab als Hard-Reifen.
     *
     * @Vorbedingung Keine
     * @Nachbedingung Abnutzung ist um die typspezifische Rate erhoeht,
     *                maximal bis MAXIMALE_ABNUTZUNG
     */
    public void abnutzen() {
        abnutzungProzent = Math.min(
                MAXIMALE_ABNUTZUNG,
                abnutzungProzent + typ.getAbnutzungsRate()
        );
    }

    /**
     * Berechnet den aktuellen Geschwindigkeitsfaktor basierend auf
     * Reifentyp und Abnutzung. Je staerker die Abnutzung, desto
     * langsamer wird das Fahrzeug.
     *
     * @Vorbedingung Keine
     * @Nachbedingung Rueckgabewert liegt zwischen 0.5 und 1.0
     *
     * @return Geschwindigkeitsfaktor (1.0 = volle Geschwindigkeit)
     */
    public double berechneGeschwindigkeitsFaktor() {
        double basisFaktor = typ.getGeschwindigkeitsFaktor();
        double abnutzungsMalus = (abnutzungProzent / MAXIMALE_ABNUTZUNG) * 0.3;
        return Math.max(0.5, basisFaktor - abnutzungsMalus);
    }

    /**
     * Prueft ob die Reifen kritisch abgenutzt sind und ein
     * Wechsel dringend empfohlen wird.
     *
     * @Vorbedingung Keine
     * @Nachbedingung Rueckgabewert ist true wenn Abnutzung >= KRITISCHE_ABNUTZUNG
     *
     * @return true wenn die Abnutzung kritisch ist
     */
    public boolean istKritischAbgenutzt() {
        return abnutzungProzent >= KRITISCHE_ABNUTZUNG;
    }

    /**
     * Prueft ob die Reifen vollstaendig verschlissen sind.
     *
     * @Vorbedingung Keine
     * @Nachbedingung Rueckgabewert ist true wenn Abnutzung >= MAXIMALE_ABNUTZUNG
     *
     * @return true wenn die Reifen verschlissen sind
     */
    public boolean istVerschlissen() {
        return abnutzungProzent >= MAXIMALE_ABNUTZUNG;
    }

    public ReifenTyp getTyp() {
        return typ;
    }

    public double getAbnutzungProzent() {
        return abnutzungProzent;
    }

    /**
     * Setzt die Abnutzung auf einen bestimmten Wert.
     * Wird verwendet fuer Testzwecke oder spezielle Szenarien.
     *
     * @Vorbedingung wert muss zwischen 0 und MAXIMALE_ABNUTZUNG liegen
     * @Nachbedingung Abnutzung ist auf den angegebenen Wert gesetzt
     *
     * @param wert Der neue Abnutzungswert in Prozent
     * @throws IllegalArgumentException wenn wert ausserhalb des gueltigen Bereichs
     */
    public void setAbnutzungProzent(double wert) {
        if (wert < 0 || wert > MAXIMALE_ABNUTZUNG) {
            throw new IllegalArgumentException(
                    "Abnutzung muss zwischen 0 und " + MAXIMALE_ABNUTZUNG + " liegen"
            );
        }
        this.abnutzungProzent = wert;
    }

    @Override
    public String toString() {
        return String.format("%s (%.1f%% abgenutzt)", typ.getBezeichnung(), abnutzungProzent);
    }
}
