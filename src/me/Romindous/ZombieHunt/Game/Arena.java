package me.Romindous.ZombieHunt.Game;

import java.io.File;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.GameMode;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

import me.Romindous.ZombieHunt.Main;
import me.Romindous.ZombieHunt.Commands.KitsCmd;
import me.Romindous.ZombieHunt.Listeners.MainLis;
import net.kyori.adventure.text.Component;
import ru.komiss77.ApiOstrov;
import ru.komiss77.Ostrov;
import ru.komiss77.enums.Stat;
import ru.komiss77.modules.player.PM;

public class Arena {

	private final HashMap<String, Byte> kls;
	private GameState state;
	private final String name;
	private final int min;
	private final int max;
	private short time;
	private final HashSet<String> spcs;
	private Location[] spawns;
	private final LinkedList<String> pls;
	private final LinkedList<String> zhs;
	private BukkitTask task;
	private final Main plug;
	private final ScoreboardManager smg;
	private final HashSet<String> ozhs;
	
	public Arena(final String name, final int min, final int max, final Location[] spawns, final Main plug) {
		this.max = max;
		this.min = min;
		this.name = name;
		this.spawns = spawns;
		this.plug = plug;
		this.kls = new HashMap<String, Byte>();
		this.spcs = new HashSet<String>();
		this.ozhs = new HashSet<String>();
		this.pls = new LinkedList<String>();
		this.zhs = new LinkedList<String>();
		this.state = GameState.LOBBY_WAIT;
		this.smg = Bukkit.getScoreboardManager();
		//подготвка карты
		spawns[0].getWorld().setGameRule(GameRule.KEEP_INVENTORY, true);
		spawns[0].getWorld().setTime(6000);
		spawns[0].getWorld().setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
		spawns[0].getWorld().setGameRule(GameRule.DO_MOB_SPAWNING, false);
	}

	public int getMin() {
		return min;
	}
	
	public int getMax() {
		return max;
	}
	
	public int getPlAmount() {
		return pls.size() + zhs.size();
	}

	public LinkedList<String> getList(String name) {
		return zhs.contains(name) ? zhs : pls;
	}
	
	public GameState getState() {
		return state;
	}

	public boolean hasPl(String name) {
		return pls.contains(name) || zhs.contains(name);
	}

	public Arena getArena() {
		return this;
	}
	
	public BukkitTask getTask() {
		return task;
	}
	
	public void addKls(final String name) {
		kls.replace(name, (byte) ((kls.get(name) == null ? 0 : kls.get(name)) + 1));
	}
	
	private void nullKls(final String name) {
		kls.replace(name, (byte) 0);
		
	}

	public int plsToZhs() {
		return 1 + (int) ((double) pls.size() / 6.0);
	}

	public String getName() {
		return name;
	}

	public boolean isZombie(String name) {
		return zhs.contains(name) ? true : false;
	}
	
	public Location getRandSpawn() {
		return spawns[new Random().nextInt(spawns.length)];
	}
	
	public HashSet<String> getSpcs() {
		return spcs;
	}

