import javafx.application.Application;
import util.RennLogger;
import view.HauptFenster;

public class Main {
    public static void main(String[] args) {
        RennLogger.info("============================================");
        RennLogger.info("   F1 Rennsimulation - Nuerburgring GP");
        RennLogger.info("============================================");
        RennLogger.info("Starte Anwendung...");


        Application.launch(HauptFenster.class, args);
    }
}
