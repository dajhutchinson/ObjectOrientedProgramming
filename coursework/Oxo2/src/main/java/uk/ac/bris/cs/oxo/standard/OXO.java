package uk.ac.bris.cs.oxo.standard;

import static java.util.Objects.requireNonNull;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import uk.ac.bris.cs.gamekit.matrix.ImmutableMatrix;
import uk.ac.bris.cs.gamekit.matrix.Matrix;
import uk.ac.bris.cs.gamekit.matrix.SquareMatrix;
import uk.ac.bris.cs.oxo.Cell;
import uk.ac.bris.cs.oxo.Outcome;
import uk.ac.bris.cs.oxo.Player;
import uk.ac.bris.cs.oxo.Side;
import uk.ac.bris.cs.oxo.Spectator;

public class OXO implements OXOGame, Consumer<Move> {

	private Player noughtSide, crossSide;
	private Side currentSide;
	private int size;
	private SquareMatrix<Cell> matrix;
	private final List<Spectator> spectators = new CopyOnWriteArrayList<>();
	private Set<Move> validMoves;

	public OXO(int size, Side startSide, Player nought, Player cross) {
		if(size <= 0) throw new IllegalArgumentException("size invalid");
		else this.size = size;

		if (startSide == null) throw new NullPointerException();
		else this.currentSide = startSide;

		if (nought == null) throw new NullPointerException();
		else this.noughtSide = nought;

		if (cross == null) throw new NullPointerException();
		else this.crossSide = cross;

		this.matrix = new SquareMatrix<Cell>(size, new Cell());
		this.validMoves = validMoves();
	}

	@Override
	public void registerSpectators(Spectator... spectators) {
		this.spectators.addAll(Arrays.asList(spectators));
	}

	@Override
	public void unregisterSpectators(Spectator... spectators) {
		this.spectators.removeAll(Arrays.asList(spectators));
	}

	@Override
	public void start() {

		Boolean play = true;

		while (play){
			Player player = (currentSide == Side.CROSS) ? crossSide : noughtSide;
			player.makeMove(this, validMoves, this);

			Boolean win = false;
			if (checkRow()) win = true;
			else if (checkCol()) win = true;
			else if (checkMainDiag()) win = true;
			else if (checkAntiDiag()) win = true;

			if (win) {
				for (Spectator item : spectators) item.gameOver(new Outcome(currentSide));
				play = false;
			} else if (full()) {
				for (Spectator item : spectators) item.gameOver(new Outcome());
				play = false;
			}

			currentSide.other();
		}

	}

	private Set<Move> validMoves() {
	  Set<Move> moves = new HashSet<>();
	  for (int row = 0; row < matrix.rowSize(); row++) {
	    for (int col = 0; col < matrix.columnSize(); col++) {
	      moves.add(new Move(row, col));
	  } }
	  return moves;
	}

	@Override
	public void accept(Move move) {
		if (validMoves().contains(move)) {
			validMoves().remove(move);
			for (Spectator item : spectators) item.moveMade(currentSide, move);
		}
		else {
			throw new IllegalArgumentException();
		}
	}

	private Boolean checkRow() {
		Boolean win = false;
		for (int row = 0; row < matrix.rowSize(); row++) {
			List<Cell> r = matrix.row(row);
			Cell target = r.get(0);
			win = true;
			for (int col = 0; col < matrix.columnSize(); col++) {
				if (r.get(col) != target || r.get(col) == null) win = false;
			}
			if (win) return true;
		}
		return false;
	}

	private Boolean checkCol() {
		Boolean win = false;
		for (int col = 0; col < matrix.columnSize(); col++) {
			List<Cell> c = matrix.column(col);
			Cell target = c.get(0);
			win = true;
			for (int row = 0; row < matrix.rowSize(); row++) {
				if (c.get(row) != target || c.get(col) == null) win = false;
			}
			if (win) return true;
		}
		return false;
	}

	private Boolean checkMainDiag() {
		Boolean win = true;
		List<Cell> d = matrix.mainDiagonal();
		Cell target = d.get(0);
		for (int k = 0; k < size; k ++) {
			if (d.get(k) != target || d.get(k) == null) win = false;
		}
		return win;
	}

	private Boolean checkAntiDiag() {
		Boolean win = true;
		List<Cell> d = matrix.antiDiagonal();
		Cell target = d.get(0);
		for (int k = 0; k < size; k ++) {
			if (d.get(k) != target || d.get(k) == null) win = false;
		}
		return win;
	}

	private Boolean full() {
		for (int row = 0; row < matrix.rowSize(); row++) {
			List<Cell> r = matrix.row(row);
			for (int col = 0; col < matrix.columnSize(); col++) {
				if (r.get(col) == null) return false;
		}}
		return true;
	}

	@Override
	public Matrix<Cell> board() {
		return new ImmutableMatrix<>(matrix);
	}

	@Override
	public Side currentSide() {
		return currentSide;
	}
}
