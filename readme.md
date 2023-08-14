### Sid Meier's Civilization 0.5! ###

This is a version of Civilization 1 with 2.5d isometric board graphics and simplified game mechanics. The objective? Simply destroy all enemy cities! Can be played by 1 - 4 people (locally), and supports multiple maps and map sizes as well as game saves!

**Environment**: This game runs using both Java and JavaFX 15.



### Introduction:

When running Civ.java you will be greeted with a simple menu screen where you can select to start a new game, or load an existing one from the file that is automatically written to when a game is closed prematurely.

Once clicking "New game", you can select which map you would like to play on. After selecting the map you can select the number of players as well as how large you want the selected map to be.

![New game](https://github.com/ryanleetesmith/Civ/assets/142176160/b5e72a7d-82eb-43eb-acdf-250fd459f762|width=20px)

You start with only one unit, the settler! Select the unit to see its stats on the left, as well as highlighted tiles it can still move to. There are a variety of terrain types, all of which impact how well units perform in battle and cost different amounts of movmement points to traverse!

	- Plains:     Standard fare! No penalty or bonuses to movement or attack.
	- Hills:      Penalty to movment, but provide an advantage when attacking and defending.
	- Swamp:      Penalties to both movment and attack stats
 	- cities:     Can be freely traversed by friendly units and buff stationed unit attack.
  	- Ocean:      Impassable!
   	- Mountains:  Impassable!
    
![selected settler](https://github.com/ryanleetesmith/Civ/assets/142176160/a1b7c30f-926a-4a45-9de1-60489fde3377)


As you explore the map for a suitable location to settle your city, the fog of war denoted by a squiggly tile border will be permanently cleared for you. Terrain that is difficult to traverse will be highlighted yellow, so avoid those tiles if possible.

![moved and resources](https://github.com/ryanleetesmith/Civ/assets/142176160/1755531a-d470-4046-8d7e-72c2c3d22d31)


After you find a suitable location, click the "Settle new city" button to found a city on the settler's current tile. Cities accrue **production points** each turn, and these production points can be used to purchase new units including settlers! Cities start out with low **population**, but over time this will change, bringing new tiles into its highlighted area of influence, increasing production per turn, and increasing the health of the city walls.

![city founded](https://github.com/ryanleetesmith/Civ/assets/142176160/bb9fbf22-490e-4bfc-96e3-dc7958730856)


Once the area of influence of a city captures a resource tile, that city will unlock a special unit which can then be purchased with production points. 

	- Scouts:     Scouts are cheap, and have high movement and sight values, but are weaker than other 
		      units in combat.
	- Warriors:   With a reasonable price and solid attack/HP stats, these will be the go-to unit 
		      to fight early on.
	- Settlers:   Settlers are very expensive and vulnerable, and take some of a cities population
		      with them when made, but are able to found a single city on a free tile.
	- Cavalry:    The cavalry unit can cover ground quickly and have high attack values, but
		      require the 'Horses' resource to produce.
	- Swordsman:  These elite units are slow but heavily armored, and boast the highest attack and 
		      HP in the game. Require the 'Iron' resource to produce.
	- Milita:     These units are the weakest in the game, but can be produced for a pittance.

Here is a relatively large city that has unlocked the militia and swordsmen units:

![cityhuge](https://github.com/ryanleetesmith/Civ/assets/142176160/4a02a3c4-8ed8-4c9b-8fc5-6cedd134d3ce)

Be careful about founding cities next to other ones, as once a tile has been claimed by a city it cannot be claimed by another one.


When you encounter an enemy unit, simply click it to attack. The attacking unit will damage the defending unit first, then the defending unit will retaliate. At the end of every turn your units will heal a small amount passively, but be careful of attacking stronger units, especially if they have a terrain advantage. 

![citycombat1](https://github.com/ryanleetesmith/Civ/assets/142176160/7b524e11-65a1-42af-bb2c-ca5c637bc78c)

If you attack a unit stationed in a city, the unit will take the damage first. Once the unit has been destroyed the city can be attacked directly. Reduce a city's health to 0 to destroy it. If your opponent has no more cities or settlers, they lose the game!

![citycombat2](https://github.com/ryanleetesmith/Civ/assets/142176160/43a54fec-aa6c-44db-aca2-48e8870b3675)

When playing in single player, you are faced with a computer opponent who defends until they have bolstered their defenses, then they go on the offensive!
There are lots of small features that have not been described in this readme, have fun exploring the game!

![endgame](https://github.com/ryanleetesmith/Civ/assets/142176160/2c13af31-35da-4120-a9e2-c8d92284ba22)






Specifics:


				  
Units:


