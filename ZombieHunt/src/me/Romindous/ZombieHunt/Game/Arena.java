package me.Romindous.ZombieHunt.Game;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Random;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

import me.Romindous.ZombieHunt.Main;
import me.Romindous.ZombieHunt.Commands.KitsCmd;
import me.Romindous.ZombieHunt.Listeners.MainLis;
import me.Romindous.ZombieHunt.Messages.TitleManager;
import net.minecraft.EnumChatFormat;
import ru.komiss77.ApiOstrov;

import static org.bukkit.ChatColor.GRAY;
import static org.bukkit.ChatColor.GOLD;

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
	
	public Arena(String name, int min, int max, Location[] spawns, Main plug) {
		this.max = max;
		this.min = min;
		this.name = name;
		this.spawns = spawns;
		this.plug = plug;
		this.kls = new HashMap<String, Byte>();
		this.spcs = new HashSet<String>();
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
	
	public void addKls(String name) {
		kls.replace(name, (byte) (kls.get(name) + 1));
	}
	
	private void nullKls(String name2) {
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
				pl.setPlayerListFooter(GRAY + "Сейчас в игре: " + GOLD + MainLis.getPlaying() + GRAY + " человек!");
			}
			ApiOstrov.sendArenaData(this.name, ru.komiss77.enums.GameState.ОЖИДАНИЕ, "§7[§6Инфекция§7]", "§2Ожидание", " ", "§7Игроков: §2" + pls.size() + "§7/§2" + min, "", pls.size());
			Bukkit.getPlayer(name).sendMessage(Main.pref() + GRAY + "Вы покинули карту " + GOLD + getName());
			for (String s : pls) {
				TitleManager.sendAcBr(Bukkit.getPlayer(s), amtToHB(), 30);
				Bukkit.getPlayer(s).sendMessage(Main.pref() + ChatColor.YELLOW + name + GRAY + " вышел с карты!");
			}
			if (getPlAmount() == 0) {
				Main.endArena(this);
			}
			break;
		case LOBBY_START:
			pls.remove(name);
			for (Player pl : Bukkit.getOnlinePlayers()) {
				pl.setPlayerListFooter(GRAY + "Сейчас в игре: " + GOLD + MainLis.getPlaying() + GRAY + " человек!");
			}
			ApiOstrov.sendArenaData(this.name, ru.komiss77.enums.GameState.СТАРТ, "§7[§6Инфекция§7]", "§6Скоро старт!", " ", "§7Игроков: §6" + pls.size() + "§7/§6" + max, "", pls.size());
			Bukkit.getPlayer(name).sendMessage(Main.pref() + GRAY + "Вы покинули карту " + GOLD + getName());
			for (final String s : pls) {
				TitleManager.sendAcBr(Bukkit.getPlayer(s), amtToHB(), 30);
				if (!s.equalsIgnoreCase(name)) {
					Bukkit.getPlayer(s).sendMessage(Main.pref() + ChatColor.YELLOW + name + GRAY + " вышел с карты!");
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
					Main.chgSbdTm(Bukkit.getPlayer(s).getScoreboard(), "onwt", "", GOLD + (min - pls.size() > 1 ? 
							"" + (min - pls.size()) + GRAY + " игроков" 
							:
							"" + (min - pls.size()) + GRAY + " игрока"));
				}
				ApiOstrov.sendArenaData(this.name, ru.komiss77.enums.GameState.СТАРТ, "§7[§6Инфекция§7]", "§6Скоро старт!", " ", "§7Игроков: §6" + pls.size() + "§7/§6" + max, "", pls.size());
			}
			break;
		case BEGINING:
			pls.remove(name);
			for (Player pl : Bukkit.getOnlinePlayers()) {
				pl.setPlayerListFooter(GRAY + "Сейчас в игре: " + GOLD + MainLis.getPlaying() + GRAY + " человек!");
			}
			Bukkit.getPlayer(name).sendMessage(Main.pref() + GRAY + "Вы покинули карту " + GOLD + getName());
			for (String s : pls) {
				TitleManager.sendAcBr(Bukkit.getPlayer(s), amtToHB(), 30);
				Bukkit.getPlayer(s).sendMessage(Main.pref() + ChatColor.YELLOW + name + GRAY + " вышел с карты!");
			}
			if (pls.size() < min) {
				if (task != null) {
					task.cancel();
					state = GameState.LOBBY_WAIT;
				}
				for (final String s : pls) {
					Bukkit.getPlayer(s).sendMessage(Main.pref() + "На карте недостаточно игроков для начала!");
					Bukkit.getPlayer(s).teleport(Main.lobby);
					waitScore(s);
				}
				ApiOstrov.sendArenaData(this.name, ru.komiss77.enums.GameState.ОЖИДАНИЕ, "§7[§6Инфекция§7]", "§2Ожидание", " ", "§7Игроков: §2" + pls.size() + "§7/§2" + min, "", pls.size());
			} else {
				ApiOstrov.sendArenaData(this.name, ru.komiss77.enums.GameState.СТАРТ, "§7[§6Инфекция§7]", "§6Скоро старт!", " ", "§7Игроков: §6" + pls.size() + "§7/§6" + max, "", pls.size());
				for (final String s : pls) {
					Main.chgSbdTm(Bukkit.getPlayer(s).getScoreboard(), "plamt", "", "" + GOLD + getPlAmount() + GRAY + "/" + GOLD + max);
				}
			}
			break;
		case RUNNING:
			int i = -1;
			Bukkit.getPlayer(name).sendMessage(Main.pref() + GRAY + "Вы покинули игру " + GOLD + getName());
			if (isZombie(name)) {
				zhs.remove(name);
				if (zhs.size() == 0) {
					i = (new Random()).nextInt(pls.size());
					for (final String s : pls) {
						Bukkit.getPlayer(s).sendMessage(Main.pref() + ChatColor.YELLOW + name + GRAY + " вышел из игры, и");
						Bukkit.getPlayer(s).sendMessage(ChatColor.YELLOW + pls.get(i) + GRAY + " неожиданно превратился в " + ChatColor.DARK_RED + "Зомби" + GRAY + "!");
					}
					zombifyPl(Bukkit.getPlayer(pls.get(i)));
				} else {
					for (final String s : pls) {
						Bukkit.getPlayer(s).sendMessage(Main.pref() + ChatColor.YELLOW + name + GRAY + " вышел из игры!");
					}
				}
			} else {
				pls.remove(name);
				if (pls.size() == 0) {
					task.cancel();
					pls.addAll(zhs);
					for (String s : pls) {
						TitleManager.sendTtlSbTtl(Bukkit.getPlayer(s), ChatColor.DARK_RED + "Зомби " + GOLD + "победили!", GRAY + "Человеческая расса уничтожена...", 50);
					}
					zhs.removeAll(zhs);
					countEnd(new Random(), true);
				}
				for (String s : pls) {
					Bukkit.getPlayer(s).sendMessage(Main.pref() + ChatColor.YELLOW + name + GRAY + " вышел из игры!");
				}
			}
			for (Player pl : Bukkit.getOnlinePlayers()) {
				pl.setPlayerListFooter(GRAY + "Сейчас в игре: " + GOLD + MainLis.getPlaying() + GRAY + " человек!");
			}
			Main.data.chngNum(name, "gms", 1, "pls");
			break;
		case END:
			Bukkit.getPlayer(name).sendMessage(Main.pref() + GRAY + "Вы покинули игру " + GOLD + getName());
			pls.remove(name);
			for (Player pl : Bukkit.getOnlinePlayers()) {
				pl.setPlayerListFooter(GRAY + "Сейчас в игре: " + GOLD + MainLis.getPlaying() + GRAY + " человек!");
			}
			break;
		default:
			break;
		}
	}

	public void addPl(final String name) {
		if (pls.size() < max) {
			pls.add(name);
			for (Player pl : Bukkit.getOnlinePlayers()) {
				pl.setPlayerListFooter(GRAY + "Сейчас в игре: " + GOLD + MainLis.getPlaying() + GRAY + " человек!");
			}
			Bukkit.getPlayer(name).sendMessage(Main.pref() + GRAY + "Вы зашли на карту " + GOLD + getName());
			final String prm = Main.data.getString(name, "prm", "pls");
			TitleManager.sendNmTg(name, "§7[§6" + getName() + "§7] ", (prm.length() > 1 ? " §7(§e" + prm + "§7)" : ""), EnumChatFormat.c);
	        Bukkit.getPlayer(name).setPlayerListName(GRAY + "[" + GOLD + getName() + GRAY + "] " + Bukkit.getPlayer(name).getName() + (prm.length() > 1 ? " §7(§e" + prm + "§7)" : ""));
			for (String s : pls) {
				TitleManager.sendAcBr(Bukkit.getPlayer(s), amtToHB(), 30);
				if (!s.equalsIgnoreCase(name)) {
					Bukkit.getPlayer(s).sendMessage(Main.pref() + ChatColor.YELLOW + name + GRAY + " зашел на карту!");
				}
			}
			Main.waitPlayer(Bukkit.getPlayer(name));
			if (pls.size() == min) {
				ApiOstrov.sendArenaData(this.name, ru.komiss77.enums.GameState.СТАРТ, "§7[§6Инфекция§7]", "§6Скоро старт!", " ", "§7Игроков: §6" + pls.size() + "§7/§6" + max, "", pls.size());
				countLobby();
			} else  if (pls.size() < min) {
				ApiOstrov.sendArenaData(this.name, ru.komiss77.enums.GameState.ОЖИДАНИЕ, "§7[§6Инфекция§7]", "§2Ожидание", " ", "§7Игроков: §2" + pls.size() + "§7/§2" + min, "", pls.size());
				for (final String s : pls) {
					if (s.equalsIgnoreCase(name)) {
						waitScore(s);
					} else {
						Main.chgSbdTm(Bukkit.getPlayer(s).getScoreboard(), "onwt", "", GOLD + (min - pls.size() > 1 ? 
								"" + (min - pls.size()) + GRAY + " игроков" 
								:
								"" + (min - pls.size()) + GRAY + " игрока"));
					}
				}
			}
		} else {
			Bukkit.getPlayer(name).sendMessage(Main.pref() + ChatColor.RED + "Карта " + ChatColor.DARK_AQUA + getName() + ChatColor.RED + " заполнена!");
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
						Main.chgSbdTm(sb, "plamt", "", "" + GOLD + getPlAmount() + GRAY + "/" + GOLD + max);
						Main.chgSbdTm(sb, "strt", "", "" + GOLD + time + GRAY + " сек");
					}
				}
				switch (time) {
				case 20:
				case 10:
				case 5:
					for (String s : pls) {
						Bukkit.getPlayer(s).playSound(Bukkit.getPlayer(s).getLocation(), Sound.BLOCK_DISPENSER_FAIL, 0.8f, 1.2f);
						TitleManager.sendAcBr(Bukkit.getPlayer(s), "§6До начала осталось §d" + time + " §6секунд!", 30);
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
		final Random rand = new Random();
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
						Main.chgSbdTm(sb, "plamt", "", "" + GOLD + getPlAmount() + GRAY + "/" + GOLD + max);
						Main.chgSbdTm(sb, "strt", "", "" + GOLD + time + GRAY + " сек");
					}
				}
				switch (time) {
				case 10:
					for (String s : pls) {
						TitleManager.sendAcBr(Bukkit.getPlayer(s), "§6Через §d" + time + " §6секунд кто-то станет зомби!", 30);
						Bukkit.getPlayer(s).playSound(Bukkit.getPlayer(s).getLocation(), Sound.BLOCK_DISPENSER_FAIL, 0.8f, 1.2f);
					}
					break;
				case 5:
					for (String s : pls) {
						TitleManager.sendSbTtl(Bukkit.getPlayer(s), String.valueOf(GOLD) + time, 10);
						TitleManager.sendAcBr(Bukkit.getPlayer(s), "§6Через §d" + time + " §6секунд кто-то станет зомби!", 30);
						Bukkit.getPlayer(s).playSound(Bukkit.getPlayer(s).getLocation(), Sound.BLOCK_DISPENSER_FAIL, 0.8f, 1.2f);
					}
					break;
				case 4:
				case 3:
				case 2:
				case 1:
					for (String s : pls) {
						TitleManager.sendSbTtl(Bukkit.getPlayer(s), String.valueOf(GOLD) + time, 10);
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

	public void countGame(Random rand) {
		state = GameState.RUNNING;
		time = (short) ((5 + ((short) (pls.size() / 5))) * 60);
		for (byte i = 0; i < plsToZhs(); i++) {
			int tr = rand.nextInt(pls.size());
			msgEveryone(Main.pref() + ChatColor.RED + pls.get(tr) + GRAY + " превратился в Зомби!");
			zhs.add(pls.get(tr));
			pls.remove(pls.get(tr));
		}
		for (String s : zhs) {
			giveKit(s, true);
			kls.put('z' + s, (byte) 0);
			TitleManager.sendNmTg(s, "", "", EnumChatFormat.m);
			Bukkit.getPlayer(s).setPlayerListName(GRAY + "[" + GOLD + getName() + GRAY + "] " + ChatColor.DARK_RED + Bukkit.getPlayer(s).getName());
			Bukkit.getPlayer(s).playSound(Bukkit.getPlayer(s).getLocation(), Sound.ENTITY_ZOMBIE_AMBIENT, 20, 1);
			TitleManager.sendTtlSbTtl(Bukkit.getPlayer(s), GOLD + "Вы - " + ChatColor.RED + "Зомби", ChatColor.DARK_RED + "Убейте всех выживших за " + GOLD + (time / 60) + ChatColor.DARK_RED + " минут!", 50);
		}
		for (String s : pls) {
			giveKit(s, false);
			kls.put('p' + s, (byte) 0);
			TitleManager.sendNmTg(s, "", "", EnumChatFormat.k);
			Bukkit.getPlayer(s).setPlayerListName(GRAY + "[" + GOLD + getName() + GRAY + "] " + ChatColor.DARK_GREEN + Bukkit.getPlayer(s).getName());
			Bukkit.getPlayer(s).playSound(Bukkit.getPlayer(s).getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 20, 1);
			TitleManager.sendTtlSbTtl(Bukkit.getPlayer(s), GOLD + "Вы - " + ChatColor.GREEN + "Выживший", ChatColor.DARK_GREEN + "Выживайте на протяжении " + GOLD + (time / 60) + ChatColor.DARK_GREEN + " минут!", 50);
		}
		
		task = new BukkitRunnable() {
			
			@Override
			public void run() {
				//scoreboard stuff
				for (String s : zhs) {
					final Scoreboard sb = Bukkit.getPlayer(s).getScoreboard();
					if (sb.getTeam("pls") == null) {
						runnScore(s, true);
					} else {
						Main.chgSbdTm(sb, "pls", "", "" + GOLD + pls.size());
						Main.chgSbdTm(sb, "zhs", "", "" + GOLD + zhs.size());
						Main.chgSbdTm(sb, "end", "", GOLD + (time % 60 < 10 ? 
								(int) (((float) time) / 60.0f) + ":0" + (time % 60) 
								: 
								(int) (((float) time) / 60.0f) + ":" + (time % 60)));
					}
				}
				for (String s : pls) {
					final Scoreboard sb = Bukkit.getPlayer(s).getScoreboard();
					if (sb.getTeam("pls") == null) {
						runnScore(s, false);
					} else {
						Main.chgSbdTm(sb, "pls", "", "" + GOLD + pls.size());
						Main.chgSbdTm(sb, "zhs", "", "" + GOLD + zhs.size());
						Main.chgSbdTm(sb, "end", "", GOLD + (time % 60 < 10 ? 
								(int) (((float) time) / 60.0f) + ":0" + (time % 60) 
								: 
								(int) (((float) time) / 60.0f) + ":" + (time % 60)));
					}
				}
				if (time % 30 == 0) {
					for (String s : pls) {
						Bukkit.getPlayer(s).addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 100, 1));
						if (Bukkit.getPlayer(s).getInventory().contains(Material.BOW) || (Bukkit.getPlayer(s).getInventory().getItemInOffHand() != null && Bukkit.getPlayer(s).getInventory().getItemInOffHand().getType() == Material.BOW)) {
							Bukkit.getPlayer(s).getInventory().addItem(new ItemStack(Material.ARROW, 2));
						}
					}
				}
				switch (time) {
				case 120:
					for (String s : pls) {
						TitleManager.sendAcBr(Bukkit.getPlayer(s), "§6У зомби осталось §62 §6минуты!", 30);
					}
					for (String s : zhs) {
						TitleManager.sendAcBr(Bukkit.getPlayer(s), "§6У зомби осталось §62 §6минуты!", 30);
					}
					break;
				case 60:
					for (String s : pls) {
						TitleManager.sendAcBr(Bukkit.getPlayer(s), "§6Выжившие победят через §61 §6минуту!", 30);
					}
					for (String s : zhs) {
						TitleManager.sendAcBr(Bukkit.getPlayer(s), "§6Выжившие победят через §61 §6минуту!", 30);
					}
					break;
				case 30:
				case 10:
					for (String s : pls) {
						TitleManager.sendAcBr(Bukkit.getPlayer(s), "§6У зомби осталось §6" + time + " §6секунд!", 30);
					}
					for (String s : zhs) {
						TitleManager.sendAcBr(Bukkit.getPlayer(s), "§6У зомби осталось §6" + time + " §6секунд!", 30);
					}
					break;
				case 5:
				case 4:
				case 3:
				case 2:
				case 1:
					for (String s : pls) {
						TitleManager.sendTtl(Bukkit.getPlayer(s), String.valueOf(GOLD) + time, 10);
						Bukkit.getPlayer(s).playSound(Bukkit.getPlayer(s).getLocation(), Sound.BLOCK_DISPENSER_FAIL, 0.8f, 1.2f);
					}
					for (String s : zhs) {
						TitleManager.sendTtl(Bukkit.getPlayer(s), String.valueOf(GOLD) + time, 10);
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
	
	public void countEnd(Random rand, boolean zwin) {
		time = 6;
		state = GameState.END;
		pls.addAll(zhs);
		if (!zwin) {
			for (String s : pls) {
				TitleManager.sendTtlSbTtl(Bukkit.getPlayer(s), ChatColor.GREEN + "Выжившие " + GOLD + "победили!", GRAY + "Человечество продолжает свою жизнь...", 50);
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
							ApiOstrov.moneyChange(s.substring(1), 120 + (kls.get(s) * (s.startsWith("p") ? 20 : 60)), "Infection");
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
							Main.chgSbdTm(Bukkit.getPlayer(s).getScoreboard(), "fin", "", "" + GOLD + time + GRAY + " сек");
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
		TitleManager.sendNmTg(p.getName(), "", "", EnumChatFormat.m);
        p.setPlayerListName(GRAY + "[" + GOLD + getName() + GRAY + "] " + ChatColor.DARK_RED + p.getName());
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
		msgEveryone(Main.pref() + ChatColor.RED + p.getName() + GRAY + " превратился в Зомби!");
		pls.remove(p.getName());
		zhs.add(p.getName());
		if (time > 60) {
			TitleManager.sendTtlSbTtl(p, GOLD + "Вы - " + ChatColor.RED + "Зомби", ChatColor.DARK_RED + "Убейте всех выживших за " + GOLD + (time / 60) + ChatColor.DARK_RED + " минут!", 50);
		} else {
			TitleManager.sendTtlSbTtl(p, GOLD + "Вы - " + ChatColor.RED + "Зомби", ChatColor.DARK_RED + "Убейте всех выживших за " + GOLD + time + ChatColor.DARK_RED + " cекунд!", 50);
		}
		giveKit(p.getName(), true);
		runnScore(p.getName(), true);
		if (pls.size() == 0) {
			Bukkit.getScheduler().scheduleSyncDelayedTask(plug, new Runnable() {
				@Override
				public void run() {
					task.cancel();
					pls.addAll(zhs);
					for (String s : pls) {
						TitleManager.sendTtlSbTtl(Bukkit.getPlayer(s), ChatColor.DARK_RED + "Зомби " + GOLD + "победили!", GRAY + "Человеческая расса уничтожена...", 50);
					}
					zhs.removeAll(zhs);
					countEnd(new Random(), true);
			}}, 2);
		}
	}

	//сколько игроков из скольки
	public String amtToHB() {
		return pls.size() < min ? 
			ChatColor.GREEN + "На карте " + GOLD + pls.size() + ChatColor.GREEN + " игроков, нужно еще " + GOLD + (min - pls.size()) + ChatColor.GREEN + " для начала" 
			: 
			ChatColor.GREEN + "На карте " + GOLD + pls.size() + ChatColor.GREEN + " игроков, максимум: " + GOLD + max;
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
		final PlayerInventory inv = Bukkit.getPlayer(name).getInventory();
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
		Bukkit.getScheduler().scheduleSyncDelayedTask(plug, new Runnable() {
			@Override
			public void run() {
				Bukkit.getPlayer(name).getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(cs.getInt("hp"));
				Bukkit.getPlayer(name).setHealth(cs.getInt("hp"));
			}
		}, 2);
	}
	
	public void waitScore(final String name) {
		final Scoreboard sb = smg.getNewScoreboard();
		final Objective ob = sb.registerNewObjective("Инфекция", "", GRAY + "[" + GOLD + "Инфекция" + GRAY + "]");
		ob.setDisplaySlot(DisplaySlot.SIDEBAR);
		ob.getScore(GRAY + "Карта: " + ChatColor.DARK_GREEN + getName())
		.setScore(8);
		ob.getScore("   ").setScore(7);
		Main.crtSbdTm(sb, "onwt", "", GRAY + "Ждем еще ", GOLD + (min - pls.size() > 1 ? 
				"" + (min - pls.size()) + GRAY + " игроков" 
				:
				"" + (min - pls.size()) + GRAY + " игрока"));
		ob.getScore(GRAY + "Ждем еще ").setScore(6);
		ob.getScore("  ").setScore(5);
		ob.getScore(GRAY + "Ваши наборы для: ")
		.setScore(4);
		Main.crtSbdTm(sb, "pkit", "", ChatColor.DARK_GREEN + "Выжившего: ", GOLD + Main.data.getString(name, "pkit", "pls"));
		ob.getScore(ChatColor.DARK_GREEN + "Выжившего: ").setScore(3);
		Main.crtSbdTm(sb, "zkit", "", ChatColor.DARK_RED + "Зомби: ", GOLD + Main.data.getString(name, "zkit", "pls"));
		ob.getScore(ChatColor.DARK_RED + "Зомби: ").setScore(2);
		ob.getScore(" ").setScore(1);
		
		ob.getScore(ChatColor.YELLOW + "     ostrov77.su")
		.setScore(0);
		Bukkit.getPlayer(name).setScoreboard(sb);
	}
	
	public void lobbyScore(final String name) {
		final Scoreboard sb = smg.getNewScoreboard();
		final Objective ob = sb.registerNewObjective("Инфекция", "", GRAY + "[" + GOLD + "Инфекция" + GRAY + "]");
		ob.setDisplaySlot(DisplaySlot.SIDEBAR);
		ob.getScore(GRAY + "Карта: " + ChatColor.DARK_GREEN + getName())
		.setScore(9);
		Main.crtSbdTm(sb, "plamt", "", GRAY + "Игроков: ", "" + GOLD + getPlAmount() + GRAY + "/" + GOLD + max);
		ob.getScore(GRAY + "Игроков: ")
		.setScore(8);
		ob.getScore("   ").setScore(7);
		Main.crtSbdTm(sb, "strt", "", GRAY + "До начала: ", "" + GOLD + time + GRAY + " сек");
		ob.getScore(GRAY + "До начала: ").setScore(6);
		ob.getScore("  ").setScore(5);
		ob.getScore(GRAY + "Ваши наборы для: ")
		.setScore(4);
		Main.crtSbdTm(sb, "pkit", "", ChatColor.DARK_GREEN + "Выжившего: ", GOLD + Main.data.getString(name, "pkit", "pls"));
		ob.getScore(ChatColor.DARK_GREEN + "Выжившего: ").setScore(3);
		Main.crtSbdTm(sb, "zkit", "", ChatColor.DARK_RED + "Зомби: ", GOLD + Main.data.getString(name, "zkit", "pls"));
		ob.getScore(ChatColor.DARK_RED + "Зомби: ").setScore(2);
		ob.getScore(" ").setScore(1);
		
		ob.getScore(ChatColor.YELLOW + "     ostrov77.su")
		.setScore(0);
		Bukkit.getPlayer(name).setScoreboard(sb);
	}
	
	public void beginScore(final String name) {
		final Scoreboard sb = smg.getNewScoreboard();
		final Objective ob = sb.registerNewObjective("Инфекция", "", GRAY + "[" + GOLD + "Инфекция" + GRAY + "]");
		ob.setDisplaySlot(DisplaySlot.SIDEBAR);
		ob.getScore(GRAY + "Карта: " + ChatColor.DARK_GREEN + getName())
		.setScore(9);
		Main.crtSbdTm(sb, "plamt", "", GRAY + "Игроков: ", "" + GOLD + getPlAmount() + GRAY + "/" + GOLD + max);
		ob.getScore(GRAY + "Игроков: ")
		.setScore(8);
		ob.getScore("   ").setScore(7);
		Main.crtSbdTm(sb, "strt", "", GRAY + "Превращение через ", "" + GOLD + time + GRAY + " сек");
		ob.getScore(GRAY + "Превращение через ").setScore(6);
		ob.getScore("  ").setScore(5);
		ob.getScore(GRAY + "Ваши наборы для: ")
		.setScore(4);
		Main.crtSbdTm(sb, "pkit", "", ChatColor.DARK_GREEN + "Выжившего: ", GOLD + Main.data.getString(name, "pkit", "pls"));
		ob.getScore(ChatColor.DARK_GREEN + "Выжившего: ").setScore(3);
		Main.crtSbdTm(sb, "zkit", "", ChatColor.DARK_RED + "Зомби: ", GOLD + Main.data.getString(name, "zkit", "pls"));
		ob.getScore(ChatColor.DARK_RED + "Зомби: ").setScore(2);
		ob.getScore(" ").setScore(1);
		
		ob.getScore(ChatColor.YELLOW + "     ostrov77.su")
		.setScore(0);
		Bukkit.getPlayer(name).setScoreboard(sb);
	}
	
	public void runnScore(final String name, final boolean isZH) {
		final Scoreboard sb = smg.getNewScoreboard();
		final Objective ob = sb.registerNewObjective("Инфекция", "", GRAY + "[" + GOLD + "Инфекция" + GRAY + "]");
		ob.setDisplaySlot(DisplaySlot.SIDEBAR);
		ob.getScore(GRAY + "Карта: " + ChatColor.DARK_GREEN + getName())
		.setScore(9);
		Main.crtSbdTm(sb, "pls", "", GRAY + "Выживших: ", "" + GOLD + pls.size());
		ob.getScore(GRAY + "Выживших: ")
		.setScore(8);
		Main.crtSbdTm(sb, "zhs", "", GRAY + "Зомбей: ", "" + GOLD + zhs.size());
		ob.getScore(GRAY + "Зомбей: ")
		.setScore(7);
		ob.getScore("   ").setScore(6);
		Main.crtSbdTm(sb, "end", "", GRAY + "Победа выживших через ", GOLD + (time % 60 < 10 ? 
				(int) (((float) time) / 60.0f) + ":0" + (time % 60) 
				: 
				(int) (((float) time) / 60.0f) + ":" + (time % 60)));
		ob.getScore(GRAY + "Победа выживших через ")
		.setScore(5);
		ob.getScore("  ").setScore(4);
		if (isZH) {
			ob.getScore(GRAY + "Вы - " + ChatColor.DARK_RED + "Зомби")
			.setScore(3);
			ob.getScore(GRAY + "Набор: " + GOLD + Main.data.getString(name, "zkit", "pls"))
			.setScore(2);
		} else {
			ob.getScore(GRAY + "Вы - " + ChatColor.DARK_GREEN + "Выживший")
			.setScore(3);
			ob.getScore(GRAY + "Набор: " + GOLD + Main.data.getString(name, "pkit", "pls"))
			.setScore(2);
		}
		ob.getScore(" ").setScore(1);
		
		ob.getScore(ChatColor.YELLOW + "     ostrov77.su")
		.setScore(0);
		Bukkit.getPlayer(name).setScoreboard(sb);
	}
	
	public void endScore(final String name, final boolean zwin) {
		final Scoreboard sb = smg.getNewScoreboard();
		final Objective ob = sb.registerNewObjective("Инфекция", "", GRAY + "[" + GOLD + "Инфекция" + GRAY + "]");
		ob.setDisplaySlot(DisplaySlot.SIDEBAR);
		ob.getScore(GRAY + "Карта: " + ChatColor.DARK_GREEN + getName())
		.setScore(7);
		ob.getScore("   ").setScore(6);
		ob.getScore(GOLD + "    Поздравляем")
		.setScore(5);
		ob.getScore(zwin ? GRAY + "Выйграли:  " + ChatColor.DARK_RED + "Зомби" + GRAY + "!" 
			: 
			GRAY + "Выйграли:  " + ChatColor.DARK_GREEN + "Выжившие" + GRAY + "!")
		.setScore(4);
		ob.getScore("  ").setScore(3);
		Main.crtSbdTm(sb, "fin", "", GRAY + "Окончание через ", "" + GOLD + time + GRAY + " сек");
		ob.getScore(GRAY + "Окончание через ")
		.setScore(2);
		ob.getScore(" ").setScore(1);
		
		ob.getScore(ChatColor.YELLOW + "     ostrov77.su")
		.setScore(0);
		Bukkit.getPlayer(name).setScoreboard(sb);
	}
}
