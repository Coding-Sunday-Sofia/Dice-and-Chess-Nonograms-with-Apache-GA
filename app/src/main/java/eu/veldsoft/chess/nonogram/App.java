package eu.veldsoft.chess.nonogram;

import org.apache.commons.math3.exception.MathIllegalArgumentException;
import org.apache.commons.math3.genetics.AbstractListChromosome;
import org.apache.commons.math3.genetics.Chromosome;
import org.apache.commons.math3.genetics.GeneticAlgorithm;
import org.apache.commons.math3.genetics.InvalidRepresentationException;
import org.apache.commons.math3.genetics.MutationPolicy;
import org.apache.commons.math3.genetics.TournamentSelection;
import org.apache.commons.math3.genetics.UniformCrossover;
import org.apache.commons.math3.genetics.FixedGenerationCount;
import org.apache.commons.math3.genetics.ElitisticListPopulation;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

public class App {
	private static final int GENERATIONS = 10000;
	private static final int POPULATION = 51;

	private static enum Cell {
		EMPTY(' '), KING('K'), QUEEN('Q'), ROOK('R'), BISHOP('B'), KNIGHT('N');

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

		private List<Step> steps = new ArrayList<>();

		Cell(char symbol) {
			this.symbol = symbol;
		}

		char symbol() {
			return symbol;
		}

		void steps(List<Step> steps) {
			this.steps = steps;
		}

		List<Step> steps() {
			return steps;
		}
	}

	private static final Random PRNG = new Random();

	private static final Set<Cell> PIECES = new HashSet<>();

	private static int[][] image = { {} };

	static {
		PIECES.add(Cell.KING);
		PIECES.add(Cell.QUEEN);
		PIECES.add(Cell.ROOK);
		PIECES.add(Cell.BISHOP);
		PIECES.add(Cell.KNIGHT);
	}

	private static Cell[][] board(List<Cell> cells, int[][] image) {
		Cell[][] board = new Cell[image.length][];

		for (int i = 0, k = 0; i < image.length; i++) {
			board[i] = new Cell[image[i].length];
			for (int j = 0; j < image[i].length; j++, k++) {
				board[i][j] = cells.get(k);
			}
		}

		return board;
	}

	private static int[][] beaten(List<Cell> cells, int[][] image) {
		int[][] counters = new int[image.length][];
		for (int i = 0; i < image.length; i++) {
			counters[i] = new int[image[i].length];
			for (int j = 0; j < image[i].length; j++) {
				counters[i][j] = 0;
			}
		}

		Cell[][] board = board(cells, image);
		for (int i = 0; i < board.length; i++) {
			for (int j = 0; j < board[i].length; j++) {
				if (board[i][j] == Cell.EMPTY) {
					continue;
				}

				for (Cell.Step step : board[i][j].steps()) {
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

					counters[i + step.dx][j + step.dy]++;
				}
			}
		}

//		System.err.println(Arrays.deepToString(counters).replace("],", "\n").replace("[", "").replace("]", "")
//				.replace(" ", "").replace(",", " "));
//		System.err.println(score);
		return counters;
	}

	private static int fitness(List<Cell> cells, int[][] image) {
		int[][] counters = beaten(cells, image);
		Cell[][] board = board(cells, image);

		int score = 0;
		final int BAD = -20;
		final int GOOD = +200;
		final int OVERLAP = -10;
		final int OVERBEATEN = -10;
		final int UNDERBEATEN = -5;
		for (int i = 0; i < counters.length; i++) {
			for (int j = 0; j < counters[i].length; j++) {
//				if (counters[i][j] == 2 && image[i][j] == 1) {
//					score += GOOD;
//				}
//				if (counters[i][j] > 2 && image[i][j] == 1) {
//					score += OVERBEATEN;
//				}
				if (counters[i][j] >= 2 && image[i][j] == 1) {
					score += GOOD;
				}
				if (counters[i][j] < 2 && image[i][j] == 1) {
					score += UNDERBEATEN;
				}
				if (board[i][j] != Cell.EMPTY && image[i][j] == 1) {
					score += OVERLAP;
				}
				if (counters[i][j] > 0 && image[i][j] == 0) {
					score += BAD;
				}
			}
		}

		return score;
	}

