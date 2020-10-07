package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import uk.ac.bris.cs.scotlandyard.model.Model.Observer;
import uk.ac.bris.cs.scotlandyard.model.Model.Observer.Event;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;

import javax.annotation.Nonnull;
import java.util.HashSet;

public final class MyModelFactory implements Factory<Model> {

	private Board.GameState modelState;
	private ImmutableSet<Observer> observers;

	// anonymous inner class instantiation of the Model interface
	Model myModel = new Model() {
		@Nonnull
		@Override
		public Board getCurrentBoard() {
			return modelState;
		}

		@Override
		public void registerObserver(@Nonnull Observer observer) {
			if (observers.contains(observer)) throw new IllegalArgumentException("Already registered!");

			HashSet<Observer> dummyObs = new HashSet<>(observers);
			dummyObs.add(observer);
			observers = ImmutableSet.copyOf(dummyObs);
		}

		@Override
		public void unregisterObserver(@Nonnull Observer observer) {
			if (observer == null) throw new NullPointerException();
			if (!observers.contains(observer)) throw new IllegalArgumentException("Not yet registered!");

			HashSet<Observer> dummyObs = new HashSet<>();
			for (final var o : observers)
				if (!observer.equals(o)) dummyObs.add(o);
			observers = ImmutableSet.copyOf(dummyObs);
		}

		@Nonnull
		@Override
		public ImmutableSet<Observer> getObservers() {
			return observers;
		}

		@Override
		public void chooseMove(@Nonnull Move move) {
			modelState = modelState.advance(move);
			var event = modelState.getWinner().isEmpty() ? Event.MOVE_MADE : Event.GAME_OVER;
			for (Observer o : observers) o.onModelChanged(modelState, event);
		}
	};

	@Nonnull @Override public Model build(GameSetup setup,
	                                      Player mrX,
	                                      ImmutableList<Player> detectives) {
		observers = ImmutableSet.of();
		modelState = new MyGameStateFactory().build(setup, mrX, detectives);
		return myModel;
	}
}
