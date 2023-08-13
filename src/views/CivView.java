package views;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import components.Cavalry;
import components.City;
import components.Militia;
import components.Scout;
import components.Settler;
import components.Swordsman;
import components.Tile;
import components.Unit;
import controllers.CivController;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import javafx.util.Duration;
import models.CivModel;
import models.Player;

/**
 * A GUI, eventually.
 *
 * @author Connie Sun, Ryan Smith, Luke Hankins, Tim Gavlick
 */
@SuppressWarnings("deprecation")
public class CivView extends Application implements Observer {

	// game data + controller
	private CivController controller;
	private CivModel model;
	private int mapNum;
	private int numPlayers;
	private int mapSize;
	private boolean isNewGame;
	private Player prevPlayer;

	// map hooks
	private ScrollPane mapScrollContainer;
	private Canvas mapCanvas;
	private Pane mapOverlayContainer;
	private Map<String, Image> markerImages;
	private ImageView mapHoverCursor;
	private ImageView mapSelectedCursor;
	private FadeTransition mapSelectedTransition;
	private Canvas fogCanvas;
	private Map<String, Image> fogImages;

	// sprite hooks
	private Pane spriteContainer;
	private Map<String, Image> spriteImages;

	// ui hooks
	private VBox unitPane;
	private Unit selectedUnit;
	private ScrollPane cityPane;
	private City selectedCity;
	private GridPane playersContainer;

	// viz constants
	private static final int WINDOW_WIDTH = 1024;
	private static final int WINDOW_HEIGHT = 576;
	private static final int TILE_SIZE = 120;
	private static final int CITY_SIZE = 100;
	private static final int SPRITE_SIZE = 60;
	private static final int RESOURCE_SIZE = 56;
	private static final double ISO_FACTOR = 0.6;
	private static final int SCROLL_GUTTER = 240;
	private static final int CITY_PANE_WIDTH = 240;

	// viz derived constants (for convenience)
	private int isoBoardWidth;
	private int isoBoardHeight;


	/**
	 * Build the UI, start the game, and regulate game flow.
	 *
	 * @param stage The stage automatically passed when called Application.launch()
	 */
	@Override
	public void start(Stage stage) {
		buildMenu(stage);
	}

	public void startGame(Stage stage) {
		this.controller = new CivController(model);
		this.spriteImages = new HashMap<>();

		model.addObserver(this);

		// calculate derived constants (less spaghetti later on)
		isoBoardWidth = model.getSize() * TILE_SIZE;
		isoBoardHeight = (int) (isoBoardWidth * ISO_FACTOR);

		// preload and save references to sprite images
		loadSpriteImages();

		// assemble ui
		Pane window = new Pane();
		buildUI(window);

		// populate initial map state
		if (isNewGame)
			controller.placeStartingUnits();
		renderAllSprites();

		// focus the map on any friendly unit so the starting player isn't lost
		// in fog
		focusFirstPlayerUnit(model.getCurPlayer(), true);

		// build the application window
		Scene scene = new Scene(window, WINDOW_WIDTH, WINDOW_HEIGHT);
		scene.getStylesheets().add("assets/CivView.css");
		stage.setScene(scene);
		stage.setTitle("Sid Meier's Civilization 0.5");

		// add global events
		mapCanvas.setOnMouseClicked(this::handleMapClick);
		mapCanvas.setOnMouseMoved(this::handleMapHover);
		scene.addEventFilter(KeyEvent.KEY_PRESSED, (KeyEvent ev) -> {
			if (ev.getCode() == KeyCode.ESCAPE) {
				deselect();
				ev.consume();
			}
		});
		// handle WindowClose event
		stage.setOnCloseRequest(ev -> {
			boolean saved = controller.close(); // serialize board
			String msg;
			if (saved) {
				msg = "Game state was successfully saved.";
			}
			else {
				msg = "Game state was not saved";
			}
			Alert endgame = new Alert(Alert.AlertType.INFORMATION);
			endgame.setContentText(msg);
			endgame.showAndWait();
			Platform.exit();
			System.exit(0);
		});
		controller.startTurn(); // begin the game
		mapCanvas.setOnMouseClicked(this::handleMapClick);
		mapCanvas.setOnMouseMoved(this::handleMapHover);
		scene.addEventFilter(KeyEvent.KEY_PRESSED, (KeyEvent ev) -> {
			if (ev.getCode() == KeyCode.ESCAPE) {
				deselect();
				ev.consume();
			}
		});

		stage.show();
	}

