package model;

import model.enumType.AutoStatus;
import model.enumType.RennStatus;

import javafx.scene.paint.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Zentrale Datenhaltungsklasse fuer die gesamte Rennsimulation.
 * Diese Klasse initialisiert alle Teams, Fahrer und Autos und
 * stellt Zugriffsmethoden fuer die anderen Schichten bereit.
 *
 * Die Klasse verwendet thread-sichere Collections fuer Daten,
 * die von mehreren Threads gelesen werden. Schreibzugriffe
 * muessen durch die Synchronisationsschicht geschuetzt werden.
 *
 * @author F1 Simulation Team
 * @version 1.0
 */
public class RennDaten {

    private final Rennstrecke strecke;
    private final List<Team> teams;
    private final List<Auto> autos;
    private final List<Box> boxen;
    private final List<Rundenzeit> alleRundenzeiten;
    private final List<Rennergebnis> ergebnisse;

    private RennStatus status;
    private long rennStartzeit;
    private double simulationsGeschwindigkeit;

    /**
     * Erstellt eine neue RennDaten-Instanz mit der Nuerburgring-Strecke
     * und allen 10 realen Formel-1-Teams mit ihren Fahrern.
     *
     * @Vorbedingung Keine
     * @Nachbedingung Alle Teams, Fahrer und Autos sind initialisiert
     */
    public RennDaten() {
        this.strecke = Rennstrecke.erstelleNuerburgring();
        this.teams = new ArrayList<>();
        this.autos = new CopyOnWriteArrayList<>();
        this.boxen = new ArrayList<>();
        this.alleRundenzeiten = new CopyOnWriteArrayList<>();
        this.ergebnisse = new CopyOnWriteArrayList<>();

        this.status = RennStatus.VORBEREITUNG;
        this.rennStartzeit = 0;
        this.simulationsGeschwindigkeit = 1.0;

        initialisiereTeamsUndFahrer();
    }

    /**
     * Initialisiert alle 10 F1-Teams mit jeweils 2 Fahrern.
     * Die Teams und Fahrer basieren auf der realen Formel 1.
     *
     * @Vorbedingung Keine
     * @Nachbedingung teams-Liste enthaelt 10 Teams mit je 2 Fahrern
     * @Nachbedingung autos-Liste enthaelt 20 Autos
     * @Nachbedingung boxen-Liste enthaelt 10 Boxen
     */
    private void initialisiereTeamsUndFahrer() {
        int startnummer = 1;
        int boxPosition = 0;

        Fahrer verstappen = new Fahrer("Max Verstappen", "VER", 98);
        Fahrer perez = new Fahrer("Sergio Perez", "PER", 85);
        Team redBull = new Team("Red Bull Racing", Color.DARKBLUE, verstappen, perez);
        teams.add(redBull);
        autos.add(new Auto(startnummer++, redBull, verstappen));
        autos.add(new Auto(startnummer++, redBull, perez));
        boxen.add(new Box(redBull, boxPosition++));

        // Ferrari
        Fahrer leclerc = new Fahrer("Charles Leclerc", "LEC", 94);
        Fahrer sainz = new Fahrer("Carlos Sainz", "SAI", 90);
        Team ferrari = new Team("Ferrari", Color.RED, leclerc, sainz);
        teams.add(ferrari);
        autos.add(new Auto(startnummer++, ferrari, leclerc));
        autos.add(new Auto(startnummer++, ferrari, sainz));
        boxen.add(new Box(ferrari, boxPosition++));

        // Mercedes
        Fahrer hamilton = new Fahrer("Lewis Hamilton", "HAM", 96);
        Fahrer russell = new Fahrer("George Russell", "RUS", 91);
        Team mercedes = new Team("Mercedes", Color.TEAL, hamilton, russell);
        teams.add(mercedes);
        autos.add(new Auto(startnummer++, mercedes, hamilton));
        autos.add(new Auto(startnummer++, mercedes, russell));
        boxen.add(new Box(mercedes, boxPosition++));

        // McLaren
        Fahrer norris = new Fahrer("Lando Norris", "NOR", 92);
        Fahrer piastri = new Fahrer("Oscar Piastri", "PIA", 88);
        Team mclaren = new Team("McLaren", Color.ORANGE, norris, piastri);
        teams.add(mclaren);
        autos.add(new Auto(startnummer++, mclaren, norris));
        autos.add(new Auto(startnummer++, mclaren, piastri));
        boxen.add(new Box(mclaren, boxPosition++));

        // Aston Martin
        Fahrer alonso = new Fahrer("Fernando Alonso", "ALO", 93);
        Fahrer stroll = new Fahrer("Lance Stroll", "STR", 78);
        Team astonMartin = new Team("Aston Martin", Color.DARKGREEN, alonso, stroll);
        teams.add(astonMartin);
        autos.add(new Auto(startnummer++, astonMartin, alonso));
        autos.add(new Auto(startnummer++, astonMartin, stroll));
        boxen.add(new Box(astonMartin, boxPosition++));

        // Alpine
        Fahrer gasly = new Fahrer("Pierre Gasly", "GAS", 84);
        Fahrer ocon = new Fahrer("Esteban Ocon", "OCO", 82);
        Team alpine = new Team("Alpine", Color.HOTPINK, gasly, ocon);
        teams.add(alpine);
        autos.add(new Auto(startnummer++, alpine, gasly));
        autos.add(new Auto(startnummer++, alpine, ocon));
        boxen.add(new Box(alpine, boxPosition++));

        // Williams
        Fahrer albon = new Fahrer("Alexander Albon", "ALB", 83);
        Fahrer sargeant = new Fahrer("Logan Sargeant", "SAR", 72);
        Team williams = new Team("Williams", Color.LIGHTBLUE, albon, sargeant);
        teams.add(williams);
        autos.add(new Auto(startnummer++, williams, albon));
        autos.add(new Auto(startnummer++, williams, sargeant));
        boxen.add(new Box(williams, boxPosition++));

        // AlphaTauri / RB
        Fahrer tsunoda = new Fahrer("Yuki Tsunoda", "TSU", 80);
        Fahrer ricciardo = new Fahrer("Daniel Ricciardo", "RIC", 81);
        Team alphaTauri = new Team("RB", Color.MIDNIGHTBLUE, tsunoda, ricciardo);
        teams.add(alphaTauri);
        autos.add(new Auto(startnummer++, alphaTauri, tsunoda));
        autos.add(new Auto(startnummer++, alphaTauri, ricciardo));
        boxen.add(new Box(alphaTauri, boxPosition++));

        // Alfa Romeo / Stake
        Fahrer bottas = new Fahrer("Valtteri Bottas", "BOT", 86);
        Fahrer zhou = new Fahrer("Zhou Guanyu", "ZHO", 76);
        Team alfaRomeo = new Team("Stake", Color.GREEN, bottas, zhou);
        teams.add(alfaRomeo);
        autos.add(new Auto(startnummer++, alfaRomeo, bottas));
        autos.add(new Auto(startnummer++, alfaRomeo, zhou));
        boxen.add(new Box(alfaRomeo, boxPosition++));

        // Haas
        Fahrer magnussen = new Fahrer("Kevin Magnussen", "MAG", 79);
        Fahrer hulkenberg = new Fahrer("Nico Huelkenberg", "HUL", 82);
        Team haas = new Team("Haas", Color.LIGHTGRAY, magnussen, hulkenberg);
        teams.add(haas);
        autos.add(new Auto(startnummer++, haas, magnussen));
        autos.add(new Auto(startnummer++, haas, hulkenberg));
        boxen.add(new Box(haas, boxPosition));
    }

