package resources;

import components.City;

/**
 * Iron resource which unlocks swordsmen
 * 
 * @author Luke Hankins
 *
 */
public class Iron extends Resource {

	public Iron(City city) {
		super(city);
		label = "Iron";
		unitUnlocked = "Swordsman";
	}
}
