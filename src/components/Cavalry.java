package components;

import java.awt.Point;

import models.Player;

/**
 * Cavalry unit stats.
 * 
 * @author Luke Hankins
 *
 */
public class Cavalry extends Unit {

	public Cavalry(Player player, Point coord) {
		super(player, coord);
		label = "Cavalry";
		HP = 100;
		maxHP = HP;
		maxMovement = 3;
		resetMovement();
		sight = 2;
		attackValue = 30;
	}

}