	private static List<Cell> random(int[][] image, double threshold) {
		List<Cell> cells = new ArrayList<>();

		for (int i = 0; i < image.length; i++) {
			for (int j = 0; j < image[i].length; j++) {
				if (image[i][j] == 1) {
					cells.add(Cell.EMPTY);
				} else if (PRNG.nextDouble() < (1D - threshold)) {
					cells.add(Cell.EMPTY);
				} else {
					cells.add(Cell.values()[PRNG.nextInt(Cell.values().length)]);
				}
			}
		}

		return cells;
	}

	private static void print(List<Cell> cells, int[][] image) {
		int[][] counters = beaten(cells, image);
		for (int i = 0, k = 0; i < image.length; i++) {
			for (int j = 0; j < image[i].length; j++, k++) {
				System.out.print(
						"[" + cells.get(k).symbol() + "" + (image[i][j] == 1 ? '*' : (counters[i][j] > 0 ? ' ' : ' '))
								+ (counters[i][j] > 0 ? String.format("%2d", counters[i][j]) : "  ") + "]");
			}
			System.out.println();
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

	public static void main(String[] args) throws IOException {
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

		int[][] board = dice(image);
		try (PrintStream out = new PrintStream(new FileOutputStream(args[1]))) {
			out.println(Arrays.deepToString(board).replace("],", "\n").replace("[", "").replace("]", "")
					.replace(" ", "").replace(",", ""));
			out.close();
		}
		System.exit(0);

		List<Cell.Step> steps = null;

		steps = new ArrayList<Cell.Step>();
		steps.add(new Cell.Step(-1, -1));
		steps.add(new Cell.Step(-1, 0));
		steps.add(new Cell.Step(-1, 1));
		steps.add(new Cell.Step(0, -1));
		steps.add(new Cell.Step(0, 1));
		steps.add(new Cell.Step(1, -1));
		steps.add(new Cell.Step(1, 0));
		steps.add(new Cell.Step(1, 1));
		Cell.KING.steps(steps);

		steps = new ArrayList<Cell.Step>();
		steps.add(new Cell.Step(-2, -1));
		steps.add(new Cell.Step(-2, 1));
		steps.add(new Cell.Step(-1, -2));
		steps.add(new Cell.Step(-1, 2));
		steps.add(new Cell.Step(1, -2));
		steps.add(new Cell.Step(1, 2));
		steps.add(new Cell.Step(2, -1));
		steps.add(new Cell.Step(2, 1));
		Cell.KNIGHT.steps(steps);

		steps = new ArrayList<Cell.Step>();
		for (int i = 1; i < size; i++) {
			steps.add(new Cell.Step(-i, -i));
			steps.add(new Cell.Step(-i, i));
			steps.add(new Cell.Step(i, -i));
			steps.add(new Cell.Step(i, i));
		}
		Cell.BISHOP.steps(steps);

		steps = new ArrayList<Cell.Step>();
		for (int i = 1; i < size; i++) {
			steps.add(new Cell.Step(-i, 0));
			steps.add(new Cell.Step(0, -i));
			steps.add(new Cell.Step(0, i));
			steps.add(new Cell.Step(i, 0));
		}
		Cell.ROOK.steps(steps);

		steps = new ArrayList<Cell.Step>();
		for (int i = 1; i < size; i++) {
			steps.add(new Cell.Step(-i, -i));
			steps.add(new Cell.Step(-i, i));
			steps.add(new Cell.Step(i, -i));
			steps.add(new Cell.Step(i, i));
			steps.add(new Cell.Step(-i, 0));
			steps.add(new Cell.Step(0, -i));
			steps.add(new Cell.Step(0, i));
			steps.add(new Cell.Step(i, 0));
		}
		Cell.QUEEN.steps(steps);

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
				representation = new ArrayList<>(representation);
				Collections.shuffle(representation);
				return new CellChromosome(representation);
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

				representation.set(PRNG.nextInt(representation.size()),
						Cell.values()[PRNG.nextInt(Cell.values().length)]);

				return new CellChromosome(representation);
			}
		}, 0.01, new TournamentSelection(3));

		List<Chromosome> chromosomes = new ArrayList<Chromosome>();
		for (int i = 0; i < POPULATION; i++) {
			chromosomes.add(new CellChromosome(random(image, 0.3)));
		}

		Chromosome fittest = ga.evolve(new ElitisticListPopulation(chromosomes, chromosomes.size() * 2, 0.05),
				new FixedGenerationCount(GENERATIONS)).getFittestChromosome();

		print(((CellChromosome) fittest).representation(), image);
		System.out.println(fitness(((CellChromosome) fittest).representation(), image));
	}
}
