package resources;

import components.City;

/**
 * Wheat resource subclass to unlock the militia unit.
 *
 * @author Connie Sun, Ryan Smith, Luke Hankins, Tim Gavlick
 */
public class Wheat extends Resource {

	public Wheat(City city) {
		super(city);
		label = "Wheat";
		unitUnlocked = "Militia";
	}

}
