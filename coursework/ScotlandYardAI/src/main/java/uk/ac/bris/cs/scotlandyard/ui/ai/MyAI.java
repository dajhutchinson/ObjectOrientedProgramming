package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.util.ArrayList;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;

import uk.ac.bris.cs.scotlandyard.ai.ManagedAI;
import uk.ac.bris.cs.scotlandyard.ai.PlayerFactory;
import uk.ac.bris.cs.scotlandyard.model.Colour;
import uk.ac.bris.cs.scotlandyard.model.Move;
import uk.ac.bris.cs.scotlandyard.model.Ticket;
import uk.ac.bris.cs.scotlandyard.model.Player;
import uk.ac.bris.cs.scotlandyard.model.Transport;
import uk.ac.bris.cs.scotlandyard.model.TicketMove;
import uk.ac.bris.cs.scotlandyard.model.DoubleMove;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYardView;

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableCollection;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableSet;
import static java.util.Objects.requireNonNull;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.function.Consumer;
import uk.ac.bris.cs.gamekit.graph.Edge;
import uk.ac.bris.cs.gamekit.graph.Node;
import uk.ac.bris.cs.gamekit.graph.Graph;
import uk.ac.bris.cs.gamekit.graph.ImmutableGraph;

import uk.ac.bris.cs.gamekit.graph.Graph;

@ManagedAI("MyAI")
public class MyAI implements PlayerFactory {

	@Override
	public Player createPlayer(Colour colour) {
		return new MyPlayer();
	}

	private static class MyPlayer implements Player {

		private final Random random = new Random();
		private Graph<Integer, Transport> graph;
		private List<Colour> players;
    private Map<Integer, Colour> colourLocations;
		private List<Optional<Integer>> locations;
		private Map<Move,Integer> scores;
		private ScotlandYardView scotlandView;
		private Set<Ticket> ticketTypes = new HashSet<>();
		private Set<Ticket> detectiveTicketTypes = new HashSet<>();

		@Override
		public void makeMove(ScotlandYardView view, int location, Set<Move> moves, Consumer<Move> callback) {

			players = view.getPlayers();
			locations = new ArrayList<>(); // Locations of detectives
			scores = new HashMap<>();
			graph = view.getGraph();
			scotlandView = view;
			colourLocations = new HashMap<>();


			// Collect all detective locations
			for(Colour player : players){
				if(player.isDetective()){
					locations.add(view.getPlayerLocation(player));
					colourLocations.put(view.getPlayerLocation(player).get(), player);
			}}

			ticketTypes.addAll(Arrays.asList(Ticket.BUS, Ticket.DOUBLE, Ticket.SECRET, Ticket.TAXI, Ticket.UNDERGROUND));
			detectiveTicketTypes.addAll(Arrays.asList(Ticket.BUS, Ticket.UNDERGROUND, Ticket.TAXI));

			System.out.printf("Location - %d.\n", location);
			moves = prune(moves);

			// Score all valid moves
			for(Move move : moves){
				if (move instanceof TicketMove) scores.put(move, sumMove(movesToLocation(((TicketMove)move).destination())));
				else if (move instanceof DoubleMove) scores.put(move, sumMove(movesToLocation(((DoubleMove)move).finalDestination())));
			}


			// Choose best move
			Set<Move> selectedMoves = selectMove(scores);
			System.out.printf("Possible moves %d.\n---------------\n", selectedMoves.size());

			// Make move
			callback.accept(new ArrayList<>(selectedMoves).get(random.nextInt(selectedMoves.size())));

		}

