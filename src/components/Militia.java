package components;

import java.awt.Point;

import models.Player;

/**
 * Class containing unit stats for the militia unit.
 * 
 * @author Luke Hankins
 *
 */
public class Militia extends Unit {

	public Militia(Player player, Point coord) {
		super(player, coord);
		label = "Militia";
		HP = 50;
		maxHP = HP;
		maxMovement = 1;
		resetMovement();
		sight = 1;
		attackValue = 10;
	}

}
