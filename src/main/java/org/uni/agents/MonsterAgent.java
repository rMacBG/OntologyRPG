package org.uni.agents;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import org.uni.service.CombatService;
import org.uni.service.OntologyService;

import java.awt.Point;
import java.util.Random;

public class MonsterAgent extends Agent {

    private CombatService combatOntology;
    private final Random random = new Random();

    @Override
    protected void setup() {
        this.combatOntology = new CombatService();
        System.out.println("Monster Agent Started!");

        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                ACLMessage message = receive();

                if (message == null) {
                    block();
                    return;
                }

                String content = message.getContent();
                System.out.println("MonsterAgent received: " + content);
                String[] parts = content.split(":");

                // Формат: MOVE_REQUEST : enemyName : monsterX : monsterY : playerX : playerY
                if (parts[0].equals("MOVE_REQUEST")) {
                    processMovementDecision(message, parts);
                }
            }
        });
    }

    private void processMovementDecision(ACLMessage originalMsg, String[] parts) {
        String enemyName = parts[1].trim();
        int monsterX = Integer.parseInt(parts[2].trim());
        int monsterY = Integer.parseInt(parts[3].trim());
        int playerX = Integer.parseInt(parts[4].trim());
        int playerY = Integer.parseInt(parts[5].trim());

        // 1. Извличане на класа за поведение от онтологията (RDF/OWL)
        String behaviorClass = combatOntology.getBehavior(enemyName);
        if (behaviorClass == null) {
            behaviorClass = "NeutralBehavior"; // Fallback по подразбиране
        }

        // 2. Вземане на решение за ход спрямо онтологичния клас
        Point nextPosition = calculateNextMove(behaviorClass, monsterX, monsterY, playerX, playerY);

        // 3. Изпращане на отговор с новите координати
        String responseContent = "MOVE_RESPONSE:" + enemyName + ":" + nextPosition.x + ":" + nextPosition.y;
        sendReply(originalMsg, responseContent);
    }

    private Point calculateNextMove(String behaviorClass, int mx, int my, int px, int py) {
        int distanceToPlayer = Math.abs(mx - px) + Math.abs(my - py);

        // Нормализиране на името на класа (ако съдържа пълното URI от онтологията)
        String behavior = behaviorClass.contains("#")
                ? behaviorClass.substring(behaviorClass.indexOf("#") + 1)
                : behaviorClass;

        switch (behavior) {
            case "AggressiveBehavior":
                // Преследва играча без значение от разстоянието
                return stepTowards(mx, my, px, py);

            case "DefensiveBehavior":
                // Пази зоната: гони играча САМО ако е наблизо (<= 3 плочки)
                if (distanceToPlayer <= 3) {
                    return stepTowards(mx, my, px, py);
                }
                return stepRandomly(mx, my);

            case "PassiveBehavior":
                // Бяга от играча, ако той се приближи (<= 3 плочки)
                if (distanceToPlayer <= 3) {
                    return stepAwayFrom(mx, my, px, py);
                }
                return new Point(mx, my); // Остава на място

            case "NeutralBehavior":
            default:
                // Блуждае свободно без значение къде е играча (50% шанс за стъпка)
                if (random.nextBoolean()) {
                    return stepRandomly(mx, my);
                }
                return new Point(mx, my);
        }
    }

    // --- Логика за движенията ---

    private Point stepTowards(int mx, int my, int px, int py) {
        int dx = Integer.compare(px, mx);
        int dy = Integer.compare(py, my);

        if (Math.abs(px - mx) >= Math.abs(py - my)) {
            if (dx != 0) return new Point(mx + dx, my);
        }
        if (dy != 0) return new Point(mx, my + dy);

        return new Point(mx, my);
    }

    private Point stepAwayFrom(int mx, int my, int px, int py) {
        int dx = Integer.compare(mx, px);
        int dy = Integer.compare(my, py);

        if (dx != 0) return new Point(mx + dx, my);
        if (dy != 0) return new Point(mx, my + dy);

        return new Point(mx, my);
    }

    private Point stepRandomly(int mx, int my) {
        int[][] dirs = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}};
        int[] dir = dirs[random.nextInt(4)];
        return new Point(mx + dir[0], my + dir[1]);
    }

    private void sendReply(ACLMessage message, String content) {
        ACLMessage reply = message.createReply();
        reply.setPerformative(ACLMessage.INFORM);
        reply.setContent(content);
        send(reply);
    }
}
