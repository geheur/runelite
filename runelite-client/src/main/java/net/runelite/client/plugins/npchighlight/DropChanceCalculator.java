package net.runelite.client.plugins.npchighlight;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

public class DropChanceCalculator
{
	private static int nexTeamSize = 4;
	private static final double[][] DROP_RATES_NEX = new double[][]{
		{nexTeamSize * 53 * 12 / 2d},
		{nexTeamSize * 53 * 12 / 2d},
		{nexTeamSize * 53 * 12 / 2d},
		{nexTeamSize * 53 * 12 / 2d},
		{nexTeamSize * 53 * 12 / 3d},
		{nexTeamSize * 53 * 12 / 1d},
	};
	private static final int[] NEX_COMPLETE = new int[]{1, 1, 1, 1, 1, 1};
	private static final double[][] DROP_RATES_BANDOS_SIMPLIFIED = new double[][]{
		{Math.pow((1 / 381d) + (3 / 16256d), -1)},
		{Math.pow((1 / 381d) + (3 / 16256d), -1)},
		{Math.pow((1 / 381d) + (3 / 16256d), -1)},
		{508},
		{Math.pow((1 / 762d) + (3 / 1524d), -1)},
		{Math.pow((1 / 762d) + (3 / 1524d), -1)},
		{Math.pow((1 / 762d) + (3 / 1524d), -1)}
	};
	private static final double[][] DROP_RATES_BANDOS = new double[][]{
		{381, 16256, 16256, 16256},
		{381, 16256, 16256, 16256},
		{381, 16256, 16256, 16256},
		{508, -1, -1, -1},
		{762, 1524, 1524, 1524},
		{762, 1524, 1524, 1524},
		{762, 1524, 1524, 1524}
	};
	private static final int[] BANDOS_COMPLETE_WITH_SHARDS_MINUS_BOOTS = new int[]{1, 1, 0, 1, 1, 1, 1};

	private static final double CHAMBERS_DROPRATE = 870000d / 30000d * 69d;
	private static final double[][] DROP_RATES_CHAMBERS = new double[][]{
		{CHAMBERS_DROPRATE / 2},
		{CHAMBERS_DROPRATE / 2},
		{CHAMBERS_DROPRATE / 2},
		{CHAMBERS_DROPRATE / 3},
		{CHAMBERS_DROPRATE / 3},
		{CHAMBERS_DROPRATE / 3},
		{CHAMBERS_DROPRATE / 3},
		{CHAMBERS_DROPRATE / 3},
		{CHAMBERS_DROPRATE / 4},
		{CHAMBERS_DROPRATE / 4},
		{CHAMBERS_DROPRATE / 20},
		{CHAMBERS_DROPRATE / 20}
	};
	private static final int[] CHAMBERS_COMPLETE = new int[]{1, 1, 0,  1, 1, 1, 1, 0,  0, 0,  1, 1};

	private static final double[][] DROP_RATES_1_X_768 = new double[][]{
		{768}
	};
//	private static final int[] requiredForCompletion = new int[]{1};
	private static final double[][] DROP_RATES_2_X_512 = new double[][]{
		{512},
		{512},
	};
//	private static final int[] requiredForCompletion = new int[]{1, 1};
	private static final double[][] DROP_RATES_CGAUNT_BOW_ARMOUR = new double[][]{
		{400},
		{50}
	};
	private static final int[] COMPLETION_CGAUNT_BOW_ARMOUR = new int[]{1, 6};
	private static final double[][] DROP_RATES_ZULRAH = new double[][]{
		{1024, 1024},
		{1024, 1024},
		{1024, 1024}
	};
	private static final int[] COMPLETION_ZULRAH = new int[]{1, 1, 1};

	/** simple droprate table that supports multiple rolls, useful for stuff like zulrah and bandos. */
	private final double[][] droprates;
	private final int[] completed;
	/** actual drop table. differs from "droprates" when there are multiple rolls. */
	private final Map<List<Integer>, Double> COMBINED_DROPRATES;
	/** COMBINED_DROPRATES with a slot for no item dropping. */
	private final Map<List<Integer>, Double> COMBINED_DROPRATES_WITH_EMPTY;

