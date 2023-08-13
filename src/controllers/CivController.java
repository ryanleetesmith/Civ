package controllers;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

import components.City;
import components.Settler;
import components.Tile;
import components.Unit;
import models.CivModel;
import models.Player;

/**
 * Provides methods to calculate data about game state or act as a computer
 * player that updates game state. This class contains all of the logic
 * necessary to play the game, such as creating units, moving units, and
 * determining when the game is over.
 *
 * @author Connie Sun, Ryan Smith, Luke Hankins, Tim Gavlick
 */
public class CivController {

	private final CivModel model;
	private Player curPlayer;

	/**
	 * Constructor for controller
	 *
	 * @param model model that the controller will interact with to store the
	 *              results of the operations it performs
	 */
	public CivController(CivModel model) {
		this.model = model;
		curPlayer = model.getCurPlayer();
	}

	/**
	 * Configure the map with the units that should exist at the start of a new game
	 * 
	 * Each player starts with a Settler, whose initial coordinates are retrieved
	 * from the model. This method must iterate through all the Players once before
	 * the game begins to ensure each unit is matched with the correct owner player.
	 *
	 */
	public void placeStartingUnits() {
		ArrayList<int[]> startingCoords = model.getPlayerStartingCoords();
		for (int i = 0; i < startingCoords.size(); i++) {
			curPlayer = model.getCurPlayer();
			int[] coord = startingCoords.get(i);
			Settler settler = new Settler(model.getCurPlayer(), new Point(coord[0], coord[1]));
			model.getTileAt(coord[0], coord[1]).setUnit(settler);
			model.getCurPlayer().addUnit(settler);
			revealTiles(settler);
			model.nextPlayer();
		}
		// go back to player 1 to start the game
		curPlayer = model.getHead();
	}

	/**
	 * Returns the Tile located at the board location x, y
	 *
	 * @param x int for x of board positionn
	 * @param y int for y of board position
	 *
	 * @return the Tile at x,y on the board
	 *
	 */
	public Tile getTileAt(int x, int y) {
		return model.getTileAt(x, y);
	}

	/**
	 * Starts a player's turn by doing all of the "housekeeping" automatic game
	 * events for a player turn
	 *
	 * All Units have their movement reset, all Cities owned by a Player are
	 * incremented and updated. Do computer turn if it is the computer's turn.
	 * 
	 * @param player
	 */
	public void startTurn() {
		curPlayer = model.getCurPlayer();
		for (Unit u : curPlayer.getUnits()) {
			u.resetMovement();
			u.healUnit();
		}
		for (City c : curPlayer.getCities()) {
			c.cityIncrement();
			updateCity(c);
		}
		if (!curPlayer.isHuman())
			computerTurn();
		model.changeAndNotify();
	}

	/**
	 * Player ends their turn; the model moves on to the next player. This will
	 * update the curPlayer in model to be retrieved by controller for when the next
	 * turn begins. Notify the model if the game is over so that view is updated
	 * accordingly.
	 */
	public void endTurn() {
		if (gameOver()) {
			model.changeAndNotify();
			return;
		}
		model.nextPlayer();
		startTurn();
		model.changeAndNotify();
	}

	/**
	 * When there is only 1 player left, the game is won.
	 *
	 * @return true if the game is over, false otherwise
	 */
	public boolean gameOver() {
		return model.numPlayers() == 1;
	}

	/**
	 * To determine if it is currently a human's turn or not
	 *
	 * @return true if it's a human turn, false otherwise
	 */
	public boolean isHumanTurn() {
		return curPlayer.isHuman();
	}

	/**
	 * Perform AI turn actions.
	 *
	 * The computer loops through all of its cities and does city actions, then
	 * loops through all its units and does unit actions. Settlers found cities, the
	 * first few units stay by their origin city and defend it, and the rest of the
	 * units move towards enemy cities to attack them. Calls endTurn() automatically
	 * when all actions are completed.
	 */
	public void computerTurn() {
		for (City c : curPlayer.getCities()) {
			computerCityActions(c);
		}
		int firstFew = 2;
		int i = 0;
		while (i < curPlayer.getUnits().size()) {
			int oldSize = curPlayer.getUnits().size();
			Unit u = curPlayer.getUnits().get(i);
			if (u instanceof Settler) {
				computerSettlerActions((Settler) u);
			}
			// these ones are defending the city
			else if (firstFew > 0) {
				computerDefenderActions(u);
				firstFew--;
			} else {
				// move towards enemy city/attack it
				computerAttackerActions(u);
			}
			if (curPlayer.getUnits().size() == oldSize)
				i++;
		}
		model.changeAndNotify();
		endTurn();
	}

