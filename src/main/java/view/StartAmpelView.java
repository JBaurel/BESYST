package view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.control.Label;

/**
 * Grafische Darstellung der F1-Startampel.
 * Zeigt 5 rote Lichter, die nacheinander aufleuchten und dann alle ausgehen.
 *
 * Die Ampel hat folgende Zustaende:
 * - Aus: Alle Lichter dunkel (vor Startsequenz und nach Freigabe)
 * - 1-5 Lichter: Waehrend der Startsequenz leuchten 1-5 Lichter
 *
 * @author F1 Simulation Team
 * @version 1.0
 */
public class StartAmpelView extends VBox {

    private static final int ANZAHL_LICHTER = 5;
    private static final double LICHT_RADIUS = 15.0;
    private static final Color LICHT_AUS = Color.rgb(60, 20, 20);
    private static final Color LICHT_AN = Color.RED;

    private Circle[] lichter;
    private Label statusLabel;

    /**
     * Erstellt eine neue StartAmpelView.
     */
    public StartAmpelView() {
        setAlignment(Pos.CENTER);
        setSpacing(5);
        setPadding(new Insets(5, 15, 5, 15));
        setStyle("-fx-background-color: #333333; -fx-background-radius: 10;");

        Label titel = new Label("Startampel");
        titel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 10px;");


        HBox lichterBox = new HBox(10);
        lichterBox.setAlignment(Pos.CENTER);


        lichter = new Circle[ANZAHL_LICHTER];
        for (int i = 0; i < ANZAHL_LICHTER; i++) {
            lichter[i] = erstelleLicht();
            lichterBox.getChildren().add(lichter[i]);
        }


        statusLabel = new Label("Bereit");
        statusLabel.setStyle("-fx-text-fill: #aaaaaa; -fx-font-size: 10px;");

        getChildren().addAll(titel, lichterBox, statusLabel);
    }

    /**
     * Erstellt ein einzelnes Licht (Kreis).
     */
    private Circle erstelleLicht() {
        Circle licht = new Circle(LICHT_RADIUS);
        licht.setFill(LICHT_AUS);
        licht.setStroke(Color.rgb(100, 100, 100));
        licht.setStrokeWidth(2);


        licht.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 3, 0, 1, 1);");

        return licht;
    }

    /**
     * Setzt ein bestimmtes Licht auf "an".
     * Alle Lichter bis zu dieser Nummer leuchten.
     *
     * @param nummer Die Nummer des Lichts (1-5)
     */
    public void setzeLicht(int nummer) {
        if (nummer < 1 || nummer > ANZAHL_LICHTER) {
            return;
        }

        for (int i = 0; i < ANZAHL_LICHTER; i++) {
            if (i < nummer) {
                lichter[i].setFill(LICHT_AN);
                // Glow-Effekt fuer leuchtende Lichter
                lichter[i].setStyle("-fx-effect: dropshadow(gaussian, red, 10, 0.5, 0, 0);");
            } else {
                lichter[i].setFill(LICHT_AUS);
                lichter[i].setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 3, 0, 1, 1);");
            }
        }

        statusLabel.setText(nummer + " / " + ANZAHL_LICHTER);
        statusLabel.setStyle("-fx-text-fill: #ff6666; -fx-font-size: 10px; -fx-font-weight: bold;");
    }

    /**
     * Schaltet alle Lichter aus (Startfreigabe).
     */
    public void alleAus() {
        for (Circle licht : lichter) {
            licht.setFill(LICHT_AUS);
            licht.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 3, 0, 1, 1);");
        }

        statusLabel.setText("GO!");
        statusLabel.setStyle("-fx-text-fill: #66ff66; -fx-font-size: 12px; -fx-font-weight: bold;");
    }

    /**
     * Setzt die Ampel in den Ausgangszustand zurueck.
     */
    public void reset() {
        for (Circle licht : lichter) {
            licht.setFill(LICHT_AUS);
            licht.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 3, 0, 1, 1);");
        }

        statusLabel.setText("Bereit");
        statusLabel.setStyle("-fx-text-fill: #aaaaaa; -fx-font-size: 10px;");
    }

    /**
     * Gibt die Anzahl der aktuell leuchtenden Lichter zurueck.
     *
     * @return Anzahl der leuchtenden Lichter (0-5)
     */
    public int getAktiveLichter() {
        int count = 0;
        for (Circle licht : lichter) {
            if (licht.getFill().equals(LICHT_AN)) {
                count++;
            }
        }
        return count;
    }
}
