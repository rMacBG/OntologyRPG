package org.uni.service;

import org.uni.model.SkillItem;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class DatabaseService {

    public static final String URL = "jdbc:sqlite:rpg.db";
    private static final Object DB_LOCK = new Object();
    private Connection connection;

    private static DatabaseService instance;

    public static synchronized DatabaseService getInstance() {
        if (instance == null) {
            instance = new DatabaseService();
        }
        return instance;
    }

    public DatabaseService(){
        connect();
        createTables();
        seedData();
    }

    private void connect(){
        try {
            connection = DriverManager.getConnection(URL);

            System.out.println("Connected");
        } catch (Exception e){
            e.printStackTrace();
        }

    }

    private void createTables(){
        String players = """
                CREATE TABLE IF NOT EXISTS players(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                username TEXT UNIQUE,
                level INTEGER,
                gold INTEGER,
                weapon TEXT,
                skill TEXT DEFAULT 'BASIC STRIKE',
                inventory TEXT DEFAULT '',
                hp INTEGER,
                atk INTEGER,
                def INTEGER DEFAULT 0
                );
                """;

        String characters = """
                CREATE TABLE IF NOT EXISTS enemies(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT UNIQUE,
                hp INTEGER,
                atk INTEGER
                );
                """;

        String battles = """
                CREATE TABLE IF NOT EXISTS battles(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                player TEXT,
                enemy TEXT,
                result TEXT
                );
                """;

        try(Statement stmt = connection.createStatement()){
            stmt.execute(players);
            stmt.execute(characters);
            stmt.execute(battles);

            try {
                stmt.executeUpdate("ALTER TABLE players ADD COLUMN def INTEGER DEFAULT 0;");
            } catch (SQLException ignored) {

            }
            try {
                stmt.executeUpdate("ALTER TABLE players ADD COLUMN skill TEXT DEFAULT 'BASIC STRIKE';");
            } catch (SQLException ignored) {

            }

            System.out.println("Tables Created");
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void seedData(){
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("DELETE FROM players;");
            stmt.execute("DELETE FROM enemies;");
        } catch (Exception e) {
            e.printStackTrace();
        }
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT OR IGNORE INTO players (username, level, gold, weapon, hp, atk) VALUES ('Player', 1, 100, 'StormStaff', 100, 20)")) {
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
        addCharacter("DragonBoss",
                250,
                20);

        addCharacter("GoblinMonster",
                50,
                12);

        addCharacter("DemonMonster",
                70,
                45);
        System.out.println("Data Seeded");
    }

    public void addPlayer(String username) {
        String sql = """
                INSERT OR IGNORE INTO players(username, level, gold, hp, atk)
                VALUES(?,?,?,?,?)
                """;
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setInt(2, 1);
            ps.setInt(3, 0);
            ps.setInt(4, 100);
            ps.setInt(5, 5);


            ps.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addCharacter(String name, int hp, int atk){
            String sql = """
                    INSERT OR IGNORE INTO enemies(name, hp, atk) 
                    VALUES(?,?,?)
                    """;

            try(PreparedStatement ps = connection.prepareStatement(sql)){
                    ps.setString(1, name);
                    ps.setInt(2, hp);
                    ps.setInt(3,atk);

                    ps.executeUpdate();
            }catch (Exception e){
                e.printStackTrace();
            }
    }

    public synchronized void addBattle(String player, String enemy, String result){
        String sql = """
                INSERT INTO battles(player, enemy, result)
                VALUES(?,?,?)
                """;

        try(PreparedStatement ps = connection.prepareStatement(sql)){
            ps.setString(1, player);
            ps.setString(2, enemy);
            ps.setString(3, result);

            ps.executeUpdate();
        } catch (Exception e){
            e.printStackTrace();
        }
    }
    public void equipWeapon(String username, String newWeaponName) {
        String query = "UPDATE players SET weapon = ? WHERE username = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, newWeaponName);
            pstmt.setString(2, username);
            pstmt.executeUpdate();
            System.out.println("⚔️ Equipped new weapon: " + newWeaponName + " for " + username);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    public void equipSkill(String username, String skillName) {
        String query = "UPDATE players SET skill = ? WHERE username = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, skillName);
            pstmt.setString(2, username);
            pstmt.executeUpdate();
            System.out.println("✨ Equipped new skill: " + skillName + " for " + username);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public String getPlayer(String username){
        String sql = """
                SELECT * FROM players
                WHERE username = ?
                """;

        try(PreparedStatement ps = connection.prepareStatement(sql)){
            ps.setString(1, username);

            ResultSet rs = ps.executeQuery();

            if (rs.next()){
                return "Player: " + rs.getString("username") + " | Level: " + rs.getInt("level") + " | Gold: " + rs.getInt("gold");

            }
        } catch (Exception e){
            e.printStackTrace();
        }


        return "Player not Found";
    }

public String getPlayerWeapon(String username) {
    String sql = "SELECT weapon FROM players WHERE username=?";
    try (PreparedStatement ps = connection.prepareStatement(sql)) {
        ps.setString(1, username);
        try (ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getString("weapon");
            }
        }
    } catch (Exception e) {
        e.printStackTrace();
    }
    return "no weapon found!";
}

    public void addGold(String username, int amount) {
        synchronized (DB_LOCK) {
            String sql = "UPDATE players SET gold = gold + ? WHERE username = ?";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setInt(1, amount);
                ps.setString(2, username);
                ps.executeUpdate();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public int getHP(String name) {
        String playerSql = "SELECT hp FROM players WHERE username=?";
        try (PreparedStatement ps = connection.prepareStatement(playerSql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("hp");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        String enemySql = "SELECT hp FROM enemies WHERE name=?";
        try (PreparedStatement ps = connection.prepareStatement(enemySql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("hp");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }

public int getAttack(String name) {
    String playerSql = "SELECT atk FROM players WHERE username=?";
    try (PreparedStatement ps = connection.prepareStatement(playerSql)) {
        ps.setString(1, name);
        try (ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt("atk");
        }
    } catch (Exception e) {
        e.printStackTrace();
    }

    String enemySql = "SELECT atk FROM enemies WHERE name=?";
    try (PreparedStatement ps = connection.prepareStatement(enemySql)) {
        ps.setString(1, name);
        try (ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt("atk");
        }
    } catch (Exception e) {
        e.printStackTrace();
    }

    return 0;
}

    public int getDefense(String username) {
        String sql = "SELECT def FROM players WHERE username = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("def");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public void deletePlayer(String username){
            String sql = """
                    DELETE FROM players
                    WHERE username = ?
                    """;

            try(PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, username);
                ps.executeUpdate();

            }    catch (Exception e ){
                e.printStackTrace();
            }


    }

    public String getPlayerInventory(String username) {
        String sql = "SELECT inventory FROM players WHERE username = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("inventory");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public String getPlayerSkillName(String username) {
        String sql = "SELECT skill FROM players WHERE username = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String skill = rs.getString("skill");
                if (skill != null && !skill.trim().isEmpty()) {
                    return skill;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "Basic Strike";
    }


    public void addLootToInventory(String username, String itemName) {
        synchronized (DB_LOCK) { // Закучваме за всички агенти
            String checkQuery = "SELECT inventory FROM players WHERE username = ?";
            String updateQuery = "UPDATE players SET inventory = ? WHERE username = ?";

            try (PreparedStatement checkStmt = connection.prepareStatement(checkQuery)) {
                checkStmt.setString(1, username);
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next()) {
                        String currentInventory = rs.getString("inventory");
                        String newInventory = (currentInventory == null || currentInventory.isEmpty())
                                ? itemName
                                : currentInventory + "," + itemName;

                        try (PreparedStatement updateStmt = connection.prepareStatement(updateQuery)) {
                            updateStmt.setString(1, newInventory);
                            updateStmt.setString(2, username);
                            updateStmt.executeUpdate();
                            System.out.println("📦 Added " + itemName + " to " + username + "'s inventory!");
                        }
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

//    public synchronized void addCustomPlayer(String username, String weapon, String skill, int hp, int atk, int def) {
//        String sql = "INSERT INTO players (username, level, gold, weapon, skill, inventory, hp, atk, def) VALUES (?, 1, 100, ?, ?, ?, ?, ?, ?)";
//        try {
//            PreparedStatement del = connection.prepareStatement("DELETE FROM players WHERE username=?");
//            del.setString(1, username);
//            del.executeUpdate();
//
//            try (PreparedStatement ps = connection.prepareStatement(sql)) {
//                ps.setString(1, username);
//                ps.setString(2, weapon);
//                ps.setString(3, skill);
//                ps.setString(4, weapon);
//                ps.setInt(5, hp);
//                ps.setInt(6, atk);
//                ps.setInt(7, def);
//                ps.executeUpdate();
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }

    public synchronized void addCustomPlayer(String username, String weapon, String skill, int hp, int atk, int def) {
        String deleteSql = "DELETE FROM players WHERE username=?";
        String insertSql = "INSERT INTO players (username, level, gold, weapon, skill, inventory, hp, atk, def) VALUES (?, 1, 100, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement del = connection.prepareStatement(deleteSql)) {
            del.setString(1, username);
            del.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }

        try (PreparedStatement ps = connection.prepareStatement(insertSql)) {
            ps.setString(1, username);
            ps.setString(2, weapon);
            ps.setString(3, skill);
            ps.setString(4, weapon);
            ps.setInt(5, hp);
            ps.setInt(6, atk);
            ps.setInt(7, def);
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public synchronized void removeItemFromInventory(String username, String itemName){
        String checkQuery = "SELECT inventory FROM players WHERE username = ?";
        String updateQuery = "UPDATE players SET inventory = ? WHERE username = ?";

        try(PreparedStatement checkStmt = connection.prepareStatement(checkQuery)){
            checkStmt.setString(1, username);
            ResultSet rs = checkStmt.executeQuery();

            if(rs.next()){
                String currentInventory = rs.getString("inventory");
                if(currentInventory != null && !currentInventory.isEmpty()){
                    List<String> items = new ArrayList<>(Arrays.asList(currentInventory.split(",")));
                    if(items.remove(itemName)) {
                        String newInventory = String.join(",", items);

                        try (PreparedStatement updateStmt = connection.prepareStatement(updateQuery)) {
                            updateStmt.setString(1, newInventory);
                            updateStmt.setString(2, username);
                            updateStmt.executeUpdate();
                            System.out.println("🗑️ Removed " + itemName + " from " + username + "'s inventory!");
                        }

                    }
                }

            }
        } catch (SQLException e){
            e.printStackTrace();
        }
    }

//
public synchronized void updatePlayerHP(String username, int newHP) {
    String sql = "UPDATE players SET hp=? WHERE username=?";
    try (PreparedStatement ps = connection.prepareStatement(sql)) {
        ps.setInt(1, newHP);
        ps.setString(2, username);
        ps.executeUpdate();
    } catch (Exception e) {
        e.printStackTrace();
    }
}
    public void updatePlayerDEF(String username, int def) {
        String sql = "UPDATE players SET def = ? WHERE username = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, def);
            pstmt.setString(2, username);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


}
