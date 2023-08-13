package components;

import java.awt.Point;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import models.Player;

/**
 * Class which represents a City object and all of its internal components such
 * as producion, population, etc.
 *
 * @author Connie Sun, Ryan Smith, Luke Hankins, Tim Gavlick
 *
 */
public class City implements Serializable{

	private final Player owner;
	private final Point coord;

	private double production;
	private double productionReserve;
	private int turnsBeforeGrowth;
	private int population; // population can represent city level
	private int controlRadius;
	private double cityHPMax;
	private double cityHPCur;

	private Set<String> producableUnits;

	public City(Player player, int row, int col) {
		this.owner = player;
		this.coord = new Point(row, col);

		this.production = 50;
		// set initial production reserve for scout creation
		this.productionReserve = 400;
		this.turnsBeforeGrowth = 5;
		this.population = 1;
		this.controlRadius = 0;
		this.cityHPMax = 100;
		this.cityHPCur = this.cityHPMax;
		this.producableUnits = new HashSet<String>();
		producableUnits.add("Warrior");
		producableUnits.add("Scout");
		producableUnits.add("Settler");
	}

	/**
	 * Deal damage to the city from a unit
	 *
	 * @param damage attack value of unit hitting the city
	 */
	public void takeAttack(double damage) {
		this.cityHPCur -= damage;
	}

	/**
	 * A new unit has been purchased in this city, create and return it.
	 *
	 * @param unitType String representing the type of unit to be created
	 * @return Unit object that has been created for a player in a city
	 */
	public Unit produceUnit(String unitType) {
		Unit retUnit = null;
		if (unitType.equals("Settler")) {
			// settlers decrease city population by 1
			this.population -= 1;
			retUnit = new Settler(owner, new Point(coord.x, coord.y));
		} else if (unitType.equals("Scout")) {
			retUnit = new Scout(owner, new Point(coord.x, coord.y));
		} else if (unitType.equals("Warrior")) {
			retUnit = new Warrior(owner, new Point(coord.x, coord.y));
		} else if (unitType.equals("Militia")) {
			retUnit = new Militia(owner, new Point(coord.x, coord.y));
		} else if (unitType.equals("Cavalry")) {
			retUnit = new Cavalry(owner, new Point(coord.x, coord.y));
		} else if (unitType.equals("Swordsman")) {
			retUnit = new Swordsman(owner, new Point(coord.x, coord.y));
		}
		this.productionReserve -= Unit.unitCosts.get(unitType);
		return retUnit;
	}

	/**
	 * Increment production, population, and city health, and grow the city if
	 * necessary.
	 */
	public void cityIncrement() {
		productionReserve += production;
		this.turnsBeforeGrowth -= 1;
		// city grows
		if (this.turnsBeforeGrowth == 0) {
			this.population += 1;
			this.controlRadius = (population / 2);
			if (controlRadius > 3) {
				controlRadius = 3;
			}
			this.turnsBeforeGrowth = this.population * 3 + (population * population) / 3;
			this.production += (10);
			this.cityHPMax += (10);
			this.cityHPCur += (10);
		}
		// repairs
		if (this.cityHPCur < this.cityHPMax) {
			this.cityHPCur += (cityHPMax / 20);
			if (this.cityHPCur > this.cityHPMax) {
				this.cityHPCur = this.cityHPMax;
			}
		}
	}

	/**
	 * Retrieve the player who owns the city
	 *
	 * @return Player object representing the city owner.
	 */
	public Player getOwner() {
		return this.owner;
	}

	/**
	 * Retrieve this city's X coordinate
	 *
	 * @return integer representing x value
	 */
	public int getX() {
		return this.coord.x;
	}

	/**
	 * Retrieve this city's Y coordinate
	 *
	 * @return integer representing y value;
	 */
	public int getY() {
		return this.coord.y;
	}

	/**
	 * retrieve the city's turn by turn production value
	 *
	 * @return double representing production per turn.
	 */
	public double getProduction() {
		return this.production;
	}

	/**
	 * retrieve the city's turn by turn production value
	 *
	 * @return double representing current accumulated production.
	 */
	public double getProductionReserve() {
		return this.productionReserve;
	}

	/**
	 * retrieve the city's population, aka level
	 *
	 * @return integer representing level
	 */
	public int getPopulation() {
		return this.population;
	}

	/**
	 * Retrieve the radius of influence of this city
	 * 
	 * @return integer representing number of tiles around the city that it
	 *         controls.
	 */
	public int getControlRadius() {
		return this.controlRadius;
	}

	/**
	 * retrieve the city's max HP, necessary for the view
	 *
	 * @return double representing the city's max HP value
	 */
	public double getMaxHP() {
		return this.cityHPMax;
	}

	/**
	 * retrieve the city's remaining HP
	 *
	 * @return double representing the city's current HP value
	 */
	public double getRemainingHP() {
		return this.cityHPCur;
	}

	/**
	 * Retrieve the city's turns before growth.
	 *
	 * @return The number of turns before this city grows
	 */
	public int getTurnsBeforeGrowth() {
		return this.turnsBeforeGrowth;
	}

	/**
	 * retrieve a set of all units that can be made in this city
	 *
	 * @return a set containing strings which represent the unit names
	 */
	public Set<String> getProducableUnits() {
		return this.producableUnits;
	}

	/*
	 * Make it so the city can produce a new unit, now that it has access to a
	 * resource.
	 */
	public void unlockUnit(String resource) {
		if (resource.equals("wheat"))
			producableUnits.add("Militia");
		if (resource.equals("iron"))
			producableUnits.add("Swordsman");
		if (resource.equals("horse"))
			producableUnits.add("Cavalry");
	}

}
