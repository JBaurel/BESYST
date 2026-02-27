package model;

import model.enumType.StreckenabschnittTyp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Repraesentiert eine vollstaendige Rennstrecke bestehend aus
 * mehreren Streckenabschnitten. Diese Klasse modelliert den
 * Nuerburgring GP-Kurs mit allen charakteristischen Kurven
 * und Geraden.
 *
 * Die Strecke wird als Ringstruktur behandelt, wobei nach dem
 * letzten Abschnitt wieder der erste folgt.
 *
 * @author F1 Simulation Team
 * @version 1.0
 */
public class Rennstrecke {


    public static final int DEFAULT_RUNDENANZAHL = 30;


    public static final int MIN_RUNDEN = 20;

    public static final int MAX_RUNDEN = 50;

    private final String name;
    private final List<Streckenabschnitt> abschnitte;
    private int anzahlRunden;

    /**
     * Erstellt eine neue Rennstrecke mit dem angegebenen Namen.
     * Die Abschnitte werden ueber die Methode addAbschnitt hinzugefuegt.
     *
     * @Vorbedingung name darf nicht null oder leer sein
     * @Nachbedingung Rennstrecke-Objekt ist mit leerem Abschnittsliste erstellt
     *
     * @param name Name der Rennstrecke
     * @throws IllegalArgumentException wenn name null oder leer ist
     */
    public Rennstrecke(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Streckenname darf nicht leer sein");
        }
        this.name = name;
        this.abschnitte = new ArrayList<>();
        this.anzahlRunden = DEFAULT_RUNDENANZAHL;
    }

    /**
     * Fuegt einen Streckenabschnitt zur Strecke hinzu.
     * Die Abschnitte sollten in der richtigen Reihenfolge hinzugefuegt werden.
     *
     * @Vorbedingung abschnitt darf nicht null sein
     * @Nachbedingung Abschnitt ist am Ende der Liste hinzugefuegt
     *
     * @param abschnitt Der hinzuzufuegende Abschnitt
     * @throws IllegalArgumentException wenn abschnitt null ist
     */
    public void addAbschnitt(Streckenabschnitt abschnitt) {
        if (abschnitt == null) {
            throw new IllegalArgumentException("Abschnitt darf nicht null sein");
        }
        abschnitte.add(abschnitt);
    }

    /**
     * Gibt den Streckenabschnitt mit der angegebenen ID zurueck.
     *
     * @Vorbedingung id muss gueltig sein (0 bis Anzahl-1)
     * @Nachbedingung Rueckgabewert ist der Abschnitt mit der ID
     *
     * @param id Die ID des gesuchten Abschnitts
     * @return Der Streckenabschnitt
     * @throws IndexOutOfBoundsException wenn ID ungueltig
     */
    public Streckenabschnitt getAbschnitt(int id) {
        if (id < 0 || id >= abschnitte.size()) {
            throw new IndexOutOfBoundsException(
                    "Ungueltige Abschnitts-ID: " + id + " (max: " + (abschnitte.size() - 1) + ")"
            );
        }
        return abschnitte.get(id);
    }

    /**
     * Gibt den naechsten Streckenabschnitt nach dem aktuellen zurueck.
     * Nach dem letzten Abschnitt kommt wieder der erste (Ringstruktur).
     *
     * @Vorbedingung aktuelleId muss gueltig sein
     * @Nachbedingung Rueckgabewert ist der nachfolgende Abschnitt
     *
     * @param aktuelleId ID des aktuellen Abschnitts
     * @return Der naechste Streckenabschnitt
     * @throws IndexOutOfBoundsException wenn ID ungueltig
     */
    public Streckenabschnitt getNaechsterAbschnitt(int aktuelleId) {
        if (aktuelleId < 0 || aktuelleId >= abschnitte.size()) {
            throw new IndexOutOfBoundsException("Ungueltige Abschnitts-ID: " + aktuelleId);
        }
        int naechsteId = (aktuelleId + 1) % abschnitte.size();
        return abschnitte.get(naechsteId);
    }

    /**
     * Prueft ob der Abschnitt der Start/Ziel-Bereich ist.
     * Dies wird benoetigt fuer die Rundenzaehlung.
     *
     * @Vorbedingung abschnittId muss gueltig sein
     * @Nachbedingung Rueckgabewert ist true wenn Abschnitt ID 0 hat
     *
     * @param abschnittId ID des zu pruefenden Abschnitts
     * @return true wenn es der Start/Ziel-Bereich ist
     */
    public boolean istStartZiel(int abschnittId) {
        return abschnittId == 0;
    }

    /**
     * Berechnet die Gesamtlaenge der Strecke in Metern.
     *
     * @Vorbedingung Mindestens ein Abschnitt muss vorhanden sein
     * @Nachbedingung Rueckgabewert ist die Summe aller Abschnittslaengen
     *
     * @return Gesamtlaenge in Metern
     */
    public int getGesamtlaengeInMetern() {
        return abschnitte.stream()
                .mapToInt(Streckenabschnitt::getLaengeInMetern)
                .sum();
    }

    /**
     * Gibt alle Ueberholzonen der Strecke zurueck.
     *
     * @Vorbedingung Keine
     * @Nachbedingung Rueckgabewert enthaelt alle Abschnitte mit Ueberholzone
     *
     * @return Liste der Ueberholzonen
     */
    public List<Streckenabschnitt> getUeberholzonen() {
        List<Streckenabschnitt> zonen = new ArrayList<>();
        for (Streckenabschnitt abschnitt : abschnitte) {
            if (abschnitt.istUeberholzone()) {
                zonen.add(abschnitt);
            }
        }
        return Collections.unmodifiableList(zonen);
    }

    /**
     * Gibt alle kritischen Bereiche der Strecke zurueck.
     * Kritische Bereiche erfordern Synchronisation.
     *
     * @Vorbedingung Keine
     * @Nachbedingung Rueckgabewert enthaelt alle kritischen Abschnitte
     *
     * @return Liste der kritischen Bereiche
     */
    public List<Streckenabschnitt> getKritischeBereiche() {
        List<Streckenabschnitt> bereiche = new ArrayList<>();
        for (Streckenabschnitt abschnitt : abschnitte) {
            if (abschnitt.istKritischerBereich()) {
                bereiche.add(abschnitt);
            }
        }
        return Collections.unmodifiableList(bereiche);
    }

    /**
     * Setzt die Anzahl der zu fahrenden Runden.
     *
     * @Vorbedingung anzahl muss zwischen MIN_RUNDEN und MAX_RUNDEN liegen
     * @Nachbedingung anzahlRunden ist auf den neuen Wert gesetzt
     *
     * @param anzahl Neue Rundenanzahl
     * @throws IllegalArgumentException wenn anzahl ausserhalb des Bereichs
     */
    public void setAnzahlRunden(int anzahl) {
        if (anzahl < MIN_RUNDEN || anzahl > MAX_RUNDEN) {
            throw new IllegalArgumentException(
                    "Rundenanzahl muss zwischen " + MIN_RUNDEN + " und " + MAX_RUNDEN + " liegen"
            );
        }
        this.anzahlRunden = anzahl;
    }

    public String getName() {
        return name;
    }

    public List<Streckenabschnitt> getAbschnitte() {
        return Collections.unmodifiableList(abschnitte);
    }

    public int getAnzahlAbschnitte() {
        return abschnitte.size();
    }

    public int getAnzahlRunden() {
        return anzahlRunden;
    }

    @Override
    public String toString() {
        return String.format("%s (%d Abschnitte, %.2f km)",
                name, abschnitte.size(), getGesamtlaengeInMetern() / 1000.0);
    }

    /**
     * Erstellt die Nuerburgring GP-Strecke mit allen Abschnitten.
     * Diese Factory-Methode konfiguriert die komplette Strecke
     * mit realistischen Abschnitten und Koordinaten.
     *
     * @Vorbedingung Keine
     * @Nachbedingung Eine vollstaendig konfigurierte Nuerburgring-Strecke
     *
     * @return Konfigurierte Rennstrecke
     */
    public static Rennstrecke erstelleNuerburgring() {
        Rennstrecke strecke = new Rennstrecke("Nuerburgring GP");

        strecke.addAbschnitt(new Streckenabschnitt(
                0, "Start/Ziel", StreckenabschnittTyp.START_ZIEL,
                400, 0.15, 0.50, 0.35, 0.50
        ));

        strecke.addAbschnitt(new Streckenabschnitt(
                1, "DRS-Zone 1", StreckenabschnittTyp.DRS_ZONE,
                300, 0.35, 0.50, 0.50, 0.50
        ));

        strecke.addAbschnitt(new Streckenabschnitt(
                2, "Yokohama-S", StreckenabschnittTyp.ENGE_KURVE,
                150, 1, false, 0.50, 0.50, 0.55, 0.40
        ));


        strecke.addAbschnitt(new Streckenabschnitt(
                3, "Mercedes-Arena", StreckenabschnittTyp.NORMALE_KURVE,
                250, 0.55, 0.40, 0.65, 0.35
        ));


        strecke.addAbschnitt(new Streckenabschnitt(
                4, "Verbindungsgerade", StreckenabschnittTyp.GERADE,
                350, 0.65, 0.35, 0.80, 0.35
        ));


        strecke.addAbschnitt(new Streckenabschnitt(
                5, "Ford-Kurve", StreckenabschnittTyp.ENGE_KURVE,
                180, 1, false, 0.80, 0.35, 0.85, 0.45
        ));


        strecke.addAbschnitt(new Streckenabschnitt(
                6, "Dunlop-Kehre", StreckenabschnittTyp.ENGE_KURVE,
                120, 1, false, 0.85, 0.45, 0.85, 0.55
        ));


        strecke.addAbschnitt(new Streckenabschnitt(
                7, "Schumacher-S", StreckenabschnittTyp.SCHIKANE,
                200, 0.85, 0.55, 0.80, 0.65
        ));

        strecke.addAbschnitt(new Streckenabschnitt(
                8, "DRS-Zone 2", StreckenabschnittTyp.DRS_ZONE,
                280, 0.80, 0.65, 0.65, 0.70
        ));


        strecke.addAbschnitt(new Streckenabschnitt(
                9, "Bit-Kurve", StreckenabschnittTyp.ENGE_KURVE,
                140, 1, false, 0.65, 0.70, 0.55, 0.75
        ));


        strecke.addAbschnitt(new Streckenabschnitt(
                10, "Haseroeder-Kurve", StreckenabschnittTyp.NORMALE_KURVE,
                200, 0.55, 0.75, 0.45, 0.75
        ));


        strecke.addAbschnitt(new Streckenabschnitt(
                11, "Coca-Cola-Kurve", StreckenabschnittTyp.ENGE_KURVE,
                160, 1, false, 0.45, 0.75, 0.35, 0.70
        ));

        strecke.addAbschnitt(new Streckenabschnitt(
                12, "DRS-Zone 3", StreckenabschnittTyp.DRS_ZONE,
                450, 0.35, 0.70, 0.20, 0.60
        ));


        strecke.addAbschnitt(new Streckenabschnitt(
                13, "Veedol-Schikane", StreckenabschnittTyp.SCHIKANE,
                180, 0.20, 0.60, 0.15, 0.55
        ));


        strecke.addAbschnitt(new Streckenabschnitt(
                14, "Letzte Kurve", StreckenabschnittTyp.NORMALE_KURVE,
                150, 0.15, 0.55, 0.15, 0.50
        ));


        strecke.addAbschnitt(new Streckenabschnitt(
                15, "Pitstop-Einfahrt", StreckenabschnittTyp.PITSTOP_EINFAHRT,
                100, 0.20, 0.50, 0.25, 0.48
        ));


        strecke.addAbschnitt(new Streckenabschnitt(
                16, "Boxengasse", StreckenabschnittTyp.BOXENGASSE,
                300, 0.25, 0.48, 0.45, 0.48
        ));


        strecke.addAbschnitt(new Streckenabschnitt(
                17, "Pitstop-Ausfahrt", StreckenabschnittTyp.PITSTOP_AUSFAHRT,
                100, 0.45, 0.48, 0.50, 0.50
        ));

        return strecke;
    }
}