		// Prunes out surplus moves
		private Set<Move> prune(Set<Move> m) {

			// If reveal round
			if (scotlandView.getRounds().get(scotlandView.getCurrentRound())) {
				if (scotlandView.getPlayerTickets(Colour.BLACK, Ticket.DOUBLE).get() > 0) { // If MrX can make a double move
					m = revealRoundDoubleMove(m);
					System.out.printf("m%d - %d\n", 0, m.size());
			}} else if (scotlandView.getPlayerTickets(Colour.BLACK, Ticket.DOUBLE).get() > 0) { // Don't make double move if not a reveal round
				m = removeDoubleMoves(m);
				System.out.printf("m%d - %d\n", 0, m.size());
			}

			System.out.printf("m%d - %d\n", 1, m.size());
			m = removeExpensiveMoves(m);
			System.out.printf("m%d - %d\n", 2, m.size());
			m = removeUnnecessaryDoubleMoves(m);
			System.out.printf("m%d - %d\n", 3, m.size());

			/*  IDEAS
			 *  Don't move to islands.
			 *  Assess risk, then decide if double/secret move is required
			 */

			return m;
		}
		// Removes moves which can be achieved using a more plentiful ticket type.
		private Set<Move> removeExpensiveMoves(Set<Move> m) {
			Map<Integer, Collection<Ticket>> map = new HashMap<>();
			Set<Move> cheapestMoves = new HashSet<>();

			// Builds map of destintations and possible tickets to get to there
			for (Move move : m) {

				if (move instanceof TicketMove) {

					TicketMove t = (TicketMove) move;

					if (!map.containsKey(t.destination())) { // If destination not currently used in map
						Collection<Ticket> tickets = new HashSet<>();
						tickets.add(t.ticket());
						map.put(t.destination(), tickets);
					} else { // If destination already in map
						Collection<Ticket> tickets = map.get(t.destination());
						tickets.add(t.ticket()); // Update collection to include new ticket type
						map.remove(t.destination());
						map.put(t.destination(), tickets);
					}

				} else {

					DoubleMove d = (DoubleMove) move;
					if (!map.containsKey(d.finalDestination())) { // If destination not currently used in map
						Collection<Ticket> tickets = new HashSet<>();
						tickets.add(Ticket.DOUBLE);
						map.put(d.finalDestination(), tickets);
					} else { // If destination already in map
						Collection<Ticket> tickets = map.get(d.finalDestination());
						tickets.add(Ticket.DOUBLE); // Update collection to include double ticket
						map.remove(d.finalDestination());
						map.put(d.finalDestination(), tickets);
					}
				}
			}

			// Builds a set of moves which excludes moves that can be achieved using a more plentiful ticket type
			for (Move move : m) {

				if (move instanceof TicketMove) {

					TicketMove t = (TicketMove) move;
					if (map.get(t.destination()).size() == 1) cheapestMoves.add(t); // If only way to get to destination
					else { // If multiple ways to get to destination
						Boolean cheapest = true;
						Integer num = scotlandView.getPlayerTickets(Colour.BLACK, t.ticket()).get();
						for (Ticket ticket : ticketTypes) { // Check for a more plentiful ticket type
							if (map.get(t.destination()).contains(ticket) && (!t.ticket().equals(ticket))) {
								Integer num1 = scotlandView.getPlayerTickets(Colour.BLACK, ticket).get();
								if (num1 < num) cheapest = false; // If alternative ticket type is more plentiful
						}}
						if (cheapest) cheapestMoves.add(t); // If this is most plentiful ticket to destination
					}

				} else if (move instanceof DoubleMove) {
					DoubleMove d = (DoubleMove) move;
					if (map.get(d.finalDestination()).size() == 1) cheapestMoves.add(d); // If only way to get to destination
					else { // If multiple ways to get to destination
						Boolean cheapest = true;
						Integer num = scotlandView.getPlayerTickets(Colour.BLACK, Ticket.DOUBLE).get();
						for (Ticket ticket : ticketTypes) { // Check for a more plentiful ticket type
							if (map.get(d.finalDestination()).contains(ticket) && (!ticket.equals(Ticket.DOUBLE))) {
								Integer num1 = scotlandView.getPlayerTickets(Colour.BLACK, ticket).get();
								if (num1 < num) cheapest = false; // If alternative ticket type is more plentiful
						}}
						if (cheapest) cheapestMoves.add(d); // If this is most plentiful ticket to destination
					}
				}

			}

			return Collections.unmodifiableSet(cheapestMoves);
		}

		// Removes double moves which can be achieved in one move.
		private Set<Move> removeUnnecessaryDoubleMoves(Set<Move> m) {
			Set<Integer> achievableInSingle = new HashSet<>();
			Set<Move> necessaryMoves = new HashSet<>();

			// Finds all destinations that can be reached without a double ticket
			for (Move move : m) {
				if (move instanceof TicketMove) {
					achievableInSingle.add(((TicketMove)move).destination());
					necessaryMoves.add(move);
				}
			}

			// Builds a set which excludes double moves that can be achieved in a single move
			for (Move move : m) {
				if (move instanceof DoubleMove) {
					if (!achievableInSingle.contains(((DoubleMove)move).finalDestination())) necessaryMoves.add(move);
				}
			}

			return Collections.unmodifiableSet(necessaryMoves);
		}

