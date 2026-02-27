package view;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import model.*;
import model.enumType.AutoStatus;
import model.enumType.StreckenabschnittTyp;

import java.util.List;

/**
 * Canvas-basierte Visualisierung der Rennstrecke.
 * Diese Klasse zeichnet die Strecke, die Autos und alle Beschriftungen.
 *
 * Die Strecke wird basierend auf den normalisierten Koordinaten (0.0-1.0)
 * der Streckenabschnitte gezeichnet und an die Canvas-Groesse angepasst.
 *
 * Farbkodierung der Abschnitte:
 * - Enge Kurven: Rot
 * - Schikanen: Orange
 * - DRS-Zonen: Grün
 * - Normale Abschnitte: Grau
 * - Boxengasse: Gelb
 *
 * @author F1 Simulation Team
 * @version 1.0
 */
public class StreckenView extends Pane {

    private static final double STRECKEN_BREITE = 12.0;
    private static final double AUTO_RADIUS = 8.0;
    private static final double PADDING = 50.0;

    private Canvas canvas;
    private GraphicsContext gc;
    private RennDaten rennDaten;

    /**
     * Erstellt eine neue StreckenView.
     *
     * @param rennDaten Die Renndaten fuer die Visualisierung
     */
    public StreckenView(RennDaten rennDaten) {
        this.rennDaten = rennDaten;

        canvas = new Canvas(800, 500);
        gc = canvas.getGraphicsContext2D();


        getChildren().add(canvas);

        widthProperty().addListener((obs, alt, neu) -> {
            canvas.setWidth(neu.doubleValue());
            zeichneStrecke();
        });
        heightProperty().addListener((obs, alt, neu) -> {
            canvas.setHeight(neu.doubleValue());
            zeichneStrecke();
        });


        zeichneStrecke();
    }

    /**
     * Setzt neue Renndaten und zeichnet die Strecke neu.
     *
     * @param rennDaten Die neuen Renndaten
     */
    public void setRennDaten(RennDaten rennDaten) {
        this.rennDaten = rennDaten;
        zeichneStrecke();
    }

    /**
     * Zeichnet die komplette Strecke mit allen Elementen.
     */
    public void zeichneStrecke() {
        if (rennDaten == null) return;

        double breite = canvas.getWidth();
        double hoehe = canvas.getHeight();


        gc.setFill(Color.rgb(40, 40, 40));
        gc.fillRect(0, 0, breite, hoehe);

        zeichneStreckenabschnitte(breite, hoehe);

        zeichneBoxengasse(breite, hoehe);

        zeichneBeschriftungen(breite, hoehe);

        zeichneAutos(breite, hoehe);

        zeichneLegende(breite, hoehe);
    }

    /**
     * Zeichnet alle Streckenabschnitte.
     */
    private void zeichneStreckenabschnitte(double breite, double hoehe) {
        Rennstrecke strecke = rennDaten.getStrecke();

        for (Streckenabschnitt abschnitt : strecke.getAbschnitte()) {

            if (abschnitt.getId() < 15) {
                zeichneAbschnitt(abschnitt, breite, hoehe);
            }
        }
    }

    /**
     * Zeichnet einen einzelnen Streckenabschnitt.
     */
    private void zeichneAbschnitt(Streckenabschnitt abschnitt, double breite, double hoehe) {

        double x1 = PADDING + abschnitt.getStartX() * (breite - 2 * PADDING);
        double y1 = PADDING + abschnitt.getStartY() * (hoehe - 2 * PADDING);
        double x2 = PADDING + abschnitt.getEndeX() * (breite - 2 * PADDING);
        double y2 = PADDING + abschnitt.getEndeY() * (hoehe - 2 * PADDING);


        Color farbe = getFarbeNachTyp(abschnitt.getTyp());

        gc.setStroke(farbe);
        gc.setLineWidth(STRECKEN_BREITE);
        gc.setLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        gc.strokeLine(x1, y1, x2, y2);

        gc.setStroke(Color.WHITE);
        gc.setLineWidth(1);
        gc.setLineDashes(5, 5);
        gc.strokeLine(x1, y1, x2, y2);
        gc.setLineDashes(null);
    }

