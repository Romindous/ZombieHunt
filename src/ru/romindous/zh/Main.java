package ru.romindous.zh;

import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import ru.komiss77.ApiOstrov;
import ru.komiss77.enums.Stat;
import ru.komiss77.modules.player.PM;
import ru.komiss77.modules.world.WXYZ;
import ru.komiss77.utils.ItemBuilder;
import ru.komiss77.utils.TCUtils;
import ru.romindous.zh.Commands.KitsCmd;
import ru.romindous.zh.Commands.ZHCmd;
import ru.romindous.zh.Game.Arena;
import ru.romindous.zh.Game.GameState;
import ru.romindous.zh.Listeners.InterractLis;
import ru.romindous.zh.Listeners.InventoryLis;
import ru.romindous.zh.Listeners.MainLis;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class Main extends JavaPlugin{

	public static String PRFX;
	public static Main plug;
//	public static YamlConfiguration config;
	public static WXYZ lobby;
	public static final HashMap<String, Arena> activearenas = new HashMap<>();
	public static final ArrayList<String> nonactivearenas = new ArrayList<>();
	
	
    public void onEnable() {
		//Ostrov things
		PM.setOplayerFun(p -> new PlHunter(p), true);
		TCUtils.N = "§7";
		TCUtils.P = "§2";
		TCUtils.A = "§4";

		PRFX = TCUtils.A + "[" + TCUtils.P + "Инфекция" + TCUtils.A + "] " + TCUtils.N;
		plug = this;

		getServer().getConsoleSender().sendMessage("§2ZombieHunt is ready!");
		getCommand("zh").setExecutor(new ZHCmd(this));
		getCommand("zkits").setExecutor(new KitsCmd());
		getServer().getPluginManager().registerEvents(new MainLis(), this);
		getServer().getPluginManager().registerEvents(new InterractLis(), this);
		getServer().getPluginManager().registerEvents(new InventoryLis(), this);
		for (World w : getServer().getWorlds()) {
			w.setGameRule(GameRule.KEEP_INVENTORY, true);
			w.setGameRule(GameRule.NATURAL_REGENERATION, false);
		}
		
		//конфиг
		loadConfigs();
	}
	
	public void onDisable() {

		getServer().getConsoleSender().sendMessage("§4ZombieHunt is disabled...");
		
	}

	public void loadConfigs() {
		try {
	        
			/*File file = new File(folder + File.separator + "config.yml");
	        if (!file.exists()) {
	        	getServer().getConsoleSender().sendMessage("Config for ZombieHunt not found, creating a new one...");
	    		getConfig().options().copyDefaults(true);
	    		getConfig().save(file);
	        }
	        config = (YamlConfiguration) getConfig();*/
	        nonactivearenas.clear();
	        //арены
	        final File file = new File(getDataFolder() + File.separator + "arenas.yml");
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
	        	lobby = new WXYZ(getServer().getWorld(ars.getString("lobby.world")), ars.getInt("lobby.x"), ars.getInt("lobby.y"), ars.getInt("lobby.z"));
	        }
        }
        catch (IOException | NullPointerException ex) {
        	ex.printStackTrace();
        }
	}
	
	public Arena createArena(final String name) {
		YamlConfiguration ars = YamlConfiguration.loadConfiguration(new File(getDataFolder() + File.separator + "arenas.yml"));
		byte num = (byte) ars.getString("arenas." + name + ".spawns.x").split(":").length;
		Location[] spawns = new Location[num];
		for (byte i = 0; i < num; i++) {
			spawns[i] = new Location(getServer().getWorld(ars.getString("arenas." + name + ".world")), 
					Integer.parseInt(ars.getString("arenas." + name + ".spawns.x").split(":")[i]), 
					Integer.parseInt(ars.getString("arenas." + name + ".spawns.y").split(":")[i]), 
					Integer.parseInt(ars.getString("arenas." + name + ".spawns.z").split(":")[i]));
		}
		return new Arena(name, ars.getInt("arenas." + name + ".min"), ars.getInt("arenas." + name + ".max"), spawns);
	}
	
	public static void lobbyPlayer(final Player p, final PlHunter ph) {
		ph.kills0();
		ph.arena(null);
		ph.zombie(false);
		ph.orgZomb(false);
		p.getInventory().clear();
		p.setGameMode(GameMode.SURVIVAL);
		p.getInventory().setItem(0, new ItemBuilder(Material.FERMENTED_SPIDER_EYE).name("§6Выбор Карты").build());
		p.getInventory().setItem(4, new ItemBuilder(Material.TURTLE_HELMET).name("§eНаборы для Игры").build());
		p.getInventory().setItem(8, new ItemBuilder(Material.MAGMA_CREAM).name("§4Выход в Лобби").build());
		p.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(20);
		p.setExp(0);
		p.setLevel(0);
		p.setFoodLevel(20);
		p.setHealth(20);
		for (PotionEffect ef : p.getActivePotionEffects()) {
	        p.removePotionEffect(ef.getType());
		}
		final String prm = ph.getTopPerm();
		ph.taq(bfr('[', TCUtils.A + "ЛОББИ", ']'),
			TCUtils.P, (prm.isEmpty() ? "" : afr('(', "§e" + prm, ')')));
		if (lobby != null) p.teleport(lobby.getCenterLoc());
		updateScore(ph);
		inGameCnt();
		for (final Player op : Bukkit.getOnlinePlayers()) {
			if (p.getEntityId() == op.getEntityId()) continue;
			p.showPlayer(plug, op);
			final Arena ar = Arena.getPlayerArena(op);
			if (ar == null || ar.getState() == GameState.WAITING) {
				op.showPlayer(plug, p);
			} else {
				op.hidePlayer(plug, p);
			}
		}
	}

	public static void inGameCnt() {
		int i = 0;
		for (final Arena ar : Main.activearenas.values()) i+=ar.getPlAmount(null);
		final Component c = TCUtils.format(TCUtils.N + "Сейчас в игре: " + TCUtils.P + i + TCUtils.N + " человек!");
		for (final Player pl : Bukkit.getOnlinePlayers()) pl.sendPlayerListFooter(c);
	}
	
	public static void updateScore(final PlHunter ph) {
		ph.score.getSideBar().reset().title(Main.PRFX)
			.add(" ")
			.add(TCUtils.N + "Карта: " + TCUtils.A + "ЛОББИ")
			.add(TCUtils.A + "=-=-=-=-=-=-=-")
			.add(" ")
			.add(TCUtils.N + "Набор для")
			.add(Arena.SKIT, Arena.SURV_CLR + "Игрока: " + TCUtils.P + ph.survKit())
			.add(Arena.ZKIT, Arena.ZOMB_CLR + "Зомби: " + TCUtils.P + ph.zombKit())
			.add(" ")
			.add(TCUtils.A + "=-=-=-=-=-=-=-")
			.add(TCUtils.N + "(" + TCUtils.P + "К" + TCUtils.N + "/" + TCUtils.A + "Д" + TCUtils.N + "): " + TCUtils.P +
				ApiOstrov.toSigFigs((float) ph.getStat(Stat.ZH_zklls) / (float) ph.getStat(Stat.ZH_pdths), (byte) 2))
			.add(" ")
			.add("§e    ostrov77.ru").build();
	}
	
	public static void endArena(final Arena ar) {
		ApiOstrov.sendArenaData(ar.getName(), ru.komiss77.enums.GameState.ОЖИДАНИЕ, "§7[§6Инфекция§7]", "§2Ожидание", " ", "§7Игроков: §20§7/§2" + ar.getMin(), "", 0);
		activearenas.remove(ar.getName());
		for (final PlHunter plh : ar.getSpcs()) {
			lobbyPlayer(plh.getPlayer(), plh);
		}
	}

	public static String bfr(final char b, final String txt, final char d) {
		return TCUtils.N + b + txt + TCUtils.N + d + " ";
	}

	public static String afr(final char b, final String txt, final char d) {
		return " " + TCUtils.N + b + txt + TCUtils.N + d;
	}
}