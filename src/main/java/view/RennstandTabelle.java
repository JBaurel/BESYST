package view;

import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import model.Auto;
import model.RennDaten;

import java.util.List;

/**
 * TableView fuer die Anzeige des aktuellen Rennstands.
 * Zeigt fuer jeden Fahrer: Position, Name, Team, Runde, Rueckstand,
 * Reifentyp, Reifenzustand und Anzahl der Pitstops.
 *
 * @author F1 Simulation Team
 * @version 1.0
 */
public class RennstandTabelle extends TableView<RennstandTabelle.RennstandZeile> {

    private ObservableList<RennstandZeile> daten;

    /**
     * Erstellt eine neue RennstandTabelle.
     */
    public RennstandTabelle() {
        daten = FXCollections.observableArrayList();
        setItems(daten);

        erstelleSpalten();

        // Tabellen-Eigenschaften
        setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        setPrefHeight(400);
    }

    /**
     * Erstellt alle Tabellenspalten.
     */
    private void erstelleSpalten() {
        // Position
        TableColumn<RennstandZeile, Integer> posCol = new TableColumn<>("Pos");
        posCol.setCellValueFactory(new PropertyValueFactory<>("position"));
        posCol.setPrefWidth(40);
        posCol.setStyle("-fx-alignment: CENTER;");

        // Fahrer
        TableColumn<RennstandZeile, String> fahrerCol = new TableColumn<>("Fahrer");
        fahrerCol.setCellValueFactory(new PropertyValueFactory<>("fahrer"));
        fahrerCol.setPrefWidth(100);

        // Team
        TableColumn<RennstandZeile, String> teamCol = new TableColumn<>("Team");
        teamCol.setCellValueFactory(new PropertyValueFactory<>("team"));
        teamCol.setPrefWidth(90);

        // Runde
        TableColumn<RennstandZeile, String> rundeCol = new TableColumn<>("Runde");
        rundeCol.setCellValueFactory(new PropertyValueFactory<>("runde"));
        rundeCol.setPrefWidth(50);
        rundeCol.setStyle("-fx-alignment: CENTER;");

        // Rueckstand
        TableColumn<RennstandZeile, String> rueckstandCol = new TableColumn<>("Rückstand");
        rueckstandCol.setCellValueFactory(new PropertyValueFactory<>("rueckstand"));
        rueckstandCol.setPrefWidth(70);
        rueckstandCol.setStyle("-fx-alignment: CENTER-RIGHT;");

        // Reifen
        TableColumn<RennstandZeile, String> reifenCol = new TableColumn<>("Reifen");
        reifenCol.setCellValueFactory(new PropertyValueFactory<>("reifen"));
        reifenCol.setPrefWidth(60);
        reifenCol.setStyle("-fx-alignment: CENTER;");

        // Reifenzustand
        TableColumn<RennstandZeile, String> zustandCol = new TableColumn<>("Zustand");
        zustandCol.setCellValueFactory(new PropertyValueFactory<>("reifenZustand"));
        zustandCol.setPrefWidth(55);
        zustandCol.setStyle("-fx-alignment: CENTER;");

        // Pitstops
        TableColumn<RennstandZeile, Integer> pitstopsCol = new TableColumn<>("Pits");
        pitstopsCol.setCellValueFactory(new PropertyValueFactory<>("pitstops"));
        pitstopsCol.setPrefWidth(35);
        pitstopsCol.setStyle("-fx-alignment: CENTER;");

        getColumns().addAll(posCol, fahrerCol, teamCol, rundeCol,
                rueckstandCol, reifenCol, zustandCol, pitstopsCol);
    }

