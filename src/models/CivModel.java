package models;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;

import components.Tile;

/**
 * Holds game state data and provides utility methods to query or update it.
 *
 * @author Connie Sun, Ryan Smith, Luke Hankins, Tim Gavlick
 */
@SuppressWarnings("deprecation")
public class CivModel extends Observable implements Serializable {

	private CivBoard board;
	private Node curPlayer;
	private Node head;
	private boolean singlePlayer;
	private int round;
	private int numPlayers;
	private ArrayList<int[]> playerStartingCoords;

	/**
	 * Initialize a new model.
	 *
	 * @param playerCount indicates how many players this game will have
	 */
	public CivModel(int playerCount, int map, int size) {
		head = new Node(new Player(1, new String("Player " + 1))); // make a human player
		curPlayer = head;
		if (playerCount == 1) { // if singleplayer
			numPlayers = 2;
			singlePlayer = true;
			Node cpu = new Node(new Player(0, "CPU Player")); // make a cpu player
			head.next = cpu;
			cpu.next = head; // have them wrap around
		} else {
			numPlayers = playerCount;
			singlePlayer = false;
			for (int i = 0; i < playerCount - 1; i++) { // if not singleplayer, add playerCount - 1 more players
				String playerID = "Player " + (i + 2);
				Node player = new Node(new Player(1, playerID)); // that are human
				curPlayer.next = player; // set next
				curPlayer = curPlayer.next; // iter
			}
			curPlayer.next = head; // have it wrap around
		}
		String mapStr = initPlayerStartingCoords(map, size);
		round = 0;
		// System.out.println(mapStr);
		if (map != 4) {
			this.board = new CivBoard(mapStr);
		} else {
			this.board = new CivBoard(size);
		}
		curPlayer = head;
	}

	/**
	 * CivModel is another constructor that will be called if the user wants to load
	 * a previous game state
	 * 
	 * CivModel() unserializes a previously existing game state and sets appropriate
	 * attributes of all relevant classes.
	 * 
	 * @throws NullPointerException if the file save_game.dat does not exist/ can't
	 *                              be opened
	 */
	public CivModel() throws NullPointerException {
		try {
			FileInputStream fileStream = new FileInputStream("save_game.dat");
			ObjectInputStream ois = new ObjectInputStream(fileStream);
			this.board = new CivBoard(ois);
			this.head = (Node) ois.readObject();
			this.singlePlayer = (boolean) ois.readObject();
			this.round = (int) ois.readObject();
			this.numPlayers = (int) ois.readObject();
			this.playerStartingCoords = (ArrayList<int[]>) ois.readObject();
			this.curPlayer = (Node) ois.readObject();
			Node endIter = (Node) ois.readObject();
			curPlayer.next = endIter;
			int i = 0;
			while (i < numPlayers - 2) {
				endIter.next = (Node) ois.readObject();
				endIter = endIter.next;
				i++;
			}
			endIter.next = curPlayer;

		} catch (Exception e) {
			throw new NullPointerException();
		}
	}

	/**
	 * getter method for the tile held at row, col in our Board
	 *
	 * @param row integer representing the outer index into our 2D array board
	 * @param col integer representing the inner index into our 2D array board
	 * @return Tile object contained at row, col loc in our board
	 */
	public Tile getTileAt(int x, int y) {
		return this.board.getTile(x, y);
	}

	/**
	 * for JUnit testing
	 */
	public CivBoard getCivBoard() {
		return this.board;
	}

	/**
	 * getter method for the size of the board for move validity checking
	 *
	 * @return integer specifying the height and width of our board
	 */
	public int getSize() {
		return this.board.getSize();
	}

	/**
	 * Set the state of the model to changed and notify Observers that the model has
	 * been updated. Pass the current game state (board) to all Observers.
	 */
	public void changeAndNotify() {
		this.setChanged();
		this.notifyObservers(this.board);
	}

	/**
	 * getter for Model's current player
	 *
	 * @return Player object whose turn it is
	 */
	public Player getCurPlayer() {
		return this.curPlayer.getPlayer();
	}

	/**
	 * void function allowing turn logic control. Sets cur player to next player.
	 */
	public void nextPlayer() {
		curPlayer = curPlayer.next;
		if (curPlayer.equals(head)) {
			round++;
		}
		// System.out.println(curPlayer.getPlayer().getID());
	}

	/**
	 * Allows the controller to know whether to execute Player Turn logic or
	 * Computer Turn logic.
	 * 
	 * @return If CurPlayer isComputer
	 */
	public boolean isComputer() {
		return !curPlayer.getPlayer().isHuman();
	}

	/**
	 * Keep track of which round it is by counting how many times we go through all
	 * the players Allows the model to know when to level-up cities and more.
	 * 
	 * @return an int specifying how many times we have made it through all of the
	 *         players
	 */

	public int roundNumber() {
		return round;
	}

	/**
	 * Remove a player who has no more cities left from the game
	 * 
	 * @param deadGuy The player whose city was just killed
	 * @return true if it worked, false if it didn't
	 */