    /**
     * Zeichnet die Boxengasse.
     */
    private void zeichneBoxengasse(double breite, double hoehe) {
        Rennstrecke strecke = rennDaten.getStrecke();

        for (int id = 15; id <= 17; id++) {
            Streckenabschnitt abschnitt = strecke.getAbschnitt(id);
            if (abschnitt != null) {
                double x1 = PADDING + abschnitt.getStartX() * (breite - 2 * PADDING);
                double y1 = PADDING + abschnitt.getStartY() * (hoehe - 2 * PADDING);
                double x2 = PADDING + abschnitt.getEndeX() * (breite - 2 * PADDING);
                double y2 = PADDING + abschnitt.getEndeY() * (hoehe - 2 * PADDING);

                gc.setStroke(Color.GOLD);
                gc.setLineWidth(STRECKEN_BREITE - 4);
                gc.strokeLine(x1, y1, x2, y2);
            }
        }


        List<Box> boxen = rennDaten.getBoxen();
        double boxY = PADDING + 0.48 * (hoehe - 2 * PADDING);

        for (Box box : boxen) {
            double boxX = PADDING + (0.25 + box.getPositionInBoxengasse() * 0.02) * (breite - 2 * PADDING);


            gc.setFill(box.getTeam().getFarbe().darker());
            gc.fillRect(boxX - 5, boxY - 10, 10, 20);


            String vollName = box.getTeam().getName();
            String kuerzel = vollName.substring(0, Math.min(vollName.length(), 3)).toUpperCase();
            gc.setFill(Color.WHITE);
            gc.setFont(Font.font("Arial", FontWeight.BOLD, 8));
            gc.fillText(kuerzel, boxX - 8, boxY + 25);
        }
    }

    /**
     * Zeichnet die Beschriftungen der Abschnitte.
     */
    private void zeichneBeschriftungen(double breite, double hoehe) {
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Arial", FontWeight.NORMAL, 10));

        Rennstrecke strecke = rennDaten.getStrecke();

        for (Streckenabschnitt abschnitt : strecke.getAbschnitte()) {

            if (abschnitt.getTyp() == StreckenabschnittTyp.ENGE_KURVE ||
                    abschnitt.getTyp() == StreckenabschnittTyp.SCHIKANE ||
                    abschnitt.getTyp() == StreckenabschnittTyp.DRS_ZONE ||
                    abschnitt.getTyp() == StreckenabschnittTyp.START_ZIEL) {

                double x = PADDING + (abschnitt.getStartX() + abschnitt.getEndeX()) / 2 * (breite - 2 * PADDING);
                double y = PADDING + (abschnitt.getStartY() + abschnitt.getEndeY()) / 2 * (hoehe - 2 * PADDING);


                String name = abschnitt.getName();
                if (name.length() > 15) {
                    name = name.substring(0, 12) + "...";
                }

                double offsetY = -15;
                if (abschnitt.getStartY() < 0.5) {
                    offsetY = 20;
                }

                gc.fillText(name, x - 20, y + offsetY);
            }
        }


        gc.setFill(Color.YELLOW);
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        Streckenabschnitt startZiel = strecke.getAbschnitt(0);
        double startX = PADDING + startZiel.getStartX() * (breite - 2 * PADDING);
        double startY = PADDING + startZiel.getStartY() * (hoehe - 2 * PADDING);
        gc.fillText("START/ZIEL", startX - 30, startY - 20);


        zeichneKarierteFlagge(startX - 5, startY - 40, 30, 15);
    }

    /**
     * Zeichnet eine karierte Flagge.
     */
    private void zeichneKarierteFlagge(double x, double y, double breite, double hoehe) {
        int quadrate = 6;
        double quadBreite = breite / quadrate;
        double quadHoehe = hoehe / 2;

        for (int i = 0; i < quadrate; i++) {
            for (int j = 0; j < 2; j++) {
                if ((i + j) % 2 == 0) {
                    gc.setFill(Color.WHITE);
                } else {
                    gc.setFill(Color.BLACK);
                }
                gc.fillRect(x + i * quadBreite, y + j * quadHoehe, quadBreite, quadHoehe);
            }
        }


        gc.setStroke(Color.BLACK);
        gc.setLineWidth(1);
        gc.strokeRect(x, y, breite, hoehe);
    }

