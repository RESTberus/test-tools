package es.us.isa.restest.util;

import org.apache.commons.lang3.RandomStringUtils;

import java.util.Random;

/**
 * Central source of randomness for RESTest (restberus addition).
 *
 * <p>Upstream RESTest draws from several independent, unseedable sources:
 * {@code new Random()} in the generators, {@code SecureRandom} in the mutation rules,
 * {@code ThreadLocalRandom} in the dictionaries and the static {@code RandomStringUtils}
 * helpers. That makes a run impossible to replay. Every one of those call sites now goes
 * through this class, so setting the {@code RANDOM_SEED} environment variable (which
 * restberus injects per run) makes the whole generation deterministic.</p>
 *
 * <p>Without {@code RANDOM_SEED} the behaviour is the original one: each component gets
 * its own unseeded {@link Random}.</p>
 *
 * <p>Component streams are derived from the base seed and the component name, not from
 * the order in which components are created, so a component's stream does not shift when
 * an unrelated part of the run changes.</p>
 */
public class RESTestRandom {

	public static final String SEED_ENV_VAR = "RANDOM_SEED";

	private static final Long BASE_SEED = readSeed();
	private static final Random SHARED = forComponent("shared");

	private RESTestRandom() {
	}

	private static Long readSeed() {
		String value = System.getenv(SEED_ENV_VAR);
		if (value == null || value.trim().isEmpty()) {
			return null;
		}
		try {
			return Long.parseLong(value.trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	/**
	 * @return true if a valid seed was provided via the environment.
	 */
	public static boolean isSeeded() {
		return BASE_SEED != null;
	}

	/**
	 * @return the shared random source, used by the static call sites (dictionaries,
	 *         mutation operators, string helpers).
	 */
	public static Random shared() {
		return SHARED;
	}

	/**
	 * @param component name identifying the caller, e.g. the class name.
	 * @return a reproducible random stream, independent from the other components'.
	 */
	public static Random forComponent(String component) {
		if (BASE_SEED == null) {
			return new Random();
		}
		return new Random(BASE_SEED ^ (component.hashCode() * 0x9E3779B97F4A7C15L));
	}

	/**
	 * @return a seed for the APIs that manage their own generator (e.g. commons-math's
	 *         RandomDataGenerator, or the test case generators that log their seed).
	 */
	public static long nextSeed() {
		return SHARED.nextLong();
	}

	public static int nextInt(int bound) {
		return SHARED.nextInt(bound);
	}

	/**
	 * ThreadLocalRandom-compatible: origin inclusive, bound exclusive.
	 */
	public static int nextInt(int origin, int bound) {
		return origin + SHARED.nextInt(bound - origin);
	}

	public static double nextDouble(double origin, double bound) {
		return origin + SHARED.nextDouble() * (bound - origin);
	}

	public static boolean nextBoolean() {
		return SHARED.nextBoolean();
	}

	public static String randomAlphabetic(int count) {
		return RandomStringUtils.random(count, 0, 0, true, false, null, SHARED);
	}

	/**
	 * @param minLengthInclusive minimum length, inclusive.
	 * @param maxLengthExclusive maximum length, exclusive.
	 */
	public static String randomAlphabetic(int minLengthInclusive, int maxLengthExclusive) {
		return randomAlphabetic(nextInt(minLengthInclusive, maxLengthExclusive));
	}

	public static String randomAlphanumeric(int count) {
		return RandomStringUtils.random(count, 0, 0, true, true, null, SHARED);
	}

	public static String randomNumeric(int count) {
		return RandomStringUtils.random(count, 0, 0, false, true, null, SHARED);
	}

	public static String randomAscii(int count) {
		return RandomStringUtils.random(count, 32, 127, false, false, null, SHARED);
	}
}
