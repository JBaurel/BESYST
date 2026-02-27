package view;

import javafx.application.Platform;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Panel fuer die Anzeige von Log-Nachrichten waehrend des Rennens.
 * Zeigt nur wichtige Ereignisse wie Ueberholungen, Pitstops und Rundenzeiten.
 *
 * Die TextArea scrollt automatisch nach unten bei neuen Nachrichten.
 * Die Anzahl der angezeigten Nachrichten ist begrenzt um Speicher zu sparen.
 *
 * @author F1 Simulation Team
 * @version 1.0
 */
public class LogPanel extends VBox {

    private static final int MAX_ZEILEN = 500;

    private TextArea textArea;
    private int zeilenZaehler;

    /**
     * Erstellt ein neues LogPanel.
     */
    public LogPanel() {
        textArea = new TextArea();
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setStyle("-fx-font-family: 'Consolas', 'Courier New', monospace; " +
                "-fx-font-size: 11px; " +
                "-fx-control-inner-background: #1e1e1e; " +
                "-fx-text-fill: #d4d4d4;");

        VBox.setVgrow(textArea, Priority.ALWAYS);
        textArea.setPrefHeight(120);

        getChildren().add(textArea);

        zeilenZaehler = 0;
    }

    /**
     * Fuegt eine neue Nachricht zum Log hinzu.
     * Die Methode ist thread-sicher und kann von jedem Thread aufgerufen werden.
     *
     * @param nachricht Die hinzuzufuegende Nachricht
     */
    public void addNachricht(String nachricht) {
        if (nachricht == null || nachricht.isEmpty()) {
            return;
        }

        if (sollAngezeigWerden(nachricht)) {

            if (Platform.isFxApplicationThread()) {
                fuegeNachrichtHinzu(nachricht);
            } else {
                Platform.runLater(() -> fuegeNachrichtHinzu(nachricht));
            }
        }
    }

    /**
     * Prueft ob eine Nachricht im Log angezeigt werden soll.
     * Filtert technische Debug-Nachrichten heraus.
     */
    private boolean sollAngezeigWerden(String nachricht) {

        if (nachricht.contains("UEBERHOLT") ||
                nachricht.contains("Pitstop") ||
                nachricht.contains("Reifenwechsel") ||
                nachricht.contains("beendet Runde") ||
                nachricht.contains("GESTARTET") ||
                nachricht.contains("IM ZIEL") ||
                nachricht.contains("RENNEN") ||
                nachricht.contains("LICHT") ||
                nachricht.contains("Startsequenz") ||
                nachricht.contains("Sieger") ||
                nachricht.contains("P1:") ||
                nachricht.contains("P2:") ||
                nachricht.contains("P3:")) {
            return true;
        }


        if (nachricht.contains("[DEBUG]") ||
                nachricht.contains("SYNC") ||
                nachricht.contains("Monitor") ||
                nachricht.contains("Semaphore") ||
                nachricht.contains("Lock") ||
                nachricht.contains("Thread") && nachricht.contains("gestartet")) {
            return false;
        }


        return nachricht.contains("[INFO]") || nachricht.contains("[WARNING]") || nachricht.contains("[ERROR]");
    }

    /**
     * Fuegt eine Nachricht zur TextArea hinzu.
     * Muss im JavaFX-Thread aufgerufen werden.
     */
    private void fuegeNachrichtHinzu(String nachricht) {
        // Bei zu vielen Zeilen alte loeschen
        if (zeilenZaehler > MAX_ZEILEN) {
            String text = textArea.getText();
            int ersteZeileEnde = text.indexOf('\n');
            if (ersteZeileEnde > 0) {
                textArea.setText(text.substring(ersteZeileEnde + 1));
            }
            zeilenZaehler--;
        }

        textArea.appendText(nachricht + "\n");
        zeilenZaehler++;

        // Automatisch nach unten scrollen
        textArea.setScrollTop(Double.MAX_VALUE);
    }

    /**
     * Leert das Log-Panel.
     */
    public void leeren() {
        textArea.clear();
        zeilenZaehler = 0;
    }

    /**
     * Gibt den aktuellen Log-Text zurueck.
     *
     * @return Der gesamte Log-Text
     */
    public String getText() {
        return textArea.getText();
    }
}
