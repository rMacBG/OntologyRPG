package org.uni.GUI;

import org.uni.service.CombatService;
import org.uni.service.DungeonGenerator;

import java.awt.Point;
import java.util.Random;

public class MonsterAI {
    private final CombatService cs;
    private final int size;

    // Интерфейс (Callback), с който AI-то уведомява GameUI, че чудовище е нападнало играча
    public interface AttackCallback {
        void onMonsterAttack(int monsterX, int monsterY, String monsterName);
    }

    public MonsterAI(CombatService cs, int size) {
        this.cs = cs;
        this.size = size;
    }

    public void wanderMonstersInRoom(DungeonGenerator.Tile[][] mapData, int playerX, int playerY, int activeMonstersCount, AttackCallback callback) {
        if (mapData == null || activeMonstersCount <= 0) return;

        boolean[][] movedThisTurn = new boolean[size][size];

        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                if ("MONSTER".equals(mapData[x][y].type) && !movedThisTurn[x][y]) {
                    String monsterName = mapData[x][y].monsterName;
                    if (monsterName == null) monsterName = "Goblin";

                    String behavior = cs.getBehavior(monsterName);
                    Point nextPos = calculateMonsterNextStep(behavior, x, y, playerX, playerY);

                    int nx = nextPos.x;
                    int ny = nextPos.y;

                    if (nx >= 0 && nx < size && ny >= 0 && ny < size && (nx != x || ny != y)) {
                        // Сблъсък с играча
                        if (nx == playerX && ny == playerY) {
                            if (callback != null) {
                                callback.onMonsterAttack(x, y, monsterName);
                            }
                            return;
                        }

                        // Празна клетка
                        if ("EMPTY".equals(mapData[nx][ny].type)) {
                            mapData[nx][ny] = mapData[x][y];
                            mapData[x][y] = new DungeonGenerator.Tile("EMPTY", null, "🟩");
                            movedThisTurn[nx][ny] = true;
                        }
                    }
                }
            }
        }
    }

    private Point calculateMonsterNextStep(String behavior, int mx, int my, int px, int py) {
        int dist = Math.abs(mx - px) + Math.abs(my - py);

        switch (behavior) {
            case "AggressiveBehavior":
            case "AggressiveBeh":
                return getStepTowards(mx, my, px, py);

            case "DefensiveBehavior":
            case "DefensiveBeh":
                if (dist <= 3) return getStepTowards(mx, my, px, py);
                return getStepRandom(mx, my);

            case "PassiveBehavior":
            case "PassiveBeh":
                if (dist <= 3) return getStepAway(mx, my, px, py);
                return new Point(mx, my);

            case "NeutralBehavior":
            case "NeutralBeh":
            default:
                if (new Random().nextBoolean()) return getStepRandom(mx, my);
                return new Point(mx, my);
        }
    }

    private Point getStepTowards(int mx, int my, int px, int py) {
        int dx = Integer.compare(px, mx);
        int dy = Integer.compare(py, my);

        if (Math.abs(px - mx) >= Math.abs(py - my) && dx != 0) return new Point(mx + dx, my);
        if (dy != 0) return new Point(mx, my + dy);

        return new Point(mx, my);
    }

    private Point getStepAway(int mx, int my, int px, int py) {
        int dx = Integer.compare(mx, px);
        int dy = Integer.compare(my, py);

        if (dx != 0) return new Point(mx + dx, my);
        if (dy != 0) return new Point(mx, my + dy);

        return new Point(mx, my);
    }

    private Point getStepRandom(int mx, int my) {
        int[][] dirs = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}};
        int[] d = dirs[new Random().nextInt(4)];
        return new Point(mx + d[0], my + d[1]);
    }
}