    /**
     * Zeichnet alle Autos auf der Strecke.
     */
    private void zeichneAutos(double breite, double hoehe) {
        List<Auto> autos = rennDaten.getAutos();
        Rennstrecke strecke = rennDaten.getStrecke();

        for (Auto auto : autos) {

            int abschnittId = auto.getAktuellerAbschnittId();
            double fortschritt = auto.getFortschrittImAbschnitt();

            fortschritt = Math.max(0, fortschritt);

            Streckenabschnitt abschnitt = strecke.getAbschnitt(abschnittId);
            if (abschnitt == null) continue;

            double autoX = abschnitt.getStartX() + fortschritt * (abschnitt.getEndeX() - abschnitt.getStartX());
            double autoY = abschnitt.getStartY() + fortschritt * (abschnitt.getEndeY() - abschnitt.getStartY());


            double x = PADDING + autoX * (breite - 2 * PADDING);
            double y = PADDING + autoY * (hoehe - 2 * PADDING);


            zeichneAuto(auto, x, y);
        }
    }

    /**
     * Zeichnet ein einzelnes Auto.
     */
    private void zeichneAuto(Auto auto, double x, double y) {
        Color teamFarbe = auto.getTeam().getFarbe();

        gc.setFill(teamFarbe);
        gc.fillOval(x - AUTO_RADIUS, y - AUTO_RADIUS, AUTO_RADIUS * 2, AUTO_RADIUS * 2);

        gc.setStroke(Color.WHITE);
        gc.setLineWidth(2);
        gc.strokeOval(x - AUTO_RADIUS, y - AUTO_RADIUS, AUTO_RADIUS * 2, AUTO_RADIUS * 2);


        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 8));
        String kuerzel = auto.getFahrer().getKuerzel();
        gc.fillText(kuerzel, x - 8, y + 3);


        if (auto.getStatus() == AutoStatus.IN_BOX ||
                auto.getStatus() == AutoStatus.FAEHRT_IN_BOX ||
                auto.getStatus() == AutoStatus.VERLAESST_BOX) {
            gc.setFill(Color.YELLOW);
            gc.fillOval(x + AUTO_RADIUS - 3, y - AUTO_RADIUS - 3, 6, 6);
        } else if (auto.getStatus() == AutoStatus.WARTET_AUF_ABSCHNITT) {
            gc.setFill(Color.RED);
            gc.fillOval(x + AUTO_RADIUS - 3, y - AUTO_RADIUS - 3, 6, 6);
        }
    }

    /**
     * Zeichnet die Legende.
     */
    private void zeichneLegende(double breite, double hoehe) {
        double legendeX = breite - 150;
        double legendeY = 20;
        double abstand = 18;

        gc.setFill(Color.rgb(60, 60, 60, 0.8));
        gc.fillRect(legendeX - 10, legendeY - 15, 140, 120);

        gc.setFont(Font.font("Arial", FontWeight.BOLD, 11));
        gc.setFill(Color.WHITE);
        gc.fillText("Legende", legendeX, legendeY);

        gc.setFont(Font.font("Arial", FontWeight.NORMAL, 10));

        legendeY += abstand;
        zeichneLegendeneintrag(legendeX, legendeY, Color.rgb(180, 60, 60), "Enge Kurve");

        legendeY += abstand;
        zeichneLegendeneintrag(legendeX, legendeY, Color.ORANGE, "Schikane");

        legendeY += abstand;
        zeichneLegendeneintrag(legendeX, legendeY, Color.rgb(60, 180, 60), "DRS-Zone");

        legendeY += abstand;
        zeichneLegendeneintrag(legendeX, legendeY, Color.GRAY, "Normal");

        legendeY += abstand;
        zeichneLegendeneintrag(legendeX, legendeY, Color.GOLD, "Boxengasse");
    }

    /**
     * Zeichnet einen Legendeneintrag.
     */
    private void zeichneLegendeneintrag(double x, double y, Color farbe, String text) {
        gc.setFill(farbe);
        gc.fillRect(x, y - 8, 20, 8);
        gc.setFill(Color.WHITE);
        gc.fillText(text, x + 25, y);
    }

    /**
     * Gibt die Farbe basierend auf dem Abschnittstyp zurueck.
     */
    private Color getFarbeNachTyp(StreckenabschnittTyp typ) {
        switch (typ) {
            case ENGE_KURVE:
                return Color.rgb(180, 60, 60);  // Rot
            case SCHIKANE:
                return Color.ORANGE;
            case DRS_ZONE:
                return Color.rgb(60, 180, 60);  // Grün
            case START_ZIEL:
                return Color.LIGHTGRAY;
            case PITSTOP_EINFAHRT:
            case BOXENGASSE:
            case PITSTOP_AUSFAHRT:
                return Color.GOLD;
            default:
                return Color.GRAY;
        }
    }
}
