package resources;

import java.awt.Point;

import components.City;

/**
 * Resource superclass - unlocks new units when a city owns a tile containing
 * one of these.
 * 
 * @author Luke Hankins
 *
 */
public class Resource {

	protected final Point coord;

	protected String label;
	protected City cityInControl;
	protected String unitUnlocked = "";

	/**
	 * Construct a new resource with the given city as the owner.
	 * 
	 * @param city City that owns the resource.
	 */
	public Resource(City city) {
		this.coord = new Point(city.getX(), city.getY());
		this.cityInControl = city;
	}


	/**
	 * Retrieve the new unit type that this resource unlocks.
	 * 
	 * @return String representing unit type.
	 */
	public String getUnitUnlocked() {
		return this.unitUnlocked;
	}


	/**
	 * Retrieve a label for this unit that can be used in the game UI.
	 *
	 * @return This unit's name or label
	 */
	public String getLabel() {
		return label;
	}

	/**
	 * Retrieve the city in control of the resource.
	 * 
	 * @return City currently making use of the resource
	 */
	public City getCityInControl() {
		return this.cityInControl;
	}

	/**
	 * retrieve this unit's x coordinate within the grid
	 *
	 * @return int representing the x position
	 */
	public int getX() {
		return coord.x;
	}

	/**
	 * retrieve this unit's y coordinate within the grid
	 *
	 * @return int representing the y position
	 */
	public int getY() {
		return coord.y;
	}

}
