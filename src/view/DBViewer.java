package view;

import model.DBHelper;
import model.Game;
import model.PartieDAO;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Visualiseur de la base de données Puissance 4.
 *
 * Layout :
 *   [  Tableau des parties  |  Plateau  Miroir  ]
 *   [                       |  Liste coups       ]
 *   [ Status bar            |  << Début Prev Coup Next Fin >> ]
 */
public class DBViewer extends JFrame {

    private static final int ROWS = 9, COLS = 9;

    // ── DB ──
    private final DBHelper dbHelper;

    // ── Tableau ──
    private final DefaultTableModel tableModel;
    private final JTable dataTable;
    private final JLabel statusLabel;

    // ── Visualiseur ──
    private final BoardRenderer boardNormal = new BoardRenderer("Plateau");
    private final BoardRenderer boardMirror = new BoardRenderer("Miroir");
    private final DefaultListModel<String> movesModel = new DefaultListModel<>();
    private final JList<String> movesList = new JList<>(movesModel);
    private final JLabel moveLabel = new JLabel("Coup : – / –", SwingConstants.CENTER);
    private final JButton btnDebut  = new JButton("|<");
    private final JButton btnPrev   = new JButton("< Prev");
    private final JButton btnNext   = new JButton("Next >");
    private final JButton btnFin    = new JButton(">|");

    // ── État navigation ──
    private final List<Integer> moves = new ArrayList<>(); // colonnes 1-based
    private int currentIndex = 0;

    // ──────────────────────────────────────────────────────────
    public DBViewer(DBHelper dbHelper) {
        super("Base de données — Puissance 4");
        this.dbHelper = dbHelper;

        setSize(1400, 700);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(4, 4));

