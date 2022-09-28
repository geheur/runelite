package net.runelite.client.plugins.hiscore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

public class KillSpeedDistribution
{
	private static final List<List<BossHpState>> states = new ArrayList<>();

	@RequiredArgsConstructor
	@EqualsAndHashCode
	private static class BossHpState {
		final int ticks;
		final int hp;
	}

	public static void mole()
	{
//		int maxhit = 64;
//		double accuracy = .5053;
//		int attackspeed = 4;
		int maxhit = 62;
		double accuracy = .8306;
		int attackspeed = 4;

		Map<Integer, Double> results = new HashMap<>();
		int MAX_HITS = 100;
		Map<BossHpState, Double> lastStates = new HashMap<>();
//		lastStates.put(new BossHpState(0, 200), 1.0);
		lastStates.put(new BossHpState(0, 1100), 1.0);
		for (int i = 0; i < MAX_HITS; i++) {
			Map<BossHpState, Double> newStates = new HashMap<>();
			for (Map.Entry<BossHpState, Double> bossHpState : lastStates.entrySet())
			{
				double probabilityMiss = (1.0 - accuracy) * bossHpState.getValue();
				double probabilityHit = (accuracy * (1.0 / (maxhit + 1))) * bossHpState.getValue();

				int ticks = bossHpState.getKey().ticks + attackspeed;
				newStates.compute(
					new BossHpState(ticks, bossHpState.getKey().hp),
					(state, likelihood) -> (likelihood == null ? 0 : likelihood) + probabilityMiss
				);

				for (int damage = 0; damage <= maxhit; damage++)
				{
					int newHp = bossHpState.getKey().hp - damage;
					if (newHp < 0) {
						results.compute(
							ticks,
							(t, probability) -> (probability == null ? 0 : probability) + probabilityHit
						);
					} else
					{
						newStates.compute(
							new BossHpState(ticks, newHp),
							(state, likelihood) -> (likelihood == null ? 0 : likelihood) + probabilityHit
						);
					}
				}
			}
			lastStates = newStates;
		}
		System.out.println(results);
		double d = 0.0;
		for (Map.Entry<Integer, Double> entry : results.entrySet())
		{
			if (entry.getKey() <= 29 * 4) {
				d += entry.getValue()	;
			}
		}
		System.out.println(d);
	}

	@RequiredArgsConstructor
	@EqualsAndHashCode
	private static class HydraState {
		enum State {
			GREEN_VENT_LURE,
			GREEN_VENT_WAIT,
			GREEN_VENT_ATTACK,
			BLUE_VENT_WAIT,
			BLUE_VENT_ATTACK,
			RED_VENT_WAIT_INCLUDE_LIGHTNING_SKIP,
			RED_VENT_ATTACK,
			BLACK_ATTACK,
		}

		final State state;
		/**
		 * vents fire when ticks % 8 == ventOffset.
		 */
		final int ventOffset;
		final int clawSpecs;
		final int ticks;
		final int hp;
	}

	public static void hydra() {
		int maxhit = 62;
		double accuracy = .8306;
		int attackspeed = 4;

		Map<Integer, Double> results = new HashMap<>();
		Map<BossHpState, Double> lastStates = new HashMap<>();
		lastStates.put(new BossHpState(0, 200), 1.0);

		for (int i = 0; i < 100; i++) {
			Map<BossHpState, Double> newStates = new HashMap<>();
			for (Map.Entry<BossHpState, Double> bossHpState : lastStates.entrySet())
			{
				double probabilityMiss = (1.0 - accuracy) * bossHpState.getValue();
				double probabilityHit = (accuracy * (1.0 / (maxhit + 1))) * bossHpState.getValue();

				int ticks = bossHpState.getKey().ticks + attackspeed;
				newStates.compute(
					new BossHpState(ticks, bossHpState.getKey().hp),
					(state, likelihood) -> (likelihood == null ? 0 : likelihood) + probabilityMiss
				);

				for (int damage = 0; damage <= maxhit; damage++)
				{
					int newHp = bossHpState.getKey().hp - damage;
					if (newHp < 0) {
						results.compute(
							ticks,
							(t, probability) -> (probability == null ? 0 : probability) + probabilityHit
						);
					} else
					{
						newStates.compute(
							new BossHpState(ticks, newHp),
							(state, likelihood) -> (likelihood == null ? 0 : likelihood) + probabilityHit
						);
					}
				}
			}
			lastStates = newStates;
		}
		System.out.println(results);
		double d = 0.0;
		for (Map.Entry<Integer, Double> entry : results.entrySet())
		{
			if (entry.getKey() <= 20) {
				d += entry.getValue()	;
			}
		}
		System.out.println(d);
	}

	public static void main(String[] args)
	{
		mole();
	}

}