    /**
     * Gibt die aktuelle Rennreihenfolge zurueck, sortiert nach Position.
     * Die Position basiert auf Runde und Streckenfortschritt.
     *
     * @Vorbedingung Keine
     * @Nachbedingung Rueckgabewert ist sortierte Liste nach Rennposition
     *
     * @return Liste der Autos in aktueller Rennreihenfolge
     */
    public List<Auto> getRennreihenfolge() {
        List<Auto> sortiert = new ArrayList<>(autos);

        sortiert.sort((a1, a2) -> {

            int rundenVergleich = Integer.compare(a2.getAktuelleRunde(), a1.getAktuelleRunde());
            if (rundenVergleich != 0) {
                return rundenVergleich;
            }


            int abschnittVergleich = Integer.compare(
                    a2.getAktuellerAbschnittId(),
                    a1.getAktuellerAbschnittId()
            );
            if (abschnittVergleich != 0) {
                return abschnittVergleich;
            }


            return Double.compare(
                    a2.getFortschrittImAbschnitt(),
                    a1.getFortschrittImAbschnitt()
            );
        });

        return sortiert;
    }

    /**
     * Berechnet den Zeitabstand eines Autos zum fuehrenden Auto.
     * Der Abstand basiert auf der Differenz der Gesamtzeiten.
     *
     * @Vorbedingung auto darf nicht null sein
     * @Nachbedingung Rueckgabewert ist der Zeitabstand in Millisekunden
     *
     * @param auto Das Auto fuer das der Abstand berechnet wird
     * @return Zeitabstand zum Ersten in Millisekunden, 0 wenn Fuehrender
     */
    public long getAbstandZumFuehrenden(Auto auto) {
        if (auto == null) {
            throw new IllegalArgumentException("Auto darf nicht null sein");
        }

        List<Auto> reihenfolge = getRennreihenfolge();
        if (reihenfolge.isEmpty() || reihenfolge.get(0).equals(auto)) {
            return 0;
        }

        Auto fuehrender = reihenfolge.get(0);
        return auto.getGesamtzeit() - fuehrender.getGesamtzeit();
    }

