package org.uni.service;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;

public class DungeonGenerator {
    private final int size;
    private final Random random = new Random();
    public DungeonGenerator(int size) {
        this.size = size;
    }

    private static class Point  {
        int x, y;
        Point(int x, int y){
            this.x = x;
            this.y = y;
        }
    }

    public static class Tile {
        public String type; 
        public String monsterName;
        public String icon;

        public Tile(String type, String monsterName, String icon) {
            this.type = type;
            this.monsterName = monsterName;
            this.icon = icon;
        }
    }



    public Tile[][] generateLevel(int dungeonLevel, List<String> availableMonstersFromOntology) {
        Tile[][] grid;
        boolean isValid = false;
        int attempts = 0;

        do {
            grid = createRawLevel(dungeonLevel, availableMonstersFromOntology);

            int exitX = -1, exitY = -1;
            for (int x = 0; x < size; x++) {
                for (int y = 0; y < size; y++) {
                    if (grid[x][y].type.equals("EXIT")) {
                        exitX = x;
                        exitY = y;
                        break;
                    }
                }
            }

            if (exitX != -1 && exitY != -1) {
                isValid = isPathValid(grid, 0, 0, exitX, exitY);
            }

            attempts++;
        } while (!isValid && attempts < 100);

        return grid;
    }

    private Tile[][] createRawLevel(int dungeonLevel, List<String> availableMonstersFromOntology) {
        Tile[][] grid = new Tile[size][size];

        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                grid[x][y] = new Tile("EMPTY", null, "🟩");
            }
        }

        int wallCount = (int) (size * size * 0.15);
        for (int i = 0; i < wallCount; i++) {
            int wx = random.nextInt(size);
            int wy = random.nextInt(size);

            if (wx != 0 || wy != 0) {
                grid[wx][wy] = new Tile("WALL", null, "⬛");
            }
        }

        int monsterCount = Math.min(3 + dungeonLevel, 10);
        for (int i = 0; i < monsterCount; i++) {
            int mx = random.nextInt(size);
            int my = random.nextInt(size);

            if ((mx != 0 || my != 0) && grid[mx][my].type.equals("EMPTY")) {
                String randomMonster = availableMonstersFromOntology.isEmpty()
                        ? "Orc"
                        : availableMonstersFromOntology.get(random.nextInt(availableMonstersFromOntology.size()));

                String icon = getMonsterIcon(randomMonster);
                grid[mx][my] = new Tile("MONSTER", randomMonster, icon);
            }
        }

        for (int i = 0; i < 2; i++) {
            int px = random.nextInt(size);
            int py = random.nextInt(size);
            if ((px != 0 || py != 0) && grid[px][py].type.equals("EMPTY")) {
                grid[px][py] = new Tile("POTION", null, "🧪");
            }
        }

        boolean exitPlaced = false;
        while (!exitPlaced) {
            int ex = random.nextInt(size);
            int ey = random.nextInt(size);

            if (ex + ey > 4 && grid[ex][ey].type.equals("EMPTY")) {
                grid[ex][ey] = new Tile("EXIT", null, "🔒");
                exitPlaced = true;
            }
        }

        return grid;
    }

    private boolean isPathValid(Tile[][] grid, int startX, int startY, int targetX, int targetY) {
        boolean[][] visited = new boolean[size][size];
        Queue<Point> queue = new LinkedList<>();

        queue.add(new Point(startX, startY));
        visited[startX][startY] = true;

        int[] dx = {-1, 1, 0, 0};
        int[] dy = {0, 0, -1, 1};

        while (!queue.isEmpty()) {
            Point current = queue.poll();

            if (current.x == targetX && current.y == targetY) {
                return true;
            }

            for (int i = 0; i < 4; i++) {
                int newX = current.x + dx[i];
                int newY = current.y + dy[i];
                if (newX >= 0 && newX < size && newY >= 0 && newY < size) {

                    if (!grid[newX][newY].type.equals("WALL") && !visited[newX][newY]) {
                        visited[newX][newY] = true;
                        queue.add(new Point(newX, newY));
                    }
                }
            }
        }

        return false;
    }
    private String getMonsterIcon(String monsterName) {
        if (monsterName.contains("Dragon")) return "🐲";
        if (monsterName.contains("Goblin")) return "👺";
        if (monsterName.contains("Demon") || monsterName.contains("Devil")) return "😈";
        return "👹";
    }
}
