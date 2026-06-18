package org.uni.GUI;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.uni.agents.GUIAgent;


public class GameUI extends Application {
    private int playerX = 0;
    private int playerY = 0;
    private final int SIZE = 10;

    public static GameUI instance;

    private Button[][] tiles = new Button[SIZE][SIZE];
    private Label info = new Label();
    private String[][] map = new String[SIZE][SIZE];
    @Override
    public void start(Stage stage) throws Exception {
        instance = this;
        GridPane grid = new GridPane();

        for (int x = 0; x < SIZE; x++){
            for (int y = 0; y < SIZE; y++){
                Button tile = new Button();

                tile.setPrefSize(50, 50);

                tiles[x][y] = tile;

                grid.add(tile,
                        x,
                        y);
            }
        }
        spawnMonsters();
        updateMap();
        tiles[0][0].setText("P");
        VBox root = new VBox();
        root.getChildren().addAll(grid, info);

        Scene scene = new Scene(root);
        scene.setOnKeyPressed(event -> {
            switch (event.getCode()){
                case W ->
                    movePlayer(playerX, playerY -1);
                case S ->
                    movePlayer(playerX, playerY +1);
                case A ->
                    movePlayer(playerX - 1, playerY);
                case D ->
                    movePlayer(playerX + 1, playerY);
            }
        });
        stage.setScene(scene);

        stage.setTitle("RPG Game");
        stage.show();
    }

    private void movePlayer(int newX, int newY){
        if(newX < 0 || newY < 0 || newX >= SIZE || newY >= SIZE){
            return;
        }


        if(map[newX][newY] != null){
            String enemy = map[newX][newY];
            System.out.println("Encounter " + enemy);
            GUIAgent.instance.sendMessage("FIGHT:" + enemy);
            return;
        }

        playerX = newX;
        playerY = newY;

        updateMap();
    }
    private void spawnMonsters(){
        map[3][4] = "Dragon";
        map[6][8] = "Goblin";
        map[4][1] = "Orc";

    }

    private void updateMap(){
        for (int x =0; x < SIZE; x++){
            for (int y = 0; y < SIZE; y++){
                tiles[x][y].setText("");
                if(map[x][y] != null){
                    if(map[x][y].equals("Dragon")){
                        tiles[x][y].setText("D");
                    }
                    else if(map[x][y].equals("Goblin")){
                        tiles[x][y].setText("G");
                    }
                    else if(map[x][y].equals("Orc")){
                        tiles[x][y].setText("O");
                    }
                }
            }
        }
        tiles[playerX][playerY].setText("P");
    }

    public void showMessage(String message){
        info.setText(message);
    }

    public static void main(String[] args) {
        launch();
    }
}
