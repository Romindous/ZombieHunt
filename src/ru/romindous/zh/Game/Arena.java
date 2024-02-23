package ru.romindous.zh.Game;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.ConfigurationSection;
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
import ru.komiss77.ApiOstrov;
import ru.komiss77.Ostrov;
import ru.komiss77.enums.Stat;
import ru.komiss77.modules.player.PM;
import ru.komiss77.utils.ItemBuilder;
import ru.komiss77.utils.TCUtils;
import ru.romindous.zh.Commands.KitsCmd;
import ru.romindous.zh.Main;
import ru.romindous.zh.PlHunter;

import java.util.Collection;
import java.util.HashMap;

public class Arena {

	private GameState state;
	private final String name;
	private final int min;
	private final int max;
	private int time;
	private final HashMap<String, PlHunter> spcs;
	private final HashMap<String, PlHunter> pls;
	private final Location[] spawns;
	private BukkitTask task;
	private final Main plug;

	public static final String SURV_CLR = "§a", ZOMB_CLR = "§c", ZKIT = "zkit", SKIT = "skit", AMT = "amt", LIMIT = "rem", ZOMB = "zbs";
	private static final PlHunter[] eps = new PlHunter[0];
	
	public Arena(final String name, final int min, final int max, final Location[] spawns, final Main plug) {
		this.max = max;
		this.min = min;
		this.name = name;
		this.spawns = spawns;
		this.plug = plug;
		this.spcs = new HashMap<>();
		this.pls = new HashMap<>();
		this.state = GameState.WAITING;
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
	
	public int getPlAmount(final Boolean zomb) {
		if (zomb == null) return pls.size();
		int i = 0;
		if (zomb) {
			for (final PlHunter ph : pls.values()) {
				if (ph.zombie()) i++;
			}
		} else {
			for (final PlHunter ph : pls.values()) {
				if (!ph.zombie()) i++;
			}
		}
		return i;
	}
	
	public GameState getState() {
		return state;
	}

	public Arena getArena() {
		return this;
	}

	public String getName() {
		return name;
	}

	public Collection<PlHunter> getPls() {
		return pls.values();
	}
	
	public Location getRandSpawn() {
		return ApiOstrov.rndElmt(spawns);
	}
	
	public Collection<PlHunter> getSpcs() {
		return spcs.values();
	}

	public void removePl(final Player p) {
		final PlHunter ph = pls.remove(p.getName());
		if (ph == null) return;

		switch (getState()) {
		case WAITING:
			ApiOstrov.sendArenaData(this.name, ru.komiss77.enums.GameState.ОЖИДАНИЕ, TCUtils.N + "[§6Инфекция§7]", "§2Ожидание", " ", TCUtils.N + "Игроков: §2" + pls.size() + TCUtils.N + "/§2" + min, "", pls.size());
			p.sendMessage(Main.PRFX + TCUtils.N + "Ты больше не на карте " + TCUtils.A + getName());
			for (final PlHunter plh : pls.values()) {
				final Player pl = plh.getPlayer();
				ApiOstrov.sendActionBarDirect(pl, amtToHB());
				pl.sendMessage(Main.PRFX + TCUtils.P + p.getName() + TCUtils.N + " вышел с карты!");
				plh.score.getSideBar().update(AMT, TCUtils.N + "Игроков: " + TCUtils.P + pls.size() + " чел.")
					.update(LIMIT, TCUtils.N + "Нужно еще " + TCUtils.A + (min - pls.size()) + " чел.");
			}
			if (pls.size() == 0) Main.endArena(this);
			break;
		case BEGINING:
			p.sendMessage(Main.PRFX + TCUtils.N + "Ты больше не на карте " + TCUtils.A + getName());
			for (final PlHunter plh : pls.values()) {
				final Player pl = plh.getPlayer();
				ApiOstrov.sendActionBarDirect(pl, amtToHB());
				pl.sendMessage(Main.PRFX + TCUtils.P + p.getName() + TCUtils.N + " вышел с карты!");
			}
			if (pls.size() < min) {
				state = GameState.WAITING;
				if (task != null) task.cancel();
				for (final PlHunter plh : pls.values()) {
					final Player pl = plh.getPlayer();
					pl.sendMessage(Main.PRFX + "На карте недостаточно игроков для начала!");
					pl.teleport(Main.lobby.getCenterLoc());
					waitScore(plh);
				}
				ApiOstrov.sendArenaData(this.name, ru.komiss77.enums.GameState.ОЖИДАНИЕ, TCUtils.N + "[§6Инфекция§7]", "§2Ожидание", " ", TCUtils.N + "Игроков: §2" + pls.size() + TCUtils.N + "/§2" + min, "", pls.size());
			} else {
				ApiOstrov.sendArenaData(this.name, ru.komiss77.enums.GameState.СТАРТ, TCUtils.N + "[§6Инфекция§7]", "§6Скоро старт!", " ", TCUtils.N + "Игроков: §6" + pls.size() + TCUtils.N + "/§6" + max, "", pls.size());
				for (final PlHunter plh : pls.values()) {
					plh.score.getSideBar().update(AMT, TCUtils.N + "Игроков: " + TCUtils.P + pls.size() + " чел.");
				}
			}
			break;
		case RUNNING:
			p.sendMessage(Main.PRFX + TCUtils.N + "Ты больше не на карте " + TCUtils.A + getName());
			if (ph.zombie()) {
				final int amt = getPlAmount(true);
				if (amt == 0) {
					final PlHunter pnz = ApiOstrov.rndElmt(pls.values().toArray(eps));
					for (final PlHunter plh : pls.values()) {
						final Player pl = plh.getPlayer();
						pl.sendMessage(Main.PRFX + TCUtils.P + p.getName() + TCUtils.N + " вышел из игры, и");
						pl.sendMessage(TCUtils.P + pnz.nik + TCUtils.N + " неожиданно превратился в §4Зомби" + TCUtils.N + "!");
					}
					zombifyPl(pnz.getPlayer(), pnz);
				} else {
					for (final PlHunter plh : pls.values()) {
						final Player pl = plh.getPlayer();
						pl.sendMessage(Main.PRFX + TCUtils.P + p.getName() + TCUtils.N + " вышел из игры!");
						plh.score.getSideBar().update(ZOMB, TCUtils.N + "Зомбей: " + ZOMB_CLR + amt + " чел.");
					}
				}
			} else {
				final int amt = getPlAmount(false);
				if (amt == 0) {
					task.cancel();
					for (final PlHunter plh : pls.values()) {
						final Player pl = plh.getPlayer();
						pl.sendMessage(Main.PRFX + TCUtils.P + p.getName() + TCUtils.N + " вышел из игры!");
						ApiOstrov.sendTitleDirect(pl, "§4Зомби §6победили!", TCUtils.N + "Человеческая расса уничтожена...", 10, 40, 20);
					}
					countEnd(true);
				} else {
					for (final PlHunter plh : pls.values()) {
						final Player pl = plh.getPlayer();
						pl.sendMessage(Main.PRFX + TCUtils.P + p.getName() + TCUtils.N + " вышел из игры!");
						plh.score.getSideBar().update(AMT, TCUtils.N + "Игроков: " + SURV_CLR + amt + " чел.");
					}
				}
			}
			break;
		case END:
			p.sendMessage(Main.PRFX + TCUtils.N + "Ты больше не на карте " + TCUtils.A + getName());
			break;
		default:
			break;
		}
		Main.lobbyPlayer(p, ph);
	}

	public void addPl(final Player p) {
		if (pls.size() < max) {
			final PlHunter ph = PM.getOplayer(p, PlHunter.class);
			switch (getState()) {
			case WAITING:
				waitScore(ph);
				break;
			case BEGINING:
				p.teleport(getRandSpawn());
				p.setGameMode(GameMode.SURVIVAL);
				p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 20, 1);
				beginScore(ph);
				for (final Player pl : Bukkit.getOnlinePlayers()) {
					final Arena oar = getPlayerArena(pl);
					if (oar == null || oar.getState() == GameState.WAITING) {
						p.hidePlayer(plug, pl);
					}
				}
				break;
			case RUNNING, END:
				p.sendMessage(Main.PRFX + "§cНа карте " + TCUtils.P + getName() + " §cуже идет игра!");
				addSpec(p);
				return;
            }
			ph.arena(this);
			pls.put(p.getName(), ph);
			final String prm = ph.getTopPerm();
			ph.taq(Main.bfr('[', TCUtils.A + getName(), ']'), TCUtils.P,
				(prm.isEmpty() ? "" : Main.afr('(', "§e" + prm, ')')));
			for (final PlHunter plh : pls.values()) {
				final Player pl = plh.getPlayer();
				ApiOstrov.sendActionBarDirect(pl, amtToHB());
				pl.sendMessage(Main.PRFX + TCUtils.P + p.getName() + TCUtils.N + " зашел на карту!");
				plh.score.getSideBar().update(AMT, TCUtils.N + "Игроков: " + TCUtils.P + pls.size() + " чел.")
					.update(LIMIT, TCUtils.N + "Нужно еще " + TCUtils.A + (min - pls.size()) + " чел.");
			}
			p.getInventory().clear();
			p.getInventory().setItem(2, new ItemBuilder(Material.GOLDEN_HELMET).name("§eВыбор Набора").build());
			p.getInventory().setItem(6, new ItemBuilder(Material.SLIME_BALL).name("§cВыход").build());
			if (pls.size() == min) {
				ApiOstrov.sendArenaData(this.name, ru.komiss77.enums.GameState.СТАРТ, TCUtils.N + "[§6Инфекция§7]", "§6Скоро старт!", " ", TCUtils.N + "Игроков: §6" + pls.size() + TCUtils.N + "/§6" + max, "", pls.size());
				countBegining();
			} else if (pls.size() < min) {
				ApiOstrov.sendArenaData(this.name, ru.komiss77.enums.GameState.ОЖИДАНИЕ, TCUtils.N + "[§6Инфекция§7]", "§2Ожидание", " ", TCUtils.N + "Игроков: §2" + pls.size() + TCUtils.N + "/§2" + min, "", pls.size());
				for (final PlHunter plh : pls.values()) {
					plh.score.getSideBar().update(AMT, TCUtils.N + "Игроков: " + TCUtils.P + pls.size() + " чел.");
				}
			}
		} else {
			p.sendMessage(Main.PRFX + "§c" + "Карта §6" + getName() + "§c" + " заполнена!");
		}
	}