	/**
	 * Preload sprite images and return references to their Image objects. This
	 * prevents us from continually loading new images as the sprite layer
	 * refreshes, which is especially bad if javafx doesn't release them.
	 */
	private void loadSpriteImages() {
		spriteImages = new HashMap<>();
		markerImages = new HashMap<>();
		fogImages = new HashMap<>();

		String[] players = { "player-1", "player-2", "player-3", "player-4", "cpu-player" };
		String[] units = { "city", "scout", "settler", "warrior", "militia", "swordsman", "cavalry" };
		String[] resources = { "horse", "iron", "wheat" };

		try {
			for (String p : players) {
				for (String u : units) {
					spriteImages.put(u + "-" + p, new Image(new FileInputStream(
							"src/assets/sprites/" + u + "-" + p + ".png"
					)));
				}
			}

			for (String r : resources) {
				spriteImages.put(r, new Image(new FileInputStream("src/assets/resources/" + r + ".png")));
				spriteImages.put(r + "-absent", new Image(new FileInputStream("src/assets/resources/" + r + "-absent.png")));
				spriteImages.put(r + "-tile", new Image(new FileInputStream("src/assets/resources/" + r + "-tile.png")));
				spriteImages.put(r + "-claimed", new Image(new FileInputStream("src/assets/resources/" + r + "-claimed.png")));
			}

			markerImages.put("attackable", new Image(new FileInputStream("src/assets/tiles/attackable.png")));
			markerImages.put("costly", new Image(new FileInputStream("src/assets/tiles/costly.png")));
			markerImages.put("hover", new Image(new FileInputStream("src/assets/tiles/hover.png")));
			markerImages.put("owned", new Image(new FileInputStream("src/assets/tiles/owned.png")));
			markerImages.put("selected", new Image(new FileInputStream("src/assets/tiles/selected.png")));
			markerImages.put("valid", new Image(new FileInputStream("src/assets/tiles/valid.png")));

			// we're keying fog images as essentially binary strings, with each
			// cardinal direction able to be on or off to maintain continuity
			for (int i = 0; i < 16; i++) {
				// the +16 is to throw a 1 in the 16s place so this string
				// isn't truncated too soon due to only leading 0s
				String str = Integer.toBinaryString(i + 16).substring(1);
				fogImages.put(str, new Image(new FileInputStream("src/assets/fog/fog-" + str + ".png")));
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Update the UI when the model changes.
	 *
	 * @param observable The observable that's been updated
	 * @param o          Arbitrary data
	 */
	@Override
	public void update(Observable observable, Object o) {
		renderAllSprites();
		renderFog();
		updatePlayers();

		// update selectedUnit/selectedCity if they died in previous turn
		if (selectedUnit != null && selectedUnit.getHP() <= 0) {
			deselect();
		}
		if (selectedCity != null && selectedCity.getRemainingHP() <= 0) {
			deselect();
		}

		// determine if control has changed hands to another human player. If
		// so, deselect the prior player's stuff and refocus the map
		if (!model.isSinglePlayer()) {
			if (prevPlayer == null) {
				prevPlayer = model.getCurPlayer();
			} else if (prevPlayer != model.getCurPlayer()) {
				deselect();
				prevPlayer = model.getCurPlayer();
				focusFirstPlayerUnit(model.getCurPlayer(), false);
			}
		}

		// refresh any open detail panes and ranges, as the selected unit's
		// values may have changed
		mapOverlayContainer.getChildren().clear();
		if (selectedCity != null)
			selectCity(selectedCity);
		if (selectedUnit != null)
			selectUnit(selectedUnit);

		// add endgame
		if (controller.gameOver()) {
			Alert endgame = new Alert(Alert.AlertType.INFORMATION);
			endgame.setContentText("Game Over!");
			endgame.showAndWait();
			File oldGame = new File("save_game.dat");
			oldGame.delete();
			System.exit(0);
		}
	}

	/**
	 * Create and assemble the game UI.
	 *
	 * @param window The main pane that will contain all UI elements
	 */
	private void buildUI(Pane window) {
		window.getStyleClass().add("ui");

		// scrollable container that houses our map group
		mapScrollContainer = new ScrollPane();
		mapScrollContainer.setPrefSize(WINDOW_WIDTH, WINDOW_HEIGHT);
		mapScrollContainer.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
		mapScrollContainer.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
		mapScrollContainer.setPannable(true);
		mapScrollContainer.getStyleClass().add("map");
		window.getChildren().add(mapScrollContainer);

		// pane to contain the map canvas. This interim layer lets us add padding
		// without screwing up canvas click math, etc
		Pane mapElementContainer = new Pane();
		mapElementContainer.setPadding(new Insets(0, SCROLL_GUTTER, SCROLL_GUTTER, 0));
		mapScrollContainer.setContent(mapElementContainer);

		// terrain map: canvas element
		mapCanvas = new Canvas(isoBoardWidth, isoBoardHeight);
		mapElementContainer.getChildren().add(mapCanvas);
		mapCanvas.setLayoutX(SCROLL_GUTTER);
		mapCanvas.setLayoutY(SCROLL_GUTTER);

		// terrain map: bg
		GraphicsContext context = mapCanvas.getGraphicsContext2D();
		context.setFill(Color.BLACK);
		context.fillRect(0, 0, isoBoardWidth, isoBoardHeight);

		// terrain map: tiles
		for (int[] coords : getDrawTraversal()) {
			Tile tile = model.getTileAt(coords[0], coords[1]);
			if (tile == null)
				continue;

			Image tileImage = getTileImage(tile.getTerrainType());
			int[] isoCoords = gridToIso(coords[0], coords[1]);

			context.drawImage(tileImage, isoCoords[0], isoCoords[1], TILE_SIZE, TILE_SIZE * ISO_FACTOR);

			if (tile.getResourceType().length() > 0) {
				Image resourceImage = spriteImages.get(tile.getResourceType() + "-tile");
				if (resourceImage != null) {
					context.drawImage(
							resourceImage,
							isoCoords[0] + TILE_SIZE / 10.0,
							isoCoords[1] + (TILE_SIZE - RESOURCE_SIZE) / 2.0 * ISO_FACTOR,
							RESOURCE_SIZE,
							RESOURCE_SIZE * ISO_FACTOR
					);
				}
			}
		}

		// claim a layer for tile indicators
		mapOverlayContainer = new Pane();
		mapOverlayContainer.setPrefSize(isoBoardWidth, isoBoardHeight);
		mapOverlayContainer.setLayoutX(SCROLL_GUTTER);
		mapOverlayContainer.setLayoutY(SCROLL_GUTTER);
		mapOverlayContainer.setMouseTransparent(true);
		mapElementContainer.getChildren().add(mapOverlayContainer);

		// store hover cursor imageview for later so we don't have to keep
		// creating and destroying it many times per second
		mapHoverCursor = new ImageView(markerImages.get("hover"));
		mapHoverCursor.setFitWidth(TILE_SIZE);
		mapHoverCursor.setFitHeight(TILE_SIZE * ISO_FACTOR);
		mapHoverCursor.setMouseTransparent(true);
		mapElementContainer.getChildren().add(mapHoverCursor);

		// same with "selected" image
		mapSelectedCursor = new ImageView(markerImages.get("selected"));
		mapSelectedCursor.setFitWidth(TILE_SIZE);
		mapSelectedCursor.setFitHeight(TILE_SIZE * ISO_FACTOR);
		mapSelectedCursor.setMouseTransparent(true);
		mapElementContainer.getChildren().add(mapSelectedCursor);

		// ... and its transition
		mapSelectedTransition = new FadeTransition();
		mapSelectedTransition.setDuration(Duration.millis(1000));
		mapSelectedTransition.setFromValue(4);
		mapSelectedTransition.setToValue(0.7);
		mapSelectedTransition.setCycleCount(Integer.MAX_VALUE);
		mapSelectedTransition.setAutoReverse(true);
		mapSelectedTransition.setNode(mapSelectedCursor);

		// sprite layer
		spriteContainer = new Pane();
		spriteContainer.setPrefWidth(isoBoardWidth);
		spriteContainer.setPrefHeight(isoBoardHeight);
		spriteContainer.setLayoutX(SCROLL_GUTTER);
		spriteContainer.setLayoutY(SCROLL_GUTTER);
		spriteContainer.setMouseTransparent(true);
		mapElementContainer.getChildren().add(spriteContainer);

		// fog of war layer
		fogCanvas = new Canvas(isoBoardWidth, isoBoardHeight);
		mapElementContainer.getChildren().add(fogCanvas);
		fogCanvas.setLayoutX(SCROLL_GUTTER);
		fogCanvas.setLayoutY(SCROLL_GUTTER);
		fogCanvas.setMouseTransparent(true);

		// unit detail pane
		unitPane = new VBox();
		unitPane.getStyleClass().addAll("detail-pane", "detail-pane--unit");
		unitPane.setLayoutX(24);
		unitPane.setVisible(false);
		window.getChildren().add(unitPane);

		// city detail pane
		cityPane = new ScrollPane();
		cityPane.getStyleClass().add("city-pane-scroll-wrapper");
		cityPane.setPrefWidth(CITY_PANE_WIDTH);
		cityPane.setFitToWidth(true);
		cityPane.setPrefHeight(WINDOW_HEIGHT);
		cityPane.setLayoutX(WINDOW_WIDTH - CITY_PANE_WIDTH + 1);
		cityPane.setLayoutY(0);
		cityPane.setVisible(false);
		cityPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
		cityPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
		window.getChildren().add(cityPane);

		// container to house player readouts
		playersContainer = new GridPane();
		playersContainer.setLayoutX(24);
		playersContainer.setLayoutY(WINDOW_HEIGHT - 126);
		playersContainer.getStyleClass().add("players");
		window.getChildren().add(playersContainer);
	}

	/**
	 * Wipe and render the entire sprite layer.
	 */
	private void renderAllSprites() {
		clearAllSprites();
		for (int[] space : getDrawTraversal()) {
			Tile tile = model.getTileAt(space[0], space[1]);
			if (tile == null)
				continue;

			City city = null;
			if (tile.isCityTile())
				city = tile.getOwnerCity();
			Unit unit = tile.getUnit();

			if (city != null)
				renderCity(city);
			if (unit != null)
				renderUnit(unit);
		}
	}

	/**
	 * Render a single city to the map.
	 *
	 * @param city The city to render. Position will be derived from the City's
	 *             stored coords
	 */
	private void renderCity(City city) {
		int[] coords = gridToIso(city.getX(), city.getY());

		ImageView cityImageView = new ImageView(
				spriteImages.get("city-" + cssClassFrom(city.getOwner().getID()))
		);
		cityImageView.setFitWidth(CITY_SIZE);
		cityImageView.setFitHeight(CITY_SIZE);
		cityImageView.setMouseTransparent(true);
		cityImageView.setX(coords[0] + ((TILE_SIZE - CITY_SIZE) / 2.0));
		cityImageView.setY(coords[1] - 34.0); // Magic Number, for now
		spriteContainer.getChildren().add(cityImageView);

		renderSpriteHPBar(city.getRemainingHP(), city.getMaxHP(), coords[0], coords[1] + 6);
	}

	/**
	 * Render a single unit to the map.
	 *
	 * @param unit The unit to render. Position will be derived from the Unit's
	 *             stored coords
	 */
	private void renderUnit(Unit unit) {
		String player = cssClassFrom(unit.getOwner().getID());
		int[] coords = gridToIso(unit.getX(), unit.getY());

		ImageView unitImageView;
		if (unit instanceof Cavalry) {
			unitImageView = new ImageView(spriteImages.get("cavalry-" + player));
		} else if (unit instanceof Militia) {
			unitImageView = new ImageView(spriteImages.get("militia-" + player));
		} else if (unit instanceof Scout) {
			unitImageView = new ImageView(spriteImages.get("scout-" + player));
		} else if (unit instanceof Settler){
			unitImageView = new ImageView(spriteImages.get("settler-" + player));
		} else if (unit instanceof Swordsman) {
			unitImageView = new ImageView(spriteImages.get("swordsman-" + player));
		} else {
			unitImageView = new ImageView(spriteImages.get("warrior-" + player));
		}

		unitImageView.setFitWidth(SPRITE_SIZE);
		unitImageView.setFitHeight(SPRITE_SIZE);
		unitImageView.setMouseTransparent(true);
		unitImageView.setX(coords[0] + ((TILE_SIZE - SPRITE_SIZE) / 2.0));
		unitImageView.setY(coords[1] - (SPRITE_SIZE / 4.0));
		spriteContainer.getChildren().add(unitImageView);

		renderSpriteHPBar(unit.getHP(), unit.getMaxHP(), coords[0], coords[1]);
	}

	/**
	 * Place an inline HP bar above a certain map square
	 *
	 * @param cur The unit or city's current hp
	 * @param max The unit or city's max hp
	 * @param x   The left iso coord of the space to render on
	 * @param y   The top iso coord of the space to render on
	 */
	private void renderSpriteHPBar(double cur, double max, int x, int y) {
		GridPane hpBar = createHPBar(cur, max);
		hpBar.setPrefWidth(TILE_SIZE / 2.0);
		hpBar.setLayoutX(x + TILE_SIZE / 4.0);
		hpBar.setLayoutY(y + (TILE_SIZE * ISO_FACTOR) / 1.35);
		spriteContainer.getChildren().add(hpBar);
	}

	/**
	 * Clear all currently rendered sprites.
	 */
	private void clearAllSprites() {
		spriteContainer.getChildren().clear();
		System.gc();
	}

	/**
	 * Render fog of war on top of the map and sprites based on what the
	 * current player has already discovered.
	 */
	private void renderFog() {
		GraphicsContext context = fogCanvas.getGraphicsContext2D();
		Player player = model.getCurPlayer();
		int size = model.getSize();

		context.clearRect(0, 0, isoBoardWidth, isoBoardHeight);

		if (!controller.isHumanTurn())
			return;

		for (int[] coords : getDrawTraversal()) {
			Tile tile = model.getTileAt(coords[0], coords[1]);
			if (tile == null || tile.canSeeTile(player))
				continue;

			// since we want some continuity to our fog but also want a little
			// hint at its edge, we'll need to load a different image depending
			// on what's bordering it. These images are named for the cardinal
			// directions stemming from this tile: up, right, down, left. A 1
			// in these places means to connect the fog in that direction (or
			// it's at the edge of the board)
			char[] imageDirs = { '0', '0', '0', '0' };

			// up
			if (coords[1] <= 0 || !model.getTileAt(coords[0], coords[1] - 1).canSeeTile(player)) {
				imageDirs[0] = '1';
			}
			// right
			if (coords[0] >= size - 1 || !model.getTileAt(coords[0] + 1, coords[1]).canSeeTile(player)) {
				imageDirs[1] = '1';
			}
			// down
			if (coords[1] >= size - 1 || !model.getTileAt(coords[0], coords[1] + 1).canSeeTile(player)) {
				imageDirs[2] = '1';
			}
			// left
			if (coords[0] <= 0 || !model.getTileAt(coords[0] - 1, coords[1]).canSeeTile(player)) {
				imageDirs[3] = '1';
			}

			// draw the thing
			Image fogImage = fogImages.get(new String(imageDirs));
			int[] isoCoords = gridToIso(coords[0], coords[1]);
			// images are 2px bigger so we can draw them with 1px of overlap.
			// Otherwise, some underlying info can peek through
			context.drawImage(
					fogImage, isoCoords[0] - 1, isoCoords[1] - 1,
					TILE_SIZE + 2, TILE_SIZE * ISO_FACTOR + 2
			);
		}

		// since a group of four "full" images in a square will leave a small
		// gap in the center due to the way these images are drawn to account
		// for corners, we'll need to fill them in with a solid color. We could
		// have accounted for this in the images themselves by reading
		// diagonals, but it would have required 256 images to cover all
		// permutations instead of 16, and ain't nobody got time for that
		context.setFill(Color.BLACK);
		int radius = 23;

		// we're iterating on corners, not tiles, so inclusive high bound
		for (int x = 0; x <= model.getSize(); x++) {
			for (int y = 0; y <= model.getSize(); y++) {
				// search each tile touching this gap on its diagonals
				int diags = 0;

				// top left
				if (x == 0 || y == 0 || !model.getTileAt(x - 1, y - 1).canSeeTile(player))
					diags++;
				// top right
				if (x >= size || y == 0 || !model.getTileAt(x, y - 1).canSeeTile(player))
					diags++;
				// bottom right
				if (x >= size || y >= size || !model.getTileAt(x, y).canSeeTile(player))
					diags++;
				// bottom left
				if (x == 0 || y >= size || !model.getTileAt(x - 1, y).canSeeTile(player))
					diags++;

				if (diags == 4) {
					int[] coords = gridToIso(x, y);
					context.fillOval(
							coords[0] + TILE_SIZE / 2.0 - radius / 2.0,
							coords[1] - radius / 2.0,
							radius, radius
					);
				}
			}
		}
	}

	/**
	 * Render an overlay with readouts of all current players. These readouts
	 * act as a key, mapping players to colors that are then reflected on units
	 * and cities, and also provide some quick data about each player's
	 * remaining units and cities.
	 */
	private void updatePlayers() {
		playersContainer.getChildren().clear();

		int i = 0;
		for (Player player : model.getAllPlayers()) {

			// add the 'end turn' button above the current human player
			if (player == model.getCurPlayer()) {
				Button endTurnButton = new Button("End Turn");
				endTurnButton.getStyleClass().addAll("button", "end-turn-button");
				playersContainer.add(endTurnButton, i, 0);
				endTurnButton.setOnMouseClicked(ev -> {
					if (controller.isHumanTurn()) {
						controller.endTurn();
					}
				});
			}

			// add player name readout
			VBox playerBox = new VBox();
			playerBox.getStyleClass().addAll("player", cssClassFrom(player.getID()));
			playersContainer.add(playerBox, i, 1);

			Text playerName = new Text(player.getID());
			playerName.getStyleClass().add("player__name");
			playerBox.getChildren().add(playerName);

			int cities = player.getCities().size();
			int units = player.getUnits().size();
			Text playerStats = new Text(
					"" + cities + (cities == 1 ? " city" : " cities") +
					"   " + units + (units == 1 ? " unit" : " units")
			);
			playerStats.getStyleClass().add("player__stats");
			playerBox.getChildren().add(playerStats);

			i++;
		}
	}

	/**
	 * Normalize a string into a form more suitable for a css class by
	 * lowercasing it and replacing spaces with dashes
	 *
	 * @param str The string to classname-ify
	 * @return A suitable css classname
	 */
	private String cssClassFrom(String str) {
		return str.toLowerCase().replace(' ', '-');
	}

	/**
	 * Center the map on a particular board index.
	 *
	 * @param x       The x index of the board grid to center on
	 * @param y       The y index of the board grid to center on
	 * @param animate True if this refocus should be smooth/animated, false if it
	 *                should be instant
	 */
	private void focusMap(int x, int y, boolean animate) {
		int[] coords = gridToIso(x, y);

		// ScrollPane scroll values are percentages (0 through 1), not raw pixel values
		double targetH = (coords[0] + TILE_SIZE / 2.0) / (double) isoBoardWidth;
		double targetV = (coords[1] + TILE_SIZE * ISO_FACTOR / 2.0) / (double) isoBoardHeight;

		if (animate) {
			Animation animation = new Timeline(new KeyFrame(Duration.millis(250),
					new KeyValue(mapScrollContainer.hvalueProperty(), targetH, Interpolator.EASE_BOTH),
					new KeyValue(mapScrollContainer.vvalueProperty(), targetV, Interpolator.EASE_BOTH)));
			animation.play();
		} else {
			mapScrollContainer.setHvalue(targetH);
			mapScrollContainer.setVvalue(targetV);
		}
	}

	/**
	 * Given a player, focus on a unit or city associated with that player.
	 * This doesn't guarantee focus on any particular unit, only the first
	 * found. Prefers units over cities.
	 *
	 * @param player The player to focus on
	 * @param useFallback If true, focus on the center of the map if no units
	 *                    or cities could be found. If false, don't refocus
	 *                    the map if no units or cities could be found
	 */
	private void focusFirstPlayerUnit(Player player, boolean useFallback) {
		if (player.getUnits().size() > 0) {
			Unit unit = player.getUnits().get(0);
			focusMap(unit.getX(), unit.getY(), false);
		} else if (player.getCities().size() > 0) {
			City city = player.getCities().get(0);
			focusMap(city.getX(), city.getY(), false);
		} else if (useFallback) {
			focusMap(model.getSize() / 2, model.getSize() / 2, false);
		}
	}

	/**
	 * Handle an arbitrary click on the map at any time.
	 *
	 * @param ev The event object generated from the click
	 */
	private void handleMapClick(MouseEvent ev) {
		// filter mouseup events that happen after releasing a drag
		// https://stackoverflow.com/questions/26590010/how-can-i-stop-javafx-parent-node-getting-click-after-drag-between-children
		if (!ev.isStillSincePress())
			return;

		int[] space = isoToGrid(ev.getX(), ev.getY());
		Tile tile = controller.getTileAt(space[0], space[1]);

		// reject clicks in the negative space left by the iso view
		if (tile == null)
			return;

		Unit targetUnit = tile.getUnit();
		City targetCity = null;
		if (tile.isCityTile())
			targetCity = tile.getOwnerCity();

		// if a friendly unit is already selected, we'll have different behaviors
		// depending on if the click was on an enemy unit in range, another
		// friendly unit, or neither
		if (selectedUnit != null) {
			// NOTE: controller does checking for moves; the move function should
			// take care of city, enemy, and empty tile cases and returns true
			// for successful move/attack. Just deselect if unsuccessful choice
			if (!controller.moveUnit(selectedUnit, space[0], space[1])) {
				deselect();

				// if move was successful and we're moving out of a city, make
				// sure focus is only on the unit
			} else if (selectedCity != null) {
				Unit tmp = selectedUnit;
				deselect();
				selectUnit(tmp);
			}
		}

		// if only a friendly city is already selected, another map click
		// always deselects it. We might reselect it again right after, but
		// don't care for now
		if (selectedCity != null && selectedUnit == null)
			deselect();

		// select a new or different friendly unit
		if (targetUnit != null && targetUnit.getOwner() == model.getCurPlayer()) {
			selectUnit(targetUnit);
		}

		// select a friendly city
		if (targetCity != null && targetCity.getOwner() == model.getCurPlayer()) {
			selectCity(targetCity);
		}
	}

	/**
	 * Give visual indication of hover within the map.
	 *
	 * @param ev The MouseEvent generated by a hover on the canvas
	 */
	private void handleMapHover(MouseEvent ev) {
		mapHoverCursor.setVisible(false);

		// "snap" to a grid space by getting its grid coord and re-translating
		// to iso coords
		int[] space = isoToGrid(ev.getX(), ev.getY());

		// reject events in the negative space left by the iso view
		if (space[0] < 0 || space[0] >= model.getSize() || space[1] < 0 || space[1] >= model.getSize()) {
			return;
		}

		int[] coords = gridToIso(space[0], space[1]);

		mapHoverCursor.setX(coords[0] + SCROLL_GUTTER);
		mapHoverCursor.setY(coords[1] + SCROLL_GUTTER);
		mapHoverCursor.setVisible(true);
	}

	/**
	 * Deselect the currently selected unit and/or city. Also hides the respective
	 * detail pane(s)
	 */
	private void deselect() {
		selectedUnit = null;
		selectedCity = null;

		unitPane.setVisible(false);
		unitPane.getChildren().clear();
		cityPane.setVisible(false);
		cityPane.setContent(null);

		mapSelectedCursor.setVisible(false);
		mapSelectedTransition.pause();

		mapOverlayContainer.getChildren().clear();
	}

	/**
	 * Select a unit. This involves marking said unit as selected, centering the map
	 * on it, and building and showing a detail pane that displays the unit's
	 * properties.
	 *
	 * @param unit The Unit to select
	 */
	private void selectUnit(Unit unit) {
		if (unit == null)
			return;

		Tile tile = controller.getTileAt(unit.getX(), unit.getY());

		// javafx doesn't calc this pane height correctly, so we've got to do
		// it ourselves
		int estimatedHeight = 230;

		selectedUnit = unit;

		unitPane.getChildren().clear();

		// pane info: label
		Text name = new Text(unit.getLabel());
		name.getStyleClass().add("detail-pane__name");
		unitPane.getChildren().add(name);

		// pane info: HP
		String hpDisp = "positive";
		if (unit.getHP() < unit.getMaxHP() * 2 / 3)
			hpDisp = "neutral";
		if (unit.getHP() < unit.getMaxHP() * 1 / 3)
			hpDisp = "negative";
		TextFlow hpFlow = createLabeledFigure("HP", (int) unit.getHP(), (int) unit.getMaxHP(), hpDisp);
		GridPane hpBar = createHPBar(unit.getHP(), unit.getMaxHP());
		unitPane.getChildren().addAll(hpFlow, hpBar);

		// pane info: attack stat
		String attackDisp = "";
		if (tile.getAttackModifier() > 1)
			attackDisp = "positive";
		if (tile.getAttackModifier() < 1)
			attackDisp = "negative";
		TextFlow attackFlow = createLabeledFigure("attack", (int) (unit.getAttackValue() * tile.getAttackModifier()),
				(tile.getAttackModifier() != 1 ? (int) unit.getAttackValue() : -1), attackDisp);
		attackFlow.getStyleClass().add("detail-pane__space-above");
		unitPane.getChildren().add(attackFlow);

		// pane info: attack modifier
		if (tile.getAttackModifier() != 1) {
			TextFlow attackHelp = new TextFlow();
			attackHelp.getStyleClass().add("detail-pane__help");
			Text attackMod = new Text((tile.getAttackModifier() > 1 ? "+" : "-") + " attack ");
			attackMod.getStyleClass().add(tile.getAttackModifier() > 1 ? "positive" : "negative");
			Text attackWhy = new Text();
			if (tile.isCityTile()) {
				attackWhy.setText("inside a city");
			} else {
				attackWhy.setText("in " + tile.getTerrainType().name().toLowerCase() + "s");
			}
			attackHelp.getChildren().addAll(attackMod, attackWhy);
			unitPane.getChildren().add(attackHelp);
			estimatedHeight += 20;
		}

		// pane info: movement
		TextFlow moveFlow = createLabeledFigure("moves remaining", unit.getMovement(), -1, "");
		moveFlow.getStyleClass().add("detail-pane__space-above");
		unitPane.getChildren().add(moveFlow);

		// settler action
		if (unit instanceof Settler) {
			Pane spacer = new Pane();
			spacer.getStyleClass().add("detail-pane__space-above");

			Button settleButton = new Button("Settle New City");
			settleButton.getStyleClass().addAll("button", "detail-pane__settle-button");

			unitPane.getChildren().addAll(spacer, settleButton);

			settleButton.setOnMouseClicked(ev -> {
				controller.foundCity(unit.getX(), unit.getY());
				deselect();
				renderAllSprites();
			});

			estimatedHeight += 50;
		}

		// show pane
		unitPane.setVisible(true);
		unitPane.setLayoutY((WINDOW_HEIGHT - estimatedHeight) / 2.0);

		// add selection indicator
		selectTile(unit.getX(), unit.getY());

		// add range indicators
		addRangeIndicators(selectedUnit);
	}

	/**
	 * Select a city. This involves marking said city as selected, centering the map
	 * on it, and building and showing a detail pane that displays the city's
	 * properties.
	 *
	 * @param city The City to select
	 */
	private void selectCity(City city) {
		if (city == null)
			return;

		selectedCity = city;

		cityPane.setContent(null);

		VBox cityPaneContent = new VBox();
		cityPaneContent.getStyleClass().addAll("detail-pane", "detail-pane--city");
		cityPaneContent.setPrefWidth(CITY_PANE_WIDTH);
		cityPaneContent.setMinHeight(WINDOW_HEIGHT - 2);

		// pane info: labeling row (name and resources)
		HBox labelRow = new HBox();
		labelRow.setAlignment(Pos.CENTER_LEFT);

		// name
		Text name = new Text("City");
		name.getStyleClass().add("detail-pane__name");

		// maximally distribute space between title and resources
		// https://stackoverflow.com/questions/40883858/how-to-evenly-distribute-elements-of-a-javafx-vbox
		Region titleSpacer = new Region();
		HBox.setHgrow(titleSpacer, Priority.ALWAYS);

		// resources
		HBox resources = new HBox();
		ImageView horseView = new ImageView(spriteImages.get(
				city.getProducableUnits().contains("Cavalry") ? "horse" : "horse-absent"
		));
		ImageView ironView = new ImageView(spriteImages.get(
				city.getProducableUnits().contains("Swordsman") ? "iron" : "iron-absent"
		));
		ImageView wheatView = new ImageView(spriteImages.get(
				city.getProducableUnits().contains("Militia") ? "wheat" : "wheat-absent"
		));
		resources.getStyleClass().add("detail-pane__resources");
		resources.getChildren().addAll(horseView, ironView, wheatView);

		labelRow.getChildren().addAll(name, titleSpacer, resources);
		cityPaneContent.getChildren().add(labelRow);

		// pane info: HP
		String hpDisp = "positive";
		if (city.getRemainingHP() < city.getMaxHP() * 2 / 3)
			hpDisp = "neutral";
		if (city.getRemainingHP() < city.getMaxHP() * 1 / 3)
			hpDisp = "negative";
		TextFlow hpFlow = createLabeledFigure("HP", (int) city.getRemainingHP(), (int) city.getMaxHP(), hpDisp);
		GridPane hpBar = createHPBar(city.getRemainingHP(), city.getMaxHP());
		hpBar.setPrefWidth(CITY_PANE_WIDTH - 50);
		cityPaneContent.getChildren().addAll(hpFlow, hpBar);

		// pane info: population
		HBox popCount = new HBox();
		popCount.getStyleClass().add("detail-pane__pop-count");
		for (int i = 0; i < city.getPopulation(); i++) {
			Rectangle icon = new Rectangle(15, 20);
			icon.getStyleClass().add("population-icon");
			popCount.getChildren().add(icon);
		}
		Text popLabel = new Text("population (grows in " + city.getTurnsBeforeGrowth()
				+ (city.getTurnsBeforeGrowth() == 1 ? " turn)" : " turns)"));
		popLabel.getStyleClass().add("detail-pane__label");
		cityPaneContent.getChildren().addAll(popCount, popLabel);

		// pane info: production points
		TextFlow prodFlow = createLabeledFigure("production points", (int) city.getProductionReserve(), -1, "");
		prodFlow.getStyleClass().add("detail-pane__space-above");
		TextFlow prodRateFlow = new TextFlow();
		prodRateFlow.getStyleClass().add("detail-pane__help");
		Text prodRate = new Text("+ " + (int) city.getProduction());
		prodRate.getStyleClass().add("positive");
		Text prodRateLabel = new Text(" per turn");
		prodRateFlow.getChildren().addAll(prodRate, prodRateLabel);
		cityPaneContent.getChildren().addAll(prodFlow, prodRateFlow);

		// pane actions
		Pane spacer = new Pane(); // text nodes can't take padding, so we'll space with this
		spacer.getStyleClass().add("detail-pane__space-above");
		Text buildLabel = new Text("Build:");
		buildLabel.getStyleClass().add("detail-pane__label");
		Node[] scoutRow = createCityBuildButton(city, "Scout", Unit.unitCosts.get("Scout"), null);
		Node[] warriorRow = createCityBuildButton(city, "Warrior", Unit.unitCosts.get("Warrior"), null);
		Node[] settlerRow = createCityBuildButton(city, "Settler", Unit.unitCosts.get("Settler"), null);
		scoutRow[1].setOnMouseClicked(ev -> controller.createUnit(
				selectedCity.getX(), selectedCity.getY(), "Scout"
		));
		warriorRow[1].setOnMouseClicked(ev -> controller.createUnit(
				selectedCity.getX(), selectedCity.getY(), "Warrior"
		));
		settlerRow[1].setOnMouseClicked(ev -> controller.createUnit(
				selectedCity.getX(), selectedCity.getY(), "Settler"
		));
		cityPaneContent.getChildren().addAll(
				spacer, buildLabel, scoutRow[0], warriorRow[0], settlerRow[0]
		);

		// additional unlockable units
		if (city.getProducableUnits().contains("Cavalry")) {
			Node[] cavalryRow = createCityBuildButton(
					city, "Cavalry", Unit.unitCosts.get("Cavalry"), spriteImages.get("horse")
			);
			cavalryRow[1].setOnMouseClicked(ev -> controller.createUnit(
					selectedCity.getX(), selectedCity.getY(), "Cavalry"
			));
			cityPaneContent.getChildren().add(cavalryRow[0]);
		}
		if (city.getProducableUnits().contains("Militia")) {
			Node[] militiaRow = createCityBuildButton(
					city, "Militia", Unit.unitCosts.get("Militia"), spriteImages.get("wheat")
			);
			militiaRow[1].setOnMouseClicked(ev -> controller.createUnit(
					selectedCity.getX(), selectedCity.getY(), "Militia"
			));
			cityPaneContent.getChildren().add(militiaRow[0]);
		}
		if (city.getProducableUnits().contains("Swordsman")) {
			Node[] swordsmanRow = createCityBuildButton(
					city, "Swordsman", Unit.unitCosts.get("Swordsman"), spriteImages.get("iron")
			);
			swordsmanRow[1].setOnMouseClicked(ev -> controller.createUnit(
					selectedCity.getX(), selectedCity.getY(), "Swordsman"
			));
			cityPaneContent.getChildren().add(swordsmanRow[0]);
		}

		// show pane
		cityPane.setContent(cityPaneContent);
		cityPane.setVisible(true);

		// add selection indicator
		selectTile(city.getX(), city.getY());

		// show ownership/influence range
		addOwnershipIndicators(selectedCity);
	}

	/**
	 * Add a selection indicator to a tile and center the view on it.
	 *
	 * @param x The x index of the tile in the map grid
	 * @param y The y index of the tile in the map grid
	 */
	private void selectTile(int x, int y) {
		// add selection indicator
		int[] coords = gridToIso(x, y);
		mapSelectedCursor.setX(coords[0] + SCROLL_GUTTER);
		mapSelectedCursor.setY(coords[1] + SCROLL_GUTTER);
		mapSelectedCursor.setVisible(true);
		mapSelectedTransition.play();

		// center map on tile
		focusMap(x, y, true);
	}

	/**
	 * Add an indicator to each tile that a given unit can currently move to and/or
	 * attack.
	 *
	 * @param unit The unit to indicate valid moves for
	 */
	private void addRangeIndicators(Unit unit) {
		HashSet<int[]> validMoves = controller.getValidMoves(unit);

		mapOverlayContainer.getChildren().clear();

		for (int[] move : validMoves) {
			int[] coords = gridToIso(move[0], move[1]);
			ImageView markerView;
			Tile moveTile = controller.getTileAt(move[0], move[1]);
			// indicate if there's an attackable unit or city in the space
			if ((moveTile.getUnit() != null && moveTile.getUnit().getOwner() != model.getCurPlayer()) ||
					(moveTile.isCityTile() && moveTile.getOwnerCity().getOwner() != model.getCurPlayer())) {
				markerView = new ImageView(markerImages.get("attackable"));
			} else if (moveTile.getMovementModifier() < 0) {
				markerView = new ImageView(markerImages.get("costly"));
			} else {
				markerView = new ImageView(markerImages.get("valid"));
			}
			markerView.setX(coords[0]);
			markerView.setY(coords[1]);
			markerView.setMouseTransparent(true);
			mapOverlayContainer.getChildren().add(markerView);
		}
	}

	/**
	 * Add an indicator to each tile under a given city's ownership/influence
	 *
	 * @param city The city to add indicators for
	 */
	private void addOwnershipIndicators(City city) {
		for (int[] space : getDrawTraversal()) {
			Tile tile = controller.getTileAt(space[0], space[1]);

			if (tile.getOwnerCity() == city) {
				int[] coords = gridToIso(space[0], space[1]);
				ImageView markerView = new ImageView(markerImages.get("owned"));
				markerView.setX(coords[0]);
				markerView.setY(coords[1]);
				markerView.setMouseTransparent(true);
				mapOverlayContainer.getChildren().add(markerView);

				if (tile.getResourceType().length() > 0) {
					Image resourceImage = spriteImages.get(tile.getResourceType() + "-tile");
					if (resourceImage != null) {
						ImageView resourceView = new ImageView(resourceImage);
						resourceView.setFitWidth(RESOURCE_SIZE);
						resourceView.setFitHeight(RESOURCE_SIZE * ISO_FACTOR);
						resourceView.setLayoutX(coords[0] + TILE_SIZE / 10.0);
						resourceView.setLayoutY(coords[1] + (TILE_SIZE - RESOURCE_SIZE) / 2.0 * ISO_FACTOR);
						resourceView.setMouseTransparent(true);
						mapOverlayContainer.getChildren().add(resourceView);
					}
				}
			}
		}
	}

	/**
	 * Assemble a TextFlow with a common layout for figures and their associated
	 * labels.
	 *
	 * @param label       The label text to render
	 * @param figure      The primary figure value
	 * @param max         If the figure has a max value, pass it here. Otherwise,
	 *                    pass a negative number
	 * @param disposition The disposition class to render ("positive", "neutral", or
	 *                    "negative") or an empty string if not applicable
	 * @return The assembled TextFlow object containing the data inside new Text
	 *         nodes
	 */
	private TextFlow createLabeledFigure(String label, int figure, int max, String disposition) {
		TextFlow result = new TextFlow();
		result.getStyleClass().add("detail-pane__figure-group");

		Text figureNode = new Text("" + figure);
		figureNode.getStyleClass().add("detail-pane__figure");
		if (disposition.length() > 0)
			figureNode.getStyleClass().add(disposition);
		result.getChildren().add(figureNode);

		if (max >= 0) {
			Text dividerNode = new Text(" / ");
			dividerNode.getStyleClass().add("detail-pane__divider");
			result.getChildren().add(dividerNode);

			Text maxNode = new Text("" + max);
			maxNode.getStyleClass().add("detail-pane__figure");
			result.getChildren().add(maxNode);
		}

		Text labelNode = new Text("  " + label);
		labelNode.getStyleClass().add("detail-pane__label");
		result.getChildren().add(labelNode);

		return result;
	}

	/**
	 * Create a standardized structure representing an HP bar.
	 *
	 * @param cur The current HP
	 * @param max The max HP
	 * @return A structure of Nodes that represent an HP bar
	 */
	private GridPane createHPBar(double cur, double max) {
		GridPane result = new GridPane();
		result.getStyleClass().add("hp-bar");

		Pane curNode = new Pane();
		curNode.getStyleClass().add("hp-bar__remainder");
		result.add(curNode, 0, 0);

		ColumnConstraints constraint = new ColumnConstraints();
		constraint.setPercentWidth(cur / max * 100);
		result.getColumnConstraints().add(constraint);

		return result;
	}

	/**
	 * Create and assemble the nodes necessary to display a 'build' button with
	 * associated costs, for display on a city detail pane.
	 *
	 * <p>
	 * Styles the button and labels depending on whether enough resources exist to
	 * build the unit.
	 *
	 * @param city      The city this would build to
	 * @param label     The label to add to the button
	 * @param pointCost The production point cost that building this unit requires
	 * @return A two-element node array. The first element is the containing
	 *         GridPane for the entire row so it can be added to the layout. The
	 *         second element is the created Button so the calling method can attach
	 *         an event listener to it
	 */
	private Node[] createCityBuildButton(City city, String label, double pointCost, Image resource) {
		GridPane container = new GridPane();
		container.getStyleClass().add("detail-pane__build-row");

		// the 'build' button itself
		Button button = new Button(label);
		button.getStyleClass().addAll("button", "detail-pane__button");
		if (1 > city.getPopulation() || pointCost > city.getProductionReserve()
				|| controller.getTileAt(city.getX(), city.getY()).getUnit() != null) {
			button.setDisable(true);
			button.getStyleClass().add("detail-pane__button--disabled");
		}
		container.add(button, 0, 0);

		// costs: population and resources
		HBox popCostNode = new HBox();
		popCostNode.setAlignment(Pos.CENTER_LEFT);
		popCostNode.getStyleClass().add("detail-pane__cost-icons");

		// population
		Rectangle icon = new Rectangle(9, 12);
		icon.getStyleClass().add("population-icon");
		if (city.getPopulation() < 1) {
			icon.getStyleClass().add("population-icon--unavailable");
		}
		popCostNode.getChildren().add(icon);

		// resources
		if (resource != null) {
			ImageView resourceView = new ImageView(resource);
			resourceView.setFitWidth(18);
			resourceView.setFitHeight(18);
			popCostNode.getChildren().add(resourceView);
		}

		// production points
		TextFlow pointCostNode = new TextFlow();
		pointCostNode.getStyleClass().add("detail-pane__help");
		Text pointCostFigure = new Text("" + (int) pointCost);
		pointCostFigure.getStyleClass().add(pointCost > city.getProductionReserve() ? "negative" : "positive");
		Text pointCostLabel = new Text(" pp");
		pointCostNode.getChildren().addAll(pointCostFigure, pointCostLabel);

		// stack costs on top of each other
		VBox costs = new VBox();
		costs.getStyleClass().add("detail-pane__build-costs");
		costs.getChildren().addAll(popCostNode, pointCostNode);
		container.add(costs, 1, 0);

		ColumnConstraints constraint = new ColumnConstraints();
		constraint.setPrefWidth(110);
		container.getColumnConstraints().add(constraint);

		Node[] result = new Node[2];
		result[0] = container;
		result[1] = button;

		return result;
	}

	/**
	 * Get a serialized list of board coordinates in diagonal-traversal order
	 * starting from the top-left of the board.
	 *
	 * <p>
	 * This is necessary because the board must be drawn back-to-front in order for
	 * perspective overlapping to appear correct.
	 *
	 * @return A list of two-element int arrays, where the first int is the board x
	 *         index and the second is the board y index
	 */
	private List<int[]> getDrawTraversal() {
		List<int[]> result = new ArrayList<>();

		// we'll start slices from each edge space on the left and bottom (one
		// shared). The slice starting at the bottom-left corner will be the
		// turning point
		int diagonalSlices = model.getSize() * 2 - 1;
		int mid = diagonalSlices / 2;
		int sliceSize = 0;
		int startX;
		int startY;

		for (int slice = 0; slice < diagonalSlices; slice++) {
			if (slice <= mid) {
				sliceSize++;
				startX = 0;
				startY = slice;
			} else {
				sliceSize--;
				startX = slice - mid;
				startY = model.getSize() - 1;
			}

			for (int i = 0; i < sliceSize; i++) {
				int[] coord = new int[2];
				coord[0] = startX + i;
				coord[1] = startY - i;
				result.add(coord);
			}
		}

		return result;
	}

	/**
	 * Given a set of grid space coordinates, return the pixel coordinates of the
	 * tile in our isometric rendering space.
	 *
	 * <p>
	 * The coordinates returned indicate the top-left bound of the iso space. Use in
	 * combination with this class' TILE_SIZE and ISO_FACTOR constants to do
	 * additional math afterwards (like finding the center point of the space, etc).
	 *
	 * @param x The x index of the space in the map grid to find
	 * @param y The y index of the space in the map grid to find
	 * @return A two-element int array containing the x and y coordinates of the
	 *         top-left pixel of the input tile in iso space
	 */
	private int[] gridToIso(int x, int y) {
		int[] result = new int[2];

		// our origin point is [half of the real rendered width, 0] (the very
		// top corner of the rendered board)
		int originX = isoBoardWidth / 2;
		result[0] = originX;

		// movements along board x add iso x and iso y
		result[0] += x * TILE_SIZE / 2;
		result[1] += (int) (x * TILE_SIZE / 2 * ISO_FACTOR);

		// movements along board y subtract iso x and add iso y
		result[0] -= y * TILE_SIZE / 2;
		result[1] += (int) (y * TILE_SIZE / 2 * ISO_FACTOR);

		// coord currently points at top-center of space. Normalize to top-left corner
		result[0] -= TILE_SIZE / 2;

		return result;
	}

	/**
	 * Translate absolute pixel coordinates in the isometric display to grid spaces.
	 *
	 * <p>
	 * Note that returned indices may be outside the real bounds of the grid due to
	 * the natural negative space that any isometric view has in the corners. Since
	 * this method doesn't necessarily return "safe" indices, make sure to check.
	 *
	 * @param x The horizontal pixel offset of the coordinate from the left edge of
	 *          the canvas
	 * @param y The vertical pixel offset of the coordinate from the top edge of the
	 *          canvas
	 * @return A two-element int array containing the x and y indices of the space
	 *         in the grid
	 */
	private int[] isoToGrid(double x, double y) {
		int[] result = new int[2];

		// derived from algebra-ing the gridToIso() calculations:
		//
		// x = tileX * TILE_SIZE / 2 - tileY * TILE_SIZE / 2;
		// y = tileX * TILE_SIZE / 2 * ISO_FACTOR + tileY * TILE_SIZE / 2 * ISO_FACTOR;
		//
		// then translating based on the board size
		double w = TILE_SIZE / 2.0;
		double h = TILE_SIZE / 2.0 * ISO_FACTOR;
		result[0] = (int) ((x / w + y / h) / 2 - model.getSize() / 2);
		result[1] = (int) ((y / h - x / w) / 2 + model.getSize() / 2);

		return result;
	}

	/**
	 * Get a tile image for a terrain type.
	 *
	 * <p>
	 * Since there can be many tile choices for a given terrain type, this method
	 * chooses one randomly. Don't expect the same image for the same terrain type
	 * each time.
	 *
	 * @param terrainType The terrain type to get a file for
	 * @return An Image object containing the image data for a tile image matching
	 *         the terrain type
	 */
	private Image getTileImage(Tile.terrainTypes terrainType) {
		try {
			if (terrainType == Tile.terrainTypes.HILL) {
				return new Image(new FileInputStream("src/assets/tiles/hill-" + getRandInt(1, 5) + ".png"));
			} else if (terrainType == Tile.terrainTypes.SWAMP) {
				return new Image(new FileInputStream("src/assets/tiles/swamp-" + getRandInt(1, 5) + ".png"));
			} else if (terrainType == Tile.terrainTypes.WATER) {
				return new Image(new FileInputStream("src/assets/tiles/water-" + getRandInt(1, 5) + ".png"));
			} else if (terrainType == Tile.terrainTypes.MOUNTAIN) {
				return new Image(new FileInputStream("src/assets/tiles/mountain-" + getRandInt(1, 5) + ".png"));
			} else {
				return new Image(new FileInputStream("src/assets/tiles/field-" + getRandInt(1, 5) + ".png"));
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Generate a random integer in a given range.
	 *
	 * @param min The minimum possible result (inclusive)
	 * @param max The maximum possible result (inclusive)
	 * @return A random integer between the two bounds, inclusive
	 */
	private int getRandInt(int min, int max) {
		return (int) (Math.random() * (max - min + 1) + min);
	}

	/**
	 * buildMenu() builds our Main Menu for our game. This menu includes
	 * 	a New Game, Load Game and Exit button
	 * @param stage our primary stage for our javafx environment
	 * @param stage our stage for our javafx environment
	 */
	private void buildMenu(Stage stage) {
		BorderPane Window = new BorderPane();
		Scene scene = new Scene(Window, WINDOW_WIDTH, WINDOW_HEIGHT);
		scene.getStylesheets().add("assets/CivView.css");
		stage.setScene(scene);
		stage.setTitle("Sid Meier's Civilization 0.5");

		VBox garbageLeft = new VBox();
		VBox garbageTop = new VBox();
		VBox MenuOptions = new VBox();
		Text title = new Text("Civilization 0.5");
		Button newGame = new Button("New Game");
		//newGame.setOnAction(new EventHandler);
		newGame.getStyleClass().addAll("button", "detail-pane__button");
		newGame.setOnAction(ev -> {
			isNewGame = true;
			newGameMapSelection(stage);
		});
		Button loadGame = new Button("Load Game");
		loadGame.getStyleClass().addAll("button", "detail-pane__button");
		loadGame.setOnAction(ev -> {
			isNewGame = false;
			attemptLoadGame(stage);
		});
		Button exit = new Button("Exit");
		exit.getStyleClass().addAll("button", "detail-pane__button");
		exit.setOnAction(ev -> {
			Platform.exit();
			System.exit(0);
		});
		BackgroundImage myBI = new BackgroundImage(new Image("file:./src/views/background.jpg",32,32,false,true),
		        BackgroundRepeat.REPEAT, BackgroundRepeat.REPEAT, BackgroundPosition.DEFAULT,
		          BackgroundSize.DEFAULT);
		MenuOptions.setBackground(new Background(myBI));

		Window.setBackground(new Background(myBI));
		MenuOptions.setSpacing(20);
		MenuOptions.getChildren().addAll(title, newGame, loadGame, exit);
		garbageLeft.setMinWidth(441);
		garbageTop.setMinHeight(150);
		Window.setCenter(MenuOptions);
		Window.setLeft(garbageLeft);
		Window.setTop(garbageTop);
		Window.setAlignment(MenuOptions, Pos.CENTER);

		stage.setScene(scene);
		stage.show();
	}
	/**
	 * newGameMapSelection builds the menu for map selection when the user selects
	 * 	New Game from the Main Menu. This includes .png previews of our Maps, a button for
	 * 	each map and a text element informing the User how many players each Map supports.
	 * 	It also includes a "return to menu" button that will redraw our Main Menu.
	 * @param stage our primary stage for our javafx environment
	 */
	private void newGameMapSelection(Stage stage) {
		BorderPane Window = new BorderPane();
		Scene scene = new Scene(Window, WINDOW_WIDTH, WINDOW_HEIGHT);
		scene.getStylesheets().add("assets/CivView.css");
		stage.setScene(scene);
		stage.setTitle("Sid Meier's Civilization 0.5");
		HBox mapSelection = new HBox();
		mapSelection.setPadding(new Insets(40, 40, 40, 40));
		mapSelection.setSpacing(40);
		VBox col1 = new VBox();
		VBox garbageLeft = new VBox();
		VBox garbageTop = new VBox();
		Window.setLeft(garbageLeft);
		Window.setTop(garbageTop);
		garbageLeft.setMinWidth(190);
		garbageTop.setMinHeight(160);
		Button map1 = new Button("Map 1");
		Text label1 = new Text("  2-4 Players");
		Image image1 = new Image("file:./src/views/Map1.PNG", 110, 110, false, true);
		Canvas canvas1 = new Canvas(110, 110);
		GraphicsContext context1 = canvas1.getGraphicsContext2D();
		context1.drawImage(image1, 0, 0);
		map1.getStyleClass().addAll("button", "detail-pane__button");
		map1.setOnAction(ev -> {
			mapNum = 1;
			mapSize = 0;
			queryPlayerCount4(stage);
		});
		col1.getChildren().addAll(canvas1, map1, label1);
		VBox col2 = new VBox();
		Button map2 = new Button("Map 2");
		Text label2 = new Text("  2-3 Players");
		Image image2 = new Image("file:./src/views/Map2.PNG", 110, 110, false, true);
		Canvas canvas2 = new Canvas(110, 110);
		GraphicsContext context2 = canvas2.getGraphicsContext2D();
		context2.drawImage(image2, 0, 0);
		map2.getStyleClass().addAll("button", "detail-pane__button");
		map2.setOnAction(ev -> {
			mapNum = 2;
			mapSize = 0;
			queryPlayerCount3(stage);
		});
		col2.getChildren().addAll(canvas2, map2, label2);
		VBox col3 = new VBox();
		Button map3 = new Button("Map 3");
		Text label3 = new Text("   2 Players ");
		Image image3 = new Image("file:./src/views/Map3.PNG", 110, 110, false, true);
		Canvas canvas3 = new Canvas(110,110);
		GraphicsContext context3 = canvas3.getGraphicsContext2D();
		context3.drawImage(image3, 0, 0);
		map3.getStyleClass().addAll("button", "detail-pane__button");
		map3.setOnAction(ev -> {
			mapNum = 3;
			mapSize = 0;
			queryPlayerCount2(stage);
		});
		col3.getChildren().addAll(canvas3, map3, label3);
		VBox col4 = new VBox();
		Button map4 = new Button("Map 4");
		Text label4 = new Text("  2-4 Players");
		Image image4 = new Image("file:./src/views/Map4.PNG", 110, 110, false, true);
		Canvas canvas4 = new Canvas(110,110);
		GraphicsContext context4 = canvas4.getGraphicsContext2D();
		context4.drawImage(image4, 0, 0);
		map4.getStyleClass().addAll("button", "detail-pane__button");
		map4.setOnAction(ev -> {
			mapNum = 4;
			queryMapSize(stage);
		});
		col4.getChildren().addAll(canvas4, map4, label4);
		col1.setSpacing(10);
		col2.setSpacing(10);
		col3.setSpacing(10);
		col4.setSpacing(10);
		mapSelection.getChildren().addAll(col1, col2, col3, col4);
		BackgroundImage myBI = new BackgroundImage(new Image("file:./src/views/background.jpg",32,32,false,true),
		        BackgroundRepeat.REPEAT, BackgroundRepeat.REPEAT, BackgroundPosition.DEFAULT,
		          BackgroundSize.DEFAULT);
		Window.setBackground(new Background(myBI));
		Window.setCenter(mapSelection);
		Button mainMenu = new Button("Return to Menu");
		mainMenu.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent e) {
				buildMenu(stage);
			}
		});
		Window.setAlignment(mainMenu, Pos.CENTER);
		Window.setMargin(mainMenu, new Insets(25,25,25,25));
		Window.setBottom(mainMenu);
		stage.setScene(scene);
		stage.show();
	}
	/**
	 * attemptLoadGame() is called when the User clicks the "Load Game" button on
	 * 	our Main Menu. If it cannot load a game, it displays an Alert indicating
	 * 	that no saved game was found and allows the user to select a different option.
	 * 	If it can load  a game, it loads the game and starts it.
	 * @param stage our primary stage for our javafx environment
	 */
	private void attemptLoadGame(Stage stage) {
		try {
			this.model = new CivModel();
			startGame(stage);
		} catch (NullPointerException e) {
			Alert noGame = new Alert(Alert.AlertType.INFORMATION);
			noGame.setContentText("No saved game state was found.");
			noGame.showAndWait();
		}
	}
	/**
	 * queryMapSize draws a Menu screen with four menu options. Map4 is variably sized and
	 * 	can be one of the four options that the user wants: 20x20, 30x30, 40x40, 50x50.
	 * 	This Menu includes buttons for all four of these options and, upon clicking, sets
	 * 	the View's mapSize field. This menu also includes a "Return to Menu" button that
	 * 	redraws our Main Menu when clicked.
	 * @param stage our primary stage for our javafx environment
	 */
	private void queryMapSize(Stage stage) {
		BorderPane Window = new BorderPane();
		Scene scene = new Scene(Window, WINDOW_WIDTH, WINDOW_HEIGHT);
		scene.getStylesheets().add("assets/CivView.css");
		stage.setScene(scene);
		stage.setTitle("Sid Meier's Civilization 0.5");
		HBox playerCountSelection = new HBox();
		playerCountSelection.setPadding(new Insets(20, 20, 20, 20));
		playerCountSelection.setSpacing(20);
		// VBox col1 = new VBox();
		VBox garbageLeft = new VBox();
		VBox garbageTop = new VBox();
		garbageLeft.setMinWidth(205);
		garbageTop.setMinHeight(250);
		Window.setLeft(garbageLeft);
		Window.setTop(garbageTop);
		Button button1 = new Button("Map: 20 x 20");
		//button1.getStyleClass().addAll("button", "detail-pane__button");
		button1.setOnAction(ev -> {
			mapSize = 20;
			queryPlayerCount4(stage);
		});
		Button button2 = new Button("Map: 30 x 30");
		//button2.getStyleClass().addAll("button", "detail-pane__button");
		button2.setOnAction(ev -> {
			mapSize = 30;
			queryPlayerCount4(stage);
		});
		Button button3 = new Button("Map: 40 x 40");
		//button3.getStyleClass().addAll("button", "detail-pane__button");
		button3.setOnAction(ev -> {
			mapSize = 40;
			queryPlayerCount4(stage);
		});
		Button button4 = new Button("Map: 50 x 50");
		//button4.getStyleClass().addAll("button", "detail-pane__button");
		button4.setOnAction(ev -> {
			mapSize = 50;
			queryPlayerCount4(stage);
		});
		playerCountSelection.getChildren().addAll(button1, button2, button3, button4);
		BackgroundImage myBI = new BackgroundImage(new Image("file:./src/views/background.jpg",32,32,false,true),
		        BackgroundRepeat.REPEAT, BackgroundRepeat.REPEAT, BackgroundPosition.DEFAULT,
		          BackgroundSize.DEFAULT);
		Window.setBackground(new Background(myBI));
		Window.setCenter(playerCountSelection);
		Button mainMenu = new Button("Return to Menu");
		mainMenu.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent e) {
				buildMenu(stage);
			}
		});
		Window.setAlignment(mainMenu, Pos.CENTER);
		Window.setMargin(mainMenu, new Insets(25,25,25,25));
		Window.setBottom(mainMenu);
		stage.setScene(scene);
		stage.show();
	}
	/**
	 * queryPlayerCount4() builds a menu for the User to select how many players
	 * 	they'd like in their game. Map1 and Map4 support 1-4 human players, so this
	 * 	menu contains four options and will set the view's numPlayers field when clicked.
	 * 	Also contains our Return to Menu Button
	 * @param stage our primary stage for our javafx environment
	 */
	private void queryPlayerCount4(Stage stage) {
		BorderPane Window = new BorderPane();
		Scene scene = new Scene(Window, WINDOW_WIDTH, WINDOW_HEIGHT);
		scene.getStylesheets().add("assets/CivView.css");
		stage.setScene(scene);
		stage.setTitle("Sid Meier's Civilization 0.5");
		HBox playerCountSelection = new HBox();
		playerCountSelection.setPadding(new Insets(20, 20, 20, 20));
		playerCountSelection.setSpacing(20);
		// VBox col1 = new VBox();
		VBox garbageLeft = new VBox();
		VBox garbageTop = new VBox();
		garbageLeft.setMinWidth(130);
		garbageTop.setMinHeight(250);
		Window.setLeft(garbageLeft);
		Window.setTop(garbageTop);
		Button button1 = new Button("Player v. CPU Player");
		//button1.getStyleClass().addAll("button", "detail-pane__button");
		button1.setOnAction(ev -> {
			numPlayers = 1;
			model = new CivModel(numPlayers, mapNum, mapSize);
			startGame(stage);
		});
		Button button2 = new Button("Player v. Player (2)");
		//button2.getStyleClass().addAll("button", "detail-pane__button");
		button2.setOnAction(ev -> {
			numPlayers = 2;
			model = new CivModel(numPlayers, mapNum, mapSize);
			startGame(stage);
		});
		Button button3 = new Button("Player v. Player (3)");
		//button3.getStyleClass().addAll("button", "detail-pane__button");
		button3.setOnAction(ev -> {
			numPlayers = 3;
			model = new CivModel(numPlayers, mapNum, mapSize);
			startGame(stage);
		});
		Button button4 = new Button("Player v. Player (4)");
		//button4.getStyleClass().addAll("button", "detail-pane__button");
		button4.setOnAction(ev -> {
			numPlayers = 4;
			model = new CivModel(numPlayers, mapNum, mapSize);
			startGame(stage);
		});
		playerCountSelection.getChildren().addAll(button1, button2, button3, button4);
		BackgroundImage myBI = new BackgroundImage(new Image("file:./src/views/background.jpg",32,32,false,true),
		        BackgroundRepeat.REPEAT, BackgroundRepeat.REPEAT, BackgroundPosition.DEFAULT,
		          BackgroundSize.DEFAULT);
		Window.setBackground(new Background(myBI));
		Window.setCenter(playerCountSelection);
		Button mainMenu = new Button("Return to Menu");
		mainMenu.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent e) {
				buildMenu(stage);
			}
		});
		Window.setAlignment(mainMenu, Pos.CENTER);
		Window.setMargin(mainMenu, new Insets(25,25,25,25));
		Window.setBottom(mainMenu);
		stage.setScene(scene);
		stage.show();
	}
	/**
	 * queryPlayerCount3() builds a menu for the User to select how many players
	 * 	they'd like in their game. Map2 supports 1-3 human players, so this
	 * 	menu contains three options and will set the view's numPlayers field when clicked.
	 *  Also contains our Return to Menu Button
	 * @param stage our primary stage for our javafx environment
	 */
	private void queryPlayerCount3(Stage stage) {
		BorderPane Window = new BorderPane();
		Scene scene = new Scene(Window, WINDOW_WIDTH, WINDOW_HEIGHT);
		scene.getStylesheets().add("assets/CivView.css");
		stage.setScene(scene);
		stage.setTitle("Sid Meier's Civilization 0.5");
		HBox playerCountSelection = new HBox();
		playerCountSelection.setPadding(new Insets(20, 20, 20, 20));
		playerCountSelection.setSpacing(20);
		// VBox col1 = new VBox();
		VBox garbageLeft = new VBox();
		VBox garbageTop = new VBox();
		garbageLeft.setMinWidth(212);
		garbageTop.setMinHeight(250);
		Window.setLeft(garbageLeft);
		Window.setTop(garbageTop);
		Button button1 = new Button("Player v. CPU Player");
		//button1.getStyleClass().addAll("button", "detail-pane__button");
		button1.setOnAction(ev -> {
			numPlayers = 1;
			model = new CivModel(numPlayers, mapNum, mapSize);
			startGame(stage);
		});
		Button button2 = new Button("Player v. Player (2)");
		//button2.getStyleClass().addAll("button", "detail-pane__button");
		button2.setOnAction(ev -> {
			numPlayers = 2;
			model = new CivModel(numPlayers, mapNum, mapSize);
			startGame(stage);
		});
		Button button3 = new Button("Player v. Player (3)");
		//button3.getStyleClass().addAll("button", "detail-pane__button");
		button3.setOnAction(ev -> {
			numPlayers = 3;
			model = new CivModel(numPlayers, mapNum, mapSize);
			startGame(stage);
		});
		playerCountSelection.getChildren().addAll(button1, button2, button3);
		BackgroundImage myBI = new BackgroundImage(new Image("file:./src/views/background.jpg",32,32,false,true),
		        BackgroundRepeat.REPEAT, BackgroundRepeat.REPEAT, BackgroundPosition.DEFAULT,
		          BackgroundSize.DEFAULT);
		Window.setBackground(new Background(myBI));
		Window.setCenter(playerCountSelection);
		Button mainMenu = new Button("Return to Menu");
		mainMenu.setOnAction(ev -> buildMenu(stage));
		Window.setAlignment(mainMenu, Pos.CENTER);
		Window.setMargin(mainMenu, new Insets(25,25,25,25));
		Window.setBottom(mainMenu);
		stage.setScene(scene);
		stage.show();
	}
	/**
	 * queryPlayerCount2() builds a menu for the User to select how many players
	 * 	they'd like in their game. Map3 supports 1-2 human players, so this
	 * 	menu contains two options and will set the view's numPlayers field when clicked.
	 *  Also contains our Return to Menu Button
	 * @param stage our primary stage for our javafx environment
	 */
	private void queryPlayerCount2(Stage stage) {
		BorderPane Window = new BorderPane();
		Scene scene = new Scene(Window, WINDOW_WIDTH, WINDOW_HEIGHT);
		scene.getStylesheets().add("assets/CivView.css");
		stage.setScene(scene);
		stage.setTitle("Sid Meier's Civilization 0.5");
		HBox playerCountSelection = new HBox();
		playerCountSelection.setPadding(new Insets(20, 20, 20, 20));
		playerCountSelection.setSpacing(20);
		// VBox col1 = new VBox();
		VBox garbageLeft = new VBox();
		VBox garbageTop = new VBox();
		garbageLeft.setMinWidth(300);
		garbageTop.setMinHeight(250);
		Window.setLeft(garbageLeft);
		Window.setTop(garbageTop);
		Button button1 = new Button("Player v. CPU Player");
		//button1.getStyleClass().addAll("button", "detail-pane__button");
		button1.setOnAction(ev -> {
			numPlayers = 1;
			model = new CivModel(numPlayers, mapNum, mapSize);
			startGame(stage);
		});
		Button button2 = new Button("Player v. Player (2)");
		//button2.getStyleClass().addAll("button", "detail-pane__button");
		button2.setOnAction(ev -> {
			numPlayers = 2;
			model = new CivModel(numPlayers, mapNum, mapSize);
			startGame(stage);
		});
		playerCountSelection.getChildren().addAll(button1, button2);
		BackgroundImage myBI = new BackgroundImage(new Image("file:./src/views/background.jpg",32,32,false,true),
		        BackgroundRepeat.REPEAT, BackgroundRepeat.REPEAT, BackgroundPosition.DEFAULT,
		          BackgroundSize.DEFAULT);
		Window.setBackground(new Background(myBI));
		Window.setCenter(playerCountSelection);
		Button mainMenu = new Button("Return to Menu");
		mainMenu.setOnAction(ev -> buildMenu(stage));
		Window.setAlignment(mainMenu, Pos.CENTER);
		Window.setMargin(mainMenu, new Insets(25,25,25,25));
		Window.setBottom(mainMenu);
		stage.setScene(scene);
		stage.show();
	}
}