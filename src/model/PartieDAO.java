package model;

import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * DAO pour sauvegarder les parties et les situations.
 *
 * PRINCIPE D'INDEXATION (cours) :
 *  - Chaque état de plateau est encodé en base 3 (72 chiffres pour 9×8)
 *    puis converti en hexadécimal → la valeur hex EST l'index.
 *  - On stocke toujours la forme CANONIQUE = min(hex, sym_hex) pour
 *    dédupliquer automatiquement les situations symétriques.
 *  - La recherche d'une situation existante est un simple lookup
 *    sur l'index UNIQUE base3_hex → O(log n), pas de scan complet.
 *  - Plusieurs parties peuvent partager la même situation (mutualisation).
 */
public class PartieDAO {
    private final DBHelper db;

    public PartieDAO(DBHelper helper) throws SQLException {
        this.db = helper;
        this.db.initDatabase();
    }

    /**
     * Sauvegarde une partie terminée avec sa situation finale.
     */
    public void savePartie(Game game, GameMode mode) {
        List<Integer> moves = game.getMoveHistory(); // colonnes 1-based
        if (moves.isEmpty()) return;

        int winner = game.isGameOver() ? game.getWinner() : 0;

        // Construire la séquence compacte ex: "4534621"
        StringBuilder seqBuilder = new StringBuilder();
        for (int col : moves) seqBuilder.append(col);
        String sequence = seqBuilder.toString();

        // Reconstruire le plateau final pour l'encoder
        int[][] finalBoard = replayBoard(moves, game.getRows(), game.getCols());
        String base3hex = toHex(finalBoard);
        String symHex   = toHex(mirror(finalBoard));

        // Forme canonique = lexicographiquement la plus petite des deux
        String canonical = canonical(base3hex, symHex);
        String symCanonical = canonical.equals(base3hex) ? symHex : base3hex;

        try (Connection c = db.getConnection()) {
            c.setAutoCommit(false);
            try {
                // 1. Chercher ou créer la situation (lookup sur l'index unique)
                int situationId = getOrCreateSituation(
                        c, canonical, symCanonical, moves.size(), winner);

                // 2. Insérer la partie liée à cette situation
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO partie(situation_id, sequence, nb_coups, winner, mode) " +
                        "VALUES (?, ?, ?, ?, ?)")) {
                    ps.setInt(1, situationId);
                    ps.setString(2, sequence);
                    ps.setInt(3, moves.size());
                    ps.setInt(4, winner);
                    ps.setString(5, mode.name());
                    ps.executeUpdate();
                }

                c.commit();
                System.out.println("Partie sauvegardée → situation=" + canonical + " winner=" + winner);

            } catch (SQLException ex) {
                c.rollback();
                throw ex;
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Lookup O(log n) sur l'index base3_hex.
     * Si la situation existe déjà → incrémente nb_parties et retourne son id.
     * Sinon → crée une nouvelle ligne.
     */
    private int getOrCreateSituation(Connection c, String canonical, String symHex,
                                     int moveNumber, int resultat) throws SQLException {
        // Chercher via l'index unique (pas de scan complet)
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT id FROM situation WHERE base3_hex = ?")) {
            ps.setString(1, canonical);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int id = rs.getInt(1);
                // Incrémenter le compteur de mutualisation
                try (PreparedStatement upd = c.prepareStatement(
                        "UPDATE situation SET nb_parties = nb_parties + 1 WHERE id = ?")) {
                    upd.setInt(1, id);
                    upd.executeUpdate();
                }
                return id;
            }
        }

        // Situation inconnue → insérer
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO situation(base3_hex, sym_base3_hex, move_number, resultat) " +
                "VALUES (?, ?, ?, ?) RETURNING id")) {
            ps.setString(1, canonical);
            ps.setString(2, symHex);
            ps.setInt(3, moveNumber);
            if (resultat == 0 && moveNumber > 0) ps.setNull(4, java.sql.Types.INTEGER);
            else ps.setInt(4, resultat);
            ResultSet rs = ps.executeQuery();
            rs.next();
            return rs.getInt(1);
        }
    }

    // ----------------------------------------------------------
    // ENCODAGE BASE 3 → HEXADÉCIMAL
    // ----------------------------------------------------------

    /**
     * Encode le plateau en un entier base 3 (72 chiffres pour 9×8),
     * puis retourne sa représentation hexadécimale.
     * Lecture : ligne par ligne, colonne par colonne.
     */
    private String toHex(int[][] board) {
        BigInteger value = BigInteger.ZERO;
        BigInteger base  = BigInteger.valueOf(3);
        int rows = board.length;
        int cols = board[0].length;
        for (int r = 0; r < rows; r++) {
            for (int col = 0; col < cols; col++) {
                value = value.multiply(base).add(BigInteger.valueOf(board[r][col]));
            }
        }
        return value.toString(16).toUpperCase();
    }

    /**
     * Retourne le miroir horizontal du plateau (colonnes inversées).
     * Si l'original et son symétrique ont le même hash hex,
     * la situation est palindromique (axe de symétrie central).
     */
    private int[][] mirror(int[][] board) {
        int rows = board.length;
        int cols = board[0].length;
        int[][] sym = new int[rows][cols];
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++)
                sym[r][c] = board[r][cols - 1 - c];
        return sym;
    }

    /**
     * Forme canonique = la plus petite des deux représentations hex
     * (lexicographique après padding à la même longueur).
     * Garantit que position et symétrique pointent vers la même ligne.
     */
    private String canonical(String a, String b) {
        // Padder à la même longueur avant comparaison lexicographique
        int len = Math.max(a.length(), b.length());
        String pa = String.format("%" + len + "s", a).replace(' ', '0');
        String pb = String.format("%" + len + "s", b).replace(' ', '0');
        return pa.compareTo(pb) <= 0 ? a : b;
    }

    /**
     * Rejoue la liste de coups sur un plateau vierge et retourne l'état final.
     */
    private int[][] replayBoard(List<Integer> moves, int rows, int cols) {
        int[][] board = new int[rows][cols];
        int player = 1;
        for (int col1based : moves) {
            Game.dropOnBoard(board, col1based - 1, player);
            player = 3 - player;
        }
        return board;
    }
}
