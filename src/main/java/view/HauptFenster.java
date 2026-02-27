package view;

import controller.RennController;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import model.Auto;
import model.Rennergebnis;
import model.enumType.RennStatus;
import util.Konfiguration;
import util.RennLogger;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Hauptfenster der F1-Rennsimulation.
 * Diese Klasse erstellt das komplette GUI-Layout mit:
 * - Streckenvisualisierung (Canvas)
 * - Rennstand-Tabelle
 * - Steuerungspanel (Buttons, Slider)
 * - Log-Panel
 * - Startampel-Anzeige
 *
 * Das Layout ist programmatisch in Java erstellt (ohne FXML).
 *
 * @author F1 Simulation Team
 * @version 1.0
 */
public class HauptFenster extends Application {

    private static final int FENSTER_BREITE = 1400;
    private static final int FENSTER_HOEHE = 700;

    private RennController controller;

    private StreckenView streckenView;
    private RennstandTabelle rennstandTabelle;
    private LogPanel logPanel;
    private StartAmpelView startAmpelView;


    private Button startButton;
    private Button pauseButton;
    private Button stoppButton;
    private Button neuesRennenButton;
    private ComboBox<String> geschwindigkeitCombo;
    private Slider rundenSlider;
    private Label rundenLabel;
    private Label statusLabel;


    private Timer updateTimer;

    @Override
    public void start(Stage primaryStage) {

        controller = new RennController();

        BorderPane hauptLayout = erstelleHauptLayout();


        Scene scene = new Scene(hauptLayout, FENSTER_BREITE, FENSTER_HOEHE);


        primaryStage.setTitle("F1 Rennsimulation - Nürburgring");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(1200);
        primaryStage.setMinHeight(700);

        primaryStage.setOnCloseRequest(event -> {
            beenden();
        });


        controller.setEventListener(new RennController.RennEventListener() {
            @Override
            public void onLogNachricht(String nachricht) {
                Platform.runLater(() -> logPanel.addNachricht(nachricht));
            }

            @Override
            public void onRennstandUpdate() {
                Platform.runLater(() -> aktualisiereRennstand());
            }

            @Override
            public void onStartLicht(int nummer) {
                Platform.runLater(() -> startAmpelView.setzeLicht(nummer));
            }

            @Override
            public void onStartFreigabe() {
                Platform.runLater(() -> {
                    startAmpelView.alleAus();
                    statusLabel.setText("Status: Rennen läuft");
                });
            }

            @Override
            public void onRennende(List<Rennergebnis> ergebnisse) {
                Platform.runLater(() -> {
                    stoppeUpdateTimer();
                    statusLabel.setText("Status: Rennen beendet");
                    zeigeErgebnisDialog(ergebnisse);
                    aktualisiereButtons(false, true);
                });
            }
        });


        primaryStage.show();

        RennLogger.info("GUI gestartet");
    }

    /**
     * Erstellt das Haupt-Layout mit allen Komponenten.
     */
    private BorderPane erstelleHauptLayout() {
        BorderPane layout = new BorderPane();
        layout.setPadding(new Insets(10));

        // Oben: Steuerungspanel und Ampel
        HBox topPanel = erstelleTopPanel();
        layout.setTop(topPanel);

        // Mitte: Streckenvisualisierung
        streckenView = new StreckenView(controller.getRennDaten());
        VBox centerBox = new VBox(10);
        centerBox.getChildren().add(streckenView);
        VBox.setVgrow(streckenView, Priority.ALWAYS);
        layout.setCenter(centerBox);

        // Rechts: Rennstand-Tabelle
        rennstandTabelle = new RennstandTabelle();
        VBox rechtsPanel = new VBox(10);
        rechtsPanel.setPadding(new Insets(0, 0, 0, 10));
        Label rennstandLabel = new Label("Rennstand");
        rennstandLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        rechtsPanel.getChildren().addAll(rennstandLabel, rennstandTabelle);
        rechtsPanel.setPrefWidth(400);
        layout.setRight(rechtsPanel);

        // Unten: Log-Panel
        logPanel = new LogPanel();
        VBox untenPanel = new VBox(5);
        untenPanel.setPadding(new Insets(10, 0, 0, 0));
        Label logLabel = new Label("Renn-Log");
        logLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        untenPanel.getChildren().addAll(logLabel, logPanel);
        untenPanel.setPrefHeight(150);
        layout.setBottom(untenPanel);

        return layout;
    }

    /**
     * Erstellt das obere Panel mit Steuerung und Ampel.
     */
    private HBox erstelleTopPanel() {
        HBox topPanel = new HBox(20);
        topPanel.setAlignment(Pos.CENTER_LEFT);
        topPanel.setPadding(new Insets(0, 0, 10, 0));

        // Steuerungsbuttons
        VBox buttonBox = erstelleSteuerungsButtons();

        // Einstellungen
        VBox einstellungenBox = erstelleEinstellungen();

        // Startampel
        startAmpelView = new StartAmpelView();

        // Status
        statusLabel = new Label("Status: Bereit");
        statusLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        // Spacer
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        topPanel.getChildren().addAll(
                buttonBox,
                new Separator(javafx.geometry.Orientation.VERTICAL),
                einstellungenBox,
                new Separator(javafx.geometry.Orientation.VERTICAL),
                startAmpelView,
                spacer,
                statusLabel
        );

        return topPanel;
    }

