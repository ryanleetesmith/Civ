package tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Point;
import java.io.File;

import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import components.City;
import components.Scout;
import components.Settler;
import components.Swordsman;
import components.Tile;
import components.Unit;
import components.Warrior;
import controllers.CivController;
import models.CivModel;
import models.Player;
import resources.Horses;
import resources.Iron;
import resources.Wheat;

/**
 * Tests the methods of CivController. Also tests some of the components and
 * resources classes.
 *
 * @author Connie Sun, Ryan Smith, Luke Hankins, Tim Gavlick
 */
@TestMethodOrder(OrderAnnotation.class)
public class CivControllerTest {

	/**
	 * Tests the basics of the game. Allows the computer to win. Does not test
	 * resources or unlockable units.
	 */
	@Test
	@Order(1)
	void testBasics() {
		CivModel model = new CivModel(1, 2, 0);
		CivController controller = new CivController(model);
		controller.placeStartingUnits();
		// basic checks
		assertEquals(model.getTileAt(4, 7), controller.getTileAt(4, 7));
		controller.startTurn();
		assertFalse(controller.gameOver());
		assertTrue(controller.isHumanTurn());
		assertTrue(controller.foundCity(3, 2));
		assertTrue(controller.getTileAt(3, 2).isThisACity());
		// give me production now
		for (int i = 0; i < 8; i++) {
			controller.getTileAt(3, 2).getOwnerCity().cityIncrement();
		}
		// create a scout and advance it forward, enemies will be defending so this is
		// fine
		assertFalse(controller.createUnit(3, 2, "Scout"));
		assertFalse(controller.createUnit(3, 2, "Warrior"));
		controller.endTurn();
		// add a computer city that's easier to get to
		model.nextPlayer();
		City c = new City(model.getCurPlayer(), 12, 3);
		controller.getTileAt(12, 3).foundCity(c);
		assertFalse(controller.getTileAt(12, 3).foundCity(c));
		model.getCurPlayer().addCity(c);
		model.nextPlayer();
		assertTrue(controller.moveUnit(model.getTileAt(3, 2).getUnit(), 4, 2));
		assertTrue(controller.moveUnit(model.getTileAt(4, 2).getUnit(), 5, 2));
		assertFalse(controller.moveUnit(model.getTileAt(5, 2).getUnit(), 4, 2));
		controller.endTurn();
		assertTrue(controller.moveUnit(model.getTileAt(5, 2).getUnit(), 6, 2));
		assertTrue(controller.moveUnit(model.getTileAt(6, 2).getUnit(), 7, 2));
		controller.endTurn();
		for (int i = 7; i < 11; i++) {
			assertTrue(controller.moveUnit(model.getTileAt(i, 2).getUnit(), i + 1, 2));
		}
		controller.endTurn();

		// attack enemy city
		assertTrue(controller.moveUnit(model.getTileAt(11, 2).getUnit(), 12, 3));
		controller.endTurn();
		assertTrue(controller.moveUnit(model.getTileAt(11, 2).getUnit(), 12, 3));
		controller.endTurn();
		// scout attacked and should eventually be killed by a defending unit
		// insert enemy unit and move it onto the city
		model.nextPlayer();
		for (int i = 0; i < 40; i++) {
			controller.getTileAt(12, 3).getOwnerCity().cityIncrement();
			controller.getTileAt(3, 2).getOwnerCity().cityIncrement();
		}
		assertTrue(controller.createUnit(12, 3, "Settler"));
		model.nextPlayer();
		assertTrue(controller.createUnit(3, 2, "Settler"));
		assertFalse(controller.createUnit(3, 2, "Settler"));
		controller.endTurn();
		// System.out.println(model.getTileAt(14, 9).getUnit());
		// create new warriors and defend so the AI can exercise its logic
		for (int i = 6; i < 12; i++) {
			controller.getTileAt(6, i).setUnit(new Warrior(model.getCurPlayer(), new Point(6, i)));
			model.getCurPlayer().addUnit(model.getTileAt(6, i).getUnit());
			controller.getTileAt(7, i).setUnit(new Warrior(model.getCurPlayer(), new Point(7, i)));
			model.getCurPlayer().addUnit(model.getTileAt(7, i).getUnit());
		}
		// hit a few more branches in moveUnit
		Unit u = controller.getTileAt(6, 8).getUnit();
		assertFalse(controller.moveUnit(u, 6, 9));
		assertFalse(controller.moveUnit(u, 10, 10));

		assertFalse(controller.gameOver());
		// TODO: make the game end painlessly
		for (int i = 0; i < 25; i++) {
			controller.endTurn();
		}
		assertTrue(controller.gameOver());
		controller.close();
	}

	/**
	 * Tests that a computer player will destroy a city and win
	 */
	@Test
	@Order(2)
	void testComputerDestroyCity() {
		CivModel model = new CivModel(1, 2, 0);
		CivController controller = new CivController(model);
		City city = new City(model.getCurPlayer(), 5, 9);
		model.getCurPlayer().addCity(city);
		model.getTileAt(5, 9).foundCity(city);
		model.nextPlayer();
		assertFalse(model.getCurPlayer().isHuman());
		Player computer = model.getCurPlayer();
		// these warriors are in horny jail
		Warrior w1 = new Warrior(computer, new Point(1, 1));
		Warrior w2 = new Warrior(computer, new Point(0, 0));
		// this warrior will take over human city
		Warrior w3 = new Warrior(computer, new Point(5, 10));
		model.getCurPlayer().addUnit(w1);
		model.getCurPlayer().addUnit(w2);
		model.getCurPlayer().addUnit(w3);
		model.getTileAt(1, 1).setUnit(w1);
		model.getTileAt(0, 0).setUnit(w2);
		model.getTileAt(5, 10).setUnit(w3);
		model.nextPlayer(); // back to the first player
		controller.startTurn();
		assertFalse(controller.gameOver());
		for (int i = 0; i < 4; i++)
			controller.endTurn();
	}

