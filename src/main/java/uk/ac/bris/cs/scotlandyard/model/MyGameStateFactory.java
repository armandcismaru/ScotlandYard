package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import uk.ac.bris.cs.scotlandyard.model.Board.GameState;
import uk.ac.bris.cs.scotlandyard.model.Move.Visitor;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Ticket;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Transport;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.stream.Stream;

import static uk.ac.bris.cs.scotlandyard.model.LogEntry.*;

public final class MyGameStateFactory implements Factory<GameState> {

	//the required implementation of GameState
	private static final class MyGameState implements GameState {
		private final GameSetup setup;
		private ImmutableSet<Piece> remaining;
		private ImmutableList<LogEntry> log;
		private Player mrX;
		private List<Player> detectives;
		private final ImmutableSet<Move> moves;
		private ImmutableSet<Piece> winner;

		@Nonnull
		@Override
		public GameState advance(Move move) {
			if (!moves.contains(move)) throw new IllegalArgumentException("Illegal move: " + move);

			// the visitor pattern is used to determine if the move is a doubleMove or singleMove
			Visitor<Boolean> isDouble = new Visitor<>() {
				@Override
				public Boolean visit(Move.SingleMove move) {
					return false;
				}
				@Override
				public Boolean visit(Move.DoubleMove move) {
					for (var t : move.tickets()) if (Objects.equals(t, Ticket.DOUBLE)) return true;
					return false;
				}
			};
			/* the functionality of the advance method is split between the three methods
			note: it would have been more elegant to split the updateLocationAndLog method into
			two but it would have been at the cost of repetitive code */

			updateRemaining(move);
			updateTickets(move);
			updateLocationAndLog(move, move.visit(isDouble));

			return new MyGameState(setup, remaining, log, mrX, detectives);
		}

		private void updateLocationAndLog (Move move, Boolean isDouble) {

			// the visitor pattern is used to get every ticket and destination of a double move
			Visitor<Integer> visitDestination = new Visitor<>() {
				@Override
				public Integer visit(Move.SingleMove singleMove) { return singleMove.destination; }
				@Override
				public Integer visit(Move.DoubleMove doubleMove) { return doubleMove.destination2; }
			};

			Visitor<Integer> visitDestination1 = new Visitor<>() {
				@Override
				public Integer visit(Move.SingleMove singleMove) { return singleMove.destination; }
				@Override
				public Integer visit(Move.DoubleMove doubleMove) { return doubleMove.destination1; }
			};

			Visitor<Ticket> visitTicket1 = new Visitor<>() {
				@Override
				public Ticket visit(Move.SingleMove singleMove) { return singleMove.ticket; }
				@Override
				public Ticket visit(Move.DoubleMove doubleMove) { return doubleMove.ticket1; }
			};

			Visitor<Ticket> visitTicket2 = new Visitor<>() {
				@Override
				public Ticket visit(Move.SingleMove singleMove) { return null; }
				@Override
				public Ticket visit(Move.DoubleMove doubleMove) { return doubleMove.ticket2; }
			};

			// checks if a detective made the move and updates the location of the player
			if (!move.commencedBy().isMrX()) {
				ArrayList<Player> detDummy = new ArrayList<>();
				for (final var i : detectives)
					detDummy.add(Objects.equals(i.piece(), move.commencedBy()) ? i.at(move.visit(visitDestination)) : i);
				detectives = List.copyOf(detDummy);
			}

			// moves mrX and updates the log in terms with the move and reveal rounds
			if (move.commencedBy().isMrX()) {

				ArrayList<LogEntry> updateLog = new ArrayList<>(log);
				boolean isreveal = false, isrevealnext = false;
				var isCurrentRound = 0;

				// checks if the current and next rounds are reveal or not, using the log as round count
				for (final var i : setup.rounds){
					isCurrentRound ++;
					if (isCurrentRound == log.size() + 1) isreveal = i;
					else if (isCurrentRound == log.size() + 2) isrevealnext = i;
				}

				// every possible log entry given any move and round
				if (isDouble && isreveal && isrevealnext){
					updateLog.add(reveal(move.visit(visitTicket1), move.visit(visitDestination1)));
					updateLog.add(reveal(move.visit(visitTicket2), move.visit(visitDestination)));
				}
				else if (isDouble && isreveal){
					updateLog.add(reveal(move.visit(visitTicket1), move.visit(visitDestination1)));
					updateLog.add(hidden(move.visit(visitTicket2)));
				}
				else if (isDouble && isrevealnext) {
					updateLog.add(hidden(move.visit(visitTicket1)));
					updateLog.add(reveal(move.visit(visitTicket2), move.visit(visitDestination)));
				}
				else if (isDouble) {
					updateLog.add(hidden(move.visit(visitTicket1)));
					updateLog.add(hidden(move.visit(visitTicket2)));
				}
				else if (isreveal) updateLog.add(reveal(move.visit(visitTicket1), move.visit(visitDestination)));
				else updateLog.add(hidden(move.visit(visitTicket1)));

				mrX = mrX.at(move.visit(visitDestination));
				log = ImmutableList.copyOf(updateLog);
			}}

