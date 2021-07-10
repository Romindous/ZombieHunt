package me.Romindous.ZombieHunt;



import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import me.Romindous.ZombieHunt.Commands.KitsCmd;
import me.Romindous.ZombieHunt.Commands.ZHCmd;
import me.Romindous.ZombieHunt.Game.Arena;
import me.Romindous.ZombieHunt.Game.GameState;
import me.Romindous.ZombieHunt.Listeners.InterractLis;
import me.Romindous.ZombieHunt.Listeners.InventoryLis;
import me.Romindous.ZombieHunt.Listeners.MainLis;
import me.Romindous.ZombieHunt.Messages.TitleManager;
import me.Romindous.ZombieHunt.SQL.SQLGet;
import net.minecraft.EnumChatFormat;
import me.Romindous.ZombieHunt.SQL.MySQL;
import ru.komiss77.ApiOstrov;

public class Main extends JavaPlugin{
	
	public static Main plug;
	public static File folder;
	public static YamlConfiguration config;
	public static Location lobby;
	public static ArrayList<Arena> activearenas = new ArrayList<Arena>();
	public static LinkedList<String> nonactivearenas = new LinkedList<String>();
	public static MySQL sql;
	public static SQLGet data;
	
	
    public void onEnable() {
		getServer().getConsoleSender().sendMessage(ChatColor.DARK_GREEN + "ZombieHunt is ready!");
		getCommand("zh").setExecutor(new ZHCmd(this));
		getCommand("zkits").setExecutor(new KitsCmd(this));
		plug = this;
		getServer().getPluginManager().registerEvents(new MainLis(), this);
		getServer().getPluginManager().registerEvents(new InterractLis(), this);
		getServer().getPluginManager().registerEvents(new InventoryLis(), this);
		for (World w : getServer().getWorlds()) {
			w.setGameRule(GameRule.KEEP_INVENTORY, true);
			w.setGameRule(GameRule.NATURAL_REGENERATION, false);
		}
		
		TitleManager.v = getServer().getClass().getPackage().getName().split("\\.")[3];
		
		//конфиг
		loadConfigs();
		dataConn();
	}
	
	public static void dataConn() {
		Bukkit.getScheduler().runTaskTimer(plug, new Runnable() {
			@Override
			public void run() {
				(sql = new MySQL()).connTo();
			}
		}, 0, 36000);
		Bukkit.getScheduler().runTaskLater(plug, new Runnable() {
			@Override
			public void run() {
				if (sql.isConOn()) {
					Bukkit.getLogger().info("Reconnected to a database! :D");
					(data = new SQLGet()).mkTbl("pls", "name", "zkit", "pkit", "zkls", "zdths", "pkls", "pdths", "gms", "prm");
				}
			}
		}, 5);
	}
	
	public void onDisable() {

		getServer().getConsoleSender().sendMessage(ChatColor.DARK_GREEN + "ZombieHunt is disabled...");
		
	}

	public void loadConfigs() {
		try {
	        folder = getDataFolder();
	        
			File file = new File(folder + File.separator + "config.yml");
	        if (!file.exists()) {
	        	getServer().getConsoleSender().sendMessage("Config for ZombieHunt not found, creating a new one...");
	    		getConfig().options().copyDefaults(true);
	    		getConfig().save(file);
	        }
	        config = (YamlConfiguration) getConfig();
	        nonactivearenas.clear();
	        //киты
	        file = new File(folder + File.separator + "kits.yml");
	        file.createNewFile();
	        YamlConfiguration kits = YamlConfiguration.loadConfiguration(file);
	        if (!kits.contains("kits")) {
	        	kits.createSection("kits.player");
	        	kits.createSection("kits.zombie");
	        	kits.save(file);
	        }
	        //арены
	        file = new File(folder + File.separator + "arenas.yml");
	        file.createNewFile();
	        YamlConfiguration ars = YamlConfiguration.loadConfiguration(file);
	        if (!ars.contains("arenas")) {
	        	ars.createSection("arenas");
		        ars.save(file);
	        } else {
				for(final String s : ars.getConfigurationSection("arenas").getKeys(false)) {
					if (ars.contains("arenas." + s + ".fin")) {
						nonactivearenas.add(s);
						ApiOstrov.sendArenaData(s, ru.komiss77.enums.GameState.ОЖИДАНИЕ, "§7[§6Инфекция§7]", "§2Ожидание", " ", "§7Игроков: §20§7/§2" + ars.get("arenas." + s + ".min"), "", 0);
					}
				}
			}
	        if (ars.contains("lobby")) {
	        	lobby = new Location(getServer().getWorld(ars.getString("lobby.world")), ars.getInt("lobby.x"), ars.getInt("lobby.y"), ars.getInt("lobby.z"));
	        }
        }
        catch (IOException | NullPointerException ex) {
        	ex.printStackTrace();
            return;
        }
	}
	
