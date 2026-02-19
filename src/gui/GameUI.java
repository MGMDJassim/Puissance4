package gui;

import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.BorderLayout;

public class GameUI extends JFrame {

    public GameUI() {
        setTitle("Puissance 4");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 600);
        setLocationRelativeTo(null);

        // Placeholder panel where the grid will be drawn later
        JPanel boardPanel = new JPanel();
        add(boardPanel, BorderLayout.CENTER);
    }

    public void showUI() {
        setVisible(true);
    }
}