		private void updateTickets(Move move) {

			if (!move.commencedBy().isMrX()) {
				ArrayList<Player> detDummy = new ArrayList<>();
				// uses the move ticket from a detective and gives it to mrX or just uses mrX's ticket
				for (var player : detectives)
					if (Objects.equals(move.commencedBy(), player.piece())) {
						detDummy.add(player.use(move.tickets()));
						mrX = mrX.give(move.tickets());
					} else
						detDummy.add(player);
				detectives = List.copyOf(detDummy);
			}
			else mrX = mrX.use(move.tickets());
		}

		private void updateRemaining(Move move) {
			HashSet<Piece> dummyRem = new HashSet<>();
			// if mrX moves then the round ends and it's the detectives's turn
			if (move.commencedBy().isMrX())
				for (final var colour : detectives)
					dummyRem.add(colour.piece());
			else
				/* this bit of code is a bit complicated and the choice of streams made it simpler and organized,
				so the remaining detectives (given they didn't move) are checked for tickets, and if they have none
				their turn is passed, thus updating the list of remaining pieces
				 */
				remaining.stream()
						 .filter(i -> !Objects.equals(move.commencedBy(), i))
						 .forEachOrdered(i -> detectives.stream()
								 .filter(det -> Objects.equals(det.piece(), i) &&
										 Stream.of(Ticket.TAXI, Ticket.BUS, Ticket.UNDERGROUND)
										 .anyMatch(ticket -> det.hasAtLeast(ticket, 1)))
								 .map(det -> i)
								 .forEachOrdered(dummyRem::add));

			if(!dummyRem.isEmpty()) remaining = ImmutableSet.copyOf(dummyRem);
			else if (log.size() == setup.rounds.size()) remaining = ImmutableSet.of();
			else remaining = ImmutableSet.of(Piece.MrX.MRX);
		}

		@Nonnull
			@Override
			public GameSetup getSetup() {
				return setup;
			}

			@Nonnull
			@Override
			public ImmutableSet<Piece> getPlayers() {
				HashSet<Piece> xs = new HashSet<>();
				xs.add (mrX.piece());
				for (Player i : detectives) xs.add(i.piece());
				return ImmutableSet.copyOf(xs);
			}

			@Nonnull
			@Override
			public Optional<Integer> getDetectiveLocation(Piece.Detective detective) {
				for (final var i : detectives)
					if (i.piece() == detective) return Optional.of(i.location());
					return Optional.empty();
			}

