package org.uni.model;

import org.uni.service.DungeonGenerator;

import java.util.HashMap;
import java.util.Map;

public class Room {

    public enum RoomType{START, NORMAL, BOSS}
    public enum Direction {NORTH, SOUTH, EAST, WEST}

    private final int id;
    private final RoomType type;
    private final int size;
    private final DungeonGenerator.Tile[][] grid;
    private final Map<Direction, Room> doors = new HashMap<>();
    public boolean isCleared = false;

    private int gridX = 0;
    private int gridY = 0;

    public Room(int id, RoomType type, int size) {
        this.id = id;
        this.type = type;
        this.size = size;
        this.grid = new DungeonGenerator.Tile[size][size];
    }



    public int getId() { return id; }
    public RoomType getType() { return type; }
    public DungeonGenerator.Tile[][] getGrid() { return grid; }
    public Map<Direction, Room> getDoors() { return doors; }
    public boolean isCleared() { return isCleared; }
    public void setCleared(boolean cleared) { isCleared = cleared; }

    public int getGridX() { return gridX; }
    public int getGridY() { return gridY; }
    public void setGridX(int gridX) { this.gridX = gridX; }
    public void setGridY(int gridY) { this.gridY = gridY; }

    public void connect(Direction dir, Room otherRoom) {
        doors.put(dir, otherRoom);
        Direction opposite = switch (dir) {
            case NORTH -> Direction.SOUTH;
            case SOUTH -> Direction.NORTH;
            case EAST -> Direction.WEST;
            case WEST -> Direction.EAST;
        };
        otherRoom.getDoors().put(opposite, this);
    }
}