	/**
	 * Actions for computer's settlers.
	 * 
	 * Try to found a city as soon as possible. Otherwise, move randomly and avoid
	 * attacking (this is only necessary if the computer is producing settlers, as
	 * these new settlers must move out of the city radius of control to found a new
	 * city).
	 *
	 * @param s a Settler owned by the computer player
	 */
	private void computerSettlerActions(Settler s) {
		boolean founded = foundCity(s.getX(), s.getY()); // try to found a city
		if (!founded) {
			HashSet<int[]> validMoves = getValidMoves(s);
			boolean moved = true;
			while (validMoves.size() != 0 && moved) { // continue moving while able
				moved = false;
				for (int[] move : validMoves) { // random set of moves
					if (getTileAt(move[0], move[1]).getUnit() == null) { // don't want to attack
						moveUnit(s, move[0], move[1]);
						moved = true;
						break;
					}
				}
				validMoves = getValidMoves(s);
			}
		}
	}

	/**
	 * Actions that the computer takes for the first two non-settler units.
	 * 
	 * These units remain close to the city and defend it against attackers. They
	 * move out of the city if newly created. They move randomly each turn and
	 * exhaust their movement to try to find an enemy unit to attack. These units
	 * will avoid moving into the city tile.
	 *
	 * @param u a Unit owned by the computer player defending computer's city
	 */
	private void computerDefenderActions(Unit u) {
		HashSet<int[]> validMoves = getValidMoves(u);
		if (getTileAt(u.getX(), u.getY()).isCityTile()) { // if newly created unit, move out of city
			validMoves = getValidMoves(u);
			for (int[] move : validMoves) {
				moveUnit(u, move[0], move[1]);
				break;
			}
		}
		// the unit should be no more than one tile away from the city it's defending
		Integer[] cityCoords = new Integer[] { -1, -1 };
		for (int i = -1; i <= 1; i++) {
			for (int j = -1; j <= 1; j++) {
				Tile t = getTileAt(u.getX() + i, u.getY() + j);
				if (t != null && t.isCityTile()) {
					cityCoords[0] = u.getX() + i;
					cityCoords[1] = u.getY() + j;
				}
			}
		}
		validMoves = getValidMoves(u);
		while (validMoves.size() != 0) { // continue moving while able
			boolean moved = false;
			for (int[] move : validMoves) {
				// don't move too far away from the city
				if (Math.abs(cityCoords[0] - move[0]) > 1 || Math.abs(cityCoords[1] - move[1]) > 1)
					continue;
				// don't move into the city
				if (cityCoords[0] == move[0] && cityCoords[1] == move[1])
					continue;
				if (getTileAt(move[0], move[1]).getUnit() != null) { // always want to attack
					moveUnit(u, move[0], move[1]);
					moved = true;
					break;
				}
			}
			if (!moved) {
				for (int[] move : validMoves) {
					// don't move too far away from the city
					if (Math.abs(cityCoords[0] - move[0]) > 1 || Math.abs(cityCoords[1] - move[1]) > 1)
						continue;
					// don't move into the city
					if (cityCoords[0] == move[0] && cityCoords[1] == move[1])
						continue;
					moveUnit(u, move[0], move[1]); // take the first good move
					moved = true;
					break;
				}
			}
			if (!moved)
				return;
			validMoves = getValidMoves(u);
		}
	}

	/**
	 * Actions for computer units that move towards and attack enemy cities.
	 * 
	 * Find the closest enemy city and move towards it by calling the moveTowards()
	 * method.
	 *
	 * @param u a Unit owned by the computer player attacking enemy city
	 */
	private void computerAttackerActions(Unit u) {
		// search the entire map for the user's closest city
		Integer[] closest = new Integer[] { -1, -1 };
		int minDist = Integer.MAX_VALUE;
		for (int i = 0; i < model.getSize(); i++) {
			for (int j = 0; j < model.getSize(); j++) {
				Tile t = getTileAt(i, j);
				if (t.isCityTile()) {
					City c = t.getOwnerCity();
					if (c.getOwner() != curPlayer) {
						int dist = Math.max(Math.abs(c.getX() - u.getX()), Math.abs(c.getY() - u.getY()));
						if (dist < minDist) {
							minDist = dist;
							closest[0] = c.getX();
							closest[1] = c.getY();
						}
					}
				}
			}
		}
		if (closest[0] == -1) // no cities left to attack
			return;
		moveTowards(u, closest);
	}