	public void removePl(final String name) {
		Main.lobbyPlayer(Bukkit.getPlayer(name));
		kls.remove(name);
		switch (getState()) {
		case LOBBY_WAIT:
			pls.remove(name);
			for (Player pl : Bukkit.getOnlinePlayers()) {
				pl.sendPlayerListFooter(Component.text("§7Сейчас в игре: §6" + MainLis.getPlaying() + "§7 человек!"));
			}
			ApiOstrov.sendArenaData(this.name, ru.komiss77.enums.GameState.ОЖИДАНИЕ, "§7[§6Инфекция§7]", "§2Ожидание", " ", "§7Игроков: §2" + pls.size() + "§7/§2" + min, "", pls.size());
			Bukkit.getPlayer(name).sendMessage(Main.pref() + "§7Вы покинули карту §6" + getName());
			for (String s : pls) {
				ApiOstrov.sendActionBarDirect(Bukkit.getPlayer(s), amtToHB());
				Bukkit.getPlayer(s).sendMessage(Main.pref() + "§e" + name + "§7 вышел с карты!");
			}
			if (getPlAmount() == 0) {
				Main.endArena(this);
			}
			break;
		case LOBBY_START:
			pls.remove(name);
			for (Player pl : Bukkit.getOnlinePlayers()) {
				pl.sendPlayerListFooter(Component.text("§7Сейчас в игре: §6" + MainLis.getPlaying() + "§7 человек!"));
			}
			ApiOstrov.sendArenaData(this.name, ru.komiss77.enums.GameState.СТАРТ, "§7[§6Инфекция§7]", "§6Скоро старт!", " ", "§7Игроков: §6" + pls.size() + "§7/§6" + max, "", pls.size());
			Bukkit.getPlayer(name).sendMessage(Main.pref() + "§7Вы покинули карту §6" + getName());
			for (final String s : pls) {
				ApiOstrov.sendActionBarDirect(Bukkit.getPlayer(s), amtToHB());
				if (!s.equalsIgnoreCase(name)) {
					Bukkit.getPlayer(s).sendMessage(Main.pref() + "§e" + name + "§7 вышел с карты!");
				}
			}
			if (pls.size() < min) {
				if (task != null) {
					task.cancel();
					state = GameState.LOBBY_WAIT;
				}
				for (final String s : pls) {
					Bukkit.getPlayer(s).sendMessage(Main.pref() + "На карте недостаточно игроков для начала!");
					waitScore(s);
				}
				ApiOstrov.sendArenaData(this.name, ru.komiss77.enums.GameState.ОЖИДАНИЕ, "§7[§6Инфекция§7]", "§2Ожидание", " ", "§7Игроков: §2" + pls.size() + "§7/§2" + min, "", pls.size());
			} else {
				for (final String s : pls) {
					Main.chgSbdTm(Bukkit.getPlayer(s).getScoreboard(), "plamt", "", "§6" + pls.size() + "§7/§6" + max);
				}
				ApiOstrov.sendArenaData(this.name, ru.komiss77.enums.GameState.СТАРТ, "§7[§6Инфекция§7]", "§6Скоро старт!", " ", "§7Игроков: §6" + pls.size() + "§7/§6" + max, "", pls.size());
			}
			break;
		case BEGINING:
			pls.remove(name);
			for (Player pl : Bukkit.getOnlinePlayers()) {
				pl.sendPlayerListFooter(Component.text("§7Сейчас в игре: §6" + MainLis.getPlaying() + "§7 человек!"));
			}
			Bukkit.getPlayer(name).sendMessage(Main.pref() + "§7Вы покинули карту §6" + getName());
			for (String s : pls) {
				ApiOstrov.sendActionBarDirect(Bukkit.getPlayer(s), amtToHB());
				Bukkit.getPlayer(s).sendMessage(Main.pref() + "§e" + name + "§7 вышел с карты!");
			}
			if (pls.size() < min) {
				if (task != null) {
					task.cancel();
					state = GameState.LOBBY_WAIT;
				}
				for (final String s : pls) {
					final Player p = Bukkit.getPlayer(s);
					p.sendMessage(Main.pref() + "На карте недостаточно игроков для начала!");
					p.teleport(Main.lobby);
					Ostrov.sync(() -> {
						for (final Player pl : Bukkit.getOnlinePlayers()) {
							final Arena ar = Arena.getPlayerArena(pl.getName());
							if (ar == null || ar.getState() == GameState.LOBBY_WAIT) {
								p.showPlayer(plug, pl);
								pl.showPlayer(plug, p);
							}
						}
					}, 4);
					waitScore(s);
				}
				ApiOstrov.sendArenaData(this.name, ru.komiss77.enums.GameState.ОЖИДАНИЕ, "§7[§6Инфекция§7]", "§2Ожидание", " ", "§7Игроков: §2" + pls.size() + "§7/§2" + min, "", pls.size());
			} else {
				ApiOstrov.sendArenaData(this.name, ru.komiss77.enums.GameState.СТАРТ, "§7[§6Инфекция§7]", "§6Скоро старт!", " ", "§7Игроков: §6" + pls.size() + "§7/§6" + max, "", pls.size());
				for (final String s : pls) {
					Main.chgSbdTm(Bukkit.getPlayer(s).getScoreboard(), "plamt", "", "§6" + getPlAmount() + "§7/§6" + max);
				}
			}
			break;
		case RUNNING:
			int i = -1;
			Bukkit.getPlayer(name).sendMessage(Main.pref() + "§7Вы покинули игру §6" + getName());
			if (isZombie(name)) {
				zhs.remove(name);
				if (zhs.size() == 0) {
					i = (new Random()).nextInt(pls.size());
					for (final String s : pls) {
						Bukkit.getPlayer(s).sendMessage(Main.pref() + "§e" + name + "§7 вышел из игры, и");
						Bukkit.getPlayer(s).sendMessage("§e" + pls.get(i) + "§7 неожиданно превратился в §4Зомби" + "§7!");
					}
					zombifyPl(Bukkit.getPlayer(pls.get(i)));
				} else {
					for (final String s : pls) {
						Bukkit.getPlayer(s).sendMessage(Main.pref() + "§e" + name + "§7 вышел из игры!");
					}
				}
			} else {
				pls.remove(name);
				if (pls.size() == 0) {
					task.cancel();
					pls.addAll(zhs);
					for (String s : pls) {
						ApiOstrov.sendTitleDirect(Bukkit.getPlayer(s), "§4Зомби §6победили!", "§7Человеческая расса уничтожена...", 10, 40, 20);
					}
					zhs.removeAll(zhs);
					countEnd(new Random(), true);
				}
				for (String s : pls) {
					Bukkit.getPlayer(s).sendMessage(Main.pref() + "§e" + name + "§7 вышел из игры!");
				}
			}
			for (Player pl : Bukkit.getOnlinePlayers()) {
				pl.sendPlayerListFooter(Component.text("§7Сейчас в игре: §6" + MainLis.getPlaying() + "§7 человек!"));
			}
			Main.data.chngNum(name, "gms", 1, "pls");
			break;
		case END:
			Bukkit.getPlayer(name).sendMessage(Main.pref() + "§7Вы покинули игру §6" + getName());
			pls.remove(name);
			for (Player pl : Bukkit.getOnlinePlayers()) {
				pl.sendPlayerListFooter(Component.text("§7Сейчас в игре: §6" + MainLis.getPlaying() + "§7 человек!"));
			}
			break;
		default:
			break;
		}
	}