			@Nonnull
			@Override
			public Optional<TicketBoard> getPlayerTickets(Piece piece) {
				class nTicketsBoard implements TicketBoard {
					@Override
					public int getCount(@Nonnull Ticket ticket) {
						if (piece.isMrX()) return mrX.tickets().getOrDefault(ticket,1);
						for (final var p : detectives)
							if (p.piece() == piece) return p.tickets().getOrDefault(ticket,1);
							return 0;
					}
				}
				nTicketsBoard myTicketB = new nTicketsBoard();
				Optional<TicketBoard> xs = Optional.of(myTicketB);
				if (piece.isMrX()) return xs;

				for (final var p : detectives)
					if (p.piece() == piece) return xs;
					return Optional.empty();
			}

			@Nonnull
			@Override
			public ImmutableList<LogEntry> getMrXTravelLog() {
				return log;
			}

			@Nonnull
			@Override
			public ImmutableSet<Piece> getWinner() {
			HashSet<Piece> winner = new HashSet<>();
			var detectivesHaveLost = false;

			// if mrX is captured detectives win
				for (var det : detectives) {
					if (det.location() == mrX.location())
						for (var winnerdet : detectives) winner.add(winnerdet.piece());
				}
			// if no detectives have any tickets left they lose
				if (winner.isEmpty()) for (var det : detectives) {
					if (Stream.of(Ticket.TAXI, Ticket.BUS, Ticket.UNDERGROUND).noneMatch(ticket -> det.hasAtLeast(ticket, 1)))
						detectivesHaveLost = true;
					if (Stream.of(Ticket.TAXI, Ticket.BUS, Ticket.UNDERGROUND).anyMatch(ticket -> det.hasAtLeast(ticket, 1))) {
						detectivesHaveLost = false;
						break;
					}
				}
				// if mrX is stuck or cornered detectives win
				if (remaining.contains(Piece.MrX.MRX) && moves.isEmpty())
					for (var winnerdet : detectives) winner.add(winnerdet.piece());

                // also detectives lose if they don't have enough tickets to finish the round
				if (detectivesHaveLost || remaining.isEmpty()) winner.add(Piece.MrX.MRX);
				return ImmutableSet.copyOf(winner);
			}

			@Nonnull
			@Override
			public ImmutableSet<Move> getAvailableMoves() {
				HashSet<Move> movesdummy = new HashSet<>();

				// if the game has a winner no moves should be available anymore
				if (!winner.isEmpty()) return ImmutableSet.of();

				// collects all the moves for mrX using helper methods
				if (remaining.contains(Piece.MrX.MRX)){
					movesdummy.addAll(makeSingleMoves(setup, detectives, mrX, mrX.location()));
					movesdummy.addAll(makeDoubleMoves(setup, detectives, mrX, mrX.location()));
				}
				else
					for (final var piece : remaining)
						// collects moves for all detectives, streams help make the code concise and clean
						detectives.stream()
								  .filter(detective -> Objects.equals(detective.piece(), piece))
								  .map(detective -> makeSingleMoves(setup, detectives, detective, detective.location()))
								  .forEach(movesdummy::addAll);

				return ImmutableSet.copyOf(movesdummy);
			}

		// the constructor of MyGameState
		private MyGameState(final GameSetup setup,
							final ImmutableSet<Piece> remaining,
							final ImmutableList<LogEntry> log,
							final Player mrX,
							final List<Player> detectives) {
			this.setup = setup;
			this.remaining = remaining;
			this.log = log;
			this.mrX = mrX;
			this.detectives = detectives;

			// checks for input correctness
			if (mrX == null || detectives.isEmpty()) throw new NullPointerException();
			if (!mrX.isMrX() ||
				setup.rounds.isEmpty() ||
				setup.graph.nodes().size() == 0) throw new IllegalArgumentException ();

			for (Player i : detectives) {
				for (Player j : detectives) {
					if (i.piece() == j.piece() && i != j) throw new IllegalArgumentException();
					if (i.location() == j.location() && i != j) throw new IllegalArgumentException();
				}
				if (i.has(Ticket.SECRET) || i.has(Ticket.DOUBLE) || !i.isDetective()) throw new IllegalArgumentException();
			}
			winner = ImmutableSet.of();
			moves = getAvailableMoves();
			winner = getWinner();
		}
	}

