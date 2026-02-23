package view;

import java.awt.*;
import javax.swing.*;
import model.GameMode;

public class HomePanel extends JPanel {

    JButton startButton;
    JRadioButton hvhButton;
    JRadioButton hvaiButton;
    JRadioButton aivaiButton;

    public HomePanel(GameUI window) {

        startButton = new JButton("Start Game");
        hvhButton = new JRadioButton("Human vs Human", true);
        hvaiButton = new JRadioButton("Human vs AI");
        aivaiButton = new JRadioButton("AI vs AI");

        ButtonGroup group = new ButtonGroup();
        group.add(hvhButton); group.add(hvaiButton); group.add(aivaiButton);

        setLayout(new BorderLayout());

        JPanel centerPanel = new JPanel(new GridBagLayout());
        JPanel buttonsPanel = new JPanel();
        buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.Y_AXIS));

        hvhButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        hvaiButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        aivaiButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        startButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        buttonsPanel.add(hvhButton);
        buttonsPanel.add(hvaiButton);
        buttonsPanel.add(aivaiButton);
        buttonsPanel.add(Box.createVerticalStrut(15));
        buttonsPanel.add(startButton);

        centerPanel.add(buttonsPanel);
        add(centerPanel, BorderLayout.CENTER);

        startButton.addActionListener(e -> {
            GameMode mode = hvhButton.isSelected() ? GameMode.HUMAN_VS_HUMAN
                    : hvaiButton.isSelected() ? GameMode.HUMAN_VS_AI
                    : GameMode.AI_VS_AI;
            window.startGame(mode);
        });
    }
}
