Welcome to Sid Meier's Civilization 0.5!

This is a version of Civilization 1 with 2.5d isometric board graphics and simplified game mechanics. The objective? Simply destroy all enemy cities! Can be played by 1 - 4 people (locally), and supports multiple maps and map sizes as well as game saves!

When running Civ.java you will be greeted with a simple menu screen where you can select to start a new game, or load an existing one from the file that is automatically written to when a game is closed prematurely.


Once clicking "New game", you can select which map you would like to play on. After selecting the map you can select the number of players as well as how large you want the selected map to be.
![New game](https://github.com/ryanleetesmith/Civ/assets/142176160/b5e72a7d-82eb-43eb-acdf-250fd459f762|width=20px)


You start with only one unit, the settler! Select the unit to see its stats on the left, as well as highlighted tiles it can still move to. There are a variety of terrain types that cost different amounts to move to and impact the performance of each unit in battle as well (ocean and mountain tiles are impassable).

![selected settler](https://github.com/ryanleetesmith/Civ/assets/142176160/a1b7c30f-926a-4a45-9de1-60489fde3377)








### FEATURES ###

Maps:
	- Terrain:    The game features a variety of terrain types. Plains are easy to move on and 
		      provide no bonuses. Hills are difficult to scale but provide a bonus when 
		      fighting enemy units. Swamps leave your units exhausted, making both movement
		      and combat more difficult. Mountains and ocean tiles are impassable and serve
		      as obstacles.
	- Selection:  The game features multiple hand-crafted maps with different designs, and the
		      ability to select the size of your map to accommodate for multiple players.
	- Fog of War: The map will be hidden at the start of the game - explore with your units
		      to reveal the map and potential enemies.
	- 2.5D View:  The view is a multi-layered isometric 2.5d board with sprites and buttons for 
		      just about everything you can think of!

Cities:
	- Population: A city's population increments over time. As the city grows so does it's worth!
		      With every increase in city population comes an increase in city health and 
	              health regeneration, production per turn, and radius of influence.
	- Production: Cities have a production value that determines how much production is added to 
		      their reserves each turn. Once a city has high enough production reserves,
		      they can create a unit
	- Influence:  Cities develop a radius of influence that grows over time with the city. Tiles
		      within the city's radius of influence are owned by that city. If an owned tile 
		      contains a resource, that resource is added to the City's resource pool, and 
		      will unlock interesting new units.
				  
Units:
	- Stats:      Units have stats that provide them an edge over different unit types. MOVEMENT 
		      dictates the number of tiles that can be crossed in one turn, but beware hills
		      and swamps! Each unit has a different starting HP value, and regenerates a
	              small amount of HP every turn. SIGHT dictates how many tiles from it's current
		      position a unit can see - the higher the sight the more tiles are revealed.
		      Each unit has a different ATTACK value, which determines the amount of damage
		      dealt to enemy units and cities.
	- Scouts:     Scouts are cheap, and can move quickly and see far, but are weaker than other 
		      units in combat.
	- Warriors:   With a reasonable price and solid attack/HP stats, these will be the go-to unit 
		      to fight early on.
	- Settlers:   Settlers are very expensive and vulnerable, and take some of a cities population
		      with them when made, but are able to found a single city (as long as it is outside 
		      of the current city's area of influence).
	- Cavalry:    The cavalry unit can cover ground quickly and have high attack values, but
		      require the 'Horses' resource to produce.
	- Swordsman:  These elite units are slow but heavily armored, and boast the highest attack and 
		      HP in the game. Require the 'Iron' resource to produce.
	- Milita:     These units are the weakest in the game, but can be produced far faster than any
		      other unit.
	
	
Miscellaneous:
	- AI: 	      If the game is entered in single player, the user will be faced with an AI opponent
		      who defends themselves early on, but quickly become aggressive, be careful!
