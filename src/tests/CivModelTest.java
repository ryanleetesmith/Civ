package tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import models.CivBoard;
import models.CivModel;
import models.Player;

/**
 * Tests the methods of CivModel.
 *
 * @author Connie Sun, Ryan Smith, Luke Hankins, Tim Gavlick
 */
public class CivModelTest {

	@Test
	/**
	 * Test that the model returns the appropriate values while reaching 100% branch
	 * coverage. Does not mess with player and board completely yet.
	 */
	void testBasics() {
		CivModel model = new CivModel(1, 2, 0);
		CivBoard board = model.getCivBoard(); // maybe make tests using a new board as well

		// model and board are looking at same tiles
		assertEquals(model.getTileAt(4, 5), board.getTile(4, 5));
		assertEquals(model.getSize(), board.size);
		// advance the game
		assertTrue(model.getCurPlayer().isHuman());
		model.nextPlayer();
		assertTrue(model.isComputer());
		assertEquals(model.roundNumber(), 0);
		model.nextPlayer();
		assertFalse(model.isComputer());
		assertEquals(model.roundNumber(), 1);

		model.getCurPlayer();
		model.changeAndNotify();

		assertEquals(model.numPlayers(), 2);
		assertTrue(model.removePlayer(model.getCurPlayer()));
		assertEquals(model.numPlayers(), 1);

		// new game with 3 players
		model = new CivModel(3, 2, 0);
		assertEquals(model.numPlayers(), 3);
		// how to make removePlayer return false?
		// ^ by trying to remove a player that doesn't exist
		Player notInList = new Player(1, "Player 1");
		assertFalse(model.removePlayer(notInList));

		assertTrue(model.removePlayer(model.getCurPlayer()));
		model.nextPlayer();
		assertTrue(model.removePlayer(model.getCurPlayer()));
		model.nextPlayer();
		assertTrue(model.removePlayer(model.getCurPlayer()));
	}

}
