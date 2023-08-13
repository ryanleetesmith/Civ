package components;

import java.awt.Point;

import models.Player;

/**
 * Basic setup for a settler unit, which can only move and found cities.
 *
 * @author Connie Sun, Ryan Smith, Luke Hankins, Tim Gavlick
 *
 */
public class Settler extends Unit {

	private int charges = 1;

	public Settler(Player player, Point coord) {
		super(player, coord);
		label = "Settler";
		HP = 1;
		maxHP = HP;
		maxMovement = 2;
		resetMovement();
		sight = 2;
		attackValue = 0;
	}

	/**
	 * Use charge to found a new city
	 * 
	 * @return newly founded City object
	 */
	public City foundCity() {
		City foundedCity = new City(owner, coord.x, coord.y);
		owner.addCity(foundedCity);
		this.charges = 0;
		return foundedCity;
	}

	/**
	 * Retrieve the number of cities this settler can still found.
	 *
	 * @return int representing the number of cities the settler can create.
	 */
	public int getCharges() {
		return this.charges;
	}


	@Override
	public double getMaxHP() {
		return 1;
	}

}