package me.Romindous.ZombieHunt.SQL;

import java.io.File;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;

import me.Romindous.ZombieHunt.Main;
import ru.komiss77.ApiOstrov;

public class SQLGet {
	
	public void mkTbl(final String tbl, final String... cts) {
		try {
			Bukkit.getLogger().info("CREATE TABLE IF NOT EXISTS " + tbl + "(" + getVars(cts) + "PRIMARY KEY (" + cts[0].toUpperCase() + "))");
			exctStrStmt("CREATE TABLE IF NOT EXISTS " + tbl + "(" + getVars(cts) + "PRIMARY KEY (" + cts[0].toUpperCase() + "))").executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	private String getVars(final String[] cts) {
		final StringBuffer sb = new StringBuffer("");
		for (final String s : cts) {
			sb.append(s.toUpperCase() + " VARCHAR(20),");
		}
		return sb.toString();
	}
	
	public void setString(final String name, final String cat, final String set, final String tbl) {
		try {
			exctStrStmt("UPDATE " + tbl + " SET " + cat.toUpperCase() + "=? WHERE NAME=?", set, name).executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public String getString(final String name, final String cat, final String tbl) {
		try {
			final ResultSet rs = exctStrStmt("SELECT * FROM " + tbl + " WHERE NAME=?", name).executeQuery();
			return rs.next() ? rs.getString(cat.toUpperCase()) : null;
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public void chngNum(final String name, final String cat, final int n, final String tbl) {
		try {
			final ResultSet rs = exctStrStmt("SELECT * FROM " + tbl + " WHERE NAME=?", name).executeQuery(); rs.next();
			final PreparedStatement ps = ApiOstrov.getLocalConnection().prepareStatement("UPDATE pls SET " + cat.toUpperCase() + "=? WHERE NAME=?");
			ps.setInt(1, rs.getInt(cat.toUpperCase()) + n);
			ps.setString(2, name);
			ps.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	//deletion
	public void delTbl(final String tbl) {
		try {
			exctStrStmt("DROP TABLE " + tbl).executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public void clMap(final String map, final String tbl) {
		try {
			exctStrStmt("DELETE FROM " + tbl + " WHERE MAP=?", map).executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public PreparedStatement exctStrStmt(final String comm, final String... vars) throws SQLException {
		final PreparedStatement ps = ApiOstrov.getLocalConnection().prepareStatement(comm);
		for (byte i = 1; i <= vars.length; i++) {
			ps.setString(i, vars[i-1]);
		}
		return ps;
	}

	public void chckIfExsts(final String name, final String tbl) {
		try {
			final ResultSet rs = exctStrStmt("SELECT * FROM " + tbl + " WHERE NAME=?", name).executeQuery();
			if (!rs.next()) {
				final YamlConfiguration kits = YamlConfiguration.loadConfiguration(new File(Main.folder + File.separator + "kits.yml"));
				Bukkit.getPlayer(name).sendMessage(Main.folder + "  " + kits.getKeys(true).toString());
				final PreparedStatement ps = ApiOstrov.getLocalConnection().prepareStatement("INSERT IGNORE INTO " + tbl + "(NAME,ZKIT,PKIT,ZKLS,ZDTHS,PKLS,PDTHS,GMS,PRM) VALUES (?,?,?,?,?,?,?,?,?)");
				ps.setString(1, name);
				ps.setString(2, (String) kits.getConfigurationSection("kits.zombie").getKeys(false).toArray()[0]);
				ps.setString(3, (String) kits.getConfigurationSection("kits.player").getKeys(false).toArray()[0]);
				ps.setInt(4, 0);
				ps.setInt(5, 0);
				ps.setInt(6, 0);
				ps.setInt(7, 0);
				ps.setInt(8, 0);
				ps.setString(9, "N");
				ps.executeUpdate();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}
