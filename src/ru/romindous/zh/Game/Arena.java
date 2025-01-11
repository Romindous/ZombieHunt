package ru.romindous.zh.Game;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.HumanEntity;
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
import ru.komiss77.enums.Game;
import ru.komiss77.enums.Stat;
import ru.komiss77.modules.games.GM;
import ru.komiss77.modules.player.PM;
import ru.komiss77.utils.*;
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

	public static final String SURV_CLR = "§a", ZOMB_CLR = "§c", ZKIT = "zkit", SKIT = "skit", AMT = "amt", LIMIT = "rem", ZOMB = "zbs";
	private static final PlHunter[] eps = new PlHunter[0];
	
	public Arena(final String name, final int min, final int max, final Location[] spawns) {
		this.max = max;
		this.min = min;
		this.name = name;
		this.spawns = spawns;
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
		return ClassUtil.rndElmt(spawns);
	}
	
	public Collection<PlHunter> getSpcs() {
		return spcs.values();
	}

	public void removePl(final Player p) {
		final PlHunter ph = pls.remove(p.getName());
		if (ph == null) return;

		switch (getState()) {
		case WAITING:
			GM.sendArenaData(Game.ZH, this.name, ru.komiss77.enums.GameState.ОЖИДАНИЕ, pls.size(), TCUtil.N + "[§6Инфекция§7]", 
				"§2Ожидание", " ", TCUtil.N + "Игроков: §2" + pls.size() + TCUtil.N + "/§2" + min);
			p.sendMessage(Main.PRFX + TCUtil.N + "Ты больше не на карте " + TCUtil.A + getName());
			for (final PlHunter plh : pls.values()) {
				final Player pl = plh.getPlayer();
				ScreenUtil.sendActionBarDirect(pl, amtToHB());
				pl.sendMessage(Main.PRFX + TCUtil.P + p.getName() + TCUtil.N + " вышел с карты!");
				plh.score.getSideBar().update(AMT, TCUtil.N + "Игроков: " + TCUtil.P + pls.size() + " чел.")
					.update(LIMIT, TCUtil.N + "Нужно еще " + TCUtil.A + (min - pls.size()) + " чел.");
			}
			if (pls.size() == 0) Main.endArena(this);
			break;
		case BEGINING:
			p.sendMessage(Main.PRFX + TCUtil.N + "Ты больше не на карте " + TCUtil.A + getName());
			for (final PlHunter plh : pls.values()) {
				final Player pl = plh.getPlayer();
				ScreenUtil.sendActionBarDirect(pl, amtToHB());
				pl.sendMessage(Main.PRFX + TCUtil.P + p.getName() + TCUtil.N + " вышел с карты!");
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
				GM.sendArenaData(Game.ZH, this.name, ru.komiss77.enums.GameState.ОЖИДАНИЕ, pls.size(), TCUtil.N + "[§6Инфекция§7]", "§2Ожидание", " ", TCUtil.N + "Игроков: §2" + pls.size() + TCUtil.N + "/§2" + min);
			} else {
				GM.sendArenaData(Game.ZH, this.name, ru.komiss77.enums.GameState.СТАРТ, pls.size(), TCUtil.N + "[§6Инфекция§7]", "§6Скоро старт!", " ", TCUtil.N + "Игроков: §6" + pls.size() + TCUtil.N + "/§6" + max);
				for (final PlHunter plh : pls.values()) {
					plh.score.getSideBar().update(AMT, TCUtil.N + "Игроков: " + TCUtil.P + pls.size() + " чел.");
				}
			}
			break;
		case RUNNING:
			ph.addStat(Stat.ZH_loose, 1);
			p.sendMessage(Main.PRFX + TCUtil.N + "Ты больше не на карте " + TCUtil.A + getName());
			for (final PlHunter plh : spcs.values()) {
				plh.getPlayer().sendMessage(Main.PRFX + TCUtil.P + p.getName() + TCUtil.N + " вышел из игры");
			}
			if (ph.zombie()) {
				final int amt = getPlAmount(true);
				if (amt == 0) {
					final PlHunter pnz = ClassUtil.rndElmt(pls.values().toArray(eps));
					for (final PlHunter plh : pls.values()) {
						final Player pl = plh.getPlayer();
						pl.sendMessage(Main.PRFX + TCUtil.P + p.getName() + TCUtil.N + " вышел из игры, и");
						pl.sendMessage(TCUtil.P + pnz.nik + TCUtil.N + " неожиданно превратился в §4Зомби" + TCUtil.N + "!");
					}
					zombifyPl(pnz.getPlayer(), pnz);
				} else {
					for (final PlHunter plh : pls.values()) {
						final Player pl = plh.getPlayer();
						pl.sendMessage(Main.PRFX + TCUtil.P + p.getName() + TCUtil.N + " вышел из игры!");
						plh.score.getSideBar().update(ZOMB, TCUtil.N + "Зомбей: " + ZOMB_CLR + amt + " чел.");
					}
				}
			} else {
				final int amt = getPlAmount(false);
				if (amt == 0) {
					task.cancel();
					for (final PlHunter plh : pls.values()) {
						final Player pl = plh.getPlayer();
						pl.sendMessage(Main.PRFX + TCUtil.P + p.getName() + TCUtil.N + " вышел из игры!");
						ScreenUtil.sendTitleDirect(pl, "§4Зомби §6победили!", TCUtil.N + "Человеческая расса уничтожена...", 10, 40, 20);
					}
					countEnd(true);
				} else {
					for (final PlHunter plh : pls.values()) {
						final Player pl = plh.getPlayer();
						pl.sendMessage(Main.PRFX + TCUtil.P + p.getName() + TCUtil.N + " вышел из игры!");
						plh.score.getSideBar().update(AMT, TCUtil.N + "Игроков: " + SURV_CLR + amt + " чел.");
					}
				}
			}
			break;
		case END:
			p.sendMessage(Main.PRFX + TCUtil.N + "Ты больше не на карте " + TCUtil.A + getName());
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
					final Arena oar = Arena.getPlayerArena(pl);
					if (oar == null || oar.getState() == GameState.WAITING) {
						p.hidePlayer(Main.plug, pl);
					} else if (oar.name.equals(name)) {
						pl.showPlayer(Main.plug, p);
					}
				}
				break;
			case RUNNING:
				addSpec(p);
			case END:
				p.sendMessage(Main.PRFX + "§cНа карте " + TCUtil.P + getName() + " §cуже идет игра!");
				return;
            }
			ph.arena(this);
			pls.put(p.getName(), ph);
			final String prm = ph.getTopPerm();
			ph.taq(Main.bfr('[', TCUtil.P + getName(), ']'), TCUtil.P,
				(prm.isEmpty() ? "" : Main.afr('(', "§e" + prm, ')')));
			for (final PlHunter plh : pls.values()) {
				final Player pl = plh.getPlayer();
				ScreenUtil.sendActionBarDirect(pl, amtToHB());
				pl.sendMessage(Main.PRFX + TCUtil.P + p.getName() + TCUtil.N + " зашел на карту!");
				if (getState() == GameState.WAITING) {
					plh.score.getSideBar().update(AMT, TCUtil.N + "Игроков: " + TCUtil.P + pls.size() + " чел.")
						.update(LIMIT, TCUtil.N + "Нужно еще " + TCUtil.A + (min - pls.size()) + " чел.");
				} else {
					plh.score.getSideBar().update(AMT, TCUtil.N + "Игроков: " + TCUtil.P + pls.size() + " чел.");
				}
			}
			p.getInventory().clear();
			p.getInventory().setItem(2, new ItemBuilder(Material.GOLDEN_HELMET).name("§eВыбор Набора").build());
			p.getInventory().setItem(6, new ItemBuilder(Material.SLIME_BALL).name("§cВыход").build());
			if (pls.size() < min) {
				GM.sendArenaData(Game.ZH, this.name, ru.komiss77.enums.GameState.ОЖИДАНИЕ, pls.size(), TCUtil.N + "[§6Инфекция§7]", 
					"§2Ожидание", " ", TCUtil.N + "Игроков: §2" + pls.size() + TCUtil.N + "/§2" + min);
			} else {
				if (pls.size() == min) countBegining();
				GM.sendArenaData(Game.ZH, this.name, ru.komiss77.enums.GameState.СТАРТ, pls.size(), TCUtil.N + "[§6Инфекция§7]", 
					"§6Скоро старт!", " ", TCUtil.N + "Игроков: §6" + pls.size() + TCUtil.N + "/§6" + max);
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
				p.hidePlayer(Main.plug, pl);
			}
		}
	}

	public void removeSpec(final Player p) {
		final PlHunter ph = spcs.remove(p.getName());
		if (ph == null) return;

		Main.lobbyPlayer(p, ph);
	}
	
	//отсчет в игре
	public void countBegining() {
		state = GameState.BEGINING;
		time = 30;
		for (final PlHunter ph : pls.values()) {
			final Player p = ph.getPlayer();
			for (Player pl : Bukkit.getOnlinePlayers()) {
				final Arena oar = getPlayerArena(pl);
				if (oar == null || oar.getState() == GameState.WAITING) {
					p.hidePlayer(Main.plug, pl);
				} else if (oar.name.equals(name)) {
					pl.showPlayer(Main.plug, p);
				}
			}
			p.teleport(getRandSpawn());
			p.setGameMode(GameMode.SURVIVAL);
			p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 20, 1);
			beginScore(ph);
		}

		GM.sendArenaData(Game.ZH, this.name, ru.komiss77.enums.GameState.ИГРА, pls.size(), TCUtil.N + "[§6Инфекция§7]", 
			"§cИдет Игра", " ", TCUtil.N + "Игроков: " + pls.size());
		task = new BukkitRunnable() {
			
			@Override
			public void run() {
				//scoreboard stuff
				final String rmn = TCUtil.N + "Подготовка: " + TCUtil.A + time + " сек.";
				for (final PlHunter plh : pls.values()) {
					plh.score.getSideBar().update(LIMIT, rmn);
				}
				switch (time) {
				case 10:
					for (final PlHunter plh : pls.values()) {
						final Player pl = plh.getPlayer();
						ScreenUtil.sendActionBarDirect(pl, "§6Через §d" + time + " §6секунд кто-то станет зомби!");
						pl.playSound(pl.getLocation(), Sound.BLOCK_DISPENSER_FAIL, 0.8f, 1.2f);
					}
					break;
				case 5:
					for (final PlHunter plh : pls.values()) {
						final Player pl = plh.getPlayer();
						ScreenUtil.sendTitleDirect(pl, "", "§6" + time, 4, 8, 4);
						ScreenUtil.sendActionBarDirect(pl, "§6Через §d" + time + " §6секунд кто-то станет зомби!");
						pl.playSound(pl.getLocation(), Sound.BLOCK_DISPENSER_FAIL, 0.8f, 1.2f);
					}
					break;
				case 4:
				case 3:
				case 2:
				case 1:
					for (final PlHunter plh : pls.values()) {
						final Player pl = plh.getPlayer();
						ScreenUtil.sendTitleDirect(pl, "", "§6" + time, 4, 8, 4);
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
		}.runTaskTimer(Main.plug, 0, 20);
	}

	public void countGame() {
		state = GameState.RUNNING;
		time = (short) ((5 + ((short) (pls.size() / 5))) * 60);
		final PlHunter[] zps = ClassUtil.shuffle(pls.values().toArray(eps));
		for (int i = pls.size() / 6 + 1; i != 0; i--) {
			final PlHunter zmb = zps[i];
			zmb.orgZomb(true);
			zmb.zombie(true);
			msgEveryone(Main.PRFX + "§c" + zmb.nik + TCUtil.N + " превратился в Зомби!");
		}
		for (final PlHunter plh : pls.values()) {
			final Player pl = plh.getPlayer();
			giveKit(pl, plh);
			runnScore(plh);
			if (plh.zombie()) {
				plh.taq(Main.bfr('[', TCUtil.P + getName(), ']'), ZOMB_CLR, "");
				pl.playSound(pl.getLocation(), Sound.ENTITY_ZOMBIE_AMBIENT, 20, 1);
				ScreenUtil.sendTitleDirect(pl, TCUtil.A + "Ты -" + ZOMB_CLR + "Зомби",
					TCUtil.P + "Убей всех игроков за " + TCUtil.A + (time / 60) + TCUtil.P + " минут!", 8, 40, 20);
			} else {
				plh.taq(Main.bfr('[', TCUtil.P + getName(), ']'), SURV_CLR, "");
				pl.playSound(pl.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 20, 1);
				ScreenUtil.sendTitleDirect(pl, TCUtil.A + "Ты - " + SURV_CLR + "Игрок",
					TCUtil.P + "Выживи на протяжении " + TCUtil.A + (time / 60) + TCUtil.P + " минут!", 8, 40, 20);
			}
		}
		
		task = new BukkitRunnable() {
			
			@Override
			public void run() {
				//scoreboard stuff
				final String rmn = TCUtil.P + TimeUtil.secondToTime(time);
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
						ScreenUtil.sendActionBarDirect(pl, "§6У зомби осталось §62 §6минуты!");
					}
					break;
				case 60:
					for (final PlHunter plh : pls.values()) {
						final Player pl = plh.getPlayer();
						ScreenUtil.sendActionBarDirect(pl, "§6Игроки победят через §61 §6минуту!");
					}
					break;
				case 30:
				case 10:
					for (final PlHunter plh : pls.values()) {
						final Player pl = plh.getPlayer();
						ScreenUtil.sendActionBarDirect(pl, "§6У зомби осталось §6" + time + " §6секунд!");
					}
					break;
				case 5:
				case 4:
				case 3:
				case 2:
				case 1:
					for (final PlHunter plh : pls.values()) {
						final Player pl = plh.getPlayer();
						ScreenUtil.sendTitleDirect(pl, "", "§6" + time, 4, 8, 4);
						pl.playSound(pl.getLocation(), Sound.BLOCK_DISPENSER_FAIL, 0.8f, 1.2f);
					}
					break;
				case 0:
					task.cancel();
					for (final PlHunter plh : pls.values()) {
						final Player pl = plh.getPlayer();
						ScreenUtil.sendTitleDirect(pl, SURV_CLR + "Игроки " + TCUtil.P + "победили!", TCUtil.N + "Человечество продолжает свою жизнь...", 8, 40, 20);
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
		}.runTaskTimer(Main.plug, 0, 20);
	}
	
	public void countEnd(final boolean zwin) {
		time = 6;
		state = GameState.END;
		if (zwin) {
			for (final PlHunter plh : pls.values()) {
				endScore(plh, true);
				plh.addStat(Stat.ZH_game, 1);
				plh.addStat(plh.orgZomb() ? Stat.ZH_win : Stat.ZH_loose, 1);
			}
		} else {
			for (final PlHunter plh : pls.values()) {
				endScore(plh, false);
				plh.addStat(Stat.ZH_game, 1);
				plh.addStat(plh.zombie() ? Stat.ZH_loose : Stat.ZH_win, 1);
			}
		}

		task = new BukkitRunnable() {
			@Override
			public void run() {
				final String rmn = TCUtil.N + "До конца: " + TCUtil.A + time + " сек.";
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
						final Firework fw = (Firework) pl.getWorld().spawnEntity(pl.getLocation(), EntityType.FIREWORK_ROCKET);
						final FireworkMeta fm = fw.getFireworkMeta();
						fm.addEffect(FireworkEffect.builder().withColor(Color.fromRGB(Ostrov.random.nextInt(16777000) + 100)).build());
						fw.setFireworkMeta(fm);
					}
					break;
				}
				time--;
			}
		}.runTaskTimer(Main.plug, 0, 20);
	}
	
	public void zombifyPl(final Player p, final PlHunter ph) {
		p.playSound(p.getLocation(), Sound.ENTITY_ZOMBIE_AMBIENT, 2, 1);
		ph.taq(Main.bfr('[', TCUtil.P + getName(), ']'), ZOMB_CLR, "");
		ph.zombie(true);
		p.closeInventory();
		p.setFireTicks(0);
		for (final PotionEffect eff : p.getActivePotionEffects()) {
	        p.removePotionEffect(eff.getType());
		}
		ph.kills0();
		giveKit(p, ph);
		msgEveryone(Main.PRFX + "§c" + p.getName() + TCUtil.N + " превратился в Зомби!");
		ApiOstrov.addStat(p, Stat.ZH_pdths);
		if (getPlAmount(false) == 0) {
			if (task != null) task.cancel();
			for (final PlHunter plh : pls.values()) {
				ScreenUtil.sendTitleDirect(plh.getPlayer(), ZOMB_CLR + "Зомби " + TCUtil.A + "победили!", TCUtil.N + "Человеческая расса уничтожена...", 8, 40, 20);
			}
			countEnd(true);
			return;
		}

		ScreenUtil.sendTitleDirect(p, TCUtil.A + "Ты -" + ZOMB_CLR + "Зомби",
			TCUtil.P + "Убей всех игроков за " + TCUtil.A + (time / 60) + TCUtil.P + " минут!", 8, 40, 20);
		final int zbs = getPlAmount(true);
		runnScore(ph);
		for (final PlHunter plh : pls.values()) {
			plh.score.getSideBar().update(AMT, TCUtil.N + "Игроков: " + SURV_CLR + (pls.size() - zbs) + " чел.");
			plh.score.getSideBar().update(ZOMB, TCUtil.N + "Зомбей: " + ZOMB_CLR + zbs + " чел.");
		}
	}

	//сколько игроков из скольки
	public String amtToHB() {
		return pls.size() < min ? 
			TCUtil.P + "На карте " + TCUtil.A + pls.size() + TCUtil.P + " игроков, нужно еще "
				+ TCUtil.A + (min - pls.size()) + TCUtil.P + " для начала" :
			TCUtil.P + "На карте " + TCUtil.A + pls.size() + TCUtil.P + " игроков, максимум: " + TCUtil.A + max;
	}
	
	//в игре ли игрок?
	public static Arena getPlayerArena(final HumanEntity pl) {
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
		p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_HURT, 4f, 0.6f);
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
			.add(TCUtil.N + "Карта: " + TCUtil.P + getName())
			.add(TCUtil.A + "=-=-=-=-=-=-=-")
			.add(TCUtil.N + "Набор для")
			.add(SKIT, SURV_CLR + "Игрока: " + TCUtil.P + ph.survKit())
			.add(ZKIT, ZOMB_CLR + "Зомби: " + TCUtil.P + ph.zombKit())
			.add(" ")
			.add(AMT, TCUtil.N + "Игроков: " + TCUtil.P + pls.size() + " чел.")
			.add(TCUtil.A + "=-=-=-=-=-=-=-")
			.add(LIMIT, TCUtil.N + "Нужно еще " + TCUtil.A + (min - pls.size()) + " чел.")
			.add(" ")
			.add("§e     ostrov77.ru").build();
	}
	
	public void beginScore(final PlHunter ph) {
		ph.score.getSideBar().reset().title(Main.PRFX)
			.add(" ")
			.add(TCUtil.N + "Карта: " + TCUtil.P + getName())
			.add(TCUtil.A + "=-=-=-=-=-=-=-")
			.add(TCUtil.N + "Набор для")
			.add(SKIT, SURV_CLR + "Игрока: " + TCUtil.P + ph.survKit())
			.add(ZKIT, ZOMB_CLR + "Зомби: " + TCUtil.P + ph.zombKit())
			.add(" ")
			.add(AMT, TCUtil.N + "Игроков: " + TCUtil.P + pls.size() + " чел.")
			.add(TCUtil.A + "=-=-=-=-=-=-=-")
			.add(LIMIT, TCUtil.N + "Подготовка: " + TCUtil.A + time + " сек.")
			.add(" ")
			.add("§e     ostrov77.ru").build();
	}
	
	public void runnScore(final PlHunter ph) {
		final int zbs = getPlAmount(true);
		ph.score.getSideBar().reset().title(Main.PRFX)
			.add(" ")
			.add(TCUtil.N + "Карта: " + TCUtil.P + getName())
			.add(TCUtil.A + "=-=-=-=-=-=-=-")
			.add(" ")
			.add(TCUtil.N + "Роль: " + (ph.zombie() ? ZOMB_CLR + "Зомби" : SURV_CLR + "Игрок"))
			.add(TCUtil.N + "Набор: " + TCUtil.P + (ph.zombie() ? ph.zombKit() : ph.survKit()))
			.add(" ")
			.add(AMT, TCUtil.N + "Игроков: " + SURV_CLR + (pls.size() - zbs) + " чел.")
			.add(ZOMB, TCUtil.N + "Зомбей: " + ZOMB_CLR + zbs + " чел.")
			.add(" ")
			.add(TCUtil.A + "=-=-=-=-=-=-=-")
			.add(TCUtil.N + "До победы живых:")
			.add(LIMIT, TCUtil.P + TimeUtil.secondToTime(time))
			.add(" ")
			.add("§e     ostrov77.ru").build();
	}
	
	public void endScore(final PlHunter ph, final boolean zwin) {
		ph.score.getSideBar().reset().title(Main.PRFX)
			.add(" ")
			.add(TCUtil.N + "Карта: " + TCUtil.P + getName())
			.add(TCUtil.A + "=-=-=-=-=-=-=-")
			.add(TCUtil.N + "Поздравляем!")
			.add(TCUtil.N + "Выиграли: " + (zwin ? ZOMB_CLR + "Зомби" : SURV_CLR + "Игроки"))
			.add(" ")
			.add(AMT, TCUtil.N + "Игроков: " + TCUtil.P + pls.size() + " чел.")
			.add(TCUtil.A + "=-=-=-=-=-=-=-")
			.add(LIMIT, TCUtil.N + "До конца: " + TCUtil.A + time + " сек.")
			.add(" ")
			.add("§e     ostrov77.ru").build();
	}
}
