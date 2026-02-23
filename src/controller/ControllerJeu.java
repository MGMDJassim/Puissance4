package controller;

import model.Game;

public class ControllerJeu {
	private final Game game;

	public ControllerJeu(Game game) {
		this.game = game;
	}

	/**
	 * Attempt to play a disc in the given 0-based column.
	 * Returns the row index where the disc landed, or -1 if move invalid.
	 */
	public int playColumn(int col) {
		int row = game.drop(col);
		if (row >= 0) System.out.println("Controller: played column " + (col + 1) + " -> row " + row);
		else System.out.println("Controller: invalid move on column " + (col + 1));
		return row;
	}
}
