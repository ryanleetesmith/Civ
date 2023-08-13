package components;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import models.Player;

/**
 * Class representing a single tile within our board.
 *
 * @author Connie Sun, Ryan Smith, Luke Hankins, Tim Gavlick
 *
 */
public class Tile implements Serializable {

	public enum terrainTypes {
		FIELD, HILL, SWAMP, WATER, MOUNTAIN
	}

	private terrainTypes terrainType;
	private int movementBonus;
	private double attackMult;
	private String resourceType = null;
	private City ownerCity = null;
	private boolean isCityTile = false;
	private Unit unitHere = null;
	private List<Player> revealedTo = new ArrayList<Player>();

	/**
	 * When initially making a game, create every tile with a terrain type in mind.
	 * This will allow for map creation.
	 */
	public Tile(terrainTypes terrainType, String resource) {
		this.terrainType = terrainType;
		this.resourceType = resource;
		if (terrainType.equals(terrainTypes.HILL)) {
			this.movementBonus = -1;
			this.attackMult = 1.25;
		} else if (terrainType.equals(terrainTypes.SWAMP)) {
			this.movementBonus = -1;
			this.attackMult = .75;
		} else if (terrainType.equals(terrainTypes.FIELD)) {
			this.movementBonus = 0;
			this.attackMult = 1;
		} else {
			// terrain type is either a mountain or water, either way it is impassable.
			this.movementBonus = -1000;
			this.attackMult = 0;
		}
	}

	/**
	 * Found a city on this tile. Returns false if failed.
	 *
	 * @param city city created by a settler attempting to be made on this tile.
	 * @return boolean representing whether city founding was a success
	 */
	public boolean foundCity(City city) {
		if (this.ownerCity == null) { // && this.unitHere instanceOf Settler?
			this.ownerCity = city;
			this.isCityTile = true;
			this.movementBonus = 0; // TODO: figure out bonuses for units in cities
			this.attackMult = 1.25; // subject to change

			return true;
		}
		return false;
	}

	/**
	 * remove a city from this tile
	 */
	public void destroyCity() {
		this.isCityTile = false;
		this.ownerCity = null;
	}

	/**
	 * Check if a city owns this tile, and this tile owns a resource, so the city
	 * should have access to the resource.
	 */
	public void checkForNewResource() {
		if (this.ownerCity != null && !resourceType.equals("")) {
			ownerCity.unlockUnit(resourceType);
		}
	}

	/**
	 * Retrieve the terrain type for use in the view
	 *
	 * @return The terrainType assigned to this tile
	 */
	public terrainTypes getTerrainType() {
		return this.terrainType;
	}

	/**
	 * Get movement reduction or bonus to be *added* to unit movement depending on
	 * tile type.
	 *
	 * @return int representing terrain bonus to be added to unit movement value
	 */
	public int getMovementModifier() {
		return this.movementBonus;
	}

	/**
	 * Get attack reduction or bonus to be *multiplied* by unit attack depending on
	 * tile type.
	 *
	 * @return double representing attack multiplier.
	 */
	public double getAttackModifier() {
		return this.attackMult;
	}

	/**
	 * Return type of resource on this tile, of null if there is no resource.
	 *
	 * @return String representing the resource type on the tile, or null if there
	 *         is no resource.
	 */
	public String getResourceType() {
		return this.resourceType;
	}

	/**
	 * Return the city object that owns this tile. That does not mean that the tile
	 * is a city, per se, but that some city's area of influence has reached this
	 * tile.
	 *
	 * @return City object representing the city which claims ownership of the tile
	 */
	public City getOwnerCity() {
		return this.ownerCity;
	}

	/**
	 * If a city has expanded its radius to encompass this tile, make that city the
	 * owner
	 *
	 * @param city which now owns the tile
	 */
	public void setOwnerCity(City city) {
		this.ownerCity = city;
	}

	/**
	 * Getter for if this tile is a city tile, not to be confused with being owned
	 * by a city
	 *
	 * @return boolean representing if this tile contains a city.
	 */
	public boolean isCityTile() {
		return this.isCityTile;
	}
	/**
	 * Determine if this tile contains the city itself
	 *
	 * @return boolean representing it this tile contains a city object
	 */
	public boolean isThisACity() {

		return this.isCityTile;
	}

	/**
	 * Return unit stationed on this tile. This method will be necessary for attack
	 * and movement logic
	 *
	 * @return Unit on this tile object, or null if the tile contains no unit.
	 */
	public Unit getUnit() {
		return this.unitHere;
	}

	/**
	 * Place a unit on this tile, will be used if a unit moves here or if a unit
	 * kills the unit stationed here.
	 *
	 * @param unit that is now stationed here.
	 */
	public void setUnit(Unit unit) {
		unitHere = unit;
	}

	/**
	 * Figure out if current player is allowed to see the current tile.
	 *
	 * @param player Player object representing the player in question
	 * @return boolean representing whether the player passed in can see the tile
	 */
	public boolean canSeeTile(Player player) {
		return revealedTo.contains(player);
	}

	/**
	 * reveal this tile to the player passed in.
	 *
	 * @param player Player that the tile will be revealed to.
	 */
	public void revealTile(Player player) {
		revealedTo.add(player);
	}

}
