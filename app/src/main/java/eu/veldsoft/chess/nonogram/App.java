package eu.veldsoft.chess.nonogram;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.apache.commons.math3.exception.MathIllegalArgumentException;
import org.apache.commons.math3.genetics.AbstractListChromosome;
import org.apache.commons.math3.genetics.Chromosome;
import org.apache.commons.math3.genetics.ElitisticListPopulation;
import org.apache.commons.math3.genetics.FixedGenerationCount;
import org.apache.commons.math3.genetics.GeneticAlgorithm;
import org.apache.commons.math3.genetics.InvalidRepresentationException;
import org.apache.commons.math3.genetics.MutationPolicy;
import org.apache.commons.math3.genetics.TournamentSelection;
import org.apache.commons.math3.genetics.UniformCrossover;

public class App {
	private static final int GENERATIONS = 10_000;
	private static final int POPULATION = 83;

	private static enum Cell {
		EMPTY(' '), OCCUPIED('*'), KING('K'), QUEEN('Q'), ROOK('R'), BISHOP('B'), KNIGHT('N');

		static class Step {
			int dx;
			int dy;

			Step(int dx, int dy) {
				this.dx = dx;
				this.dy = dy;
			}

			@Override
			public String toString() {
				return "(" + dx + "," + dy + ")";
			}

		}

		private char symbol;

		private List<List<Step>> steps = new ArrayList<>();

		Cell(char symbol) {
			this.symbol = symbol;
		}

		char symbol() {
			return symbol;
		}

		void symbol(char symbol) {
			this.symbol = symbol;
		}

		void steps(List<List<Step>> steps) {
			this.steps = steps;
		}

		List<List<Step>> steps() {
			return steps;
		}
	}

	private static final Random PRNG = new Random();

	private static final Cell PIECES_ARRAY[] = { Cell.KING, Cell.QUEEN, Cell.ROOK, Cell.BISHOP, Cell.KNIGHT, };

	private static final List<Cell> PIECES_LIST = Arrays.asList(PIECES_ARRAY);

	private static int[][] image = { {} };

	private static Cell[][] board(List<Cell> representation, int[][] image) {
		Cell[][] board = new Cell[image.length][];

		for (int i = 0, k = 0; i < image.length; i++) {
			board[i] = new Cell[image[i].length];
			for (int j = 0; j < image[i].length; j++, k++) {
				board[i][j] = representation.get(k);
			}
		}

		return board;
	}

	private static int[][] beaten(List<Cell> representation, int[][] image) {
		int[][] counters = new int[image.length][];
		for (int i = 0; i < image.length; i++) {
			counters[i] = new int[image[i].length];
			for (int j = 0; j < image[i].length; j++) {
				counters[i][j] = 0;
			}
		}

		Cell[][] board = board(representation, image);
		for (int i = 0; i < board.length; i++) {
			for (int j = 0; j < board[i].length; j++) {
				if (board[i][j] == Cell.EMPTY) {
					continue;
				}
				if (board[i][j] == Cell.OCCUPIED) {
					continue;
				}

				for (List<Cell.Step> directions : board[i][j].steps()) {
					for (Cell.Step step : directions) {
						if (i + step.dx < 0) {
							continue;
						}
						if (i + step.dx >= counters.length) {
							continue;
						}
						if (j + step.dy < 0) {
							continue;
						}
						if (j + step.dy >= counters[i].length) {
							continue;
						}

						if (PIECES_LIST.contains(board[i + step.dx][j + step.dy])) {
							break;
						}

						counters[i + step.dx][j + step.dy]++;
					}
				}
			}
		}

		return counters;
	}

