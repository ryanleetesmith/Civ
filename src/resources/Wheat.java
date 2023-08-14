package resources;

import components.City;

/**
 * Wheat resource subclass to unlock the militia unit.
 *
 * @author Luke Hankins
 */
public class Wheat extends Resource {

	public Wheat(City city) {
		super(city);
		label = "Wheat";
		unitUnlocked = "Militia";
	}

}
