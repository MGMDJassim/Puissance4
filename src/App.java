import gui.GameUI;

public class App {
    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(() -> {
            GameUI ui = new GameUI();
            ui.showUI();
        });
    }
}