	public Arena createArena(String name) {
		YamlConfiguration ars = YamlConfiguration.loadConfiguration(new File(getDataFolder() + File.separator + "arenas.yml"));
		byte num = (byte) ars.getString("arenas." + name + ".spawns.x").split(":").length;
		Location[] spawns = new Location[num];
		for (byte i = 0; i < num; i++) {
			spawns[i] = new Location(getServer().getWorld(ars.getString("arenas." + name + ".world")), 
					Integer.parseInt(ars.getString("arenas." + name + ".spawns.x").split(":")[i]), 
					Integer.parseInt(ars.getString("arenas." + name + ".spawns.y").split(":")[i]), 
					Integer.parseInt(ars.getString("arenas." + name + ".spawns.z").split(":")[i]));
		}
		return new Arena(name, ars.getInt("arenas." + name + ".min"), ars.getInt("arenas." + name + ".max"), spawns, this);
	}
	
	public static void lobbyPlayer(final Player p) {
		if (p == null) {
			return;
		}
		p.getInventory().clear();
		p.setGameMode(GameMode.SURVIVAL);
		ItemStack item = new ItemStack(Material.FERMENTED_SPIDER_EYE);
		ItemMeta meta = item.getItemMeta();
		meta.setDisplayName(ChatColor.GOLD + "Выбор Карты");
		item.setItemMeta(meta);
		p.getInventory().setItem(0, item);
		item = new ItemStack(Material.TURTLE_HELMET);
		meta = item.getItemMeta();
		meta.setDisplayName(ChatColor.YELLOW + "Наборы для Игры");
		item.setItemMeta(meta);
		p.getInventory().setItem(4, item);
		item = new ItemStack(Material.MAGMA_CREAM);
		meta = item.getItemMeta();
		meta.setDisplayName(ChatColor.DARK_RED + "Выход в Лобби");
		item.setItemMeta(meta);
		p.getInventory().setItem(8, item);
		p.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(20);
		p.setExp(0);
		p.setLevel(0);
		p.setFoodLevel(20);
		p.setHealth(20);
		final String prm = data.getString(p.getName(), "prm", "pls");
		p.setPlayerListName(ChatColor.GRAY + "[" + ChatColor.DARK_PURPLE + "ЛОББИ" + ChatColor.GRAY + "] " + p.getName() + (prm.length() > 1 ? " §7(§e" + prm + "§7)" : ""));
		for (PotionEffect ef : p.getActivePotionEffects()) {
	        p.removePotionEffect(ef.getType());
		}
		
		TitleManager.sendNmTg(p.getName(), "§7[§5ЛОББИ§7] ", (prm.length() > 1 ? " §7(§e" + prm + "§7)" : ""), EnumChatFormat.c);
        Bukkit.getScheduler().runTaskLater(plug, new Runnable() {
			@Override
			public void run() {
				for (final Player other : Bukkit.getOnlinePlayers()) {
					final Arena oar = Arena.getPlayerArena(other.getName());
					if (oar == null || oar.getState() == GameState.LOBBY_WAIT || oar.getState() == GameState.LOBBY_START) {
						p.showPlayer(plug, other);
						other.showPlayer(plug, p);
					} else {
						p.hidePlayer(plug, other);
						other.hidePlayer(plug, p);
					}
				}
			}
		}, 4);
		if (lobby != null) {
			p.teleport(lobby);
		}
		updateScore(p.getName());
	}
	