	public void addPl(final String name) {
		final Player p = Bukkit.getPlayer(name);
		if (pls.size() < max) {
			pls.add(name);
			for (Player pl : Bukkit.getOnlinePlayers()) {
				pl.sendPlayerListFooter(Component.text("§7Сейчас в игре: §6" + MainLis.getPlaying() + "§7 человек!"));
			}
			p.sendMessage(Main.pref() + "§7Вы зашли на карту §6" + getName());
			final String prm = Main.data.getString(name, "prm", "pls");
			PM.getOplayer(p).tag("§7[§6" + getName() + "§7] ", "§2", (prm.length() > 1 ? " §7(§e" + prm + "§7)" : ""));
	        p.playerListName(Component.text("§7[§6" + getName() + "§7] " + p.getName() + (prm.length() > 1 ? " §7(§e" + prm + "§7)" : "")));
			for (String s : pls) {
				ApiOstrov.sendActionBarDirect(Bukkit.getPlayer(s), amtToHB());
				if (!s.equalsIgnoreCase(name)) {
					Bukkit.getPlayer(s).sendMessage(Main.pref() + "§e" + name + "§7 зашел на карту!");
				}
			}
			Main.waitPlayer(p);
			if (pls.size() == min) {
				ApiOstrov.sendArenaData(this.name, ru.komiss77.enums.GameState.СТАРТ, "§7[§6Инфекция§7]", "§6Скоро старт!", " ", "§7Игроков: §6" + pls.size() + "§7/§6" + max, "", pls.size());
				countLobby();
			} else if (pls.size() < min) {
				ApiOstrov.sendArenaData(this.name, ru.komiss77.enums.GameState.ОЖИДАНИЕ, "§7[§6Инфекция§7]", "§2Ожидание", " ", "§7Игроков: §2" + pls.size() + "§7/§2" + min, "", pls.size());
				for (final String s : pls) {
					if (s.equalsIgnoreCase(name)) {
						waitScore(s);
					} else {
						Main.chgSbdTm(Bukkit.getPlayer(s).getScoreboard(), "onwt", "", "§6" +(min - pls.size() > 1 ? 
								"" + (min - pls.size()) + "§7 игроков" 
								:
								"" + (min - pls.size()) + "§7 игрока"));
					}
				}
			}
		} else {
			p.sendMessage(Main.pref() + "§c" + "Карта §6" + getName() + "§c" + " заполнена!");
		}
	}
	
	//отсчет в лобби
	public void countLobby() {
		state = GameState.LOBBY_START;
		time = 20;
		task = new BukkitRunnable() {
			
			@Override
			public void run() {
				for (String s : pls) {
					final Scoreboard sb = Bukkit.getPlayer(s).getScoreboard();
					if (sb.getTeam("plamt") == null) {
						lobbyScore(s);
					} else {
						Main.chgSbdTm(sb, "plamt", "", "§6" + getPlAmount() + "§7/§6" + max);
						Main.chgSbdTm(sb, "strt", "", "§6" + time + "§7 сек");
					}
				}
				switch (time) {
				case 20:
				case 10:
				case 5:
					for (String s : pls) {
						Bukkit.getPlayer(s).playSound(Bukkit.getPlayer(s).getLocation(), Sound.BLOCK_DISPENSER_FAIL, 0.8f, 1.2f);
						ApiOstrov.sendActionBarDirect(Bukkit.getPlayer(s), "§6До начала осталось §d" + time + " §6секунд!");
					}
					break;
				case 4:
				case 3:
				case 2:
				case 1:
					for (String s : pls) {
						Bukkit.getPlayer(s).playSound(Bukkit.getPlayer(s).getLocation(), Sound.BLOCK_DISPENSER_FAIL, 0.8f, 1.2f);
					}
					break;
				case 0:
					task.cancel();
					countBegining();
					break;
				default:
					break;
				}
				time--;
			}
		}.runTaskTimer(plug, 0, 20);
	}
	