	/**
	 * Tests that a player is not allowed to attack their own unit. Tests that a
	 * player can kill an enemy. Also tests that a unit correctly dies in a
	 * counterattack.
	 */
	@Test
	@Order(3)
	void attackOwnKillEnemy() {
		CivModel model = new CivModel(1, 2, 0);
		CivController controller = new CivController(model);
		Player human = model.getCurPlayer();
		model.nextPlayer();
		Player computer = model.getCurPlayer();
		model.nextPlayer();
		City city = new City(model.getCurPlayer(), 2, 9);
		human.addCity(city);
		Settler se = new Settler(human, new Point(2, 10));
		Warrior w1 = new Warrior(human, new Point(2, 11));
		human.addUnit(se);
		human.addUnit(w1);
		Warrior w2 = new Warrior(computer, new Point(3, 10));
		// these warriors are in horny jail
		Warrior w3 = new Warrior(computer, new Point(1, 1));
		Warrior w4 = new Warrior(computer, new Point(0, 0));
		computer.addUnit(w3);
		computer.addUnit(w4);
		computer.addUnit(w2);
		Scout s = new Scout(human, new Point(0, 1));
		human.addUnit(s);
		model.getTileAt(2, 9).foundCity(city);
		model.getTileAt(2, 10).setUnit(se);
		model.getTileAt(2, 11).setUnit(w1);
		model.getTileAt(3, 10).setUnit(w2);
		model.getTileAt(1, 1).setUnit(w3);
		model.getTileAt(0, 0).setUnit(w4);
		model.getTileAt(0, 1).setUnit(s);
		controller.startTurn();
		assertFalse(controller.moveUnit(controller.getTileAt(2, 10).getUnit(), 2, 11));
		assertFalse(controller.moveUnit(controller.getTileAt(2, 10).getUnit(), 3, 10));
		for (int i = 0; i < 6; i++)
			controller.endTurn();
	}

	/**
	 * Tests the special units (militia, cavalry, and swordsman) that can be
	 * unlocked with resources
	 */
	@Test
	@Order(4)
	void testUnlockableUnits() {
		CivModel model = new CivModel(2, 4, 30);
		Player p1 = model.getCurPlayer();
		assertEquals(p1.getID(), "Player 1");
		City city = new City(p1, 1, 1);
		city.unlockUnit("horse");
		assertEquals(city.getTurnsBeforeGrowth(), 5);
		assertEquals(city.getMaxHP(), 100);
		assertEquals(city.getProduction(), 50);
		assertEquals(city.getPopulation(), 1);
		city.produceUnit("Militia");
		city.produceUnit("Cavalry");
		city.produceUnit("Swordsman");
		for (int i = 0; i < 200; i++)
			city.cityIncrement();
		Tile t = new Tile(Tile.terrainTypes.SWAMP, "wheat");
		assertEquals(t.getResourceType(), "wheat");
		assertEquals(t.getTerrainType(), Tile.terrainTypes.SWAMP);

	}

	/**
	 * Tests that the methods shared by all units work correctly.
	 */
	@Test
	@Order(5)
	void testUnits() {
		Player p = new Player(1, "1");
		Scout scout = new Scout(p, new Point(0, 0));
		assertEquals(scout.getMaxHP(), 50);
		Settler settler = new Settler(p, new Point(0, 0));
		assertEquals(settler.getMaxHP(), 1);
		Warrior warrior = new Warrior(p, new Point(0, 0));
		assertEquals(warrior.getMaxHP(), 100);
		assertEquals(warrior.getLabel(), "Warrior");
		assertEquals(warrior.getCost("Warrior"), 600);
		Swordsman sword = new Swordsman(p, new Point(0, 0));
		assertEquals(sword.getMaxHP(), 150);
	}

	/**
	 * Tests the three resources classes and that their getters work correctly.
	 */
	@Test
	@Order(6)
	void testResources() {
		City c = new City(new Player(1, "1"), 0, 0);
		Horses horse = new Horses(c);
		Iron iron = new Iron(c);
		Wheat wheat = new Wheat(c);
		assertEquals(horse.getUnitUnlocked(), "Cavalry");
		assertEquals(iron.getLabel(), "Iron");
		assertEquals(wheat.getCityInControl(), c);
		assertEquals(horse.getX(), 0);
		assertEquals(wheat.getY(), 0);
	}

	/**
	 * Tests that the controller's start and end turn methods work correctly.
	 * Deletes the old saved game state.
	 */
	@Test
	@Order(7)
	void testGameSave() {
		File oldGame = new File("save_game.dat");
		oldGame.delete();
		CivModel model = new CivModel(2, 3, 0);
		assertEquals(model.getAllPlayers().size(), 2);
		CivController controller = new CivController(model);
		controller.placeStartingUnits();
		controller.startTurn();
		controller.endTurn();
		controller.endTurn();
		controller.close();
		CivModel savedModel = new CivModel();
		savedModel.done();
		CivModel newModel = new CivModel(2, 1, 0);
		CivController newController = new CivController(newModel);
		newController.placeStartingUnits();
		oldGame = new File("save_game.dat");
		oldGame.delete();
	}
}