	public static void updateScore(final String name) {
		final Scoreboard sb = Bukkit.getScoreboardManager().getNewScoreboard();
		final Objective ob = sb.registerNewObjective("Инфекция", "", ChatColor.GRAY + "[" + ChatColor.GOLD + "Инфекция" + ChatColor.GRAY + "]");
		ob.setDisplaySlot(DisplaySlot.SIDEBAR);
		ob.getScore(ChatColor.GRAY + "Карта: " + ChatColor.DARK_GREEN + "Лобби")
		.setScore(6);
		ob.getScore("  ").setScore(5);
		ob.getScore(ChatColor.GRAY + "Ваши наборы для: ")
		.setScore(4);
		crtSbdTm(sb, "pkit", "", ChatColor.DARK_GREEN + "Выжившего: ", ChatColor.GOLD + data.getString(name, "pkit", "pls"));
		ob.getScore(ChatColor.DARK_GREEN + "Выжившего: ").setScore(3);
		crtSbdTm(sb, "zkit", "", ChatColor.DARK_RED + "Зомби: ", ChatColor.GOLD + data.getString(name, "zkit", "pls"));
		ob.getScore(ChatColor.DARK_RED + "Зомби: ").setScore(2);
		ob.getScore(" ").setScore(1);
		
		ob.getScore(ChatColor.YELLOW + "     ostrov77.su")
		.setScore(0);
		Bukkit.getPlayer(name).setScoreboard(sb);
	}

	public static void waitPlayer(Player p) {
		p.getInventory().clear();
		ItemStack item = new ItemStack(Material.GOLDEN_HELMET);
		ItemMeta meta = item.getItemMeta();
		meta.setDisplayName(ChatColor.YELLOW + "Выбор Набора");
		item.setItemMeta(meta);
		p.getInventory().setItem(2, item);
		item = new ItemStack(Material.SLIME_BALL);
		meta = item.getItemMeta();
		meta.setDisplayName(ChatColor.RED + "Выход");
		item.setItemMeta(meta);
		p.getInventory().setItem(6, item);
	}
	
	public static String pref() {
		return "§7[§6ZH§7] ";
	}
	
	public static void endArena(Arena ar) {
		ApiOstrov.sendArenaData(ar.getName(), ru.komiss77.enums.GameState.ОЖИДАНИЕ, "§7[§6Инфекция§7]", "§2Ожидание", " ", "§7Игроков: §20§7/§2" + ar.getMin(), "", 0);
		activearenas.remove(ar);
		for (final String s : ar.getSpcs()) {
			lobbyPlayer(Bukkit.getPlayer(s));
		}
		ar = null;
		for (Player pl : Bukkit.getOnlinePlayers()) {
			pl.setPlayerListFooter(ChatColor.GRAY + "Сейчас в игре: " + ChatColor.GOLD + MainLis.getPlaying() + ChatColor.GRAY + " человек!");
		}
	}
	
	public static void crtSbdTm(final Scoreboard sb, final String nm, final String prf, final String val, final String sfx) {
		final Team tm = sb.registerNewTeam(nm);
		tm.addEntry(val);
		tm.setPrefix(prf);
		tm.setSuffix(sfx);
	}
	
	public static void chgSbdTm(final Scoreboard sb, final String nm, final String prf, final String sfx) {
		final Team tm = sb.getTeam(nm);
		tm.setPrefix(prf);
		tm.setSuffix(sfx);
	}
}