	//отсчет в игре
	public void countBegining() {
		final SecureRandom rand = new SecureRandom();
		state = GameState.BEGINING;
		time = 11;
		for (String name : pls) {
			for (Player pl : Bukkit.getOnlinePlayers()) {
				if (hasPl(pl.getName())) {
					Bukkit.getPlayer(name).showPlayer(plug, pl);
					pl.showPlayer(plug, Bukkit.getPlayer(name));
				} else {
					Bukkit.getPlayer(name).hidePlayer(plug, pl);
					pl.hidePlayer(plug, Bukkit.getPlayer(name));
				}
			}
			int i = rand.nextInt(spawns.length);
			Bukkit.getPlayer(name).teleport(spawns[i]);
			Bukkit.getPlayer(name).setGameMode(GameMode.SURVIVAL);
			Bukkit.getPlayer(name).playSound(Bukkit.getPlayer(name).getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 20, 1);
		}
		for (Player pl : Bukkit.getOnlinePlayers()) {
			pl.sendMessage(Main.pref() + "Игра §6" + getName() + "§7 началась!");
			pl.closeInventory();
		}
		ApiOstrov.sendArenaData(this.name, ru.komiss77.enums.GameState.ИГРА, "§7[§6Инфекция§7]", "§cИдет Игра", " ", "§7Игроков: " + pls.size(), "", pls.size());
		task = new BukkitRunnable() {
			
			@Override
			public void run() {
				//scoreboard stuff
				for (String s : pls) {
					final Scoreboard sb = Bukkit.getPlayer(s).getScoreboard();
					if (sb.getTeam("plamt") == null) {
						beginScore(s);
					} else {
						Main.chgSbdTm(sb, "plamt", "", "§6" + getPlAmount() + "§7/§6" + max);
						Main.chgSbdTm(sb, "strt", "", "§6" + time + "§7 сек");
					}
				}
				switch (time) {
				case 10:
					for (String s : pls) {
						ApiOstrov.sendActionBarDirect(Bukkit.getPlayer(s), "§6Через §d" + time + " §6секунд кто-то станет зомби!");
						Bukkit.getPlayer(s).playSound(Bukkit.getPlayer(s).getLocation(), Sound.BLOCK_DISPENSER_FAIL, 0.8f, 1.2f);
					}
					break;
				case 5:
					for (String s : pls) {
						ApiOstrov.sendTitleDirect(Bukkit.getPlayer(s), "", "§6" + time, 4, 8, 4);
						ApiOstrov.sendActionBarDirect(Bukkit.getPlayer(s), "§6Через §d" + time + " §6секунд кто-то станет зомби!");
						Bukkit.getPlayer(s).playSound(Bukkit.getPlayer(s).getLocation(), Sound.BLOCK_DISPENSER_FAIL, 0.8f, 1.2f);
					}
					break;
				case 4:
				case 3:
				case 2:
				case 1:
					for (String s : pls) {
						ApiOstrov.sendTitleDirect(Bukkit.getPlayer(s), "", "§6" + time, 4, 8, 4);
						Bukkit.getPlayer(s).playSound(Bukkit.getPlayer(s).getLocation(), Sound.BLOCK_DISPENSER_FAIL, 0.8f, 1.2f);
					}
					break;
				case 0:
					task.cancel();
					countGame(rand);
					break;
				default:
					break;
				}
				time--;
			}
		}.runTaskTimer(plug, 0, 20);
	}

