package yudxx.minecraft.spigot.yudxxTerritory.data;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DatabaseManager {

    private Connection connection;
    private final File databaseFile;

    public DatabaseManager(File dataFolder) {
        databaseFile = new File(dataFolder, "territories.db");
    }

    public boolean connect() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + databaseFile.getAbsolutePath());
            createTables();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void createTables() throws SQLException {
        Statement stmt = connection.createStatement();
        stmt.executeUpdate(
            "CREATE TABLE IF NOT EXISTS plots (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "world TEXT NOT NULL," +
            "plot_x INTEGER NOT NULL," +
            "plot_z INTEGER NOT NULL," +
            "owner TEXT," +
            "name TEXT," +
            "merged INTEGER DEFAULT 0," +
            "settings TEXT DEFAULT '{}'," +
            "UNIQUE(world, plot_x, plot_z)" +
            ")"
        );
        try {
            stmt.executeUpdate("ALTER TABLE plots ADD COLUMN name TEXT");
        } catch (SQLException ignored) {
        }
        stmt.executeUpdate(
            "CREATE TABLE IF NOT EXISTS trusted (" +
            "plot_id INTEGER NOT NULL," +
            "player_uuid TEXT NOT NULL," +
            "PRIMARY KEY(plot_id, player_uuid)," +
            "FOREIGN KEY(plot_id) REFERENCES plots(id) ON DELETE CASCADE" +
            ")"
        );
        stmt.executeUpdate(
            "CREATE TABLE IF NOT EXISTS merged (" +
            "plot_id INTEGER NOT NULL," +
            "merged_key TEXT NOT NULL," +
            "PRIMARY KEY(plot_id, merged_key)," +
            "FOREIGN KEY(plot_id) REFERENCES plots(id) ON DELETE CASCADE" +
            ")"
        );
        stmt.executeUpdate(
            "CREATE TABLE IF NOT EXISTS banned (" +
            "plot_id INTEGER NOT NULL," +
            "player_uuid TEXT NOT NULL," +
            "PRIMARY KEY(plot_id, player_uuid)," +
            "FOREIGN KEY(plot_id) REFERENCES plots(id) ON DELETE CASCADE" +
            ")"
        );
        stmt.close();
    }

    public void disconnect() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public PlotData getPlot(String world, int plotX, int plotZ) {
        try {
            PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM plots WHERE world = ? AND plot_x = ? AND plot_z = ?"
            );
            ps.setString(1, world);
            ps.setInt(2, plotX);
            ps.setInt(3, plotZ);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                PlotData plot = parsePlot(rs);
                rs.close();
                ps.close();
                return plot;
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public PlotData createPlot(String world, int plotX, int plotZ) {
        try {
            PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO plots (world, plot_x, plot_z) VALUES (?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS
            );
            ps.setString(1, world);
            ps.setInt(2, plotX);
            ps.setInt(3, plotZ);
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                int id = rs.getInt(1);
                rs.close();
                ps.close();
                return new PlotData(id, world, plotX, plotZ);
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public PlotData getOrCreatePlot(String world, int plotX, int plotZ) {
        PlotData plot = getPlot(world, plotX, plotZ);
        if (plot == null) {
            plot = createPlot(world, plotX, plotZ);
        }
        return plot;
    }

    public void setOwner(int plotId, UUID owner) {
        try {
            PreparedStatement ps = connection.prepareStatement(
                "UPDATE plots SET owner = ? WHERE id = ?"
            );
            ps.setString(1, owner != null ? owner.toString() : null);
            ps.setInt(2, plotId);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void setName(int plotId, String name) {
        try {
            PreparedStatement ps = connection.prepareStatement(
                "UPDATE plots SET name = ? WHERE id = ?"
            );
            ps.setString(1, name);
            ps.setInt(2, plotId);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void setMerged(int plotId, boolean merged) {
        try {
            PreparedStatement ps = connection.prepareStatement(
                "UPDATE plots SET merged = ? WHERE id = ?"
            );
            ps.setInt(1, merged ? 1 : 0);
            ps.setInt(2, plotId);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void saveSettings(int plotId, PlotSettings settings) {
        try {
            PreparedStatement ps = connection.prepareStatement(
                "UPDATE plots SET settings = ? WHERE id = ?"
            );
            ps.setString(1, PlotSettings.toJson(settings));
            ps.setInt(2, plotId);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void addTrusted(int plotId, UUID uuid) {
        try {
            PreparedStatement ps = connection.prepareStatement(
                "INSERT OR IGNORE INTO trusted (plot_id, player_uuid) VALUES (?, ?)"
            );
            ps.setInt(1, plotId);
            ps.setString(2, uuid.toString());
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void removeTrusted(int plotId, UUID uuid) {
        try {
            PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM trusted WHERE plot_id = ? AND player_uuid = ?"
            );
            ps.setInt(1, plotId);
            ps.setString(2, uuid.toString());
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void addMerged(int plotId, String key) {
        try {
            PreparedStatement ps = connection.prepareStatement(
                "INSERT OR IGNORE INTO merged (plot_id, merged_key) VALUES (?, ?)"
            );
            ps.setInt(1, plotId);
            ps.setString(2, key);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void removeMerged(int plotId, String key) {
        try {
            PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM merged WHERE plot_id = ? AND merged_key = ?"
            );
            ps.setInt(1, plotId);
            ps.setString(2, key);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void addBanned(int plotId, UUID uuid) {
        try {
            PreparedStatement ps = connection.prepareStatement(
                "INSERT OR IGNORE INTO banned (plot_id, player_uuid) VALUES (?, ?)"
            );
            ps.setInt(1, plotId);
            ps.setString(2, uuid.toString());
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void removeBanned(int plotId, UUID uuid) {
        try {
            PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM banned WHERE plot_id = ? AND player_uuid = ?"
            );
            ps.setInt(1, plotId);
            ps.setString(2, uuid.toString());
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void clearMerged(int plotId) {
        try {
            PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM merged WHERE plot_id = ?"
            );
            ps.setInt(1, plotId);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private PlotData parsePlot(ResultSet rs) throws SQLException {
        PlotData plot = new PlotData(rs.getInt("id"), rs.getString("world"), rs.getInt("plot_x"), rs.getInt("plot_z"));
        String ownerStr = rs.getString("owner");
        if (ownerStr != null && !ownerStr.isEmpty()) {
            plot.setOwner(UUID.fromString(ownerStr));
        }
        String name = rs.getString("name");
        if (name != null && !name.isEmpty()) {
            plot.setName(name);
        }
        plot.setMerged(rs.getInt("merged") == 1);
        plot.setSettings(PlotSettings.fromJson(rs.getString("settings")));
        loadTrusted(plot);
        loadMerged(plot);
        loadBanned(plot);
        return plot;
    }

    private void loadTrusted(PlotData plot) {
        try {
            PreparedStatement ps = connection.prepareStatement(
                "SELECT player_uuid FROM trusted WHERE plot_id = ?"
            );
            ps.setInt(1, plot.getId());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                plot.addTrusted(UUID.fromString(rs.getString("player_uuid")));
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadMerged(PlotData plot) {
        try {
            PreparedStatement ps = connection.prepareStatement(
                "SELECT merged_key FROM merged WHERE plot_id = ?"
            );
            ps.setInt(1, plot.getId());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                plot.addMergedPlot(rs.getString("merged_key"));
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadBanned(PlotData plot) {
        try {
            PreparedStatement ps = connection.prepareStatement(
                "SELECT player_uuid FROM banned WHERE plot_id = ?"
            );
            ps.setInt(1, plot.getId());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                plot.addBanned(UUID.fromString(rs.getString("player_uuid")));
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<PlotData> getPlayerPlots(UUID uuid) {
        List<PlotData> plots = new ArrayList<>();
        try {
            PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM plots WHERE owner = ?"
            );
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                plots.add(parsePlot(rs));
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return plots;
    }

    public PlotData getFirstPlot(UUID uuid) {
        List<PlotData> plots = getPlayerPlots(uuid);
        return plots.isEmpty() ? null : plots.get(0);
    }

    public PlotData getPlotById(int id) {
        try {
            PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM plots WHERE id = ?"
            );
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                PlotData plot = parsePlot(rs);
                rs.close();
                ps.close();
                return plot;
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void clearPlot(int plotId) {
        try {
            PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM trusted WHERE plot_id = ?"
            );
            ps.setInt(1, plotId);
            ps.executeUpdate();
            ps.close();
            PreparedStatement ps2 = connection.prepareStatement(
                "DELETE FROM banned WHERE plot_id = ?"
            );
            ps2.setInt(1, plotId);
            ps2.executeUpdate();
            ps2.close();
            clearMerged(plotId);
            setMerged(plotId, false);
            saveSettings(plotId, new PlotSettings());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void deletePlot(int plotId) {
        try {
            connection.setAutoCommit(false);
            PreparedStatement ps1 = connection.prepareStatement("DELETE FROM trusted WHERE plot_id = ?");
            ps1.setInt(1, plotId);
            ps1.executeUpdate();
            ps1.close();
            PreparedStatement ps2 = connection.prepareStatement("DELETE FROM banned WHERE plot_id = ?");
            ps2.setInt(1, plotId);
            ps2.executeUpdate();
            ps2.close();
            PreparedStatement ps3 = connection.prepareStatement("DELETE FROM merged WHERE plot_id = ?");
            ps3.setInt(1, plotId);
            ps3.executeUpdate();
            ps3.close();
            PreparedStatement ps4 = connection.prepareStatement("DELETE FROM plots WHERE id = ?");
            ps4.setInt(1, plotId);
            ps4.executeUpdate();
            ps4.close();
            connection.commit();
            connection.setAutoCommit(true);
        } catch (SQLException e) {
            try {
                connection.rollback();
                connection.setAutoCommit(true);
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            e.printStackTrace();
        }
    }

    public int getPlotCountInWorld(String world) {
        try {
            PreparedStatement ps = connection.prepareStatement(
                "SELECT COUNT(*) FROM plots WHERE world = ? AND owner IS NOT NULL"
            );
            ps.setString(1, world);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int count = rs.getInt(1);
                rs.close();
                ps.close();
                return count;
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public List<PlotData> getAllPlotsInWorld(String world) {
        List<PlotData> plots = new ArrayList<>();
        try {
            PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM plots WHERE world = ?"
            );
            ps.setString(1, world);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                plots.add(parsePlot(rs));
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return plots;
    }
}