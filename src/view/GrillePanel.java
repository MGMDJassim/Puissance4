package view;

import javax.swing.*;
import java.awt.*;
import model.Game;
import model.GameMode;
import model.MinimaxAI;
import model.DBHelper;
import model.PartieDAO;
import controller.ControllerJeu;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class GrillePanel extends JPanel {

    JPanel menuPanel;
    JPanel boardPanel;
    Game plateau;
    ControllerJeu controller;
    GameMode mode;

    public GrillePanel(Game plateau, GameMode mode) {
        this.plateau = plateau;
        this.mode = mode;
        this.controller = new ControllerJeu(plateau);

        setLayout(new BorderLayout());
        menuPanel = new JPanel();
        menuPanel.setPreferredSize(new Dimension(200, 0)); // largeur fixe
        menuPanel.setBackground(Color.DARK_GRAY);
        menuPanel.add(new JLabel("Menu"));

        BoardCanvas canvas = new BoardCanvas(plateau);
        canvas.setBackground(new Color(30, 144, 255));

        JPanel boardContainer = new JPanel(new BorderLayout());
        int cols = plateau.getCols();
        JPanel topButtons = new JPanel(new GridLayout(1, cols, 4, 0));
        for (int c = 0; c < cols; c++) {
            JButton btn = new JButton(Integer.toString(c + 1));
            final int colIndex = c; // 0-based for Game.drop
            btn.addActionListener(e -> {
                // allow human move depending on mode and current player
                if (plateau.isGameOver()) return;
                if (mode == GameMode.HUMAN_VS_AI && plateau.getCurrentPlayer() != 1) return;
                if (mode == GameMode.AI_VS_AI) return;
                controller.playColumn(colIndex);
                canvas.repaint();
                if (plateau.isGameOver()) savePartie(canvas);
                runAIMoveIfNeeded(canvas);
            });
            topButtons.add(btn);
        }
        topButtons.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        boardContainer.add(topButtons, BorderLayout.NORTH);
        boardContainer.add(canvas, BorderLayout.CENTER);

        boardPanel = boardContainer;

        add(menuPanel, BorderLayout.WEST);
        add(boardPanel, BorderLayout.CENTER);

        // Connexion DB : initialise PartieDAO (lookup + insert sur situation/partie)
        try {
            DBHelper helper = new DBHelper("localhost", 5432, "puissance4", "postgres", "postgre");
            canvas.putClientProperty("dao", new PartieDAO(helper));
        } catch (Exception ex) {
            System.err.println("DB non disponible : " + ex.getMessage());
        }

        if (mode == GameMode.AI_VS_AI) startAIVsAITimer(canvas);
        else runAIMoveIfNeeded(canvas);
    }

    private void runAIMoveIfNeeded(BoardCanvas canvas) {
        if (plateau.isGameOver()) return;
        if (mode == GameMode.HUMAN_VS_AI && plateau.getCurrentPlayer() == 2) {
            // schedule AI move with small delay
            Timer t = new Timer(300, e -> {
                MinimaxAI ai = new MinimaxAI(2, 3);
                int col = ai.chooseColumn(plateau);
                controller.playColumn(col);
                canvas.repaint();
                if (plateau.isGameOver()) savePartie(canvas);
                ((Timer) e.getSource()).stop();
            });
            t.setRepeats(false);
            t.start();
        }
    }

    private void startAIVsAITimer(BoardCanvas canvas) {
        Timer t = new Timer(300, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (plateau.isGameOver()) { ((Timer) e.getSource()).stop(); return; }
                int me = plateau.getCurrentPlayer();
                MinimaxAI ai = new MinimaxAI(me, 2);
                int col = ai.chooseColumn(plateau);
                controller.playColumn(col);
                canvas.repaint();
                if (plateau.isGameOver()) savePartie(canvas);
            }
        });
        t.start();
    }

    private void savePartie(BoardCanvas canvas) {
        Object o = canvas.getClientProperty("dao");
        if (o instanceof PartieDAO) ((PartieDAO) o).savePartie(plateau, mode);
    }
    
    private static class BoardCanvas extends JPanel {
        private final int rows;
        private final int cols;
        private final Game plateau;

        BoardCanvas(Game plateau) {
            this.plateau = plateau;
            this.rows = plateau.getRows();
            this.cols = plateau.getCols();
            setPreferredSize(new Dimension(420, 360));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();

                // calculer taille des cellules occupant tout le canvas
                int cellW = w / cols;
                int cellH = h / rows;
                int diameter = Math.min(cellW, cellH) - 10; // marge intérieure
                if (diameter < 4) diameter = Math.max(2, Math.min(cellW, cellH) - 2);

                // dessiner le fond du plateau
                g2.setColor(getBackground());
                g2.fillRect(0, 0, w, h);

                // dessiner les cases (cercles) en fonction de l'état du plateau
                for (int r = 0; r < rows; r++) {
                    for (int c = 0; c < cols; c++) {
                        int cx = c * cellW + (cellW - diameter) / 2;
                        int cy = r * cellH + (cellH - diameter) / 2;
                        int cell = plateau.getCell(r, c);
                        if (cell == 0) g2.setColor(Color.WHITE);
                        else if (cell == 1) g2.setColor(Color.RED);
                        else g2.setColor(Color.YELLOW);
                        g2.fillOval(cx, cy, diameter, diameter);
                        g2.setColor(Color.DARK_GRAY);
                        g2.drawOval(cx, cy, diameter, diameter);
                    }
                }
            } finally {
                g2.dispose();
            }
        }
    }
}