        // ── Tableau des parties ──
        tableModel = new DefaultTableModel(
                new String[]{"id", "coups", "winner", "mode", "situation_id"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        dataTable = new JTable(tableModel);
        dataTable.setAutoCreateRowSorter(true);
        dataTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        dataTable.setRowHeight(22);
        dataTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) onRowSelected();
        });
        JScrollPane tableScroll = new JScrollPane(dataTable);
        tableScroll.setBorder(BorderFactory.createTitledBorder("Parties"));
        tableScroll.setPreferredSize(new Dimension(480, 0));

        // ── Panneau de visualisation ──
        JPanel vizPanel = buildVizPanel();

        // ── Split gauche / droite ──
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tableScroll, vizPanel);
        split.setResizeWeight(0.35);
        split.setContinuousLayout(true);
        add(split, BorderLayout.CENTER);

        // ── Status bar + bouton refresh ──
        statusLabel = new JLabel("Chargement...", SwingConstants.LEFT);
        statusLabel.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
        JButton btnRefresh = new JButton("⟳ Refresh");
        btnRefresh.addActionListener(e -> loadFromDb());
        JButton btnImport = new JButton("📂 Importer fichier");
        btnImport.addActionListener(e -> importFromFile());
        JPanel south = new JPanel(new BorderLayout());
        south.add(statusLabel, BorderLayout.CENTER);
        JPanel southButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        southButtons.add(btnImport);
        southButtons.add(btnRefresh);
        south.add(southButtons, BorderLayout.EAST);
        add(south, BorderLayout.SOUTH);

        loadFromDb();
    }

    // ══════════════════════════════════════════════════════════
    // Construction du panneau de visualisation
    // ══════════════════════════════════════════════════════════

    private JPanel buildVizPanel() {
        // Plateaux côte à côte
        JPanel boards = new JPanel(new GridLayout(1, 2, 6, 0));
        boards.add(boardNormal);
        boards.add(boardMirror);

        // Liste des coups
        movesList.setFixedCellHeight(20);
        movesList.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane movesScroll = new JScrollPane(movesList);
        movesScroll.setBorder(BorderFactory.createTitledBorder("Coups joués"));
        movesScroll.setPreferredSize(new Dimension(0, 150));

        // Barre de navigation
        for (JButton b : new JButton[]{btnDebut, btnPrev, btnNext, btnFin}) b.setEnabled(false);
        btnDebut.addActionListener(e -> goTo(0));
        btnPrev.addActionListener(e  -> goTo(currentIndex - 1));
        btnNext.addActionListener(e  -> goTo(currentIndex + 1));
        btnFin.addActionListener(e   -> goTo(moves.size()));

        moveLabel.setFont(moveLabel.getFont().deriveFont(Font.BOLD, 13f));
        JPanel nav = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 4));
        nav.add(btnDebut); nav.add(btnPrev);
        nav.add(moveLabel);
        nav.add(btnNext);  nav.add(btnFin);

        JPanel viz = new JPanel(new BorderLayout(4, 4));
        viz.setBorder(BorderFactory.createTitledBorder("Visualisation"));
        viz.add(boards,      BorderLayout.CENTER);
        viz.add(movesScroll, BorderLayout.SOUTH);
        viz.add(nav,         BorderLayout.NORTH);
        return viz;
    }

    // ══════════════════════════════════════════════════════════
    // Chargement des données
    // ══════════════════════════════════════════════════════════

    private void importFromFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Importer des parties (fichiers nommés par leur séquence)");
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Fichiers texte (*.txt)", "txt"));
        chooser.setMultiSelectionEnabled(true);
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File[] files = chooser.getSelectedFiles();
        try {
            PartieDAO dao = new PartieDAO(dbHelper);
            int count = dao.importFromFiles(files);
            statusLabel.setText(count + " partie(s) importée(s) sur " + files.length + " fichier(s)");
            loadFromDb();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                "Erreur lors de l'importation :\n" + ex.getMessage(),
                "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadFromDb() {
        tableModel.setRowCount(0);
        try (Connection c = dbHelper.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(
                 "SELECT id, sequence, winner, mode, situation_id " +
                 "FROM partie ORDER BY id DESC LIMIT 500")) {
            int n = 0;
            while (rs.next()) {
                String seq = rs.getString("sequence");
                int nbCoups = (seq != null) ? seq.replaceAll("\\D", "").length() : 0;
                tableModel.addRow(new Object[]{
                    rs.getObject("id"),
                    nbCoups + " coups  [" + (seq != null ? seq : "") + "]",
                    rs.getObject("winner"),
                    rs.getString("mode"),
                    rs.getObject("situation_id")
                });
                n++;
            }
            statusLabel.setText(n + " partie(s) en base");
            // ajuster largeurs
            int[] widths = {50, 220, 60, 150, 80};
            for (int i = 0; i < widths.length; i++)
                dataTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        } catch (Exception ex) {
            statusLabel.setText("Erreur DB : " + ex.getMessage());
        }
        clearViz();
    }

    // ══════════════════════════════════════════════════════════
    // Sélection d'une ligne → chargement immédiat
    // ══════════════════════════════════════════════════════════

    private void onRowSelected() {
        int view = dataTable.getSelectedRow();
        if (view < 0) { clearViz(); return; }
        int row = dataTable.convertRowIndexToModel(view);

        // La colonne 1 contient "N coups  [sequence]"
        Object cell = tableModel.getValueAt(row, 1);
        if (cell == null) { clearViz(); return; }
        String raw = cell.toString();
        // extraire ce qui est entre [ ]
        int lb = raw.indexOf('['), rb = raw.lastIndexOf(']');
        String sequence = (lb >= 0 && rb > lb) ? raw.substring(lb + 1, rb).trim() : raw.trim();

        moves.clear();
        for (char ch : sequence.toCharArray())
            if (Character.isDigit(ch)) moves.add(Character.getNumericValue(ch)); // 1-based

        currentIndex = 0;
        goTo(moves.size()); // affiche la position finale directement
    }

    // ══════════════════════════════════════════════════════════
    // Navigation
    // ══════════════════════════════════════════════════════════

    private void goTo(int idx) {
        if (moves.isEmpty()) return;
        currentIndex = Math.max(0, Math.min(idx, moves.size()));
        renderBoards();
        updateMovesUI();
        btnDebut.setEnabled(currentIndex > 0);
        btnPrev.setEnabled(currentIndex > 0);
        btnNext.setEnabled(currentIndex < moves.size());
        btnFin.setEnabled(currentIndex < moves.size());
        moveLabel.setText("Coup : " + currentIndex + " / " + moves.size());
    }

    private void updateMovesUI() {
        movesModel.clear();
        for (int i = 0; i < moves.size(); i++) {
            String arrow = (i == currentIndex - 1) ? "▶" : " ";
            String player = (i % 2 == 0) ? "J1" : "J2";
            movesModel.addElement(String.format("%s %2d. %s → col %d", arrow, i + 1, player, moves.get(i)));
        }
        if (currentIndex > 0)
            movesList.ensureIndexIsVisible(currentIndex - 1);
    }

    private void renderBoards() {
        int[][] board = new int[ROWS][COLS];
        int player = 1;
        for (int i = 0; i < currentIndex; i++) {
            Game.dropOnBoard(board, moves.get(i) - 1, player);
            player = 3 - player;
        }

        // Vérifier victoire sur le plateau courant
        int[][] winPos = null;
        if (currentIndex > 0) {
            int lastCol = moves.get(currentIndex - 1) - 1;
            // trouver la ligne du dernier jeton
            for (int r = 0; r < ROWS; r++) {
                if (board[r][lastCol] != 0) {
                    if (Game.checkWinOnBoard(board, r, lastCol, 4)) {
                        winPos = collectWin(board, r, lastCol);
                    }
                    break;
                }
            }
        }

        boardNormal.setBoard(board, winPos);

        int[][] mirror = new int[ROWS][COLS];
        for (int r = 0; r < ROWS; r++)
            for (int c = 0; c < COLS; c++)
                mirror[r][c] = board[r][COLS - 1 - c];
        boardMirror.setBoard(mirror, null);
    }

    /** Collecte les 4 cases gagnantes autour de (r,c). */
    private int[][] collectWin(int[][] board, int startR, int startC) {
        int val = board[startR][startC];
        int[][] dirs = {{0,1},{1,0},{1,1},{1,-1}};
        for (int[] d : dirs) {
            List<int[]> cells = new ArrayList<>();
            for (int k = -3; k <= 3; k++) {
                int r = startR + k * d[0], c2 = startC + k * d[1];
                if (r >= 0 && r < ROWS && c2 >= 0 && c2 < COLS && board[r][c2] == val)
                    cells.add(new int[]{r, c2});
                else cells.clear();
                if (cells.size() == 4) return cells.toArray(new int[0][]);
            }
        }
        return null;
    }

    private void clearViz() {
        moves.clear();
        currentIndex = 0;
        movesModel.clear();
        moveLabel.setText("Coup : – / –");
        boardNormal.setBoard(null, null);
        boardMirror.setBoard(null, null);
        for (JButton b : new JButton[]{btnDebut, btnPrev, btnNext, btnFin}) b.setEnabled(false);
    }

    // ══════════════════════════════════════════════════════════
    // Rendu du plateau
    // ══════════════════════════════════════════════════════════

    static class BoardRenderer extends JPanel {
        private int[][] board;
        private int[][] wins;

        BoardRenderer(String title) {
            setBorder(BorderFactory.createTitledBorder(title));
            setBackground(new Color(0, 80, 180));
            setPreferredSize(new Dimension(360, 360));
        }

        void setBoard(int[][] b, int[][] w) { board = b; wins = w; repaint(); }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int rows = ROWS, cols = COLS;
            int inset = 6;
            int cellW = (getWidth()  - inset * 2) / cols;
            int cellH = (getHeight() - inset * 2) / rows;
            int diam  = Math.min(cellW, cellH) - 6;

            // Grille de fond
            g2.setColor(new Color(0, 60, 150));
            g2.fillRoundRect(inset - 3, inset - 3,
                    cols * cellW + 6, rows * cellH + 6, 12, 12);

            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    int cx = inset + c * cellW + (cellW - diam) / 2;
                    int cy = inset + r * cellH + (cellH - diam) / 2;
                    int val = (board != null) ? board[r][c] : 0;

                    // Ombre
                    g2.setColor(new Color(0, 0, 0, 60));
                    g2.fillOval(cx + 2, cy + 2, diam, diam);

                    // Jeton
                    if (val == 1)      g2.setColor(new Color(220, 40, 40));
                    else if (val == 2) g2.setColor(new Color(240, 200, 0));
                    else               g2.setColor(new Color(200, 220, 255, 180));
                    g2.fillOval(cx, cy, diam, diam);

                    // Contour
                    g2.setColor(new Color(0, 0, 0, 100));
                    g2.drawOval(cx, cy, diam, diam);
                }
            }

            // Surbrillance victoire
            if (wins != null) {
                g2.setColor(new Color(0, 255, 80));
                g2.setStroke(new BasicStroke(3));
                for (int[] p : wins) {
                    int cx = inset + p[1] * cellW + (cellW - diam) / 2;
                    int cy = inset + p[0] * cellH + (cellH - diam) / 2;
                    g2.drawOval(cx - 2, cy - 2, diam + 4, diam + 4);
                }
            }
        }
    }
}