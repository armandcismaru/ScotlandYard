package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;

import uk.ac.bris.cs.scotlandyard.model.Piece.Detective;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Ticket;

/**
 * A POJO representing an immutable game board.
 * This is useful for snapshotting or serialising game states.
 * <br>
 * <strong>NOTE:</strong>
 * This class isn't really intended for use with the cw-model part but if you can justify the use
 * then feel free to include it.
 */
public final class ImmutableBoard implements Board, Serializable {
	private static final long serialVersionUID = -7495825440220065823L;

	private final GameSetup setup;
	private final ImmutableMap<Detective, Integer> detectiveLocations;
	private final ImmutableMap<Piece, ImmutableMap<Ticket, Integer>> tickets;
	private final ImmutableList<LogEntry> mrXTravelLog;
	private final ImmutableSet<Piece> winner;
	private final ImmutableSet<Move> availableMoves;

	public ImmutableBoard(GameSetup setup,
	                      ImmutableMap<Detective, Integer> detectiveLocations,
	                      ImmutableMap<Piece, ImmutableMap<Ticket, Integer>> tickets,
	                      ImmutableList<LogEntry> mrXTravelLog,
	                      ImmutableSet<Piece> winner,
	                      ImmutableSet<Move> availableMoves) {
		this.setup = Objects.requireNonNull(setup);
		this.detectiveLocations = Objects.requireNonNull(detectiveLocations);
		this.tickets = Objects.requireNonNull(tickets);
		this.mrXTravelLog = Objects.requireNonNull(mrXTravelLog);
		this.winner = Objects.requireNonNull(winner);
		this.availableMoves = Objects.requireNonNull(availableMoves);
	}

	@Nonnull @Override public GameSetup getSetup() { return setup; }
	@Nonnull @Override public ImmutableSet<Piece> getPlayers() { return tickets.keySet(); }
	@Nonnull @Override public Optional<Integer> getDetectiveLocation(Detective detective) {
		return Optional.ofNullable(detectiveLocations.get(detective));
	}
	@Nonnull @Override public Optional<TicketBoard> getPlayerTickets(Piece piece) {
		return Optional.ofNullable(tickets.get(piece))
				.map(tickets -> ticket -> tickets.getOrDefault(ticket, 0));
	}
	@Nonnull @Override public ImmutableList<LogEntry> getMrXTravelLog() { return mrXTravelLog; }
	@Nonnull @Override public ImmutableSet<Piece> getWinner() { return winner; }
	@Nonnull @Override public ImmutableSet<Move> getAvailableMoves() { return availableMoves; }
}