	public void countGame(final SecureRandom rand) {
		state = GameState.RUNNING;
		time = (short) ((5 + ((short) (pls.size() / 5))) * 60);
		for (byte i = 0; i < plsToZhs(); i++) {
			int tr = rand.nextInt(pls.size());
			msgEveryone(Main.pref() + "§c" + pls.get(tr) + "§7 превратился в Зомби!");
			ozhs.add(pls.get(tr));
			zhs.add(pls.get(tr));
			pls.remove(pls.get(tr));
		}
		for (String s : zhs) {
			giveKit(s, true);
			kls.put('z' + s, (byte) 0);
			final Player p = Bukkit.getPlayer(s);
			PM.getOplayer(p).tag("", "§c", "");
			p.playerListName(Component.text("§7[§6" + getName() + "§7] §4" + s));
			p.playSound(Bukkit.getPlayer(s).getLocation(), Sound.ENTITY_ZOMBIE_AMBIENT, 20, 1);
			ApiOstrov.sendTitleDirect(Bukkit.getPlayer(s), "§6Вы - §cЗомби", "§4Убейте всех выживших за §6" + (time / 60) + "§4 минут!", 8, 40, 20);
		}
		for (String s : pls) {
			giveKit(s, false);
			kls.put('p' + s, (byte) 0);
			final Player p = Bukkit.getPlayer(s);
			PM.getOplayer(p).tag("", "§a", "");
			p.playerListName(Component.text("§7[§6" + getName() + "§7] §2" + s));
			p.playSound(Bukkit.getPlayer(s).getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 20, 1);
			ApiOstrov.sendTitleDirect(Bukkit.getPlayer(s), "§6Вы - " + "§aВыживший", "§2Выживайте на протяжении §6" + (time / 60) + "§2 минут!", 8, 40, 20);
		}
		
		task = new BukkitRunnable() {
			
			@Override
			public void run() {
				//scoreboard stuff
				for (final String s : zhs) {
					final Scoreboard sb = Bukkit.getPlayer(s).getScoreboard();
					if (sb.getTeam("pls") == null) {
						runnScore(s, true);
					} else {
						Main.chgSbdTm(sb, "pls", "", "§6" + pls.size());
						Main.chgSbdTm(sb, "zhs", "", "§6" + zhs.size());
						Main.chgSbdTm(sb, "end", "", "§6" +(time % 60 < 10 ? 
								(int) (((float) time) / 60.0f) + ":0" + (time % 60) 
								: 
								(int) (((float) time) / 60.0f) + ":" + (time % 60)));
					}
				}
				for (final String s : pls) {
					final Scoreboard sb = Bukkit.getPlayer(s).getScoreboard();
					if (sb.getTeam("pls") == null) {
						runnScore(s, false);
					} else {
						Main.chgSbdTm(sb, "pls", "", "§6" + pls.size());
						Main.chgSbdTm(sb, "zhs", "", "§6" + zhs.size());
						Main.chgSbdTm(sb, "end", "", "§6" +(time % 60 < 10 ? 
								(int) (((float) time) / 60.0f) + ":0" + (time % 60) 
								: 
								(int) (((float) time) / 60.0f) + ":" + (time % 60)));
					}
				}
				if (time % 30 == 0) {
					for (final String s : pls) {
						Bukkit.getPlayer(s).addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 100, 1));
						if (Bukkit.getPlayer(s).getInventory().contains(Material.BOW) || (Bukkit.getPlayer(s).getInventory().getItemInOffHand() != null && Bukkit.getPlayer(s).getInventory().getItemInOffHand().getType() == Material.BOW)) {
							Bukkit.getPlayer(s).getInventory().addItem(new ItemStack(Material.ARROW, 2));
						}
					}
				}
				switch (time) {
				case 120:
					for (final String s : pls) {
						ApiOstrov.sendActionBarDirect(Bukkit.getPlayer(s), "§6У зомби осталось §62 §6минуты!");
					}
					for (final String s : zhs) {
						ApiOstrov.sendActionBarDirect(Bukkit.getPlayer(s), "§6У зомби осталось §62 §6минуты!");
					}
					break;
				case 60:
					for (final String s : pls) {
						ApiOstrov.sendActionBarDirect(Bukkit.getPlayer(s), "§6Выжившие победят через §61 §6минуту!");
					}
					for (final String s : zhs) {
						ApiOstrov.sendActionBarDirect(Bukkit.getPlayer(s), "§6Выжившие победят через §61 §6минуту!");
					}
					break;
				case 30:
				case 10:
					for (final String s : pls) {
						ApiOstrov.sendActionBarDirect(Bukkit.getPlayer(s), "§6У зомби осталось §6" + time + " §6секунд!");
					}
					for (final String s : zhs) {
						ApiOstrov.sendActionBarDirect(Bukkit.getPlayer(s), "§6У зомби осталось §6" + time + " §6секунд!");
					}
					break;
				case 5:
				case 4:
				case 3:
				case 2:
				case 1:
					for (final String s : pls) {
						ApiOstrov.sendTitleDirect(Bukkit.getPlayer(s), "", "§6" + time, 4, 8, 4);
						Bukkit.getPlayer(s).playSound(Bukkit.getPlayer(s).getLocation(), Sound.BLOCK_DISPENSER_FAIL, 0.8f, 1.2f);
					}
					for (final String s : zhs) {
						ApiOstrov.sendTitleDirect(Bukkit.getPlayer(s), "", "§6" + time, 4, 8, 4);
						Bukkit.getPlayer(s).playSound(Bukkit.getPlayer(s).getLocation(), Sound.BLOCK_DISPENSER_FAIL, 0.8f, 1.2f);
					}
					break;
				case 0:
					task.cancel();
					countEnd(rand, false);
					break;
				default:
					if (time < 0) {
						Main.endArena(getArena());
					}
					break;
				}
				time--;
			}
		}.runTaskTimer(plug, 0, 20);
	}
	
	public void countEnd(final Random rand, final boolean zwin) {
		time = 6;
		state = GameState.END;
		if (zwin) {
			for (final String s : ozhs) {
				final Player p = Bukkit.getPlayer(s);
				if (p != null) {
					ApiOstrov.addStat(p, Stat.ZH_game);
					ApiOstrov.addStat(p, Stat.ZH_win);
				}
			}
		} else {
			for (final String s : pls) {
				ApiOstrov.addStat(Bukkit.getPlayer(s), Stat.ZH_game);
				ApiOstrov.addStat(Bukkit.getPlayer(s), Stat.ZH_win);
			}
		}
		pls.addAll(zhs);
		if (!zwin) {
			for (final String s : pls) {
				ApiOstrov.sendTitleDirect(Bukkit.getPlayer(s), "§aВыжившие §6победили!", "§7Человечество продолжает свою жизнь...", 8, 40, 20);
				Main.data.chngNum(s, "gms", 1, "pls");
			}
		}
		zhs.removeAll(zhs);
		task = new BukkitRunnable() {
		
			
			@Override
			public void run() {
				switch (time) {
				case 0:
					task.cancel();
					for (final String s : kls.keySet()) {
						if (pls.contains(s.substring(1))) {
							//ApiOstrov.moneyChange(s.substring(1), 5 + (kls.get(s) * (s.startsWith("p") ? 2 : 6)), "Infection");
						}
					}
					Main.endArena(getArena());
					for (final String s : pls) {
						Main.lobbyPlayer(Bukkit.getPlayer(s));
					}
					break;
				default:
					for (final String s : pls) {
						if (Bukkit.getPlayer(s).getScoreboard().getTeam("fin") == null) {
							endScore(s, zwin);
						} else {
							Main.chgSbdTm(Bukkit.getPlayer(s).getScoreboard(), "fin", "", "§6" + time + "§7 сек");
						}
						final Firework fw = (Firework) Bukkit.getPlayer(s).getWorld().spawnEntity(Bukkit.getPlayer(s).getLocation(), EntityType.FIREWORK);
						fw.setTicksLived(1);
						final FireworkMeta fm = fw.getFireworkMeta();
						fm.addEffect(FireworkEffect.builder().withColor(Color.fromRGB(rand.nextInt(16777000) + 100)).build());
						fw.setFireworkMeta(fm);
						fm.setPower(2);
					}
					if (time < 0) {
						Main.endArena(getArena());
					}
					break;
				}
				time--;
			}
		}.runTaskTimer(plug, 0, 20);
	}
	
	public void zombifyPl(final Player p) {
		p.playSound(p.getLocation(), Sound.ENTITY_ZOMBIE_AMBIENT, 2, 1);
		PM.getOplayer(p).tag("", "§c", "");
        p.playerListName(Component.text("§7[§6" + getName() + "§7] §4" + p.getName()));
		p.closeInventory();
		p.setFireTicks(0);
		for (final PotionEffect eff : p.getActivePotionEffects()) {
	        p.removePotionEffect(eff.getType());
		}
		nullKls(p.getName());
		for (String s : kls.keySet()) {
			if (s.equalsIgnoreCase('p' + p.getName())) {
				s.replaceFirst("p", "z");
			}
		}
		msgEveryone(Main.pref() + "§c" + p.getName() + "§7 превратился в Зомби!");
		ApiOstrov.addStat(p, Stat.ZH_pdths);
		ApiOstrov.addStat(p, Stat.ZH_game);
		ApiOstrov.addStat(p, Stat.ZH_loose);
		pls.remove(p.getName());
		zhs.add(p.getName());
		ApiOstrov.sendTitleDirect(p, "§6Вы - §cЗомби", "§4Убейте всех выживших за §6" + (time > 60 ? (time / 60) : time) + "§4 минут!", 8, 40, 20);
		giveKit(p.getName(), true);
		runnScore(p.getName(), true);
		if (pls.size() == 0) {
			Bukkit.getScheduler().scheduleSyncDelayedTask(plug, new Runnable() {
				@Override
				public void run() {
					task.cancel();
					pls.addAll(zhs);
					for (String s : pls) {
						ApiOstrov.sendTitleDirect(Bukkit.getPlayer(s), "§4Зомби §6победили!", "§7Человеческая расса уничтожена...", 8, 40, 20);
					}
					zhs.removeAll(zhs);
					countEnd(new Random(), true);
			}}, 2);
		}
	}

	//сколько игроков из скольки
	public String amtToHB() {
		return pls.size() < min ? 
			"§aНа карте §6" + pls.size() + "§a игроков, нужно еще §6" + (min - pls.size()) + "§a для начала" 
			: 
			"§aНа карте §6" + pls.size() + "§a игроков, максимум: §6" + max;
	}
	
	//в игре ли игрок?
	public static Arena getPlayerArena(String name) {
		for (Arena ar : Main.activearenas) {
			if (ar.hasPl(name)) {
				return ar;
			}
		}
		return null;
	}
	
	public static Arena getNameArena(String name) {
		for (Arena ar : Main.activearenas) {
			if (ar.getName().equalsIgnoreCase(name)) {
				return ar;
			}
		}
		return null;
	}
	
	public void msgEveryone(final String msg) {
		for (final String s : pls) {
			Bukkit.getPlayer(s).sendMessage(msg);
		}
		for (final String s : zhs) {
			Bukkit.getPlayer(s).sendMessage(msg);
		}
	}

	public void respZh(final Player p) {
		p.playSound(p.getLocation(), Sound.ITEM_AXE_STRIP, 4, 1);
		for (final PotionEffect effect : p.getActivePotionEffects()) {
	        p.removePotionEffect(effect.getType());
		}
		p.closeInventory();
		p.setFireTicks(0);
		p.teleport(spawns[(new Random()).nextInt(spawns.length)]);
		giveKit(p.getName(), true);
	}
	
	public void giveKit(final String name, final boolean isZH) {
		final YamlConfiguration kits = YamlConfiguration.loadConfiguration(new File(Main.folder + File.separator + "kits.yml"));
		final Player p = Bukkit.getPlayer(name);
		final PlayerInventory inv = p.getInventory();
		final ConfigurationSection cs = kits.getConfigurationSection(isZH ? 
			"kits.zombie." + Main.data.getString(name, "zkit", "pls")
			: 
			"kits.player." + Main.data.getString(name, "pkit", "pls"));
		inv.clear();
		inv.setHelmet(KitsCmd.getItemStack(cs.getConfigurationSection("helm")));
		inv.setChestplate(KitsCmd.getItemStack(cs.getConfigurationSection("chest")));
		inv.setLeggings(KitsCmd.getItemStack(cs.getConfigurationSection("leggs")));
		inv.setBoots(KitsCmd.getItemStack(cs.getConfigurationSection("boots")));
		for (byte i = 0; cs.contains("" + i); i++) {
			inv.setItem(i, KitsCmd.getItemStack(cs.getConfigurationSection("" + i)));
		}
		p.closeInventory();
		Bukkit.getScheduler().scheduleSyncDelayedTask(plug, new Runnable() {
			@Override
			public void run() {
				p.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(cs.getInt("hp"));
				p.setHealth(cs.getInt("hp"));
			}
		}, 2);
	}
	
	public void waitScore(final String name) {
		final Scoreboard sb = smg.getNewScoreboard();
		final Objective ob = sb.registerNewObjective("Инфекция", Criteria.DUMMY, Component.text("§7[§6Инфекция" + "§7]"));
		ob.setDisplaySlot(DisplaySlot.SIDEBAR);
		ob.getScore("§7Карта: §2" + getName())
		.setScore(8);
		ob.getScore("   ").setScore(7);
		Main.crtSbdTm(sb, "onwt", "", "§7Ждем еще ", "§6" +(min - pls.size() > 1 ? 
				"" + (min - pls.size()) + "§7 игроков" 
				:
				"" + (min - pls.size()) + "§7 игрока"));
		ob.getScore("§7Ждем еще ").setScore(6);
		ob.getScore("  ").setScore(5);
		ob.getScore("§7Ваши наборы для: ")
		.setScore(4);
		Main.crtSbdTm(sb, "pkit", "", "§2Выжившего: ", "§6" +Main.data.getString(name, "pkit", "pls"));
		ob.getScore("§2Выжившего: ").setScore(3);
		Main.crtSbdTm(sb, "zkit", "", "§4Зомби: ", "§6" +Main.data.getString(name, "zkit", "pls"));
		ob.getScore("§4Зомби: ").setScore(2);
		ob.getScore(" ").setScore(1);
		
		ob.getScore("§e     ostrov77.su")
		.setScore(0);
		Bukkit.getPlayer(name).setScoreboard(sb);
	}
	
	public void lobbyScore(final String name) {
		final Scoreboard sb = smg.getNewScoreboard();
		final Objective ob = sb.registerNewObjective("Инфекция", Criteria.DUMMY, Component.text("§7[§6Инфекция" + "§7]"));
		ob.setDisplaySlot(DisplaySlot.SIDEBAR);
		ob.getScore("§7Карта: §2" + getName())
		.setScore(9);
		Main.crtSbdTm(sb, "plamt", "", "§7Игроков: ", "§6" + getPlAmount() + "§7/§6" + max);
		ob.getScore("§7Игроков: ")
		.setScore(8);
		ob.getScore("   ").setScore(7);
		Main.crtSbdTm(sb, "strt", "", "§7До начала: ", "§6" + time + "§7 сек");
		ob.getScore("§7До начала: ").setScore(6);
		ob.getScore("  ").setScore(5);
		ob.getScore("§7Ваши наборы для: ")
		.setScore(4);
		Main.crtSbdTm(sb, "pkit", "", "§2Выжившего: ", "§6" +Main.data.getString(name, "pkit", "pls"));
		ob.getScore("§2Выжившего: ").setScore(3);
		Main.crtSbdTm(sb, "zkit", "", "§4Зомби: ", "§6" +Main.data.getString(name, "zkit", "pls"));
		ob.getScore("§4Зомби: ").setScore(2);
		ob.getScore(" ").setScore(1);
		
		ob.getScore("§e     ostrov77.su")
		.setScore(0);
		Bukkit.getPlayer(name).setScoreboard(sb);
	}
	
	public void beginScore(final String name) {
		final Scoreboard sb = smg.getNewScoreboard();
		final Objective ob = sb.registerNewObjective("Инфекция", Criteria.DUMMY, Component.text("§7[§6Инфекция" + "§7]"));
		ob.setDisplaySlot(DisplaySlot.SIDEBAR);
		ob.getScore("§7Карта: §2" + getName())
		.setScore(9);
		Main.crtSbdTm(sb, "plamt", "", "§7Игроков: ", "§6" + getPlAmount() + "§7/§6" + max);
		ob.getScore("§7Игроков: ")
		.setScore(8);
		ob.getScore("   ").setScore(7);
		Main.crtSbdTm(sb, "strt", "", "§7Превращение через ", "§6" + time + "§7 сек");
		ob.getScore("§7Превращение через ").setScore(6);
		ob.getScore("  ").setScore(5);
		ob.getScore("§7Ваши наборы для: ")
		.setScore(4);
		Main.crtSbdTm(sb, "pkit", "", "§2Выжившего: ", "§6" +Main.data.getString(name, "pkit", "pls"));
		ob.getScore("§2Выжившего: ").setScore(3);
		Main.crtSbdTm(sb, "zkit", "", "§4Зомби: ", "§6" +Main.data.getString(name, "zkit", "pls"));
		ob.getScore("§4Зомби: ").setScore(2);
		ob.getScore(" ").setScore(1);
		
		ob.getScore("§e     ostrov77.su")
		.setScore(0);
		Bukkit.getPlayer(name).setScoreboard(sb);
	}
	
	public void runnScore(final String name, final boolean isZH) {
		final Scoreboard sb = smg.getNewScoreboard();
		final Objective ob = sb.registerNewObjective("Инфекция", Criteria.DUMMY, Component.text("§7[§6Инфекция" + "§7]"));
		ob.setDisplaySlot(DisplaySlot.SIDEBAR);
		ob.getScore("§7Карта: §2" + getName())
		.setScore(9);
		Main.crtSbdTm(sb, "pls", "", "§7Выживших: ", "§6" + pls.size());
		ob.getScore("§7Выживших: ")
		.setScore(8);
		Main.crtSbdTm(sb, "zhs", "", "§7Зомбей: ", "§6" + zhs.size());
		ob.getScore("§7Зомбей: ")
		.setScore(7);
		ob.getScore("   ").setScore(6);
		Main.crtSbdTm(sb, "end", "", "§7Победа выживших через ", "§6" +(time % 60 < 10 ? 
				(int) (((float) time) / 60.0f) + ":0" + (time % 60) 
				: 
				(int) (((float) time) / 60.0f) + ":" + (time % 60)));
		ob.getScore("§7Победа выживших через ")
		.setScore(5);
		ob.getScore("  ").setScore(4);
		if (isZH) {
			ob.getScore("§7Вы - §4Зомби")
			.setScore(3);
			ob.getScore("§7Набор: §6" + Main.data.getString(name, "zkit", "pls"))
			.setScore(2);
		} else {
			ob.getScore("§7Вы - §2" + "Выживший")
			.setScore(3);
			ob.getScore("§7Набор: §6" + Main.data.getString(name, "pkit", "pls"))
			.setScore(2);
		}
		ob.getScore(" ").setScore(1);
		
		ob.getScore("§e     ostrov77.su")
		.setScore(0);
		Bukkit.getPlayer(name).setScoreboard(sb);
	}
	
	public void endScore(final String name, final boolean zwin) {
		final Scoreboard sb = smg.getNewScoreboard();
		final Objective ob = sb.registerNewObjective("Инфекция", Criteria.DUMMY, Component.text("§7[§6Инфекция" + "§7]"));
		ob.setDisplaySlot(DisplaySlot.SIDEBAR);
		ob.getScore("§7Карта: §2" + getName())
		.setScore(7);
		ob.getScore("   ").setScore(6);
		ob.getScore("§6    Поздравляем")
		.setScore(5);
		ob.getScore(zwin ? "§7Выйграли:  §4Зомби" + "§7!" 
			: 
			"§7Выйграли:  §2" + "Выжившие" + "§7!")
		.setScore(4);
		ob.getScore("  ").setScore(3);
		Main.crtSbdTm(sb, "fin", "", "§7Окончание через ", "§6" + time + "§7 сек");
		ob.getScore("§7Окончание через ")
		.setScore(2);
		ob.getScore(" ").setScore(1);
		
		ob.getScore("§e     ostrov77.su")
		.setScore(0);
		Bukkit.getPlayer(name).setScoreboard(sb);
	}
}