    /**
     * Aktualisiert den Rennstand mit den aktuellen Daten.
     *
     * @param reihenfolge Die sortierte Liste der Autos
     * @param rennDaten Die Renndaten fuer zusaetzliche Informationen
     */
    public void aktualisiereRennstand(List<Auto> reihenfolge, RennDaten rennDaten) {
        daten.clear();

        int position = 1;
        long fuehrerZeit = 0;

        for (Auto auto : reihenfolge) {
            if (position == 1) {
                fuehrerZeit = berechneVirtuelleZeit(auto);
            }

            long virtuelleZeit = berechneVirtuelleZeit(auto);
            long rueckstand = virtuelleZeit - fuehrerZeit;

            RennstandZeile zeile = new RennstandZeile(
                    position,
                    auto.getFahrer().getKuerzel(),
                    kuerzeTeamName(auto.getTeam().getName()),
                    auto.getAktuelleRunde() + "/" + rennDaten.getAnzahlRunden(),
                    formatRueckstand(rueckstand, position),
                    auto.getAktuelleReifen().getTyp().name().charAt(0) + "",
                    String.format("%.0f%%", auto.getAktuelleReifen().getAbnutzungProzent()),
                    auto.getAnzahlPitstops()
            );

            daten.add(zeile);
            position++;
        }
    }

    /**
     * Berechnet eine virtuelle Zeit basierend auf Runde und Fortschritt.
     */
    private long berechneVirtuelleZeit(Auto auto) {
        // Runde * 1000000 + Abschnitt * 1000 + Fortschritt * 1000
        return auto.getAktuelleRunde() * 1000000L +
                auto.getAktuellerAbschnittId() * 1000L +
                (long)(auto.getFortschrittImAbschnitt() * 1000);
    }

    /**
     * Formatiert den Rueckstand fuer die Anzeige.
     */
    private String formatRueckstand(long rueckstand, int position) {
        if (position == 1) {
            return "Führer";
        }

        // In Sekunden-artige Darstellung umrechnen
        double sekunden = rueckstand / 1000.0;
        if (sekunden < 10) {
            return String.format("+%.1fs", sekunden);
        } else {
            return String.format("+%.0fs", sekunden);
        }
    }

    /**
     * Kuerzt den Teamnamen fuer die Anzeige.
     */
    private String kuerzeTeamName(String name) {
        if (name.length() > 10) {
            return name.substring(0, 8) + "..";
        }
        return name;
    }

    /**
     * Datenklasse fuer eine Zeile in der Tabelle.
     */
    public static class RennstandZeile {
        private final SimpleIntegerProperty position;
        private final SimpleStringProperty fahrer;
        private final SimpleStringProperty team;
        private final SimpleStringProperty runde;
        private final SimpleStringProperty rueckstand;
        private final SimpleStringProperty reifen;
        private final SimpleStringProperty reifenZustand;
        private final SimpleIntegerProperty pitstops;

        public RennstandZeile(int position, String fahrer, String team, String runde,
                              String rueckstand, String reifen, String reifenZustand, int pitstops) {
            this.position = new SimpleIntegerProperty(position);
            this.fahrer = new SimpleStringProperty(fahrer);
            this.team = new SimpleStringProperty(team);
            this.runde = new SimpleStringProperty(runde);
            this.rueckstand = new SimpleStringProperty(rueckstand);
            this.reifen = new SimpleStringProperty(reifen);
            this.reifenZustand = new SimpleStringProperty(reifenZustand);
            this.pitstops = new SimpleIntegerProperty(pitstops);
        }

        // Getter fuer PropertyValueFactory
        public int getPosition() { return position.get(); }
        public String getFahrer() { return fahrer.get(); }
        public String getTeam() { return team.get(); }
        public String getRunde() { return runde.get(); }
        public String getRueckstand() { return rueckstand.get(); }
        public String getReifen() { return reifen.get(); }
        public String getReifenZustand() { return reifenZustand.get(); }
        public int getPitstops() { return pitstops.get(); }

        // Property-Getter
        public SimpleIntegerProperty positionProperty() { return position; }
        public SimpleStringProperty fahrerProperty() { return fahrer; }
        public SimpleStringProperty teamProperty() { return team; }
        public SimpleStringProperty rundeProperty() { return runde; }
        public SimpleStringProperty rueckstandProperty() { return rueckstand; }
        public SimpleStringProperty reifenProperty() { return reifen; }
        public SimpleStringProperty reifenZustandProperty() { return reifenZustand; }
        public SimpleIntegerProperty pitstopsProperty() { return pitstops; }
    }
}