	public DropChanceCalculator(double[][] droprates, int[] completed) {
		this.droprates = droprates;
		this.completed = completed;
		COMBINED_DROPRATES = generateMultiRollDropTable();
		COMBINED_DROPRATES_WITH_EMPTY = new HashMap<>(COMBINED_DROPRATES);
		COMBINED_DROPRATES.remove(Collections.nCopies(droprates.length, 0));

//		printSomeExtraStatsidk();
	}

	private boolean isCompleted(List<Integer> state) {
		for (int i = 0; i < state.size(); i++)
		{
			if (state.get(i) < completed[i]) {
				return false;
			}
		}
		return true;
	}

	private boolean isCompleted(List<Integer> state, int kc) {
		double dKc = kc;
		for (int i = 0; i < state.size(); i++)
		{
			if (state.get(i) < completed[i]) {
				dKc -= droprates[i][0];
//				System.out.println("subtracting " + (droprates[i][0] / 3.333D) + " from " + dKc);
				if (dKc < 0) return false;
//				return false;
			}
		}
		return true;
	}

	/** constructor only. */
	private Map<List<Integer>, Double> generateMultiRollDropTable() {
		Map<List<Integer>, Double> soFar = new HashMap<>();
		soFar.put(Collections.nCopies(droprates.length, 0), 1d);
		// iterate through all the rolls on the table (in gwd this would be the boss + 3 minions).
		for (int rollIndex = 0; rollIndex < droprates[0].length; rollIndex++)
		{
			soFar = addRollToDropTable(soFar, rollIndex);
		}
		return soFar;
	}

	private Map<List<Integer>, Double> addRollToDropTable(Map<List<Integer>, Double> soFar, int rollIndex)
	{
		Map<List<Integer>, Double> newMap = new HashMap<>();
		for (Map.Entry<List<Integer>, Double> stateSoFar : soFar.entrySet())
		{
			double chanceOfAnyItem = 0d;
			for (int itemIndex = 0; itemIndex < droprates.length; itemIndex++)
			{
				List<Integer> state = new ArrayList<>(stateSoFar.getKey());
				state.set(itemIndex, state.get(itemIndex) + 1);
				double dropRate = droprates[itemIndex][rollIndex];
				if (dropRate == -1) continue;
				double dropChance = dropRate == -1 ? 0 : 1 / dropRate;
				chanceOfAnyItem += dropChance;
				double combinedDropChance = stateSoFar.getValue() * dropChance;
				newMap.compute(state, (list, v) -> (v == null ? 0d : v) + combinedDropChance);
			}
			// possible for no items to drop.
			double chanceOfNoItem = 1 - chanceOfAnyItem;
			newMap.compute(stateSoFar.getKey(), (state, probability) -> stateSoFar.getValue() * chanceOfNoItem + (probability == null ? 0 : probability));
		}
		return newMap;
	}

	private final Random random = new Random();

	@RequiredArgsConstructor
	private final class CompletionChanceTable {
		public final Map<List<Integer>, Double> completionChances = new HashMap<>();
		public final int kc;

		public double completionChanceAtKc() {
			return completionChances.entrySet().stream()
				.filter(entry -> isCompleted(entry.getKey(), kc))
				.mapToDouble(entry -> entry.getValue()).sum();
		}

		public double completionChanceOnKc()
		{
			if (kc == 0) return 0d;
			double completionChance = completionChances.entrySet().stream()
				.filter(entry -> isCompleted(entry.getKey(), kc))
				.mapToDouble(entry -> entry.getValue()).sum();
			double completionChanceKcMinusOne = computeCct(kc - 1).completionChances.entrySet().stream()
				.filter(entry -> isCompleted(entry.getKey(), kc - 1 ))
				.mapToDouble(entry -> entry.getValue()).sum();
			return completionChance - completionChanceKcMinusOne;
		}
	}

	private static Map<Integer, CompletionChanceTable> cctMemo = new HashMap<>();

