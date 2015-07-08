package simpletanksmapcreator;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 *
 * @author Naik
 */
public class SimpleTanksMapCreator extends Application {

    @Override
    public void start(Stage primaryStage) {
        Scene scene = null;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/simpletanksmapcreator/main.fxml"));
            primaryStage.setTitle("SimpleTanks Map Generator");
            primaryStage.setScene(new Scene(loader.load()));
        } catch (IOException ex) {
            Logger.getLogger(SimpleTanksMapCreator.class.getName()).log(Level.SEVERE, null, ex);
        }

        primaryStage.show();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }

    public static boolean saveMap(int[][] map, String name, File file) {
        try {
            DataOutputStream dos = new DataOutputStream(new FileOutputStream(file));
            dos.writeUTF(name);
            int w = map.length;
            int h = map[0].length;
            dos.writeInt(w);
            dos.writeInt(h);
            for (int i = 0; i < w; i++) {
                for (int j = 0; j < h; j++) {
                    dos.writeByte(map[i][j]);
                }
            }
            dos.flush();
            dos.close();
            return true;
        } catch (FileNotFoundException ex) {
            Logger.getLogger(SimpleTanksMapCreator.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        } catch (IOException ex) {
            Logger.getLogger(SimpleTanksMapCreator.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
    }

}
