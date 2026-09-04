package org.uni.GUI;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import org.uni.model.Room;
import org.uni.model.Room.RoomType;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MiniMapWidget extends GridPane {

    private final Set<Room> visitedRooms = new HashSet<>();
    private List<Room> currentFloorRooms;

    public MiniMapWidget() {
        setHgap(4);
        setVgap(4);
        setAlignment(Pos.CENTER);
        setStyle("-fx-background-color: #1a1a1a; -fx-padding: 8; -fx-border-color: #34495e; -fx-border-radius: 5;");
    }

    public void resetForNewFloor(List<Room> rooms) {
        this.currentFloorRooms = rooms;
        this.visitedRooms.clear();
    }

    public void update(Room currentRoom, List<Room> floorRooms) {
        if (floorRooms != null) {
            this.currentFloorRooms = floorRooms;
        }

        if (currentRoom != null) {
            visitedRooms.add(currentRoom);
        }

        if (currentFloorRooms == null || currentFloorRooms.isEmpty()) return;

        Platform.runLater(() -> {
            getChildren().clear();

            int minX = currentFloorRooms.stream().mapToInt(Room::getGridX).min().orElse(0);
            int minY = currentFloorRooms.stream().mapToInt(Room::getGridY).min().orElse(0);

            int offsetX = minX < 0 ? Math.abs(minX) : 0;
            int offsetY = minY < 0 ? Math.abs(minY) : 0;

            for (Room r : currentFloorRooms) {
                Label roomNode = new Label();
                roomNode.setPrefSize(28, 28);
                roomNode.setAlignment(Pos.CENTER);

                if (r.equals(currentRoom)) {
                    roomNode.setText("🧙‍♂️");
                    roomNode.setStyle("-fx-background-color: #f1c40f; -fx-border-color: white; -fx-border-width: 2; -fx-background-radius: 4;");
                } else if (visitedRooms.contains(r)) {
                    if (r.getType() == RoomType.BOSS) {
                        roomNode.setText("👑");
                        roomNode.setStyle("-fx-background-color: #e74c3c; -fx-background-radius: 4;");
                    } else {
                        roomNode.setText("✓");
                        roomNode.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-background-radius: 4;");
                    }
                } else {
                    roomNode.setText("?");
                    roomNode.setStyle("-fx-background-color: #34495e; -fx-text-fill: #7f8c8d; -fx-background-radius: 4;");
                }

                add(roomNode, r.getGridX() + offsetX, r.getGridY() + offsetY);
            }
        });
    }
}
