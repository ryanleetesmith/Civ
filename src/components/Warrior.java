package components;

import java.awt.Point;

import models.Player;

/**
 * Subclass containing stats for the warrior unit.
 *
 * @author Luke Hankins
 *
 */
public class Warrior extends Unit {

	public Warrior(Player player, Point coord) {
		super(player, coord);
		label = "Warrior";
		HP = 100;
		maxHP = HP;
		maxMovement = 2;
		resetMovement();
		sight = 2;
		attackValue = 25;
	}


	@Override
	public double getMaxHP() {
		return 100;
	}

}
