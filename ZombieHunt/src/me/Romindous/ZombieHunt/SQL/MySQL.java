package me.Romindous.ZombieHunt.SQL;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;

import me.Romindous.ZombieHunt.Main;

public class MySQL {
	
	private Connection conn;
	
	public void connTo() {
		if (!isConOn()) {
			final YamlConfiguration ost = YamlConfiguration.loadConfiguration(new File(Main.plug.getDataFolder().getParent() + File.separator + "Ostrov" + File.separator + "config.yml"));
			if (ost.getBoolean("local_database.use")) {
				try {
					conn = DriverManager.getConnection(ost.getString("local_database.mysql_host"), ost.getString("local_database.mysql_user"), ost.getString("local_database.mysql_passw"));
				} catch (SQLException e) {
					Bukkit.getLogger().info(ost.getString("local_database.mysql_host") + ", " + ost.getString("local_database.mysql_user") + ", " + ost.getString("local_database.mysql_passw"));
					Bukkit.getLogger().info("Not connected to a database... D:");
					conn = null;
				}
			} else {
				try {
					conn = DriverManager.getConnection(ost.getString("local_database.mysql_host"), ost.getString("local_database.mysql_user"), ost.getString("local_database.mysql_passw"));
				} catch (SQLException e) {
					Bukkit.getLogger().info(ost.getString("local_database.mysql_host") + ", " + ost.getString("local_database.mysql_user") + ", " + ost.getString("local_database.mysql_passw"));
					Bukkit.getLogger().info("Not connected to a database... D:");
					conn = null;
				}
			}
			if (!isConOn()) {
				Bukkit.getLogger().info(ost.getString("local_database.mysql_host") + ", " + ost.getString("local_database.mysql_user") + ", " + ost.getString("local_database.mysql_passw"));
				Bukkit.getLogger().info("Not connected to a database... D:");
			}
		}
	}
	
	public void disConn() {
		if (isConOn()) {
			try {
				conn.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}
	
	public boolean isConOn() {
		return conn == null ? false : true;
	}
	
	public Connection getConn() {
		return conn;
	}
}