	public boolean removePlayer(Player deadGuy) {
		Node prev = head;
		Node cur = head.next;
		Node next = cur.next;
		while (cur.getPlayer() != deadGuy) {
			prev = prev.next;
			cur = cur.next;
			next = next.next;
			if (prev == head)
				return false;
		}
		numPlayers--;
		prev.next = next;
		return true;
	}

	/**
	 * Provides controller access to however many players are in the game
	 * 
	 * @return an int of the number of players in the game
	 */
	public int numPlayers() {
		return numPlayers;
	}

	/**
	 * Get a list of all players in the model.
	 *
	 * @return A flat list of all this model's Player objects
	 */
	public List<Player> getAllPlayers() {
		List<Player> result = new ArrayList<>();

		Node cur = head;
		int i = 0;

		while (cur != null && i < numPlayers) {
			result.add(cur.getPlayer());
			cur = cur.next;
			i++;
		}

		return result;
	}

	/**
	 * Determine whether this is a single-player game (one with a human vs a CPU
	 * player).
	 *
	 * @return True if this is a single-player game, false otherwise
	 */
	public boolean isSinglePlayer() {
		return singlePlayer;
	}

	/**
	 * Initialize player starting coordinates based on map and number of players
	 * 
	 * @param map  int specifying which map the user has selected (1-4)
	 * @param size int specifying the size of the map (only applicable if map 4)
	 * @return a String that will be the location of the map to open, unless map ==
	 *         4
	 */
	private String initPlayerStartingCoords(int map, int size) {
		ArrayList<int[]> allStartingCoords = new ArrayList<int[]>();
		this.playerStartingCoords = new ArrayList<int[]>();
		String mapName = "";
		if (map == 1) { // Map1.txt starting locations
			allStartingCoords.add(new int[] { 1, 1 });
			allStartingCoords.add(new int[] { 18, 18 });
			allStartingCoords.add(new int[] { 18, 1 });
			allStartingCoords.add(new int[] { 1, 18 });
			mapName = "./src/models/Map1.txt";
		} else if (map == 2) { // Map2.txt starting locations
			allStartingCoords.add(new int[] { 3, 2 });
			allStartingCoords.add(new int[] { 18, 18 });
			allStartingCoords.add(new int[] { 18, 1 });
			mapName = "./src/models/Map2.txt";
		} else if (map == 3) {
			allStartingCoords.add(new int[] { 15, 2 });
			allStartingCoords.add(new int[] { 2, 13 });
			mapName = "./src/models/Thermopylae.txt";
		} else if (map == 4) {
			allStartingCoords.add(new int[] { 1, 1 });
			allStartingCoords.add(new int[] { size - 2, size - 2 });
			allStartingCoords.add(new int[] { size - 2, 1 });
			allStartingCoords.add(new int[] { 1, size - 2 });
			mapName = "";
		}
		for (int i = 0; i < numPlayers; i++) {
			playerStartingCoords.add(allStartingCoords.get(i));
		}
		return mapName;

	}

	/**
	 * Provide access to the list of starting coordinates for the controller to
	 * place starting units
	 * 
	 * @return an ArrayList of length-2 int arrays, each containing a starting
	 *         coordinate
	 */
	public ArrayList<int[]> getPlayerStartingCoords() {
		return this.playerStartingCoords;
	}

	/**
	 * returns "Player 1" Player object for controller
	 * 
	 * @return "Player 1" object
	 */
	public Player getHead() {
		return this.head.getPlayer();
	}

	/**
	 * Controller calls done in its close() method. done() saves the whole game
	 * state by writing it to an ObjectOutputStream.
	 * 
	 * @return true if the save was successful, false if it failed.
	 */
	public boolean done() {
		try {
			FileOutputStream fileStream = new FileOutputStream("save_game.dat");
			ObjectOutputStream oos = new ObjectOutputStream(fileStream);
			this.board.serializeBoard(oos);
			oos.writeObject(this.head);
			oos.writeObject(this.singlePlayer);
			oos.writeObject(this.round);
			oos.writeObject(this.numPlayers);
			oos.writeObject(this.playerStartingCoords);
			Node playerWhoseTurnItIs = curPlayer;
			oos.writeObject(curPlayer);
			nextPlayer();
			while (!(curPlayer.equals(playerWhoseTurnItIs))) {
				oos.writeObject(curPlayer);
				nextPlayer();
			}
			oos.close();
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * Node class for keeping a wrapped list of players
	 *
	 * @author Luke
	 * @field player Player object associated with this node
	 * @field next Next node that contains the player whose turn it is next
	 */
	private class Node implements Serializable {
		Player player;
		Node next;

		/**
		 * constructor takes a Player and creates a new node containing that player
		 *
		 * @param player Player object that is the player
		 */
		private Node(Player player) {
			this.player = player;
			next = null;
		}

		/**
		 * getter for Node's player object
		 *
		 * @return Node's player object
		 */
		private Player getPlayer() {
			return this.player;
		}
	}

}
