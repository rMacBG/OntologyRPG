package org.uni.service;

import org.uni.model.Room;
import org.uni.model.Room.RoomType;
import org.uni.model.Room.Direction;

import java.util.*;

public class DungeonGenerator {
//    private final int size;
//    private final Random random = new Random();
//    public DungeonGenerator(int size) {
//        this.size = size;
//    }
//
//    private static class Point  {
//        int x, y;
//        Point(int x, int y){
//            this.x = x;
//            this.y = y;
//        }
//    }
//
//    public static class Tile {
//        public String type;
//        public String monsterName;
//        public String icon;
//
//        public Tile(String type, String monsterName, String icon) {
//            this.type = type;
//            this.monsterName = monsterName;
//            this.icon = icon;
//        }
//    }
//
//
//
//    public Tile[][] generateLevel(int dungeonLevel, List<String> availableMonstersFromOntology) {
//        Tile[][] grid;
//        boolean isValid = false;
//        int attempts = 0;
//
//        do {
//            grid = createRawLevel(dungeonLevel, availableMonstersFromOntology);
//
//            int exitX = -1, exitY = -1;
//            for (int x = 0; x < size; x++) {
//                for (int y = 0; y < size; y++) {
//                    if (grid[x][y].type.equals("EXIT")) {
//                        exitX = x;
//                        exitY = y;
//                        break;
//                    }
//                }
//            }
//
//            if (exitX != -1 && exitY != -1) {
//                isValid = isPathValid(grid, 0, 0, exitX, exitY);
//            }
//
//            attempts++;
//        } while (!isValid && attempts < 100);
//
//        return grid;
//    }
//
//    private Tile[][] createRawLevel(int dungeonLevel, List<String> availableMonstersFromOntology) {
//        Tile[][] grid = new Tile[size][size];
//
//        for (int x = 0; x < size; x++) {
//            for (int y = 0; y < size; y++) {
//                grid[x][y] = new Tile("EMPTY", null, "🟩");
//            }
//        }
//
//        int wallCount = (int) (size * size * 0.15);
//        for (int i = 0; i < wallCount; i++) {
//            int wx = random.nextInt(size);
//            int wy = random.nextInt(size);
//
//            if (wx != 0 || wy != 0) {
//                grid[wx][wy] = new Tile("WALL", null, "⬛");
//            }
//        }
//
//        int monsterCount = Math.min(3 + dungeonLevel, 10);
//        for (int i = 0; i < monsterCount; i++) {
//            int mx = random.nextInt(size);
//            int my = random.nextInt(size);
//
//            if ((mx != 0 || my != 0) && grid[mx][my].type.equals("EMPTY")) {
//                String randomMonster = availableMonstersFromOntology.isEmpty()
//                        ? "Orc"
//                        : availableMonstersFromOntology.get(random.nextInt(availableMonstersFromOntology.size()));
//
//                String icon = getMonsterIcon(randomMonster);
//                grid[mx][my] = new Tile("MONSTER", randomMonster, icon);
//            }
//        }
//
//        for (int i = 0; i < 2; i++) {
//            int px = random.nextInt(size);
//            int py = random.nextInt(size);
//            if ((px != 0 || py != 0) && grid[px][py].type.equals("EMPTY")) {
//                grid[px][py] = new Tile("POTION", null, "🧪");
//            }
//        }
//
//        boolean exitPlaced = false;
//        while (!exitPlaced) {
//            int ex = random.nextInt(size);
//            int ey = random.nextInt(size);
//
//            if (ex + ey > 4 && grid[ex][ey].type.equals("EMPTY")) {
//                grid[ex][ey] = new Tile("EXIT", null, "🔒");
//                exitPlaced = true;
//            }
//        }
//
//        return grid;
//    }
//
//    private boolean isPathValid(Tile[][] grid, int startX, int startY, int targetX, int targetY) {
//        boolean[][] visited = new boolean[size][size];
//        Queue<Point> queue = new LinkedList<>();
//
//        queue.add(new Point(startX, startY));
//        visited[startX][startY] = true;
//
//        int[] dx = {-1, 1, 0, 0};
//        int[] dy = {0, 0, -1, 1};
//
//        while (!queue.isEmpty()) {
//            Point current = queue.poll();
//
//            if (current.x == targetX && current.y == targetY) {
//                return true;
//            }
//
//            for (int i = 0; i < 4; i++) {
//                int newX = current.x + dx[i];
//                int newY = current.y + dy[i];
//                if (newX >= 0 && newX < size && newY >= 0 && newY < size) {
//
//                    if (!grid[newX][newY].type.equals("WALL") && !visited[newX][newY]) {
//                        visited[newX][newY] = true;
//                        queue.add(new Point(newX, newY));
//                    }
//                }
//            }
//        }
//
//        return false;
//    }
//    private String getMonsterIcon(String monsterName) {
//        if (monsterName.contains("Dragon")) return "🐲";
//        if (monsterName.contains("Goblin")) return "👺";
//        if (monsterName.contains("Demon") || monsterName.contains("Devil")) return "😈";
//        return "👹";
//    }

