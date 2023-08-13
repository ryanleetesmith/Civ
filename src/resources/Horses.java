package resources;

import components.City;

/**
 * Horses resource which unlocks cavalry
 * 
 * @author Connie Sun, Ryan Smith, Luke Hankins, Tim Gavlick
 *
 */
public class Horses extends Resource {

	public Horses(City city) {
		super(city);
		label = "Horses";
		unitUnlocked = "Cavalry";
	}
}