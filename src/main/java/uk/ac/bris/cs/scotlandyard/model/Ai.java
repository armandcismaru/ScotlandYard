package uk.ac.bris.cs.scotlandyard.model;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Nonnull;

/**
 * All CPU (Ai) players should implement this interface and be on the classpath.
 */
public interface Ai {

	/**
	 * @return the name of your AI, be creative
	 */
	@Nonnull String name();

	/**
	 * Called before the game starts
	 * Defaults to no-op
	 */
	default void onStart() {}

	/**
	 * @param board the game board
	 * @param terminate a flag that signals whether this method should be terminated. This only
	 * makes sense if your Ai takes a lot of time to finish. The general idea is that your
	 * algorithm watches for the flag to signal termination and then give up early by returning
	 * the best possible result at the time of termination.
	 * @return a correct move from {@link Board#getAvailableMoves()} in the game board
	 */
	@Nonnull Move pickMove(@Nonnull Board board, @Nonnull AtomicBoolean terminate);


	/**
	 * Called after the game has ended and that this Ai is about to be terminated
	 * Defaults to no-op
	 */
	default void onTerminate() {}

}