    private final int roomSize;
    private final Random random = new Random();

    public DungeonGenerator(int roomSize) {
        this.roomSize = roomSize;
    }

    public static class Tile {
        public String type; // "EMPTY", "WALL", "MONSTER", "POTION", "DOOR"
        public String monsterName;
        public String icon;
        public Direction doorDirection;

        public Tile(String type, String monsterName, String icon) {
            this.type = type;
            this.monsterName = monsterName;
            this.icon = icon;
        }
    }

    /**
     * Пресмята броя стаи според нивото на етажа (dungeonLevel).
     */
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
                // Скалиране за Endless Mode (Етаж 9+)
                minRooms = 9 + (dungeonLevel - 8);
                maxRooms = minRooms + 2;
            }
        }

        return minRooms + random.nextInt(maxRooms - minRooms + 1);
    }

    /**
     * Генерира пълен етаж с динамичен брой свързани стаи.
     */
    public List<Room> generateFloor(int dungeonLevel, List<String> availableMonsters, List<String> availableBosses) {
        int totalRooms = calculateRoomCount(dungeonLevel);
        List<Room> floorRooms = new ArrayList<>();

        // 1. Създаване на начална стая
        Room startRoom = new Room(1, RoomType.START, roomSize);
        startRoom.setGridX(0); // 👈 Началната стая започва от (0, 0)
        startRoom.setGridY(0);
        floorRooms.add(startRoom);

        // 2. Създаване на обикновени стаи
        for (int i = 2; i < totalRooms; i++) {
            floorRooms.add(new Room(i, RoomType.NORMAL, roomSize));
        }

        // 3. Последната стая е Бос стая
        Room bossRoom = new Room(totalRooms, RoomType.BOSS, roomSize);
        floorRooms.add(bossRoom);

        // 4. Свързване на стаите последователно И изчисляване на техните координати
        for (int i = 0; i < floorRooms.size() - 1; i++) {
            Room current = floorRooms.get(i);
            Room next = floorRooms.get(i + 1);

            Direction dir = getRandomAvailableDirection(current);
            if (dir == null) {
                dir = Direction.EAST;
            }

            current.connect(dir, next);

            // 🔴 ТУК ИЗЧИСЛЯВАМЕ КООРДИНАТИТЕ НА СЛЕДВАЩАТА СТАЯ СПРЯМО ТЕКУЩАТА:
            int nextX = current.getGridX();
            int nextY = current.getGridY();

            switch (dir) {
                case NORTH -> nextY--; // Север = нагоре по Y (-1)
                case SOUTH -> nextY++; // Юг = надолу по Y (+1)
                case EAST  -> nextX++; // Изток = надясно по X (+1)
                case WEST  -> nextX--; // Запад = наляво по X (-1)
            }

            next.setGridX(nextX);
            next.setGridY(nextY);
        }

        // 5. Генериране на мрежата (grid) за всяка стая със защита от софтлок
        for (Room room : floorRooms) {
            generateRoomGrid(room, dungeonLevel, availableMonsters, availableBosses);
        }

        return floorRooms;
    }

    private void generateRoomGrid(Room room, int level, List<String> monsters, List<String> bosses) {
        Tile[][] grid = room.getGrid();

        // 1. Първоначално попълване с празни клетки
        for (int x = 0; x < roomSize; x++) {
            for (int y = 0; y < roomSize; y++) {
                grid[x][y] = new Tile("EMPTY", null, "🟩");
            }
        }

        Set<String> doorBufferZone = new HashSet<>();

        // 2. Поставяне на вратите и маркиране на техните буферни зони
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

            // Добавяне на защитена зона (1 клетка около вратата)
            addBufferZone(doorBufferZone, dx, dy);
        }

        // Защитена зона в центъра на стаята (начална позиция при влизане)
        addBufferZone(doorBufferZone, roomSize / 2, roomSize / 2);

        // 3. Поставяне на Стени (само извън защитените зони)
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

        // 4. Поставяне на Поции (1-2 в обикновените стаи)
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

        // 5. Поставяне на Мобове / Бос
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
