package model;

public class Plateau {
    private final int COLONNE = 9;
    private final int LIGNE = 9;
    private int[][] plateau;
    private int winLength = 4;

    public Plateau() {
        this.plateau = new int[LIGNE][COLONNE];
    }

    public int getCOLONNE() {
        return COLONNE;
    }

    public int getLIGNE() {
        return LIGNE;
    }

    public int[][] getPlateau() {
        return plateau;
    }

    public int getCellule(int ligne, int colonne) {
        checkBounds(ligne, colonne);
        return plateau[ligne][colonne];
    }

    public void setCellule(int ligne, int colonne, int idJoueur) {
        checkBounds(ligne, colonne);
        plateau[ligne][colonne] = idJoueur;
    }

    public boolean isFull() {
        for (int col = 0; col < COLONNE; col++) {
            if (!colonnePleine(col)) {
                return false;
            }
        }
        return true;
    }

    public void reset() {
        for (int i = 0; i < LIGNE; i++) {
            for (int j = 0; j < COLONNE; j++) {
                plateau[i][j] = 0;
            }
        }
    }

    private boolean colonnePleine(int colonne) {
        if (colonne < 0 || colonne >= COLONNE) {
            throw new IllegalArgumentException("Colonne hors bornes: " + colonne);
        }
        return plateau[0][colonne] != 0;
    }

    private int getCelluleLibre(int colonne) {
        if (colonne < 0 || colonne >= COLONNE) {
            return -1;
        }
        for (int ligne = LIGNE - 1; ligne >= 0; ligne--) {
            if (plateau[ligne][colonne] == 0) {
                return ligne;
            }
        }
        return -1;
    }
    public int dropDisc(int colonne, int idJoueur) {
        if (colonne < 0 || colonne >= COLONNE) {
            return -1;
        }
        int ligne = getCelluleLibre(colonne);
        if (ligne == -1) {
            return -1; // colonne pleine
        }
        setCellule(ligne, colonne, idJoueur);
        return ligne;
    }

    public boolean checkWin(int ligne, int col, int idJoueur) {
        checkBounds(ligne, col);
        return checkDirection(ligne, col, idJoueur, 1, 0) || // Vertical
               checkDirection(ligne, col, idJoueur, 0, 1) || // Horizontal
               checkDirection(ligne, col, idJoueur, 1, 1) || // Diagonale \
               checkDirection(ligne, col, idJoueur, 1, -1);   // Diagonale /
    }

    public boolean checkDirection(int ligne, int col, int idJoueur, int dLigne, int dCol) {
        int count = 1;
        for (int i = 1; i < winLength; i++) {
            int newLigne = ligne + i * dLigne;
            int newCol = col + i * dCol;
            if (newLigne < 0 || newLigne >= LIGNE || newCol < 0 || newCol >= COLONNE || plateau[newLigne][newCol] != idJoueur) {
                break;
            }
            count++;
        }
        for (int i = 1; i < winLength; i++) {
            int newLigne = ligne - i * dLigne;
            int newCol = col - i * dCol;
            if (newLigne < 0 || newLigne >= LIGNE || newCol < 0 || newCol >= COLONNE || plateau[newLigne][newCol] != idJoueur) {
                break;
            }
            count++;
        }
        return count >= winLength;
    }

    private void checkBounds(int ligne, int colonne) {
        if (ligne < 0 || ligne >= LIGNE || colonne < 0 || colonne >= COLONNE) {
            throw new IndexOutOfBoundsException("Indices hors bornes: ligne=" + ligne + " colonne=" + colonne);
        }
    }
}