    /**
     * Findet ein Auto anhand seiner Startnummer.
     *
     * @Vorbedingung startnummer muss zwischen 1 und 20 liegen
     * @Nachbedingung Rueckgabewert ist das Auto oder null wenn nicht gefunden
     *
     * @param startnummer Die Startnummer des gesuchten Autos
     * @return Das Auto oder null
     */
    public Auto findeAutoNachStartnummer(int startnummer) {
        for (Auto auto : autos) {
            if (auto.getStartnummer() == startnummer) {
                return auto;
            }
        }
        return null;
    }

    /**
     * Findet die Box eines Teams.
     *
     * @Vorbedingung team darf nicht null sein
     * @Nachbedingung Rueckgabewert ist die Box oder null wenn nicht gefunden
     *
     * @param team Das Team dessen Box gesucht wird
     * @return Die Box des Teams oder null
     */
    public Box findeBoxNachTeam(Team team) {
        if (team == null) {
            throw new IllegalArgumentException("Team darf nicht null sein");
        }
        for (Box box : boxen) {
            if (box.getTeam().equals(team)) {
                return box;
            }
        }
        return null;
    }

    /**
     * Fuegt eine neue Rundenzeit hinzu.
     * Diese Methode ist thread-sicher durch CopyOnWriteArrayList.
     *
     * @Vorbedingung rundenzeit darf nicht null sein
     * @Nachbedingung Rundenzeit ist zur Liste hinzugefuegt
     *
     * @param rundenzeit Die hinzuzufuegende Rundenzeit
     */
    public void addRundenzeit(Rundenzeit rundenzeit) {
        if (rundenzeit == null) {
            throw new IllegalArgumentException("Rundenzeit darf nicht null sein");
        }
        alleRundenzeiten.add(rundenzeit);
    }

    /**
     * Setzt alle Renndaten fuer einen Neustart zurueck.
     *
     * @Vorbedingung Keine
     * @Nachbedingung Alle Autos sind zurueckgesetzt, Listen sind geleert
     */
    public void zuruecksetzen() {
        for (Auto auto : autos) {
            auto.zuruecksetzen();
        }
        alleRundenzeiten.clear();
        ergebnisse.clear();
        status = RennStatus.VORBEREITUNG;
        rennStartzeit = 0;
    }

    /**
     * Prueft ob alle Autos das Rennen beendet haben.
     *
     * @Vorbedingung Keine
     * @Nachbedingung Rueckgabewert ist true wenn alle im Ziel sind
     *
     * @return true wenn alle Autos im Ziel sind
     */
    public boolean sindAlleImZiel() {
        for (Auto auto : autos) {
            if (auto.getStatus() != AutoStatus.IM_ZIEL) {
                return false;
            }
        }
        return true;
    }



    public Rennstrecke getStrecke() {
        return strecke;
    }

    public List<Team> getTeams() {
        return Collections.unmodifiableList(teams);
    }

    public List<Auto> getAutos() {
        return Collections.unmodifiableList(autos);
    }

    public List<Box> getBoxen() {
        return Collections.unmodifiableList(boxen);
    }

    public List<Rundenzeit> getAlleRundenzeiten() {
        return Collections.unmodifiableList(alleRundenzeiten);
    }

    public List<Rennergebnis> getErgebnisse() {
        return Collections.unmodifiableList(ergebnisse);
    }

    public void addErgebnis(Rennergebnis ergebnis) {
        if (ergebnis != null) {
            ergebnisse.add(ergebnis);
        }
    }

    public RennStatus getStatus() {
        return status;
    }

    public void setStatus(RennStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("Status darf nicht null sein");
        }
        this.status = status;
    }

    public long getRennStartzeit() {
        return rennStartzeit;
    }

    public void setRennStartzeit(long zeit) {
        this.rennStartzeit = zeit;
    }

    public double getSimulationsGeschwindigkeit() {
        return simulationsGeschwindigkeit;
    }

    public void setSimulationsGeschwindigkeit(double geschwindigkeit) {
        if (geschwindigkeit <= 0) {
            throw new IllegalArgumentException("Geschwindigkeit muss > 0 sein");
        }
        this.simulationsGeschwindigkeit = geschwindigkeit;
    }

    /**
     * Gibt die Anzahl der zu fahrenden Runden zurueck.
     *
     * @Vorbedingung Keine
     * @Nachbedingung Rueckgabewert ist die konfigurierte Rundenanzahl
     *
     * @return Anzahl der Runden
     */
    public int getAnzahlRunden() {
        return strecke.getAnzahlRunden();
    }

    /**
     * Setzt die Anzahl der zu fahrenden Runden.
     *
     * @Vorbedingung anzahl muss zwischen 20 und 50 liegen
     * @Nachbedingung Rundenanzahl der Strecke ist aktualisiert
     *
     * @param anzahl Neue Rundenanzahl
     */
    public void setAnzahlRunden(int anzahl) {
        strecke.setAnzahlRunden(anzahl);
    }
}