	public void addSpec(final Player p) {
		final PlHunter ph = PM.getOplayer(p, PlHunter.class);
		p.teleport(getRandSpawn());
		spcs.put(p.getName(), ph);
		ph.arena(this);
		p.setGameMode(GameMode.SPECTATOR);
		for (Player pl : Bukkit.getOnlinePlayers()) {
			final Arena oar = getPlayerArena(pl);
			if (oar == null || oar.getState() == GameState.WAITING) {
				p.hidePlayer(plug, pl);
			}
		}
	}

	public void removeSpec(final Player p) {
		final PlHunter ph = spcs.remove(p.getName());
		if (ph == null) return;

		Main.lobbyPlayer(p, ph);
	}
	
	//отсчет в лобби
	/*public void countLobby() {
		for (final PlHunter plh : pls.values()) {
			lobbyScore(plh);
		}
		state = GameState.LOBBY_START;
		time = 20;
		task = new BukkitRunnable() {
			
			@Override
			public void run() {
				final String rmn = TCUtils.N + "До начала: " + TCUtils.P + time + " сек.";
				for (final PlHunter plh : pls.values()) {
					plh.score.getSideBar().update(LIMIT, rmn);
				}
				switch (time) {
				case 20:
				case 10:
				case 5:
					for (final PlHunter plh : pls.values()) {
						final Player pl = plh.getPlayer();
						pl.playSound(pl.getLocation(), Sound.BLOCK_DISPENSER_FAIL, 0.8f, 1.2f);
						ApiOstrov.sendActionBarDirect(pl, "§6До начала осталось §d" + time + " §6секунд!");
					}
					break;
				case 4:
				case 3:
				case 2:
				case 1:
					for (final PlHunter plh : pls.values()) {
						final Player pl = plh.getPlayer();
						pl.playSound(pl.getLocation(), Sound.BLOCK_DISPENSER_FAIL, 0.8f, 1.2f);
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
	}*/
	
