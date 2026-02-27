import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import model.RennDaten;
import util.RennLogger;

public class App extends Application {
    private RennDaten rennDaten;

    /**
     * Einstiegspunkt der Anwendung.
     * Diese Methode startet die JavaFX-Laufzeitumgebung.
     * @Vorbedingung Keine
     * @Nachbedingung JavaFX-Anwendung ist gestartet
     * @param args Kommandozeilenargumente (werden nicht verwendet)
     */
    public static void main(String[] args) {
        RennLogger.info("Anwendung wird gestartet...");
        launch(args);
    }

    /**
     * Initialisiert und startet die JavaFX-Anwendung.
     * Diese Methode wird von JavaFX nach dem Start aufgerufen.
     * @Vorbedingung primaryStage darf nicht null sein
     * @Nachbedingung Hauptfenster ist sichtbar und Renndaten initialisiert
     * @param primaryStage Das Hauptfenster der Anwendung
     * @throws Exception wenn die Initialisierung fehlschlaegt
     */
    @Override
    public void start(Stage primaryStage) throws Exception {
        RennLogger.info("Initialisiere Renndaten...");
        rennDaten = new RennDaten();

        RennLogger.info("Strecke: " + rennDaten.getStrecke().getName());
        RennLogger.info("Teams: " + rennDaten.getTeams().size());
        RennLogger.info("Autos: " + rennDaten.getAutos().size());

        // TODO: HauptfensterView erstellen und anzeigen (Phase 5)
        RennLogger.info("GUI-Initialisierung (Phase 5)");

        primaryStage.setTitle("Formel 1 Rennsimulation - Nuerburgring GP");
        primaryStage.setOnCloseRequest(event -> {
            RennLogger.info("Anwendung wird beendet...");
            Platform.exit();
            System.exit(0);
        });

        // Platzhalter bis Phase 5
        RennLogger.info("Anwendung erfolgreich gestartet");
    }

    /**
     * Wird aufgerufen wenn die Anwendung beendet wird.
     * Raeumt Ressourcen auf und beendet alle Threads.
     * @Vorbedingung Keine
     * @Nachbedingung Alle Ressourcen sind freigegeben
     * @throws Exception wenn das Aufraumen fehlschlaegt
     */
    @Override
    public void stop() throws Exception {
        RennLogger.info("Anwendung wird heruntergefahren...");
        // TODO: Threads beenden (Phase 3)
        super.stop();
    }
}
