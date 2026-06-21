package org.uni.service;

import java.sql.*;


public class DatabaseService {

    public static final String URL = "jdbc:sqlite:rpg.db";

    private Connection connection;

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
                hp INTEGER,
                atk INTEGER
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

            System.out.println("Tables Created");
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void seedData(){
        addPlayer("player");

        addCharacter("Yrspur the Dragon",
                180,
                30);

        addCharacter("Ashvak Goblin Thief",
                50,
                12);

        addCharacter("Ohvak the Blind Orc",
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

    public void addBattle(String player, String enemy, String result){
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

    public String getPlayer(String username){
        String sql = """
                SELECT * FROM players
                WHERE username = ?
                """;

        try(PreparedStatement ps = connection.prepareStatement(sql)){
            ps.setString(1, username);

            ResultSet rs = ps.executeQuery();

            if (rs.next()){
                return
                "Player: " + rs.getString("username")
                +
                "Level: " + rs.getInt("level")
                +
                "Gold: "  + rs.getInt("gold");

            }
        } catch (Exception e){
            e.printStackTrace();
        }


        return "Player not Found";
    }

    public void addGold(String username, int amount){
            String sql = """
                    UPDATE players
                    SET gold = gold + ?
                    WHERE username = ?
                    """;

            try(PreparedStatement ps = connection.prepareStatement(sql)){
                ps.setInt(1, amount);
                ps.setString(2, username);

                ps.executeUpdate();
            } catch (Exception e) {
                e.printStackTrace();
            }

    }

    public int getHP(String name){
        try{
            PreparedStatement ps = connection.prepareStatement("SELECT hp FROM enemies WHERE name=?");

            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();

            if(rs.next()){
                return rs.getInt("hp");


            }
        } catch (Exception e){
            e.printStackTrace();
        }

        return 0;
    }

    public int getAttack(String name) {
        try {
                PreparedStatement ps = connection.prepareStatement("SELECT atk FROM enemies WHERE name=?");
                ps.setString(1, name);
                ResultSet rs = ps.executeQuery();

                if(rs.next()){
                    return rs.getInt("atk");
                }
        } catch (Exception e) {
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


}