	private static int fitness(List<Cell> representation, int[][] image) {
		int[][] counters = beaten(representation, image);
		Cell[][] board = board(representation, image);

		int score = 0;
		final int BAD = -150;
		final int GOOD = +50;
		final int BEST = +100;
		final int UNDERBEATEN = +10;
		for (int i = 0; i < counters.length; i++) {
			for (int j = 0; j < counters[i].length; j++) {
				if (counters[i][j] == 2 && board[i][j] == Cell.OCCUPIED) {
					score += BEST;
				}
				if (counters[i][j] > 2 && board[i][j] == Cell.OCCUPIED) {
					score += GOOD;
				}
				if (counters[i][j] < 2 && board[i][j] == Cell.OCCUPIED) {
					score += UNDERBEATEN;
				}
				if (counters[i][j] > 1 && image[i][j] == 0) {
					score += BAD;
				}
			}
		}

		return score;
	}

	private static boolean hitting(int x, int y, Cell piece, int[][] image) {
		for (List<Cell.Step> directions : piece.steps()) {
			for (Cell.Step step : directions) {
				if (x + step.dx < 0) {
					continue;
				}
				if (x + step.dx >= image.length) {
					continue;
				}
				if (y + step.dy < 0) {
					continue;
				}
				if (y + step.dy >= image[x].length) {
					continue;
				}

				if (image[x + step.dx][y + step.dy] == 1) {
					return true;
				}
			}
		}

		return false;
	}

	private static boolean hitting(int index, Cell cell, int[][] image2) {
		for (int i = 0, k = 0; i < image.length; i++) {
			for (int j = 0; j < image[i].length; j++, k++) {
				if (index == k) {
					return hitting(i, j, cell, image);
				}
			}
		}

		return false;
	}

	private static List<Cell> emptyOnly(int[][] image) {
		List<Cell> representation = new ArrayList<>();

		for (int i = 0; i < image.length; i++) {
			for (int j = 0; j < image[i].length; j++) {
				if (image[i][j] == 1) {
					representation.add(Cell.OCCUPIED);
				} else {
					representation.add(Cell.EMPTY);
				}
			}
		}

		return representation;
	}

	private static List<Cell> randomOnly(int[][] image, double threshold) {
		List<Cell> representation = new ArrayList<>();

		for (int i = 0; i < image.length; i++) {
			for (int j = 0; j < image[i].length; j++) {
				if (image[i][j] == 1) {
					representation.add(Cell.OCCUPIED);
				} else {
					Cell piece = PIECES_ARRAY[PRNG.nextInt(PIECES_ARRAY.length)];
					if (PRNG.nextDouble() < threshold) {
						representation.add(piece);
					} else {
						representation.add(Cell.EMPTY);
					}
				}
			}
		}

		return representation;
	}

	private static List<Cell> randomSearch(int[][] image, int limit) {
		List<Cell> representation = new ArrayList<>();

		for (int i = 0; i < image.length; i++) {
			for (int j = 0; j < image[i].length; j++) {
				if (image[i][j] == 1) {
					representation.add(Cell.OCCUPIED);
				} else {
					representation.add(Cell.EMPTY);
				}
			}
		}

		for (int l = 0; l < limit && unbeaten(representation, image) > 0; l++) {
			int index = -1;
			do {
				index = PRNG.nextInt(representation.size());
			} while (representation.get(index) != Cell.EMPTY);

			Cell cell = PIECES_ARRAY[PRNG.nextInt(PIECES_ARRAY.length)];
			representation.set(index, cell);
			representation = removeUnused(representation, image);
			representation = removeHarmful(representation, image);
		}

		return representation;
	}

	private static List<Cell> removeUnused(List<Cell> representation, int[][] image) {
		List<Cell> result = new ArrayList<>();
		Cell[][] board = board(representation, image);

		for (int i = 0, k = 0; i < image.length; i++) {
			for (int j = 0; j < image[i].length; j++, k++) {
				if (PIECES_LIST.contains(board[i][j]) == true && hitting(i, j, board[i][j], image) == false) {
					result.add(Cell.EMPTY);
				} else {
					result.add(representation.get(k));
				}
			}
		}

		return result;
	}

