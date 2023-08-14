package resources;

import components.City;

/**
 * Horses resource which unlocks cavalry
 * 
 * @author Luke Hankins
 *
 */
public class Horses extends Resource {

	public Horses(City city) {
		super(city);
		label = "Horses";
		unitUnlocked = "Cavalry";
	}
}