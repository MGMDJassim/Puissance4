package view;

import javax.swing.JFrame;

public class GameUI extends JFrame {

    public GameUI() {
        setTitle("Puissance 4");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 600);
        setLocationRelativeTo(null);
        HomePanel homePanel = new HomePanel();
        add(homePanel);

        setVisible(true);
    }
}
