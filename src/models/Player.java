package models;

import java.io.Serializable;
import java.util.ArrayList;

import components.City;
import components.Unit;
/**
 * Player class exists to keep an aggregation of data associated with each player
 * 	so the controller knows what Units/Tiles each player is allowed to interact with.
 * 
 *  @field units is an ArrayList of Units that this Player "owns"
 *  @field cities is an ArrayList of Cities that this Player "owns"
 *  @field isHuman is a boolean indicating whether this Player is a human player or CPU player
 *  @field ID is a String that gives this Player's "name" for displaying in the view and specifying
 *  		in the controller. 
 *  
 * @author Luke
 *
 */
public class Player implements Serializable {
	private ArrayList<Unit> units;
	private ArrayList<City> cities;
	private boolean isHuman;

	String ID;
	/**
	 * Player() is our constructor that takes an int and a String and sets the appropriate fields
	 * @param isHuman int indicating whether this player is human or not
	 * @param ID String giving the ID of this Player object. 
	 */
	public Player(int isHuman, String ID) {
		units = new ArrayList<Unit>();
		cities = new ArrayList<City>();
		if (isHuman == 1)
			this.isHuman = true;
		else
			this.isHuman = false;
		this.ID = ID;
	}
	/**
	 * addCity() adds a new City Object to the list of cities this player owns
	 * @param city
	 */
	public void addCity(City city) {
		this.cities.add(city);
	}
	/**
	 * addUnit() adds a new Unit Object to the list of Units this player owns
	 * @param city
	 */
	public void addUnit(Unit unit) {
		units.add(unit);
	}
	/**
	 * getUnits() returns the collection of Units that belong to this Player for the controller
	 * @return ArrayList<Unit> of Units this player owns. 
	 */
	public ArrayList<Unit> getUnits() {
		return units;
	}
	/**
	 * getCities() returns the collection of Cities that belong to this Player for the controller
	 * @return ArrayList<City> of Cities this player owns.
	 */
	public ArrayList<City> getCities() {
		return cities;
	}
	/**
	 * Informs the controller whether this Player is a human or a CPU
	 * @return true if this player is human, false if CPU
	 */
	public boolean isHuman() {
		return this.isHuman;
	}
	/**
	 * Remove a Unit from this Player's Unit collection (for use when Unit dies or Settler settles city)
	 * @param unit Unit to be removed from this Player's Unit collection
	 */
	public void removeUnit(Unit unit) {
		this.units.remove(unit);
	}
	/**
	 * Remove a City from this Player's City collection (for use when City dies)
	 * @param city City to be removed from this Player's Unit collection
	 */
	public void removeCity(City city) {
		this.cities.remove(city);
	}
	/**
	 * Gives access to this Player's ID/name for displaying in the View or specifying in the Controller
	 * @return a String that is this Player's ID
	 */
	public String getID() {
		return this.ID;
	}
}
