package models;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;

import components.Tile;

/**
 * Holds the collection of individual tiles that make up a single Civ map.
 *
 * @field tiles 2D array containing our tile objects
 * @field size int specifying the size of our board --> board is size x size
 *        tiles
 * @field playerStartingCoords ArrayList of int[2] specifying the starting coordinates
 * 		(x,y) of each player for each map, wherein the first two elements give the starting coordinates
 * 		2 players for a 2 player game, the third gives the starting coordinate of player three
 * 		(if there is one) and similarly for player 4. --> Will spawn players across the map from
 * 		each other rather than close to each other given the opportunity
 * @author Connie Sun, Ryan Smith, Luke Hankins, Tim Gavlick
 */
public class CivBoard implements Serializable {

	public Tile[][] tiles;
	public int size;
	private ArrayList<int[]> playerStartingCoords;

	/**
	 * Constructor for our board
	 * 
	 * Map4 has several areas where the elements in those areas are randomly generated, 
	 * 	but part of a cohesive "idea" for that area (swamp area contains field, swamp, water). 
	 * 	This constructor takes a size and creates a size x size map with these areas. 
	 * 
	 * @param size size provides a size for the board; given size board is size x size
	 * 
	 */
	public CivBoard(int size) {
		this.size = size;
		Tile[][] board = new Tile[size][size];
		int i = 0;
		int j;
		Random rng = new Random();
		int oneThird = size/3;
		int twoThird = size * 2/3;
		while (i < size - 1) {
			j = 0;
			while (j < size - 1) {
				boolean isTopCorner = ((i < (oneThird) && j < (oneThird)));
				boolean isBottomCorner = (i > twoThird) && (j > twoThird); 
				int coordSum = i + j;
				boolean isMiddleStrip = (coordSum < (size + oneThird)) && (coordSum > (size - oneThird));
				int type = rng.nextInt(10);
				boolean resource = (type == 4 && (i % 2 == 1) && (j % 2 == 1));
				if (type > 2  && (isTopCorner || isBottomCorner)){// top left and bot. right
					if (resource) 
						board[i][j] = new Tile(Tile.terrainTypes.SWAMP,"horse"); // corners are swamp or water
					else if (type < 8)
						board[i][j] = new Tile(Tile.terrainTypes.SWAMP,"");
					else
						board[i][j] = new Tile(Tile.terrainTypes.FIELD,"");
				}
				else if (type <= 2  && (isTopCorner || isBottomCorner)) { 
					board[i][j] = new Tile(Tile.terrainTypes.WATER, "");
				}
				else if (type > 3 && isMiddleStrip) { // diagonal strip down the middle is mostly hills
					if (resource)
						board[i][j] = new Tile(Tile.terrainTypes.HILL, "iron");
					else if (type < 9)
						board[i][j] = new Tile(Tile.terrainTypes.HILL, "");
					else
						board[i][j] = new Tile(Tile.terrainTypes.MOUNTAIN, "");
				}
				else { // rest are fields. 
					if (resource)
						board[i][j] = new Tile(Tile.terrainTypes.FIELD, "wheat");
					else
						board[i][j] = new Tile(Tile.terrainTypes.FIELD, "");
				}
				j++;
			}
			i++;
		}
		i = 0;
		j = 0;
		while (i < size) { // set border to water
			board[i][0] = new Tile(Tile.terrainTypes.WATER, "");
			board[0][i] = new Tile(Tile.terrainTypes.WATER, "");
			board[size - 1][i] = new Tile(Tile.terrainTypes.WATER, "");
			board[i][size - 1] = new Tile(Tile.terrainTypes.WATER, "");
			i++;
		}
		board[size-2][size-2] = new Tile(Tile.terrainTypes.FIELD, ""); // guarantee players dont start on water blocks
		board[1][1] = new Tile(Tile.terrainTypes.FIELD, "");
		board[size-2][1] = new Tile(Tile.terrainTypes.FIELD, "");
		board[1][size-2] = new Tile(Tile.terrainTypes.FIELD, "");
		this.tiles = board;
	}
	/**
	 * Second constructor that will build itself out of a save file
	 * @param ois ois is an ObjectInputStream for deserializing an instance of the board class
	 * @throws ClassNotFoundException if class cannot be found
	 * @throws IOException if unsuccessful read from ois
	 */
	public CivBoard(ObjectInputStream ois) throws ClassNotFoundException, IOException {
		unserializeBoard(ois);
	}
	/**
	 * getTile returns the Tile object at CivBoard's x,y location
	 * @param x int specifying x location on our board
	 * @param y int specifying y location on our board
	 * @return a Tile object that is the tile on our board at x,y
	 */
	public Tile getTile(int x, int y) {
		if (x < 0 || x >= size || y < 0 || y >= size) return null;
		return this.tiles[y][x];
	}
	/**
	 * Third constructor for our board that takes a file which contains
	 * 	information on how to build a board, and builds a board out of it. 
	 * 
	 * Takes files of the form
	 * 	"tile_type resource_type\n"
	 * 
	 * 	where each line provides information about one tile and its resource 
	 * 
	 * @param file file with format specified above.
	 */
	public CivBoard(String file) {
		Scanner sc = null;
		try {
			File fileObj = new File(file);
			sc = new Scanner(fileObj);
		} catch (Exception e) {
			e.printStackTrace();
		}
		String line = null;
		if (sc.hasNextLine()) {
			line = sc.nextLine();
		}
		this.size = Integer.valueOf(line);
		Tile[][] board = new Tile[size][size];
		int i = 0;
		int j = 0;
		Tile.terrainTypes type = null;
		String resource = "";
		while (sc.hasNextLine()) {
			line = sc.nextLine();
			String[] type_res = line.split(" ");
			if (type_res[0].equals("field"))
					type = Tile.terrainTypes.FIELD;
			else if (type_res[0].equals("swamp"))
				type = Tile.terrainTypes.SWAMP;
			else if (type_res[0].equals("hill"))
				type = Tile.terrainTypes.HILL;
			else if (type_res[0].equals("water"))
				type = Tile.terrainTypes.WATER;
			else if (type_res[0].equals("mountain"))
				type = Tile.terrainTypes.MOUNTAIN;
			if (type_res[1].equals("w"))
				resource = "wheat";
			else if (type_res[1].equals("h"))
				resource = "horse";
			else if (type_res[1].equals("i"))
				resource = "iron";
			else
				resource = "";
			board[j][i] = new Tile(type, resource);
			j++;
			if (j == size) {
				i++;
				j = 0;
			}
			if (i == size) {
				break;
				//System.out.println("error with passed size");
			}
		}
		this.tiles = board;
		
		
	}

	/**
	 * getSize() returns this board's size attribute (board is size x size)
	 * @return int giving the dimension of this board
	 */
	public int getSize() {
		return this.size;
	}

	/**
	 * serializeBoard() is called when saving the game state to capture all of the board's
	 * 	attributes. 
	 * @param oos AnObjectOutputStream to write this board's fields to.
	 * @throws IOException If error when writing to output stream
	 */
	public void serializeBoard(ObjectOutputStream oos) throws IOException {
		oos.writeObject(this.tiles);
		oos.writeObject(this.size);
		oos.writeObject(this.playerStartingCoords);
	}
	/**
	 * unserializeBoard() is called when attempting to load a saved game state
	 * 	into this board. 
	 * @param ois an ObjectInputSteam to load CivBoard attributes from
	 * @throws ClassNotFoundException if class not found
	 * @throws IOException if error reading from input stream
	 */
	public void unserializeBoard(ObjectInputStream ois) throws ClassNotFoundException, IOException {
		this.tiles = (Tile[][]) ois.readObject();
		this.size = (int) ois.readObject();
		this.playerStartingCoords = (ArrayList<int[]>) ois.readObject();
	}

}