	public CompletionChanceTable computeCct(int kc)
	{
		if (kc == 0) {
			CompletionChanceTable cct = new CompletionChanceTable(0);
			cct.completionChances.put(Collections.nCopies(droprates.length, 0), 1d);
			return cct;
		}
		CompletionChanceTable memoized = cctMemo.get(kc);
		if (memoized != null) {
			return memoized;
		}

		CompletionChanceTable previousCct = computeCct(kc - 1);
		CompletionChanceTable cct = new CompletionChanceTable(kc);
		for (Map.Entry<List<Integer>, Double> completionChance : previousCct.completionChances.entrySet())
		{
			for (Map.Entry<List<Integer>, Double> drop : COMBINED_DROPRATES_WITH_EMPTY.entrySet())
			{
				List<Integer> newState = createNewState(completionChance.getKey(), drop.getKey());
				if (newState == null) newState = completionChance.getKey();
				double chance = completionChance.getValue() * drop.getValue();
				cct.completionChances.compute(newState, (state, previousChance) -> (previousChance == null ? 0d : previousChance) + chance);
			}
		}
		cctMemo.put(kc, cct);
		return cct;
	}

	/**
	 * Turns double droprates into ints. Fixable, but I'm lazy.
	 */
	private int kcToCompleteSim(int iterations)
	{
		return kcToCompleteSim(Collections.nCopies(droprates.length, 0), iterations);
	}

	private int kcToCompleteSim(List<Integer> startState, int iterations)
	{
		long kills = 0;
		for(int i = 0; i < iterations; i++) {
			List<Integer> state = new ArrayList<>(startState);
			int kc = 0;
			while (!isCompleted(state)) {
				outer_loop:
				for (int rollIndex = 0; rollIndex < droprates[0].length; rollIndex++)
				{
					List<Double> drops = new ArrayList<>();
					int denominator = 1;
					for (int j = 0; j < droprates.length; j++)
					{
						drops.add(droprates[j][rollIndex]);
						if (droprates[j][rollIndex] != -1) denominator = lcm(denominator, (int) droprates[j][rollIndex]);
					}
					int finalDenominator = denominator;
					List<Double> numerators = drops.stream().map(rate -> finalDenominator / rate).collect(Collectors.toList());

					int roll = random.nextInt(denominator);
					for (int j = 0; j < numerators.size(); j++)
					{
						int entrySlots = (int) (double) numerators.get(j);
						if (entrySlots < 0) continue;
						if (roll < entrySlots) {
							state.set(j, state.get(j) + 1);
							continue outer_loop;
						} else {
							roll -= entrySlots;
						}
					}
				}
				kc++;
			}
			kills += kc;
		}
		return (int) (kills / iterations);
	}

	public static int lcm(int number1, int number2) {
		if (number1 == 0 || number2 == 0) {
			return 0;
		}
		int absNumber1 = Math.abs(number1);
		int absNumber2 = Math.abs(number2);
		int absHigherNumber = Math.max(absNumber1, absNumber2);
		int absLowerNumber = Math.min(absNumber1, absNumber2);
		int lcm = absHigherNumber;
		while (lcm % absLowerNumber != 0) {
			lcm += absHigherNumber;
		}
		return lcm;
	}

	private Map<List<Integer>, Double> memo = new HashMap<>();

	private double kcToComplete()
	{
		return kcToComplete(Collections.nCopies(droprates.length, 0));
	}
	/**
	 * Returns the average kc remaining to achieve the end condition from the current state.
	 *
	 * Simplifies the problem by assuming that all chests contain at most 1 barrows item. This might have a negligible
	 * impact on the accuracy of the result.
	 */
	private double kcToComplete(List<Integer> state)
	{
		Double memoKc = memo.get(state);
		if (memoKc != null) return memoKc;

		if (isCompleted(state)) {
			memo.put(state, 0d);
			return 0;
		}

		Set<Map.Entry<List<Integer>, Double>> stateToProbability = getPossibleStates(state);
		double probabilitySum = stateToProbability.stream().mapToDouble(e -> e.getValue()).sum();
		double kcRequired = 1 / probabilitySum;
//		System.out.println("kc " + state + " " + kcRequired);
		for (Map.Entry<List<Integer>, Double> entry : stateToProbability)
		{
//			System.out.println("\t" + entry.getKey() + " " + entry.getValue());
			kcRequired += kcToComplete(entry.getKey()) * (entry.getValue() / probabilitySum);
		}

//		System.out.println("putting " + state + " " + kcRequired);
		memo.put(state, kcRequired);
		return kcRequired;
	}