    /**
     * Erstellt die Steuerungsbuttons.
     */
    private VBox erstelleSteuerungsButtons() {
        VBox box = new VBox(5);

        HBox buttonReihe = new HBox(10);

        startButton = new Button("▶ Start");
        startButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
        startButton.setPrefWidth(100);
        startButton.setOnAction(e -> starteRennen());

        pauseButton = new Button("⏸ Pause");
        pauseButton.setPrefWidth(100);
        pauseButton.setDisable(true);
        pauseButton.setOnAction(e -> togglePause());

        stoppButton = new Button("⏹ Stopp");
        stoppButton.setStyle("-fx-background-color: #f44336; -fx-text-fill: white;");
        stoppButton.setPrefWidth(100);
        stoppButton.setDisable(true);
        stoppButton.setOnAction(e -> stoppeRennen());

        neuesRennenButton = new Button("🔄 Neues Rennen");
        neuesRennenButton.setPrefWidth(120);
        neuesRennenButton.setVisible(false);
        neuesRennenButton.setOnAction(e -> neuesRennen());

        buttonReihe.getChildren().addAll(startButton, pauseButton, stoppButton, neuesRennenButton);

        Label steuerungLabel = new Label("Steuerung");
        steuerungLabel.setStyle("-fx-font-weight: bold;");

        box.getChildren().addAll(steuerungLabel, buttonReihe);

        return box;
    }

    /**
     * Erstellt die Einstellungs-Steuerelemente.
     */
    private VBox erstelleEinstellungen() {
        VBox box = new VBox(5);

        HBox einstellungenReihe = new HBox(20);

        // Geschwindigkeit
        VBox geschwindigkeitBox = new VBox(3);
        Label geschwindigkeitLabel = new Label("Geschwindigkeit:");
        geschwindigkeitCombo = new ComboBox<>();
        geschwindigkeitCombo.getItems().addAll("1x", "2x", "5x", "10x");
        geschwindigkeitCombo.setValue("1x");
        geschwindigkeitCombo.setOnAction(e -> aendereGeschwindigkeit());
        geschwindigkeitBox.getChildren().addAll(geschwindigkeitLabel, geschwindigkeitCombo);

        // Rundenanzahl
        VBox rundenBox = new VBox(3);
        rundenLabel = new Label("Runden: " + Konfiguration.DEFAULT_RUNDENANZAHL);
        rundenSlider = new Slider(
                Konfiguration.MIN_RUNDENANZAHL,
                Konfiguration.MAX_RUNDENANZAHL,
                Konfiguration.DEFAULT_RUNDENANZAHL
        );
        rundenSlider.setShowTickLabels(true);
        rundenSlider.setShowTickMarks(true);
        rundenSlider.setMajorTickUnit(10);
        rundenSlider.setBlockIncrement(5);
        rundenSlider.setPrefWidth(150);
        rundenSlider.valueProperty().addListener((obs, alt, neu) -> {
            int runden = neu.intValue();
            rundenLabel.setText("Runden: " + runden);
            controller.setRundenanzahl(runden);
        });
        rundenBox.getChildren().addAll(rundenLabel, rundenSlider);

        einstellungenReihe.getChildren().addAll(geschwindigkeitBox, rundenBox);

        Label einstellungenLabel = new Label("Einstellungen");
        einstellungenLabel.setStyle("-fx-font-weight: bold;");

        box.getChildren().addAll(einstellungenLabel, einstellungenReihe);

        return box;
    }

    /**
     * Startet ein neues Rennen.
     */
    private void starteRennen() {
        RennLogger.info("Starte Rennen...");

        rundenSlider.setDisable(true);

        aktualisiereButtons(true, false);

        controller.initialisieren();


        streckenView.setRennDaten(controller.getRennDaten());


        rennstandTabelle.aktualisiereRennstand(controller.getRennDaten().getRennreihenfolge(),
                controller.getRennDaten());


        starteUpdateTimer();


        controller.starteRennen();

        statusLabel.setText("Status: Startsequenz...");
    }

    /**
     * Pausiert oder setzt das Rennen fort.
     */
    private void togglePause() {
        if (controller.getRennDaten().getStatus() == RennStatus.PAUSIERT) {
            controller.fortsetzen();
            pauseButton.setText("⏸ Pause");
            statusLabel.setText("Status: Rennen läuft");
        } else {
            controller.pausieren();
            pauseButton.setText("▶ Fortsetzen");
            statusLabel.setText("Status: Pausiert");
        }
    }

    /**
     * Stoppt das Rennen vorzeitig.
     */
    private void stoppeRennen() {
        RennLogger.info("Stoppe Rennen...");

        stoppeUpdateTimer();
        controller.stoppeRennen();

        statusLabel.setText("Status: Abgebrochen");
        aktualisiereButtons(false, true);
    }