	/**
	 * Moves the unit towards the target coords by searching for the ideal move
	 * first, then choosing any good move, and moving randomly if there is no "good"
	 * move. If target is in range, attack target.
	 *
	 * If y distance to target is greater than x distance, move in the y direction,
	 * and vice versa. If equal, move diagonally. If no move exists for these
	 * "better" choices, choose a random move. Continue moving until the unit's
	 * movement is fully depleted.
	 *
	 * @param u      the Unit to be moved
	 * @param target Integer[] of size 2 representing the x,y target location that
	 *               the unit is to be moved towards
	 */
	private void moveTowards(Unit u, Integer[] target) {
		HashSet<int[]> validMoves = getValidMoves(u);
		while (validMoves.size() != 0) {
			if (getTileAt(target[0], target[1]).getOwnerCity() == null)
				return;
			int xDiff = target[0] - u.getX();
			int yDiff = target[1] - u.getY();
			int priority = 0; // give x direction priority
			if (Math.abs(yDiff) > Math.abs(xDiff))
				priority = 1; // y diff is greater, so give y priority
			int goodX = Integer.signum(xDiff) + u.getX(); // ideal x to go to
			int goodY = Integer.signum(yDiff) + u.getY(); // ideal y to go to
			boolean moved = false;
			for (int[] move : validMoves) { // loop for the ideal move
				if ((move[0] == target[0] && move[1] == target[1]) || // city, attack
						move[0] == goodX && move[1] == goodY) { // ideal move
					moveUnit(u, move[0], move[1]);
					moved = true;
					break;
				}
			}
			if (!moved) { // if not moved, loop again for good move
				for (int[] move : validMoves) {
					if ((priority == 0 && move[0] == goodX) || (priority == 1 && move[1] == goodY)) {
						moveUnit(u, move[0], move[1]);
						moved = true;
						break;
					}
				}
			}
			// got through all the moves and didn't move, just make a random move
			if (!moved) {
				Iterator<int[]> iterator = validMoves.iterator();
				int[] move = iterator.next();
				moveUnit(u, move[0], move[1]);
			}
			validMoves = getValidMoves(u);
		}

	}

	/**
	 * Computer cities crank out warrior fodder each turn.
	 *
	 * @param c computer city attempting to create units
	 */
	private void computerCityActions(City c) {
		createUnit(c.getX(), c.getY(), "Warrior");
		// note that in defender and attacker actions, created units will be moved out
		// of the city
	}

