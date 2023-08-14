package components;

import java.awt.Point;

import models.Player;

/**
 * Subclass containing stats for the swordsman unit.
 * 
 * @author Luke Hankins
 *
 */
public class Swordsman extends Unit {

	public Swordsman(Player player, Point coord) {
		super(player, coord);
		label = "Swordsman";
		HP = 150;
		maxHP = HP;
		maxMovement = 1;
		resetMovement();
		sight = 1;
		attackValue = 35;
	}

}