    /**
     * Bereitet ein neues Rennen vor.
     */
    private void neuesRennen() {
        RennLogger.info("Bereite neues Rennen vor...");

        startAmpelView.reset();
        logPanel.leeren();


        rundenSlider.setDisable(false);


        aktualisiereButtons(false, false);
        neuesRennenButton.setVisible(false);


        controller = new RennController();
        controller.setEventListener(new RennController.RennEventListener() {
            @Override
            public void onLogNachricht(String nachricht) {
                Platform.runLater(() -> logPanel.addNachricht(nachricht));
            }

            @Override
            public void onRennstandUpdate() {
                Platform.runLater(() -> aktualisiereRennstand());
            }

            @Override
            public void onStartLicht(int nummer) {
                Platform.runLater(() -> startAmpelView.setzeLicht(nummer));
            }

            @Override
            public void onStartFreigabe() {
                Platform.runLater(() -> {
                    startAmpelView.alleAus();
                    statusLabel.setText("Status: Rennen läuft");
                });
            }

            @Override
            public void onRennende(List<Rennergebnis> ergebnisse) {
                Platform.runLater(() -> {
                    stoppeUpdateTimer();
                    statusLabel.setText("Status: Rennen beendet");
                    zeigeErgebnisDialog(ergebnisse);
                    aktualisiereButtons(false, true);
                });
            }
        });


        controller.setRundenanzahl((int) rundenSlider.getValue());


        streckenView.setRennDaten(controller.getRennDaten());
        streckenView.zeichneStrecke();


        rennstandTabelle.aktualisiereRennstand(controller.getRennDaten().getRennreihenfolge(),
                controller.getRennDaten());

        statusLabel.setText("Status: Bereit");
    }

    /**
     * Aendert die Simulationsgeschwindigkeit.
     */
    private void aendereGeschwindigkeit() {
        String auswahl = geschwindigkeitCombo.getValue();
        double geschwindigkeit = Double.parseDouble(auswahl.replace("x", ""));
        controller.setSimulationsGeschwindigkeit(geschwindigkeit);
    }

    /**
     * Aktualisiert den Zustand der Buttons.
     */
    private void aktualisiereButtons(boolean rennLaeuft, boolean rennBeendet) {
        startButton.setDisable(rennLaeuft || rennBeendet);
        pauseButton.setDisable(!rennLaeuft || rennBeendet);
        stoppButton.setDisable(!rennLaeuft || rennBeendet);
        neuesRennenButton.setVisible(rennBeendet);
    }

    /**
     * Startet den Timer fuer regelmaessige GUI-Updates.
     */
    private void starteUpdateTimer() {
        if (updateTimer != null) {
            updateTimer.cancel();
        }

        updateTimer = new Timer("GUI-Update-Timer", true);
        updateTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> {
                    aktualisiereGUI();
                });
            }
        }, 0, Konfiguration.GUI_UPDATE_INTERVALL_MS);
    }

    /**
     * Stoppt den Update-Timer.
     */
    private void stoppeUpdateTimer() {
        if (updateTimer != null) {
            updateTimer.cancel();
            updateTimer = null;
        }
    }

    /**
     * Aktualisiert alle GUI-Komponenten.
     */
    private void aktualisiereGUI() {
        if (controller == null || controller.getRennDaten() == null) {
            return;
        }

        streckenView.zeichneStrecke();

        aktualisiereRennstand();
    }

    /**
     * Aktualisiert die Rennstand-Tabelle.
     */
    private void aktualisiereRennstand() {
        if (controller != null && controller.getRennDaten() != null) {
            List<Auto> reihenfolge = controller.getRennDaten().getRennreihenfolge();
            rennstandTabelle.aktualisiereRennstand(reihenfolge, controller.getRennDaten());
        }
    }

    /**
     * Zeigt den Ergebnis-Dialog nach dem Rennen.
     */
    private void zeigeErgebnisDialog(List<Rennergebnis> ergebnisse) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Rennergebnis");
        alert.setHeaderText("Das Rennen ist beendet!");

        StringBuilder sb = new StringBuilder();
        sb.append("=== ENDERGEBNIS ===\n\n");

        int position = 1;
        for (Auto auto : controller.getRennDaten().getRennreihenfolge()) {
            sb.append(String.format("P%d: %s (%s)\n",
                    position++,
                    auto.getFahrer().getName(),
                    auto.getTeam().getName()));

            if (position > 10) break;  // Nur Top 10 anzeigen
        }

        alert.setContentText(sb.toString());
        alert.showAndWait();
    }

    /**
     * Beendet die Anwendung sauber.
     */
    private void beenden() {
        RennLogger.info("Beende Anwendung...");

        stoppeUpdateTimer();

        if (controller != null && controller.isRennLaeuft()) {
            controller.stoppeRennen();
        }

        Platform.exit();
        System.exit(0);
    }

    /**
     * Main-Methode zum Starten der Anwendung.
     */
    public static void main(String[] args) {
        launch(args);
    }
}