	private Set<Map.Entry<List<Integer>, Double>> getPossibleStates(List<Integer> state)
	{
		Map<List<Integer>, Double> newStates = new HashMap<>();
		for (Map.Entry<List<Integer>, Double> combinedDroprate : COMBINED_DROPRATES.entrySet())
		{
			List<Integer> newState = createNewState(state, combinedDroprate.getKey());
			if (newState == null) continue;
			newStates.compute(newState, (s, probability) -> (probability == null ? 0d : probability) + combinedDroprate.getValue());
		}
		return newStates.entrySet();
	}

	/**
	 * returns null if the difference is not meaningful.
	 */
	private List<Integer> createNewState(List<Integer> state, List<Integer> diff)
	{
		boolean sawChange = false;
		List<Integer> newState = new ArrayList<>();
		for (int i = 0; i < state.size(); i++)
		{
			int newValue = Math.min(state.get(i) + diff.get(i), completed[i]);
			newState.add(newValue);
			if (diff.get(i) != 0 && (newValue < completed[i] || state.get(i) < completed[i])) {
				sawChange = true;
			}
		}
		return sawChange ? Collections.unmodifiableList(newState) : null;
	}

	private void printSomeExtraStatsidk()
	{
		double chanceForAtLeastOneItem = 1 - COMBINED_DROPRATES.entrySet().stream()
			.filter(entry -> entry.getKey().stream().mapToInt(i -> i).sum() == 0)
			.findAny().get().getValue();
		System.out.println("chance for at least one item is " + (1 / chanceForAtLeastOneItem));
		System.out.println("droptable is:");
		double sum = 0d;
//		COMBINED_DROPRATES.entrySet().stream().sorted((e1, e2) -> Double.compare(e1.getValue(), e2.getValue())).collect(Collectors.toList())
		for (Map.Entry<List<Integer>, Double> entry : COMBINED_DROPRATES.entrySet().stream().sorted((e1, e2) -> Double.compare(e1.getValue(), e2.getValue())).collect(Collectors.toList()))
		{
			System.out.println("\t" + entry.getKey() + ": " + (1 / entry.getValue()));
			sum += entry.getValue();
		}
		System.out.println("drop table size (should be approximately 1): " + sum);
	}

	public static void main(String[] args)
	{
		long t = System.currentTimeMillis();
		DropChanceCalculator calculator = new DropChanceCalculator(DROP_RATES_CHAMBERS, CHAMBERS_COMPLETE);

		System.out.println((System.currentTimeMillis() - t) + "ms"); t = System.currentTimeMillis();
		System.out.println("kc to complete bandos with shard minus boots: " + calculator.kcToComplete());
		System.out.println((System.currentTimeMillis() - t) + "ms"); t = System.currentTimeMillis();
//		System.out.println("memo table for above operation:");
//		calculator.printCompletionKcMemoTable();

		double totalkcs = 0;
		for (int i = 0; i < 10000; i++)
		{
			CompletionChanceTable cct = calculator.computeCct(i);
			double probability = cct.completionChanceOnKc();
//			System.out.println(i + "," + cct.completionChanceAtKc() + "," + probability);
			totalkcs += i * probability;
		}
		System.out.println("average kc: " + totalkcs);
		System.out.println((System.currentTimeMillis() - t) + "ms"); t = System.currentTimeMillis();

		System.out.println("simmed completion: " + calculator.kcToCompleteSim(10000));
		System.out.println((System.currentTimeMillis() - t) + "ms"); t = System.currentTimeMillis();
	}

	private void printCompletionKcMemoTable()
	{
		for (Map.Entry<List<Integer>, Double> entry : memo.entrySet())
		{
			System.out.println("\t" + entry.getKey() + ": " + entry.getValue());
		}
	}

}