	/**
	 * Moves a unit from its old location to the new player-specified location.
	 *
	 * Unit moves one tile at a time. If an enemy unit or city on the location to
	 * move to, the move is an attack. Units cannot move on tiles with friendly
	 * units. Depletes remaining movement for the unit to move. Also deals with the
	 * case where the unit dies in a counterattack.
	 *
	 * @param toMove the Unit that is attempting a move/attack
	 * @param newx   int of new x location of unit
	 * @param newy   int of new y location of unit
	 * @return true if the unit successfully moved/attacked, false otherwise
	 */
	public boolean moveUnit(Unit toMove, int newX, int newY) {
		int oldX = toMove.getX(), oldY = toMove.getY();
		Tile moveFrom = getTileAt(oldX, oldY);
		int movement = toMove.getMovement();
		// this conditional checks that the unit is only moving 1 space
		if (Math.abs(newX - oldX) > 1 || Math.abs(newY - oldY) > 1)
			return false;
		Tile moveTo = getTileAt(newX, newY);
		int cost = -moveTo.getMovementModifier();
		if (cost + 1 > movement)
			return false;
		Unit onTile = moveTo.getUnit();
		boolean movesOnto = true;
		if (onTile != null) { // unit exists here, attack it
			if (onTile.getOwner().equals(curPlayer))
				return false;
			movesOnto = attack(moveFrom, moveTo);
			cost = toMove.getMovement() - 1; // have to deplete to if successful move
		} else if (moveTo.isCityTile() && !moveTo.getOwnerCity().getOwner().equals(curPlayer)) // city, attack
			movesOnto = attack(moveFrom, moveTo.getOwnerCity());
		if (movesOnto) {
			moveFrom.setUnit(null); // unit gone
			moveTo.setUnit(toMove); // successfully moves to new tile
			toMove.move(cost + 1, newX, newY); // update costs and unit location
			revealTiles(toMove); // reveal tiles around unit
		}
		model.changeAndNotify();
		if (moveTo.getUnit() != null) { // died in counterattack
			if (moveTo.getUnit().getOwner() != curPlayer && moveFrom.getUnit() == null) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Set all the tiles in the unit's sight range around the given location as
	 * revealed for that unit's owner.
	 *
	 * @param unit the Unit who is revealing tiles
	 */
	private void revealTiles(Unit unit) {
		int sight = unit.getSight();
		for (int i = -sight; i <= sight; i++) {
			for (int j = -sight; j <= sight; j++) {
				int toRevealRow = unit.getX() + i;
				int toRevealCol = unit.getY() + j;
				Tile toRevealTile = getTileAt(toRevealRow, toRevealCol);
				if (toRevealTile != null && !toRevealTile.canSeeTile(unit.getOwner()))
					toRevealTile.revealTile(unit.getOwner());
			}
		}
	}

	/**
	 * Unit on attackerTile attacks the Unit on defenderTile.
	 *
	 * Unit gets attack modifier based on its current terrain; defender gets to
	 * counterattack. Movement for attacker unit is set to 0, as attack can only
	 * happen once per Unit turn. Removes a unit from the board and player if the
	 * unit dies in attack/counterattack
	 *
	 * @param attackerTile the Tile where the attacker Unit is located
	 * @param defenderTile the Tile where where the defender Unit is located
	 * @return true if the attack was successful (meaning the attacker killed the
	 *         unit and can move onto that space), false otherwise
	 */
	private boolean attack(Tile attackerTile, Tile defenderTile) {
		Unit attacker = attackerTile.getUnit();
		Unit defender = defenderTile.getUnit();
		double attack = attacker.getAttackValue();
		attack *= attackerTile.getAttackModifier();
		defender.takeAttack(attack);
		if ((int) defender.getHP() <= 0) {
			defenderTile.setUnit(null);
			defender.getOwner().removeUnit(defender);
			return !defenderTile.isCityTile();
		}
		double counterattack = defender.getAttackValue();
		counterattack *= defenderTile.getAttackModifier();
		attacker.takeAttack(counterattack);
		if ((int) attacker.getHP() <= 0) {
			attacker.move(attacker.getMovement(), attacker.getX(), attacker.getY());
			curPlayer.removeUnit(attacker);
			attackerTile.setUnit(null);
			return false;
		}
		attacker.move(attacker.getMovement(), attacker.getX(), attacker.getY()); // failed move
		return false;
	}

	/**
	 * Overloaded method, to be called when a unit attacks a city
	 *
	 * Attacker attacks the city; city's health is checked. If <= 0, the city is
	 * defeated and removed from the owner's list of cities. This is how the game
	 * ends, so checks the end game condition as well.
	 *
	 * @param attackerTile the Tile where the attacker Unit is located.
	 * @param defender     the City that is taking the attack
	 * @return false for the moveUnit() method; never move onto a city tile
	 */
	private boolean attack(Tile attackerTile, City defender) {
		Unit attacker = attackerTile.getUnit();
		double attack = attacker.getAttackValue();
		attack *= attackerTile.getAttackModifier();
		defender.takeAttack(attack);
		if ((int) defender.getRemainingHP() <= 0) {
			getTileAt(defender.getX(), defender.getY()).destroyCity();
			Player lostACity = defender.getOwner();
			lostACity.removeCity(defender);
			if (lostACity.getCities().size() == 0) {
				model.removePlayer(lostACity); // player has no cities left, remove from game
			}
		}
		attacker.move(attacker.getMovement(), attacker.getX(), attacker.getY()); // set move to 0
		return false;
	}

	/**
	 * Creates a unit on the given location.
	 *
	 * Assumes that a valid tile coord is passed (i.e., only a tile with a city on
	 * it can create a unit). Checks that the city has enough in its production
	 * reserve to produce the specified unit type, checks that a unit does not
	 * already exist on that tile, and checks that the specified type is a
	 * producible (unlocked) unit. Updates the tile so that it has the new unit on
	 * it. Also depletes the movement, as newly created units cannot move.
	 *
	 * @param x        int representing the x location of new unit (city tile)
	 * @param y        int representing the y location of new unit (city tile)
	 * @param unitType String representing the type of unit to create
	 * @return true if the unit was successfully created; false otherwise
	 */
	public boolean createUnit(int x, int y, String unitType) {
		Tile tile = getTileAt(x, y);
		City city = tile.getOwnerCity();
		if (city.getProductionReserve() >= Unit.unitCosts.get(unitType) && tile.getUnit() == null
				&& city.getProducableUnits().contains(unitType)) {
			Unit newUnit = city.produceUnit(unitType);
			tile.setUnit(newUnit);
			newUnit.move(newUnit.getMovement(), x, y);
			city.getOwner().addUnit(newUnit);
			model.changeAndNotify();
			return true;
		}
		return false;
	}

	/**
	 * Found a city on the tile at x, y in the board
	 *
	 * Checks that the settler is still able to found a city and checks that the
	 * tile to found the city upon is not already within the territory of an
	 * existing city. Adds the new city to the current player's list of cities.
	 *
	 * @param x int of x location of new city (settler location)
	 * @param y int of y location of new city (settler location)
	 * @return true if a city was sucessfully founded; false otherwise
	 */
	public boolean foundCity(int x, int y) {
		Tile tile = getTileAt(x, y);
		Settler settler = (Settler) tile.getUnit();
		if (settler != null && settler.getCharges() > 0 && tile.getOwnerCity() == null) {
			City city = settler.foundCity();
			tile.foundCity(city);
			curPlayer.removeUnit(settler);
			tile.setUnit(null);
			createUnit(x, y, "Scout");
			model.changeAndNotify();
			return true;
		}
		return false;
	}

	/**
	 * Expand the city's influence and check for resources on each added tile
	 * 
	 * Iterates through the city's control radius and checks the new tiles in the
	 * updated range. Updates all of the tiles within the city's control to be owned
	 * by that city, and makes each tile check for a newly owned resource.
	 *
	 * @param c the City whose resources are to be updated
	 */
	private void updateCity(City c) {
		int range = c.getControlRadius();
		int top = c.getY() - range;
		int bottom = c.getY() + range;
		int left = c.getX() - range;
		int right = c.getX() + range;
		int[] dirs = new int[] { top, bottom, left, right };
		for (int i = -range; i <= range; i++) {
			int x = c.getX() + i;
			int y = c.getY() + i;
			for (int j = 0; j < 4; j++) {
				Tile t = null;
				if (j == 0 || j == 1)
					t = getTileAt(x, dirs[j]);
				else
					t = getTileAt(dirs[j], y);
				if (t != null && t.getOwnerCity() == null) {
					t.setOwnerCity(c);
					t.checkForNewResource();
				}
			}
		}
	}

	/**
	 * Returns a set of all the valid moves that the given unit can currently make.
	 *
	 * A unit can move onto a tile if it has enough movement left based on the cost
	 * of moving (1) and the movement modifier for the tile. A unit can "move" onto
	 * (attack) a tile with an enemy unit or enemy city but cannot move onto a tile
	 * with a friendly unit.
	 *
	 * @param unit the Unit whose valid moves are to be retrieved
	 * @return HashSet of int[]s representing all the valid moves for the given
	 *         unit, where each int[] is of length two holding (x, y) coords
	 */
	public HashSet<int[]> getValidMoves(Unit unit) {
		HashSet<int[]> moves = new HashSet<int[]>();
		int curX = unit.getX(), curY = unit.getY();
		for (int i = -1; i < 2; i++) {
			for (int j = -1; j < 2; j++) {
				int newX = curX + i, newY = curY + j;
				int movement = unit.getMovement();
				Tile moveTo = getTileAt(newX, newY);
				if (moveTo != null) {
					int cost = -moveTo.getMovementModifier();
					if (cost + 1 <= movement) {
						Unit unitOnMoveTile = moveTo.getUnit();
						if (unitOnMoveTile == null || unitOnMoveTile.getOwner() != curPlayer)
							moves.add(new int[] { newX, newY });
					}
				}
			}
		}
		return moves;
	}

	/**
	 * Closes the game and saves the current game state.
	 * 
	 * @return true if the game was successfully saved, false otherwise
	 */
	public boolean close() {
		return this.model.done();
	}

}
