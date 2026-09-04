package org.uni.service;

import org.uni.model.Room;
import org.uni.model.Room.RoomType;
import org.uni.model.Room.Direction;

import java.util.*;

public class DungeonGenerator {

    private final int roomSize;
    private final Random random = new Random();

    public DungeonGenerator(int roomSize) {
        this.roomSize = roomSize;
    }

    public static class Tile {
        public String type;
        public String monsterName;
        public String icon;
        public Direction doorDirection;

        public Tile(String type, String monsterName, String icon) {
            this.type = type;
            this.monsterName = monsterName;
            this.icon = icon;
        }
    }

    private int calculateRoomCount(int dungeonLevel) {
        int minRooms;
        int maxRooms;

        switch (dungeonLevel) {
            case 1 -> { minRooms = 3; maxRooms = 4; }
            case 2 -> { minRooms = 4; maxRooms = 5; }
            case 3 -> { minRooms = 4; maxRooms = 6; }
            case 4 -> { minRooms = 5; maxRooms = 6; }
            case 5 -> { minRooms = 6; maxRooms = 7; }
            case 6 -> { minRooms = 7; maxRooms = 8; }
            case 7 -> { minRooms = 8; maxRooms = 9; }
            case 8 -> { minRooms = 9; maxRooms = 10; }
            default -> {
                minRooms = 9 + (dungeonLevel - 8);
                maxRooms = minRooms + 2;
            }
        }

        return minRooms + random.nextInt(maxRooms - minRooms + 1);
    }

    public List<Room> generateFloor(int dungeonLevel, List<String> availableMonsters, List<String> availableBosses) {
        int totalRooms = calculateRoomCount(dungeonLevel);
        List<Room> floorRooms = new ArrayList<>();

        Room startRoom = new Room(1, RoomType.START, roomSize);
        startRoom.setGridX(0);
        startRoom.setGridY(0);
        floorRooms.add(startRoom);

        for (int i = 2; i < totalRooms; i++) {
            floorRooms.add(new Room(i, RoomType.NORMAL, roomSize));
        }

        Room bossRoom = new Room(totalRooms, RoomType.BOSS, roomSize);
        floorRooms.add(bossRoom);

        for (int i = 0; i < floorRooms.size() - 1; i++) {
            Room current = floorRooms.get(i);
            Room next = floorRooms.get(i + 1);

            Direction dir = getRandomAvailableDirection(current);
            if (dir == null) {
                dir = Direction.EAST;
            }

            current.connect(dir, next);

            int nextX = current.getGridX();
            int nextY = current.getGridY();

            switch (dir) {
                case NORTH -> nextY--;
                case SOUTH -> nextY++;
                case EAST  -> nextX++;
                case WEST  -> nextX--;
            }

            next.setGridX(nextX);
            next.setGridY(nextY);
        }

        for (Room room : floorRooms) {
            generateRoomGrid(room, dungeonLevel, availableMonsters, availableBosses);
        }

        return floorRooms;
    }

    private void generateRoomGrid(Room room, int level, List<String> monsters, List<String> bosses) {
        Tile[][] grid = room.getGrid();

        for (int x = 0; x < roomSize; x++) {
            for (int y = 0; y < roomSize; y++) {
                grid[x][y] = new Tile("EMPTY", null, "🟩");
            }
        }

        Set<String> doorBufferZone = new HashSet<>();

        for (Map.Entry<Direction, Room> entry : room.getDoors().entrySet()) {
            Direction dir = entry.getKey();
            int dx = roomSize / 2;
            int dy = roomSize / 2;

            switch (dir) {
                case NORTH -> { dx = roomSize / 2; dy = 0; }
                case SOUTH -> { dx = roomSize / 2; dy = roomSize - 1; }
                case WEST  -> { dx = 0; dy = roomSize / 2; }
                case EAST  -> { dx = roomSize - 1; dy = roomSize / 2; }
            }

            Tile doorTile = new Tile("DOOR", null, "🚪");
            doorTile.doorDirection = dir;
            grid[dx][dy] = doorTile;

            addBufferZone(doorBufferZone, dx, dy);
        }

        addBufferZone(doorBufferZone, roomSize / 2, roomSize / 2);

        int wallCount = (int) (roomSize * roomSize * 0.12);
        int placedWalls = 0;
        int attempts = 0;

        while (placedWalls < wallCount && attempts < 100) {
            int wx = random.nextInt(roomSize);
            int wy = random.nextInt(roomSize);
            attempts++;

            if (!doorBufferZone.contains(wx + "," + wy) && grid[wx][wy].type.equals("EMPTY")) {
                grid[wx][wy] = new Tile("WALL", null, "⬛");
                placedWalls++;
            }
        }

        if (room.getType() != RoomType.BOSS) {
            int potionCount = random.nextInt(2) + 1;
            for (int i = 0; i < potionCount; i++) {
                int px = random.nextInt(roomSize);
                int py = random.nextInt(roomSize);
                if (!doorBufferZone.contains(px + "," + py) && grid[px][py].type.equals("EMPTY")) {
                    grid[px][py] = new Tile("POTION", null, "🧪");
                }
            }
        }

        if (room.getType() == RoomType.BOSS) {
            String bossName = (bosses == null || bosses.isEmpty()) ? "DragonBoss" : bosses.get(random.nextInt(bosses.size()));
            grid[roomSize / 2][roomSize / 2] = new Tile("MONSTER", bossName, "🐲");
        } else if (room.getType() == RoomType.NORMAL) {
            int monsterCount = Math.min(2 + level, 5);
            for (int i = 0; i < monsterCount; i++) {
                int mx = random.nextInt(roomSize);
                int my = random.nextInt(roomSize);

                if (!doorBufferZone.contains(mx + "," + my) && grid[mx][my].type.equals("EMPTY")) {
                    String mName = monsters.isEmpty() ? "Orc" : monsters.get(random.nextInt(monsters.size()));
                    grid[mx][my] = new Tile("MONSTER", mName, getMonsterIcon(mName));
                }
            }
        }
    }

    private void addBufferZone(Set<String> buffer, int cx, int cy) {
        for (int x = cx - 1; x <= cx + 1; x++) {
            for (int y = cy - 1; y <= cy + 1; y++) {
                if (x >= 0 && x < roomSize && y >= 0 && y < roomSize) {
                    buffer.add(x + "," + y);
                }
            }
        }
    }

    private Direction getRandomAvailableDirection(Room room) {
        List<Direction> available = new ArrayList<>();
        for (Direction d : Direction.values()) {
            if (!room.getDoors().containsKey(d)) {
                available.add(d);
            }
        }
        if (available.isEmpty()) return null;
        return available.get(random.nextInt(available.size()));
    }

    private String getMonsterIcon(String monsterName) {
        if (monsterName.contains("Dragon")) return "🐲";
        if (monsterName.contains("Goblin")) return "👺";
        if (monsterName.contains("Demon") || monsterName.contains("Devil")) return "😈";
        return "👹";
    }

}