	@Nonnull @Override public GameState build(
			GameSetup setup,
			Player mrX,
			ImmutableList<Player> detectives) {
		return new MyGameState(setup, ImmutableSet.of(Piece.MrX.MRX), ImmutableList.of(), mrX, detectives);
	}

	private static ImmutableSet<Move.SingleMove> makeSingleMoves(GameSetup setup, List<Player> detectives, Player player, int source) {
		final var singleMoves = new ArrayList<Move.SingleMove>();
		for (int destination : setup.graph.adjacentNodes(source)) {
            // checks that the destination is not occupied
			if (detectives.stream().anyMatch(det -> destination == det.location())) continue;
			// gets the required ticket and makes the moves if there are enough tickets
			for (Transport t : Objects.requireNonNull(setup.graph.edgeValueOrDefault(source, destination, ImmutableSet.of()))) {
				if (player.has(t.requiredTicket()))
					singleMoves.add(new Move.SingleMove(player.piece(), source, t.requiredTicket(), destination));
				if (player.has(Ticket.SECRET))
					singleMoves.add(new Move.SingleMove(player.piece(), source, Ticket.SECRET, destination));
			}
		}
		return ImmutableSet.copyOf(singleMoves);
	}

	private static ImmutableSet<Move.DoubleMove> makeDoubleMoves(GameSetup setup, List<Player> detectives, Player mrX, int source){
		final var doubleMoves = new ArrayList<Move.DoubleMove>();

		if (mrX.has(Ticket.DOUBLE))
			for (final var destination1 : setup.graph.adjacentNodes(source))
			{
				// checks if the first destination is occupied by a detective
				if (detectives.stream().anyMatch(detective1 -> destination1 == detective1.location())) continue;

                // gets the required ticket for the move, t1 is the first and t2 the second ticket of the doubleMove
				Ticket t1 = Ticket.TAXI;
				for (Transport t : Objects.requireNonNull(setup.graph.edgeValueOrDefault(source, destination1, ImmutableSet.of()))) {
					t1 = t.requiredTicket();
				}
				// gets travel options for the second destination of the doubleTicket
				for (final var destination2 : setup.graph.adjacentNodes(destination1)) {
					// second location should not be occupied as well and there should be enough rounds to make a doubleMove
					if (detectives.stream().anyMatch(det2 -> destination2 == det2.location()) || setup.rounds.size() < 2) continue;

                        // gets the second required ticket for the move and adds the moves for which mrX has tickets
						for (Transport t2 : Objects.requireNonNull(setup.graph.edgeValueOrDefault(destination1, destination2, ImmutableSet.of()))) {

							if (mrX.hasAtLeast(t1,1) && mrX.hasAtLeast(t2.requiredTicket(),1) && !Objects.equals(t1,t2.requiredTicket()))
								doubleMoves.add(new Move.DoubleMove(mrX.piece(), source, t1, destination1, t2.requiredTicket(), destination2));
							if (mrX.hasAtLeast(t1,2) && Objects.equals(t1,t2.requiredTicket()))
								doubleMoves.add(new Move.DoubleMove(mrX.piece(), source, t1, destination1, t2.requiredTicket(), destination2));
							if (mrX.has(Ticket.SECRET)) {
								doubleMoves.add(new Move.DoubleMove(mrX.piece(), source, Ticket.SECRET, destination1, Ticket.SECRET, destination2));
								doubleMoves.add(new Move.DoubleMove(mrX.piece(), source, t1, destination1, Ticket.SECRET, destination2));
								doubleMoves.add(new Move.DoubleMove(mrX.piece(), source, Ticket.SECRET, destination1, t2.requiredTicket(), destination2));
							} } }
			}
		return ImmutableSet.copyOf(doubleMoves);
	}
}
