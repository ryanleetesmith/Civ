package components;

import java.awt.Point;

import models.Player;

/**
 * Basic setup for a scout unit which has higher movement and sight.
 *
 * @author Luke Hankins
 *
 */
public class Scout extends Unit {

	public Scout(Player player, Point coord) {
		super(player, coord);
		label = "Scout";
		HP = 50;
		maxHP = HP;
		maxMovement = 4;
		resetMovement();
		sight = 4;
		attackValue = 15;
	}


	@Override
	public double getMaxHP() {
		return 50;
	}

}
