package model;

/**
 * Repraesentiert eine Boxengarage eines Teams in der Boxengasse.
 * Jedes Team verfuegt ueber genau eine Box, in der Reifenwechsel
 * und andere Servicearbeiten durchgefuehrt werden.
 *
 * Die Box hat eine Position in der Boxengasse und kann von nur
 * einem Auto des Teams gleichzeitig genutzt werden.
 *
 * @author F1 Simulation Team
 * @version 1.0
 */
public class Box {

    private final Team team;
    private final int positionInBoxengasse;
    private boolean belegt;
    private Auto aktuellesAuto;
    private long belegungStartzeit;

    /**
     * Erstellt eine neue Box fuer das angegebene Team.
     *
     * @Vorbedingung team darf nicht null sein
     * @Vorbedingung positionInBoxengasse muss >= 0 sein
     * @Nachbedingung Box-Objekt ist erstellt und nicht belegt
     *
     * @param team Das Team dem diese Box gehoert
     * @param positionInBoxengasse Position der Box (0 = erste Box)
     * @throws IllegalArgumentException wenn Parameter ungueltig sind
     */
    public Box(Team team, int positionInBoxengasse) {
        if (team == null) {
            throw new IllegalArgumentException("Team darf nicht null sein");
        }
        if (positionInBoxengasse < 0) {
            throw new IllegalArgumentException("Position muss >= 0 sein");
        }

        this.team = team;
        this.positionInBoxengasse = positionInBoxengasse;
        this.belegt = false;
        this.aktuellesAuto = null;
        this.belegungStartzeit = 0;
    }

    /**
     * Belegt die Box mit dem angegebenen Auto.
     * Das Auto muss zum Team dieser Box gehoeren.
     *
     * @Vorbedingung auto darf nicht null sein
     * @Vorbedingung auto muss zum Team der Box gehoeren
     * @Vorbedingung Box darf nicht bereits belegt sein
     * @Nachbedingung Box ist belegt mit dem angegebenen Auto
     *
     * @param auto Das einfahrende Auto
     * @throws IllegalArgumentException wenn auto nicht zum Team gehoert
     * @throws IllegalStateException wenn Box bereits belegt ist
     */
    public void belegenMitAuto(Auto auto) {
        if (auto == null) {
            throw new IllegalArgumentException("Auto darf nicht null sein");
        }
        if (!auto.getTeam().equals(team)) {
            throw new IllegalArgumentException("Auto gehoert nicht zu diesem Team");
        }
        if (belegt) {
            throw new IllegalStateException("Box ist bereits belegt");
        }

        this.belegt = true;
        this.aktuellesAuto = auto;
        this.belegungStartzeit = System.currentTimeMillis();
    }

    /**
     * Gibt die Box frei nachdem das Auto sie verlassen hat.
     *
     * @Vorbedingung Box muss belegt sein
     * @Nachbedingung Box ist frei und aktuellesAuto ist null
     *
     * @return Das Auto das die Box verlassen hat
     * @throws IllegalStateException wenn Box nicht belegt ist
     */
    public Auto freigeben() {
        if (!belegt) {
            throw new IllegalStateException("Box ist nicht belegt");
        }

        Auto verlassendesAuto = this.aktuellesAuto;
        this.belegt = false;
        this.aktuellesAuto = null;
        this.belegungStartzeit = 0;

        return verlassendesAuto;
    }

    /**
     * Berechnet die bisherige Belegungsdauer in Millisekunden.
     *
     * @Vorbedingung Keine
     * @Nachbedingung Rueckgabewert ist Dauer seit Belegungsbeginn oder 0
     *
     * @return Belegungsdauer in Millisekunden, 0 wenn nicht belegt
     */
    public long getBelegungsdauer() {
        if (!belegt || belegungStartzeit == 0) {
            return 0;
        }
        return System.currentTimeMillis() - belegungStartzeit;
    }

    /**
     * Berechnet die X-Koordinate der Box in der GUI.
     * Die Position wird basierend auf der Boxengassen-Geometrie berechnet.
     *
     * @Vorbedingung Keine
     * @Nachbedingung Rueckgabewert liegt zwischen 0.25 und 0.45
     *
     * @return X-Koordinate fuer die GUI (normalisiert)
     */
    public double getGuiX() {
        // Boxengasse geht von x=0.25 bis x=0.45
        double startX = 0.25;
        double endeX = 0.45;
        double schrittweite = (endeX - startX) / 10.0; // 10 Teams
        return startX + (positionInBoxengasse * schrittweite);
    }

    /**
     * Berechnet die Y-Koordinate der Box in der GUI.
     * Alle Boxen liegen auf der gleichen Hoehe.
     *
     * @Vorbedingung Keine
     * @Nachbedingung Rueckgabewert ist die Y-Position der Boxengasse
     *
     * @return Y-Koordinate fuer die GUI (normalisiert)
     */
    public double getGuiY() {
        return 0.48; // Boxengasse liegt bei y=0.48
    }

    public Team getTeam() {
        return team;
    }

    public int getPositionInBoxengasse() {
        return positionInBoxengasse;
    }

    public boolean istBelegt() {
        return belegt;
    }

    public Auto getAktuellesAuto() {
        return aktuellesAuto;
    }

    @Override
    public String toString() {
        String status = belegt
                ? "belegt mit " + aktuellesAuto.getFahrer().getKuerzel()
                : "frei";
        return String.format("Box %s (%s)", team.getName(), status);
    }
}