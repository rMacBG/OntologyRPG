package org.uni.service;

import java.util.List;
import java.util.Random;

public class DungeonGenerator {
    private final int size;
    private final Random random = new Random();

    public DungeonGenerator(int size) {
        this.size = size;
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
        Tile[][] grid = new Tile[size][size];

        // 1. Запълваме всичко с празни клетки
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
    private String getMonsterIcon(String monsterName) {
        if (monsterName.contains("Dragon")) return "🐲";
        if (monsterName.contains("Goblin")) return "👺";
        if (monsterName.contains("Demon") || monsterName.contains("Devil")) return "😈";
        return "👹";
    }
}