	//отсчет в игре
	public void countBegining() {
		state = GameState.BEGINING;
		time = 30;
		for (final PlHunter ph : pls.values()) {
			final Player p = ph.getPlayer();
			for (Player pl : Bukkit.getOnlinePlayers()) {
				final Arena oar = getPlayerArena(pl);
				if (oar == null || oar.getState() == GameState.WAITING) {
					p.hidePlayer(plug, pl);
				}
			}
			p.teleport(getRandSpawn());
			p.setGameMode(GameMode.SURVIVAL);
			p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 20, 1);
			beginScore(ph);
		}

		ApiOstrov.sendArenaData(this.name, ru.komiss77.enums.GameState.ИГРА, TCUtils.N + "[§6Инфекция§7]", "§cИдет Игра", " ", TCUtils.N + "Игроков: " + pls.size(), "", pls.size());
		task = new BukkitRunnable() {
			
			@Override
			public void run() {
				//scoreboard stuff
				final String rmn = TCUtils.N + "Подготовка: " + TCUtils.A + time + " сек.";
				for (final PlHunter plh : pls.values()) {
					plh.score.getSideBar().update(LIMIT, rmn);
				}
				switch (time) {
				case 10:
					for (final PlHunter plh : pls.values()) {
						final Player pl = plh.getPlayer();
						ApiOstrov.sendActionBarDirect(pl, "§6Через §d" + time + " §6секунд кто-то станет зомби!");
						pl.playSound(pl.getLocation(), Sound.BLOCK_DISPENSER_FAIL, 0.8f, 1.2f);
					}
					break;
				case 5:
					for (final PlHunter plh : pls.values()) {
						final Player pl = plh.getPlayer();
						ApiOstrov.sendTitleDirect(pl, "", "§6" + time, 4, 8, 4);
						ApiOstrov.sendActionBarDirect(pl, "§6Через §d" + time + " §6секунд кто-то станет зомби!");
						pl.playSound(pl.getLocation(), Sound.BLOCK_DISPENSER_FAIL, 0.8f, 1.2f);
					}
					break;
				case 4:
				case 3:
				case 2:
				case 1:
					for (final PlHunter plh : pls.values()) {
						final Player pl = plh.getPlayer();
						ApiOstrov.sendTitleDirect(pl, "", "§6" + time, 4, 8, 4);
						pl.playSound(pl.getLocation(), Sound.BLOCK_DISPENSER_FAIL, 0.8f, 1.2f);
					}
					break;
				case 0:
					task.cancel();
					countGame();
					break;
				default:
					break;
				}
				time--;
			}
		}.runTaskTimer(plug, 0, 20);
	}

	public void countGame() {
		state = GameState.RUNNING;
		time = (short) ((5 + ((short) (pls.size() / 5))) * 60);
		final PlHunter[] zps = ApiOstrov.shuffle(pls.values().toArray(eps));
		for (int i = pls.size() / 6 + 1; i != 0; i--) {
			final PlHunter zmb = zps[i];
			zmb.orgZomb(true);
			zmb.zombie(true);
			msgEveryone(Main.PRFX + "§c" + zmb.nik + TCUtils.N + " превратился в Зомби!");
		}
		for (final PlHunter plh : pls.values()) {
			final Player pl = plh.getPlayer();
			giveKit(pl, plh);
			runnScore(plh);
			if (plh.zombie()) {
				plh.taq(Main.bfr('[', TCUtils.A + getName(), ']'), ZOMB_CLR, "");
				pl.playSound(pl.getLocation(), Sound.ENTITY_ZOMBIE_AMBIENT, 20, 1);
				ApiOstrov.sendTitleDirect(pl, TCUtils.A + "Ты -" + ZOMB_CLR + "Зомби",
					TCUtils.P + "Убей всех игроков за " + TCUtils.A + (time / 60) + TCUtils.P + " минут!", 8, 40, 20);
			} else {
				plh.taq(Main.bfr('[', TCUtils.A + getName(), ']'), SURV_CLR, "");
				pl.playSound(pl.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 20, 1);
				ApiOstrov.sendTitleDirect(pl, TCUtils.A + "Ты - " + SURV_CLR + "Игрок",
					TCUtils.P + "Выживи на протяжении " + TCUtils.A + (time / 60) + TCUtils.P + " минут!", 8, 40, 20);
			}
		}
		
		task = new BukkitRunnable() {
			
			@Override
			public void run() {
				//scoreboard stuff
				final String rmn = TCUtils.P + ApiOstrov.secondToTime(time);
				for (final PlHunter plh : pls.values()) {
					plh.score.getSideBar().update(LIMIT, rmn);
				}
				if (time % 30 == 0) {
					for (final PlHunter plh : pls.values()) {
						if (plh.zombie()) continue;
						final Player pl = plh.getPlayer();
						pl.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 100, 1));
						final PlayerInventory inv = pl.getInventory();
						if (inv.contains(Material.BOW) || inv.getItemInOffHand().getType() == Material.BOW) {
							inv.addItem(new ItemStack(Material.ARROW, 2));
						}
					}
				}
				switch (time) {
				case 120:
					for (final PlHunter plh : pls.values()) {
						final Player pl = plh.getPlayer();
						ApiOstrov.sendActionBarDirect(pl, "§6У зомби осталось §62 §6минуты!");
					}
					break;
				case 60:
					for (final PlHunter plh : pls.values()) {
						final Player pl = plh.getPlayer();
						ApiOstrov.sendActionBarDirect(pl, "§6Игроки победят через §61 §6минуту!");
					}
					break;
				case 30:
				case 10:
					for (final PlHunter plh : pls.values()) {
						final Player pl = plh.getPlayer();
						ApiOstrov.sendActionBarDirect(pl, "§6У зомби осталось §6" + time + " §6секунд!");
					}
					break;
				case 5:
				case 4:
				case 3:
				case 2:
				case 1:
					for (final PlHunter plh : pls.values()) {
						final Player pl = plh.getPlayer();
						ApiOstrov.sendTitleDirect(pl, "", "§6" + time, 4, 8, 4);
						pl.playSound(pl.getLocation(), Sound.BLOCK_DISPENSER_FAIL, 0.8f, 1.2f);
					}
					break;
				case 0:
					task.cancel();
					for (final PlHunter plh : pls.values()) {
						final Player pl = plh.getPlayer();
						ApiOstrov.sendTitleDirect(pl, SURV_CLR + "Игроки " + TCUtils.P + "победили!", TCUtils.N + "Человечество продолжает свою жизнь...", 8, 40, 20);
					}
					countEnd(false);
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
	
	public void countEnd(final boolean zwin) {
		time = 6;
		state = GameState.END;
		if (zwin) {
			for (final PlHunter plh : pls.values()) {
				endScore(plh, true);
				plh.addStat(Stat.ZH_game, 1);
				if (!plh.orgZomb()) continue;
				plh.addStat(Stat.ZH_win, 1);
			}
		} else {
			for (final PlHunter plh : pls.values()) {
				endScore(plh, false);
				plh.addStat(Stat.ZH_game, 1);
				if (plh.orgZomb()) continue;
				plh.addStat(Stat.ZH_win, 1);
			}
		}

		task = new BukkitRunnable() {
			@Override
			public void run() {
				final String rmn = TCUtils.N + "До конца: " + TCUtils.A + time + " сек.";
				switch (time) {
				case 0:
					task.cancel();
					Main.endArena(getArena());
					for (final PlHunter plh : pls.values()) {
						Main.lobbyPlayer(plh.getPlayer(), plh);
					}
					break;
				default:
					for (final PlHunter plh : pls.values()) {
						plh.score.getSideBar().update(LIMIT, rmn);
						final Player pl = plh.getPlayer();
						final Firework fw = (Firework) pl.getWorld().spawnEntity(pl.getLocation(), EntityType.FIREWORK);
						final FireworkMeta fm = fw.getFireworkMeta();
						fm.addEffect(FireworkEffect.builder().withColor(Color.fromRGB(Ostrov.random.nextInt(16777000) + 100)).build());
						fw.setFireworkMeta(fm);
					}
					break;
				}
				time--;
			}
		}.runTaskTimer(plug, 0, 20);
	}
	
	public void zombifyPl(final Player p, final PlHunter ph) {
		p.playSound(p.getLocation(), Sound.ENTITY_ZOMBIE_AMBIENT, 2, 1);
		ph.taq(Main.bfr('[', TCUtils.A + getName(), ']'), ZOMB_CLR, "");
		ph.zombie(true);
		p.closeInventory();
		p.setFireTicks(0);
		for (final PotionEffect eff : p.getActivePotionEffects()) {
	        p.removePotionEffect(eff.getType());
		}
		ph.kills0();
		giveKit(p, ph);
		msgEveryone(Main.PRFX + "§c" + p.getName() + TCUtils.N + " превратился в Зомби!");
		ApiOstrov.addStat(p, Stat.ZH_pdths);
		ApiOstrov.addStat(p, Stat.ZH_loose);
		if (getPlAmount(false) == 0) {
			if (task != null) task.cancel();
			for (final PlHunter plh : pls.values()) {
				ApiOstrov.sendTitleDirect(plh.getPlayer(), ZOMB_CLR + "Зомби " + TCUtils.A + "победили!", TCUtils.N + "Человеческая расса уничтожена...", 8, 40, 20);
			}
			countEnd(true);
			return;
		}

		ApiOstrov.sendTitleDirect(p, TCUtils.A + "Ты -" + ZOMB_CLR + "Зомби",
			TCUtils.P + "Убей всех игроков за " + TCUtils.A + (time / 60) + TCUtils.P + " минут!", 8, 40, 20);
		final int zbs = getPlAmount(true);
		runnScore(ph);
		for (final PlHunter plh : pls.values()) {
			plh.score.getSideBar().update(AMT, TCUtils.N + "Игроков: " + SURV_CLR + (pls.size() - zbs) + " чел.");
			plh.score.getSideBar().update(ZOMB, TCUtils.N + "Зомбей: " + ZOMB_CLR + zbs + " чел.");
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
	public static Arena getPlayerArena(final Player pl) {
		return PM.getOplayer(pl, PlHunter.class).arena();
	}
	
	public static Arena getNameArena(final String name) {
		return Main.activearenas.get(name);
	}
	
	public void msgEveryone(final String msg) {
		for (final PlHunter plh : pls.values()) {
			plh.getPlayer().sendMessage(msg);
		}
	}

	public void respZh(final Player p, final PlHunter ph) {
		p.playSound(p.getLocation(), Sound.ITEM_AXE_STRIP, 4, 1);
		for (final PotionEffect effect : p.getActivePotionEffects()) {
	        p.removePotionEffect(effect.getType());
		}
		p.closeInventory();
		p.setFireTicks(0);
		p.teleport(getRandSpawn());
		giveKit(p, ph);
	}
	
	public void giveKit(final Player p, final PlHunter ph) {
		final PlayerInventory inv = p.getInventory();
		final String pth = ph.zombie() ? "kits.zombie." + ph.zombKit() : "kits.player." + ph.survKit();
		final ConfigurationSection cs = KitsCmd.kits.getConfigurationSection(pth);
		inv.clear();
		inv.setHelmet(KitsCmd.getItemStack(pth + ".helm"));
		inv.setChestplate(KitsCmd.getItemStack(pth + ".chest"));
		inv.setLeggings(KitsCmd.getItemStack(pth + ".leggs"));
		inv.setBoots(KitsCmd.getItemStack(pth + ".boots"));
		for (int i = 0; cs.contains(String.valueOf(i)); i++) {
			inv.setItem(i, KitsCmd.getItemStack(pth + "." + i));
		}
		p.closeInventory();
		final int hp = cs.getInt("hp");
		p.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(hp);
		p.setHealth(hp);
	}
	
	public void waitScore(final PlHunter ph) {
		ph.score.getSideBar().reset().title(Main.PRFX)
			.add(" ")
			.add(TCUtils.N + "Карта: " + TCUtils.A + getName())
			.add(TCUtils.A + "=-=-=-=-=-=-=-")
			.add(TCUtils.N + "Набор для")
			.add(SKIT, SURV_CLR + "Игрока: " + TCUtils.P + ph.survKit())
			.add(ZKIT, ZOMB_CLR + "Зомби: " + TCUtils.P + ph.zombKit())
			.add(" ")
			.add(AMT, TCUtils.N + "Игроков: " + TCUtils.P + pls.size() + " чел.")
			.add(TCUtils.A + "=-=-=-=-=-=-=-")
			.add(LIMIT, TCUtils.N + "Нужно еще " + TCUtils.A + (min - pls.size()) + " чел.")
			.add(" ")
			.add("§e     ostrov77.ru").build();
	}
	
	/*public void lobbyScore(final PlHunter ph) {
		ph.score.getSideBar().reset().title(Main.PRFX)
			.add(" ")
			.add(TCUtils.N + "Карта: " + TCUtils.P + getName())
			.add(TCUtils.A + "=-=-=-=-=-=-=-")
			.add(TCUtils.N + "Набор для")
			.add(SKIT, SURV_CLR + "Игрока: " + TCUtils.P + ph.survKit())
			.add(ZKIT, ZOMB_CLR + "Зомби: " + TCUtils.P + ph.zombKit())
			.add(" ")
			.add(AMT, TCUtils.N + "Игроков: " + TCUtils.P + pls.size() + " чел.")
			.add(TCUtils.A + "=-=-=-=-=-=-=-")
			.add(LIMIT, TCUtils.N + "До начала: " + TCUtils.P + time + " сек.")
			.add(" ")
			.add("§e     ostrov77.ru").build();
	}*/
	
	public void beginScore(final PlHunter ph) {
		ph.score.getSideBar().reset().title(Main.PRFX)
			.add(" ")
			.add(TCUtils.N + "Карта: " + TCUtils.A + getName())
			.add(TCUtils.A + "=-=-=-=-=-=-=-")
			.add(TCUtils.N + "Набор для")
			.add(SKIT, SURV_CLR + "Игрока: " + TCUtils.P + ph.survKit())
			.add(ZKIT, ZOMB_CLR + "Зомби: " + TCUtils.P + ph.zombKit())
			.add(" ")
			.add(AMT, TCUtils.N + "Игроков: " + TCUtils.P + pls.size() + " чел.")
			.add(TCUtils.A + "=-=-=-=-=-=-=-")
			.add(LIMIT, TCUtils.N + "Подготовка: " + TCUtils.A + time + " сек.")
			.add(" ")
			.add("§e     ostrov77.ru").build();
	}
	
	public void runnScore(final PlHunter ph) {
		final int zbs = getPlAmount(true);
		ph.score.getSideBar().reset().title(Main.PRFX)
			.add(" ")
			.add(TCUtils.N + "Карта: " + TCUtils.A + getName())
			.add(TCUtils.A + "=-=-=-=-=-=-=-")
			.add(" ")
			.add(TCUtils.N + "Роль: " + (ph.zombie() ? ZOMB_CLR + "Зомби" : SURV_CLR + "Игрок"))
			.add(TCUtils.N + "Набор: " + TCUtils.P + (ph.zombie() ? ph.zombKit() : ph.survKit()))
			.add(" ")
			.add(AMT, TCUtils.N + "Игроков: " + SURV_CLR + (pls.size() - zbs) + " чел.")
			.add(ZOMB, TCUtils.N + "Зомбей: " + ZOMB_CLR + zbs + " чел.")
			.add(" ")
			.add(TCUtils.A + "=-=-=-=-=-=-=-")
			.add(TCUtils.N + "До победы живых:")
			.add(LIMIT, TCUtils.P + ApiOstrov.secondToTime(time))
			.add(" ")
			.add("§e     ostrov77.ru").build();
	}
	
	public void endScore(final PlHunter ph, final boolean zwin) {
		ph.score.getSideBar().reset().title(Main.PRFX)
			.add(" ")
			.add(TCUtils.N + "Карта: " + TCUtils.A + getName())
			.add(TCUtils.A + "=-=-=-=-=-=-=-")
			.add(TCUtils.N + "Поздравляем!")
			.add(TCUtils.N + "Выиграли: " + (zwin ? ZOMB_CLR + "Зомби" : SURV_CLR + "Игроки"))
			.add(" ")
			.add(AMT, TCUtils.N + "Игроков: " + TCUtils.P + pls.size() + " чел.")
			.add(TCUtils.A + "=-=-=-=-=-=-=-")
			.add(LIMIT, TCUtils.N + "До конца: " + TCUtils.A + time + " сек.")
			.add(" ")
			.add("§e     ostrov77.ru").build();
	}
}