	private static List<Cell> removeHarmful(List<Cell> representation, int[][] image) {
		List<Cell> result = new ArrayList<>(representation);

		int[][] counters = beaten(result, image);
		for (int i = 0, k = 0; i < image.length; i++) {
			for (int j = 0; j < image[i].length; j++, k++) {
				boolean isHarmful = false;

				done: for (List<Cell.Step> directions : result.get(k).steps()) {
					for (Cell.Step step : directions) {
						if (i + step.dx < 0) {
							continue;
						}
						if (i + step.dx >= counters.length) {
							continue;
						}
						if (j + step.dy < 0) {
							continue;
						}
						if (j + step.dy >= counters[i].length) {
							continue;
						}

						if (image[i + step.dx][j + step.dy] == 0 && counters[i + step.dx][j + step.dy] > 1) {
							isHarmful = true;
							break done;
						}
					}
				}

				if (isHarmful) {
					result.set(k, Cell.EMPTY);
					counters = beaten(result, image);
					i = 0;
					j = 0;
					k = 0;
				}
			}
		}

		return result;
	}

	private static void print(PrintStream out, boolean debug, List<Cell> representation, int[][] image) {
		int[][] counters = beaten(representation, image);
		for (int i = 0, k = 0; i < image.length; i++) {
			for (int j = 0; j < image[i].length; j++, k++) {
				if (debug == true) {
					out.print("[" + representation.get(k).symbol() + ""
							+ (counters[i][j] > 1 ? String.format("%2d", counters[i][j]) : "  ") + "]");
				} else {
					out.print(representation.get(k).symbol());
				}
			}
			out.println();
		}
	}

	private static int[][] dice(int[][] image) {
		int[][] board = new int[image.length][];
		for (int i = 0; i < image.length; i++) {
			board[i] = new int[image[i].length];
			for (int j = 0; j < image[i].length; j++) {
				board[i][j] = 0;

				if (image[i][j] == 1) {
					continue;
				}

				for (int k = -1; k <= +1; k++) {
					for (int l = -1; l <= +1; l++) {
						if (k == 0 && l == 0) {
							continue;
						}
						if (i + k < 0) {
							continue;
						}
						if (j + l < 0) {
							continue;
						}
						if (i + k >= image.length) {
							continue;
						}
						if (j + l >= image[i].length) {
							continue;
						}

						board[i][j] += image[i + k][j + l];
					}
				}

				if (board[i][j] > 6) {
					board[i][j] = 0;
				}
			}
		}

		return board;
	}

	private static int unbeaten(List<Cell> representation, int[][] image) {
		int[][] counters = beaten(representation, image);

		int sum = 0;
		for (int i = 0; i < image.length; i++) {
			for (int j = 0; j < image[i].length; j++) {
				if (image[i][j] == 1 && counters[i][j] < 2) {
					sum++;
				}
			}
		}

		return sum;
	}

