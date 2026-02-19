package console;

import controller.GameController;
import model.Plateau;
import model.Joueur;

import java.util.Scanner;

public class GameConsole implements Runnable {
    private final GameController controller;
    private final Scanner scanner = new Scanner(System.in);

    public GameConsole(GameController controller) {
        this.controller = controller;
    }

    private void printBoard() {
        Plateau p = controller.getPlateau();
        int rows = p.getLIGNE();
        int cols = p.getCOLONNE();
        System.out.println();
        for (int r = 0; r < rows; r++) {
            System.out.print("|");
            for (int c = 0; c < cols; c++) {
                int id = p.getCellule(r, c);
                char ch = id == 0 ? ' ' : (id == 1 ? 'X' : 'O');
                System.out.print(ch + "|");
            }
            System.out.println();
        }
        // print column numbers
        System.out.print(" ");
        for (int c = 1; c <= cols; c++) System.out.print(c % 10 + " ");
        System.out.println();
        System.out.println();
    }

    @Override
    public void run() {
        while (true) {
            controller.reset();
            while (!controller.isGameOver()) {
                printBoard();
                System.out.print("Joueur " + controller.getCurrentPlayer().getId() + " (entrez numéro de colonne 1-" + controller.getPlateau().getCOLONNE() + "): ");
                String line = scanner.nextLine();
                if (line == null) return;
                line = line.trim();
                if (line.isEmpty()) continue;
                if (line.equalsIgnoreCase("q") || line.equalsIgnoreCase("quit")) {
                    System.out.println("Au revoir.");
                    return;
                }
                int col;
                try {
                    col = Integer.parseInt(line) - 1;
                } catch (NumberFormatException e) {
                    System.out.println("Entrée invalide.");
                    continue;
                }
                boolean played = controller.playMove(col);
                if (!played) {
                    System.out.println("Coup invalide. Réessayez.");
                }
            }
            printBoard();
            int winner = controller.getWinnerId();
            if (winner == 0) {
                System.out.println("Match nul !");
            } else {
                System.out.println("Le joueur " + winner + " a gagné !");
            }
            System.out.print("Rejouer ? (o/N): ");
            String again = scanner.nextLine();
            if (again == null || (!again.equalsIgnoreCase("o") && !again.equalsIgnoreCase("y"))) {
                System.out.println("Fin du jeu.");
                break;
            }
        }
    }
}
