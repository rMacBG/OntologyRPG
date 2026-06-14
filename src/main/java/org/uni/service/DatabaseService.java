package org.uni.service;

import java.sql.*;


public class DatabaseService {

    public static final String URL = "jdbc.sqlite:rpg.db";

    private Connection connection;

    public DatabaseService(){
        connect();
        createTables();
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
                );
                """;

        String battles = """
                CREATE TABLE IF NOT EXTISTS battles(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                player TEXT,
                enemy TEXT, 
                result TEXT
                );
                """;

        try(Statement stmt = connection.createStatement()){
            stmt.execute(players);
            stmt.execute(battles);

            System.out.println("Tables Created");
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void addPlayer(String username) {
        String sql = """
                INSERT INTO players(username, level, gold)
                VALUES(?,?,?)
                """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setInt(2, 1);
            ps.setInt(3, 0);

            ps.executeUpdate();

        } catch (Exception e) {
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
