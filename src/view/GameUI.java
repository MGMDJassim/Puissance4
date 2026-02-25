package view;

import javax.swing.*;
import java.awt.*;
import model.Game;
import model.GameMode;
import model.DBHelper;

public class GameUI extends JFrame {

    private CardLayout cardLayout;
    private JPanel container;
    Game plateau;
    private GrillePanel currentGrid;

    public GameUI() {
        setTitle("Puissance 4");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 800);
        setLocationRelativeTo(null);

        cardLayout = new CardLayout();
        container = new JPanel(cardLayout);

        // Ajout des panels
        container.add(new HomePanel(this), "HOME");

        add(container);
        setVisible(true);
    }

    public void startGame(GameMode mode) {
        Game plateau = new Game();
        currentGrid = new GrillePanel(plateau, mode, this);
        container.add(currentGrid, "GRID");
        showPanel("GRID");
    }

    public void undoLastMove() {
        if (currentGrid != null) {
            currentGrid.undo();
        } else {
            JOptionPane.showMessageDialog(this, "Aucune partie en cours.", "Undo", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    public void showPanel(String name) {
        cardLayout.show(container, name);
    }

    public void showDatabase() {
        try {
            DBHelper helper = new DBHelper("localhost", 5432, "puissance4", "postgres", "postgre");
            DBViewer viewer = new DBViewer(helper);
            viewer.setVisible(true);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Impossible de se connecter à la base de données.\n" + ex.getMessage(),
                    "Erreur DB", JOptionPane.ERROR_MESSAGE);
        }
    }
}
