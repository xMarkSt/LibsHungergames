package me.libraryaddict.Hungergames.Types;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import me.libraryaddict.Hungergames.Configs.LoggerConfig;
import me.libraryaddict.Hungergames.Configs.MySqlConfig;
import me.libraryaddict.Hungergames.Managers.PlayerManager;

public class PlayerQuitThread extends Thread {
    private final LoggerConfig cm = HungergamesApi.getConfigManager().getLoggerConfig();
    private Connection con = null;
    private boolean uuids;

    public PlayerQuitThread(boolean useUuids) {
        this.uuids = useUuids;
    }

    public void mySqlConnect() {
        try {
            System.out.printf((cm.getMySqlConnecting()) + "%n", getClass().getSimpleName());
            Class.forName("com.mysql.jdbc.Driver").getDeclaredConstructor().newInstance();
            MySqlConfig config = HungergamesApi.getConfigManager().getMySqlConfig();
            String conn = "jdbc:mysql://" + config.getMysql_host() + "/" + config.getMysql_database();
            con = DriverManager.getConnection(conn, config.getMysql_username(), config.getMysql_password());
        } catch (Exception ex) {
            System.err.printf((cm.getMySqlConnectingError()) + "%n", getClass().getSimpleName(), ex.getMessage());
        }
    }

    public void mySqlDisconnect() {
        if (!HungergamesApi.getConfigManager().getMySqlConfig().isStatsEnabled())
            return;
        try {
            System.out.printf((cm.getMySqlClosing()) + "%n", getClass().getSimpleName());
            this.con.close();
        } catch (Exception ex) {
            System.err.printf((cm.getMySqlClosingError()) + "%n", getClass().getSimpleName());
        }
    }

    public void run() {
        if (!HungergamesApi.getConfigManager().getMySqlConfig().isStatsEnabled())
            return;
        mySqlConnect();
        PlayerManager pm = HungergamesApi.getPlayerManager();
        uuids = uuids && HungergamesApi.getReflectionManager().hasGameProfiles();
        while (true) {
            if (pm.saveGamer.peek() != null) {
                Stats stats = pm.saveGamer.poll();
                try {
                    try {
                        con.createStatement().execute("DO 1");
                    } catch (Exception ex) {
                        mySqlConnect();
                    }
                    Statement stmt = con.createStatement();
                    if (stats.isNewStats()) {
                        stmt.execute("INSERT INTO HGStats (uuid, Name, Kills, Killstreak, Wins, Losses) VALUES ('"
                                + stats.getUuid().toString() + "', '" + stats.getPlayer() + "', " + stats.getKillsTotal() + ", "
                                + stats.getKillsBest() + ", " + stats.getWins() + ", " + stats.getLossses() + ")");
                    } else {
                        stmt.execute("UPDATE HGStats SET Kills=" + stats.getKillsTotal() + ", Killstreak=" + stats.getKillsBest()
                                + ", Wins=" + stats.getWins() + ", Losses=" + stats.getLossses() + " WHERE "
                                + (uuids ? "uuid" : "Name") + "='" + (uuids ? stats.getUuid().toString() : stats.getPlayer())
                                + "'");
                    }
                    stmt.close();
                } catch (Exception ex) {
                    System.out.printf((cm.getMySqlErrorSaveStats()) + "%n", stats.getOwningPlayer(), ex.getMessage());
                }
            }
            if (pm.saveGamer.peek() == null) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignored) {
                }
            }
        }
    }
}
