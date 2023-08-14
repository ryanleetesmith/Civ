import javafx.application.Application;
import views.CivView;

/**
 * Main launcher for Civ rpg.
 *
 * <p>
 * This class acts as an entry point to launch the game UI. The current
 * interface is defined in views/CivView and is drawn via JavaFX.
 *
 * @author Connie Sun, Ryan Smith, Luke Hankins, Tim Gavlick
 */
public class Civ {
	public static void main(String[] args) {
		Application.launch(CivView.class, args);
	}
}
