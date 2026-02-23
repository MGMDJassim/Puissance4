package view;

import javax.swing.*;
import java.awt.*;
import model.Game;
import model.GameMode;

public class GameUI extends JFrame {

    private CardLayout cardLayout;
    private JPanel container;
    Game plateau;

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
        GrillePanel grid = new GrillePanel(plateau, mode);
        container.add(grid, "GRID");
        showPanel("GRID");
    }

    public void showPanel(String name) {
        cardLayout.show(container, name);
    }
}
