package controller;

import model.Plateau;
import model.Joueur;

import java.util.ArrayList;
import java.util.List;

public class GameController {
    public interface GameListener {
        void onBoardUpdated();
        void onGameEnd(int winnerId); // winnerId == 0 means draw
    }

    private final Plateau plateau;
    private final Joueur[] joueurs;
    private int currentPlayerIndex = 0;
    private boolean gameOver = false;
    private int winnerId = -1;
    private final List<GameListener> listeners = new ArrayList<>();

    public GameController(Plateau plateau, Joueur[] joueurs) {
        if (joueurs == null || joueurs.length == 0) throw new IllegalArgumentException("Au moins un joueur requis");
        this.plateau = plateau;
        this.joueurs = joueurs;
    }

    public Joueur getCurrentPlayer() {
        return joueurs[currentPlayerIndex];
    }

    public Plateau getPlateau() {
        return plateau;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public int getWinnerId() {
        return winnerId;
    }

    public void addGameListener(GameListener l) {
        if (l != null) listeners.add(l);
    }

    public void removeGameListener(GameListener l) {
        listeners.remove(l);
    }

    private void notifyBoardUpdated() {
        for (GameListener l : listeners) l.onBoardUpdated();
    }

    private void notifyGameEnd() {
        for (GameListener l : listeners) l.onGameEnd(winnerId == -1 ? 0 : winnerId);
    }

    public boolean playMove(int colonne) {
        if (gameOver) return false;
        Joueur player = getCurrentPlayer();
        int ligne = plateau.dropDisc(colonne, player.getId());
        if (ligne == -1) return false; // coup invalide

        // vérification de victoire
        if (plateau.checkWin(ligne, colonne, player.getId())) {
            gameOver = true;
            winnerId = player.getId();
            notifyBoardUpdated();
            notifyGameEnd();
            return true;
        }

        // vérification match nul
        if (plateau.isFull()) {
            gameOver = true;
            winnerId = 0;
            notifyBoardUpdated();
            notifyGameEnd();
            return true;
        }

        // basculer le joueur
        currentPlayerIndex = (currentPlayerIndex + 1) % joueurs.length;
        notifyBoardUpdated();
        return true;
    }

    public void reset() {
        currentPlayerIndex = 0;
        gameOver = false;
        winnerId = -1;
        plateau.reset();
        notifyBoardUpdated();
    }
}
