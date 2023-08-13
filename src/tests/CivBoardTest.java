package tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.io.File;

import org.junit.Test;

import components.Tile;
import controllers.CivController;
import models.CivModel;

public class CivBoardTest {
	@Test
	public void testBoard() {
		CivModel model1 = new CivModel(1, 4, 40); // test constructor1
		CivController cont1 = new CivController(model1);
		assertEquals(model1.getSize(), 40); // size should be third param
		assertEquals(model1.getTileAt(0, 0).getTerrainType(), Tile.terrainTypes.WATER);
		assertNotEquals(model1.getTileAt(1, 1).getTerrainType(), Tile.terrainTypes.WATER);
		 // make sure spawn tile is NOT water
		
		
		
		CivModel model2 = new CivModel(1, 3, 0); // test constructor3
		CivController cont2 = new CivController(model2);
		assertEquals(model2.getSize(), 20);
		assertEquals(model2.getTileAt(2, 13).getTerrainType(), Tile.terrainTypes.FIELD); // spawn point for player 2 is grass
		model2.nextPlayer(); // increment to player 2 or CPU player
		boolean useless = cont2.close(); // save_game.dat now exists 
		
		
		
		CivModel model3 = new CivModel(); // test constructor2 --> should open model2 technically
		CivController controller3 = new CivController(model3);
		assertEquals(model3.getSize(), model2.getSize()); // should be the same board
		assertEquals(model3.getCurPlayer().getID(), model2.getCurPlayer().getID()); // with same cur player
		assertEquals(model3.getHead().getID(), model2.getHead().getID()); // and same first player
		
		CivModel model4 = new CivModel(1, 1, 0); // test Map1
		CivController cont4 = new CivController(model4);
		assertEquals(model4.getSize(), 20); // should have size 20
		assertNotEquals(model4.getCurPlayer().getID(), model2.getCurPlayer().getID()); //should have diff players because are not the same board
		assertEquals(model4.getHead().getID(), model2.getHead().getID()); // but first player for both should be called player 1
		assertNotEquals(model4.getTileAt(1, 1).getTerrainType(), Tile.terrainTypes.WATER);
		assertNotEquals(model4.getTileAt(1, 18).getTerrainType(), Tile.terrainTypes.WATER);
		assertNotEquals(model4.getTileAt(18, 1).getTerrainType(), Tile.terrainTypes.WATER);
		assertNotEquals(model4.getTileAt(18, 18).getTerrainType(), Tile.terrainTypes.WATER);
		// spawn locations shouldn't be water
		
		CivModel model5 = new CivModel(1,2,0); 
		CivController cont5 = new CivController(model5);
		assertEquals(model5.getSize(), 20);
		assertEquals(model5.getTileAt(3,2).getTerrainType(), Tile.terrainTypes.FIELD); //player1 starts on a field
		assertEquals(model5.getTileAt(2,2).getTerrainType(), Tile.terrainTypes.HILL); // in a hilly area
		assertEquals(model5.getTileAt(4,2).getTerrainType(), Tile.terrainTypes.HILL);
	}
	
	
	
	
}