	public static void main(String[] args) throws IOException {
//		args = new String[] { "C:\\Users\\Todor Balabanov\\Desktop\\Icons-32x32-03-May-2024\\01.bin",
//				"C:\\Users\\Todor Balabanov\\Desktop\\Icons-32x32-03-May-2024\\01.chess" };
//
		List<int[]> rows = new ArrayList<>();
		try (BufferedReader reader = new BufferedReader(new FileReader(args[0]))) {
			String line;
			while ((line = reader.readLine()) != null) {
				int[] row = new int[line.length()];
				for (int i = 0; i < line.length(); i++) {
					row[i] = Character.getNumericValue(line.charAt(i));
				}
				rows.add(row);
			}
		}
		int size = rows.size();
		image = new int[rows.size()][];
		for (int i = 0; i < rows.size(); i++) {
			image[i] = rows.get(i);
			if (image[i].length > size) {
				size = image[i].length;
			}
		}

		/* Dice brute force. */ {
//			int[][] board = dice(image);
//			try (PrintStream out = new PrintStream(new FileOutputStream(args[1]))) {
//				out.println(Arrays.deepToString(board).replace("],", "\n").replace("[", "").replace("]", "")
//						.replace(" ", "").replace(",", ""));
//				out.close();
//			}
//			System.exit(0);
		}

		List<Cell.Step> direction = null;
		List<List<Cell.Step>> steps = null;

		steps = new ArrayList<>();

		direction = new ArrayList<>();
		direction.add(new Cell.Step(-1, -1));
		steps.add(direction);

		direction = new ArrayList<>();
		direction.add(new Cell.Step(-1, 0));
		steps.add(direction);

		direction = new ArrayList<>();
		direction.add(new Cell.Step(-1, 1));
		steps.add(direction);

		direction = new ArrayList<>();
		direction.add(new Cell.Step(0, -1));
		steps.add(direction);

		direction = new ArrayList<>();
		direction.add(new Cell.Step(0, 1));
		steps.add(direction);

		direction = new ArrayList<>();
		direction.add(new Cell.Step(1, -1));
		steps.add(direction);

		direction = new ArrayList<>();
		direction.add(new Cell.Step(1, 0));
		steps.add(direction);

		direction = new ArrayList<>();
		direction.add(new Cell.Step(1, 1));
		steps.add(direction);

		Cell.KING.steps(steps);

		steps = new ArrayList<>();

		direction = new ArrayList<>();
		direction.add(new Cell.Step(-2, -1));
		steps.add(direction);

		direction = new ArrayList<>();
		direction.add(new Cell.Step(-2, 1));
		steps.add(direction);

		direction = new ArrayList<>();
		direction.add(new Cell.Step(-1, -2));
		steps.add(direction);

		direction = new ArrayList<>();
		direction.add(new Cell.Step(-1, 2));
		steps.add(direction);

		direction = new ArrayList<>();
		direction.add(new Cell.Step(1, -2));
		steps.add(direction);

		direction = new ArrayList<>();
		direction.add(new Cell.Step(1, 2));
		steps.add(direction);

		direction = new ArrayList<>();
		direction.add(new Cell.Step(2, -1));
		steps.add(direction);

		direction = new ArrayList<>();
		direction.add(new Cell.Step(2, 1));
		steps.add(direction);

		Cell.KNIGHT.steps(steps);

		steps = new ArrayList<>();

		direction = new ArrayList<>();
		for (int i = 1; i < size; i++) {
			direction.add(new Cell.Step(-i, -i));
		}
		steps.add(direction);

		direction = new ArrayList<>();
		for (int i = 1; i < size; i++) {
			direction.add(new Cell.Step(-i, i));
		}
		steps.add(direction);

		direction = new ArrayList<>();
		for (int i = 1; i < size; i++) {
			direction.add(new Cell.Step(i, -i));
		}
		steps.add(direction);

		direction = new ArrayList<>();
		for (int i = 1; i < size; i++) {
			direction.add(new Cell.Step(i, i));
		}
		steps.add(direction);

		Cell.BISHOP.steps(steps);

		steps = new ArrayList<>();

		direction = new ArrayList<>();
		for (int i = 1; i < size; i++) {
			direction.add(new Cell.Step(-i, 0));
		}
		steps.add(direction);

		direction = new ArrayList<>();
		for (int i = 1; i < size; i++) {
			direction.add(new Cell.Step(0, -i));
		}
		steps.add(direction);

		direction = new ArrayList<>();
		for (int i = 1; i < size; i++) {
			direction.add(new Cell.Step(0, i));
		}
		steps.add(direction);

		direction = new ArrayList<>();
		for (int i = 1; i < size; i++) {
			direction.add(new Cell.Step(i, 0));
		}
		steps.add(direction);

		Cell.ROOK.steps(steps);

		steps = new ArrayList<>();

		direction = new ArrayList<>();
		for (int i = 1; i < size; i++) {
			direction.add(new Cell.Step(-i, -i));
		}
		steps.add(direction);

		direction = new ArrayList<>();
		for (int i = 1; i < size; i++) {
			direction.add(new Cell.Step(-i, i));
		}
		steps.add(direction);

		direction = new ArrayList<>();
		for (int i = 1; i < size; i++) {
			direction.add(new Cell.Step(i, -i));
		}
		steps.add(direction);

		direction = new ArrayList<>();
		for (int i = 1; i < size; i++) {
			direction.add(new Cell.Step(i, i));
		}
		steps.add(direction);

		direction = new ArrayList<>();
		for (int i = 1; i < size; i++) {
			direction.add(new Cell.Step(-i, 0));
		}
		steps.add(direction);

		direction = new ArrayList<>();
		for (int i = 1; i < size; i++) {
			direction.add(new Cell.Step(0, -i));
		}
		steps.add(direction);

		direction = new ArrayList<>();
		for (int i = 1; i < size; i++) {
			direction.add(new Cell.Step(0, i));
		}
		steps.add(direction);

		direction = new ArrayList<>();
		for (int i = 1; i < size; i++) {
			direction.add(new Cell.Step(i, 0));
		}
		steps.add(direction);

		Cell.QUEEN.steps(steps);

		/* Only random search try. */ {
//			List<Cell> representation = randomSearch(image, 100_000);
//			print(System.out, true, representation, image);
//			Cell.EMPTY.symbol('.');
//			Cell.OCCUPIED.symbol('.');
//			try (PrintStream out = new PrintStream(new FileOutputStream(args[1]))) {
//				print(out, false, representation, image);
//				out.close();
//			}
//			System.exit(0);
		}

		class CellChromosome extends AbstractListChromosome<Cell> {
			@Override
			protected void checkValidity(List<Cell> representation) throws InvalidRepresentationException {
			}

			List<Cell> representation() {
				return getRepresentation();
			}

			public CellChromosome(List<Cell> representation) {
				super(representation);
			}

			@Override
			public CellChromosome newFixedLengthChromosome(List<Cell> representation) {
				// return new CellChromosome(emptyOnly(image));
				// return new CellChromosome(random(image, 0.01));
				return new CellChromosome(randomSearch(image, 10_000));
				// return new CellChromosome(new ArrayList<>(representation));
			}

			@Override
			public double fitness() {
				return App.fitness(getRepresentation(), image);
			}
		}

		GeneticAlgorithm ga = new GeneticAlgorithm(new UniformCrossover<Cell>(0.5), 0.9, new MutationPolicy() {
			@Override
			public Chromosome mutate(Chromosome original) throws MathIllegalArgumentException {
				List<Cell> representation = new ArrayList<>(((CellChromosome) original).representation());

				int index = PRNG.nextInt(representation.size());
				if (representation.get(index) != Cell.OCCUPIED) {
					if (PRNG.nextDouble() < 0.5D) {
						Cell cell = PIECES_ARRAY[PRNG.nextInt(PIECES_ARRAY.length)];
						representation.set(index, cell);
					} else {
						representation.set(index, Cell.EMPTY);
					}
				}

				return new CellChromosome(representation);
			}
		}, 0.01, new TournamentSelection(3));

		List<Chromosome> chromosomes = new ArrayList<Chromosome>();
		for (int i = 0; i < POPULATION; i++) {
			List<Cell> representation = randomOnly(image, 0.09);
			// List<Cell> representation = randomSearch(image, 10_000);
			chromosomes.add(new CellChromosome(representation));
		}

		Chromosome fittest = ga.evolve(new ElitisticListPopulation(chromosomes, chromosomes.size() * 2, 0.05),
				new FixedGenerationCount(GENERATIONS)).getFittestChromosome();

		List<Cell> representation = ((CellChromosome) fittest).representation();
		representation = removeUnused(representation, image);
		representation = removeHarmful(representation, image);
		System.out.println(fitness(representation, image));
		print(System.out, true, representation, image);
		Cell.EMPTY.symbol('.');
		Cell.OCCUPIED.symbol('.');
		try (PrintStream out = new PrintStream(new FileOutputStream(args[1]))) {
			print(out, false, representation, image);
			out.close();
		}
	}
}
