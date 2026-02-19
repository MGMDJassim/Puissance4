package view;

import java.awt.*;
import javax.swing.*;

public class HomePanel extends JPanel {

    JButton startButton;
    JButton bdButton;

    public HomePanel() {

        startButton = new JButton("Start Game");
        bdButton = new JButton("Board Size");

        setLayout(new BorderLayout());

        JPanel centerPanel = new JPanel(new GridBagLayout());

        JPanel buttonsPanel = new JPanel();
        buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.Y_AXIS));

        startButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        bdButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        buttonsPanel.add(startButton);
        buttonsPanel.add(Box.createVerticalStrut(15)); 
        buttonsPanel.add(bdButton);

        centerPanel.add(buttonsPanel);

        add(centerPanel, BorderLayout.CENTER);
    }
}