		// Returns set of only double moves.
		// If MrX has a secret ticket then returns set of only double moves which use a secret ticket as the second move.
		private Set<Move> revealRoundDoubleMove(Set<Move> m) {
			Set<Move> doubleMoves = new HashSet<>();
			Set<Move> secretMoves = new HashSet<>();

			// Builds set of double moves
			for (Move move : m) {
				if (move instanceof DoubleMove) doubleMoves.add(move);
			}

			// If Mr X has a secret ticket
			if (scotlandView.getPlayerTickets(Colour.BLACK, Ticket.SECRET).get() > 0d) {
				// Filter out double moves that don't use a secret ticket for the second move
				for (Move move : doubleMoves) {
					DoubleMove d = (DoubleMove) move;
					if (d.secondMove().ticket().equals(Ticket.SECRET)) secretMoves.add(d);
				}
				return Collections.unmodifiableSet(secretMoves);
			}

			return Collections.unmodifiableSet(doubleMoves);
		}

		// Removes double moves.
		// If no double moves then returns passed set.
		private Set<Move> removeDoubleMoves(Set<Move> m) {
			Set<Move> singleMoves = new HashSet<>();

			for (Move move : m) {
				if (move instanceof TicketMove) {
					singleMoves.add(move);
			}}

			if (singleMoves.size() > 0) return Collections.unmodifiableSet(singleMoves);

			return m;
		}

		// Returns the fewest number of moves required for a detecive to rech a given location.
		private Map<Colour, Integer> movesToLocation(Integer location) {

			Integer count = 1;
			Collection<Edge<Integer, Transport>> e = new HashSet<>();
			Collection<Edge<Integer, Transport>> e2 = new HashSet<>();

			Collection<Edge<Integer, Transport>> edges = graph.getEdgesFrom(new Node<Integer>(location));
			e2.addAll(edges);

			Map<Colour, Integer> movesForColour = new HashMap<>();
			for(Colour player : players) {
				if (player.isDetective()) movesForColour.put(player, -1);
			}

			while(movesForColour.values().contains(-1)){
			// While no detectives on layer
				while(checkLayer(e2).size() == 0) {
					count++;
					e.clear();
					for (Edge<Integer, Transport> edge: e2) {
						e.addAll(graph.getEdgesFrom(new Node<Integer>(edge.destination().value())));
					}
					e2.clear();
					e2.addAll(e);
				}

				for(Colour colour : checkLayer(e2)){
					if(movesForColour.get(colour) == -1){
						movesForColour.remove(colour);
						movesForColour.put(colour, count);
					}
				}

				e.clear();
				for (Edge<Integer, Transport> edge: e2) {
					e.addAll(graph.getEdgesFrom(new Node<Integer>(edge.destination().value())));
				}
				e2.clear();
				e2.addAll(e);

				count++;
			}

			return movesForColour;

		}


		//not used currently
		private List<Integer> possibleLocations(Colour player){

			List<Integer> playerlocations = new ArrayList<>();
			Map<Ticket,Integer> tickets = new HashMap<>();
			Integer location = scotlandView.getPlayerLocation(player).get();

			tickets.put(Ticket.BUS, scotlandView.getPlayerTickets(player, Ticket.BUS).get());
			tickets.put(Ticket.TAXI, scotlandView.getPlayerTickets(player, Ticket.TAXI).get());
			tickets.put(Ticket.UNDERGROUND, scotlandView.getPlayerTickets(player, Ticket.UNDERGROUND).get());

			 Collection<Edge<Integer, Transport>> edges =graph.getEdgesFrom(new Node<Integer>(location));
			 for(Edge<Integer, Transport> edge : edges){
				if(tickets.get(Ticket.fromTransport(edge.data())) > 0) playerlocations.add(edge.destination().value());
			}

			return playerlocations;
		}


		// Returns true if a detective is located at the end of a given edge.
		private Collection<Colour> checkLayer(Collection<Edge<Integer, Transport>> edges) {

			Collection<Colour> colours = new HashSet<>();

			for (Edge<Integer, Transport> edge : edges) {
				if (colourLocations.containsKey(edge.destination().value())) colours.add(colourLocations.get(edge.destination().value()));
			}

			return colours;
		}

		// Returns the sum how far each detecive is from a location
		private Integer sumMove(Map<Colour,Integer> map){
			Collection<Integer> ints = map.values();
			Integer score = 0 ;
			for (Integer i : ints){
				score += i;
			}
			return score;
		}

		// Creates set of highest scoring moves
		private Set<Move> selectMove(Map<Move,Integer> scores){
			Set<Move> selectedMoves = new HashSet<>();
			Set<Move> moves = scores.keySet();
      Integer highScore = 0;

			for(Move move : moves){
				// If move is new highest scorer, clear list then add move
				if(scores.get(move) > highScore){
          selectedMoves.clear();
					selectedMoves.add(move);
					highScore = scores.get(move);
				} else if(scores.get(move) == highScore){ // If move is equal highest scorer, add to list
					selectedMoves.add(move);
				}
			}
			return selectedMoves;
		}

	}
}
