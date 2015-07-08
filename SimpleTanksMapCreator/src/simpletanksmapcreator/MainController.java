package simpletanksmapcreator;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import jfx.messagebox.MessageBox;

/**
 * FXML Controller class
 *
 * @author naik
 */
public class MainController implements Initializable {

    @FXML
    private TextField mapName;
    @FXML
    private TextField width;
    @FXML
    private TextField height;
    @FXML
    private Canvas canvas;
    @FXML
    private Button saveBtn;
    @FXML
    private Button updateBtn;
    @FXML
    private ScrollPane scrollPane;

    private static final int TILE_SIZE = 48;
    private int mapW, mapH;
    private int map[][];
    private GraphicsContext graphics;

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        graphics = canvas.getGraphicsContext2D();
        updateBtn.setOnAction((ActionEvent event) -> {
            try {
                mapW = Integer.parseInt(width.getText());
                mapH = Integer.parseInt(height.getText());
                canvas.setWidth(TILE_SIZE * mapW);
                canvas.setHeight(TILE_SIZE * mapH);
                map = new int[mapW][mapH];
                updateMap();
                scrollPane.setPrefSize(0, 0);
            } catch (NumberFormatException e) {
            }
        });
        canvas.setOnMouseClicked(new EventHandler<MouseEvent>() {

            @Override
            public void handle(MouseEvent event) {
                int x = (int) event.getX() / TILE_SIZE;
                int y = (int) event.getY() / TILE_SIZE;
                if (map[x][y] != 1) {
                    map[x][y] = 1;
                } else {
                    map[x][y] = 0;
                }
                updateMap();
            }
        });
        saveBtn.setOnAction((ActionEvent event) -> {
            if (mapName.getText().trim().isEmpty() || map.length < 10 || map[0].length < 10) {
                return;
            }
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save map");
            Stage stage = (Stage) saveBtn.getScene().getWindow();
            File file = fileChooser.showSaveDialog(stage);
            if (file != null) {
                if (SimpleTanksMapCreator.saveMap(map, mapName.getText(), file)) {
                    MessageBox.show(stage, "Map saved to " + file.getAbsolutePath(), "", MessageBox.ICON_INFORMATION);
                } else {
                    MessageBox.show(stage, "I/O error", "", MessageBox.ICON_ERROR);
                }
                
            }
        });
    }

    private void updateMap() {
        Paint p;
        int x, y;
        for (int i = 0; i < mapW; i++) {
            x = TILE_SIZE * i;
            for (int j = 0; j < mapH; j++) {
                y = TILE_SIZE * j;
                graphics.setFill(switchColor(map[i][j]));
                graphics.fillRect(x, y, TILE_SIZE, TILE_SIZE);
            }
        }
        graphics.setFill(Color.GREY);
        graphics.setLineDashes(5, 5);
        graphics.setLineWidth(1);
        x = mapW * TILE_SIZE;
        for (int j = 0; j < mapH; j++) {
            y = j * TILE_SIZE;
            graphics.strokeLine(0, y, x, y);
        }
        y = mapH * TILE_SIZE;
        for (int i = 0; i < mapW; i++) {
            x = i * TILE_SIZE;
            graphics.strokeLine(x, 0, x, y);
        }
    }

    private Paint switchColor(int tileID) {
        switch (tileID) {
            case 1:
                return Color.SADDLEBROWN;
            default:
                return Color.GAINSBORO;
        }
    }

}
