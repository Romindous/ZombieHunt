package ru.romindous.zh.Listeners;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.bukkit.Sound;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import ru.komiss77.ApiOstrov;
import ru.komiss77.Ostrov;
import ru.komiss77.enums.Data;
import ru.komiss77.enums.Stat;
import ru.komiss77.events.BungeeDataRecieved;
import ru.komiss77.events.ChatPrepareEvent;
import ru.komiss77.events.LocalDataLoadEvent;
import ru.komiss77.modules.player.PM;
import ru.komiss77.utils.TCUtils;
import ru.romindous.zh.Game.Arena;
import ru.romindous.zh.Game.GameState;
import ru.romindous.zh.Main;
import ru.romindous.zh.PlHunter;

import java.util.Collection;

public class MainLis implements Listener{
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onBungee(final BungeeDataRecieved e) {
		final Player p = e.getPlayer();
        final String wantArena = e.getOplayer().getDataString(Data.WANT_ARENA_JOIN);
        if (!wantArena.isEmpty()) {
			final Arena ar = Arena.getPlayerArena(p);
            if (ar != null && ar.getState() == GameState.WAITING) {
            	ar.addPl(p);
            }
        }
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onJoin(final LocalDataLoadEvent e) {
		final Player p = e.getPlayer();
		Main.lobbyPlayer(p, PM.getOplayer(p, PlHunter.class));
    }

	@EventHandler
	public void onTarget(final EntityTargetLivingEntityEvent e) {
		if (e.getTarget() instanceof final Player pl) {
			final PlHunter ph = PM.getOplayer(pl, PlHunter.class);
			final Arena ar = ph.arena();
			switch (e.getEntityType()) {
			case ZOMBIE_VILLAGER:
				e.setCancelled(ar == null || ph.zombie());
				break;
			case VEX:
				e.setCancelled(ar == null || !ph.zombie());
				break;
			default:
				e.setCancelled(ar == null);
				break;
			}
		}
	}

	@EventHandler
	public void onOpen(InventoryOpenEvent e) {
		final Player pl = (Player) e.getPlayer();
		final Arena ar = Arena.getPlayerArena(pl);
		if (ar != null && ar.getState() == GameState.RUNNING) {
			e.setCancelled(!e.getPlayer().isOp());
			e.getPlayer().closeInventory();
		}
	}
	
	@EventHandler
	public void onLeave(PlayerQuitEvent e) {
		final Player pl = e.getPlayer();
		e.quitMessage(null);
		final Arena ar = Arena.getPlayerArena(pl);
		if (ar != null) ar.removePl(pl);
	}
	
	@EventHandler
	public void onFood(FoodLevelChangeEvent e) {
		e.setFoodLevel(19);
	}
	
	@EventHandler
	public void onDeath(PlayerRespawnEvent e) {
		if (Main.lobby != null) {
			e.setRespawnLocation(Main.lobby.getCenterLoc());
		}
	}
	
	@EventHandler(ignoreCancelled = false, priority = EventPriority.LOW)
	public void onDamage(final EntityDamageEvent e) {
		if (e.getEntity() instanceof final Player p) {
			final PlHunter ph = PM.getOplayer(p, PlHunter.class);
			final Arena ar = ph.arena();
            if (ar == null) {
				e.setCancelled(!(e instanceof EntityDamageByEntityEvent
					&& ((EntityDamageByEntityEvent) e).getDamager() instanceof Player
					&& ((EntityDamageByEntityEvent) e).getDamager().isOp()));
				return;
			}

			switch (ar.getState()) {
			case RUNNING:
				final LivingEntity ld = ApiOstrov.getDamager(e, true);
				if (ld == null) {
					if (p.getHealth() - e.getFinalDamage() < 0.1) {
						e.setCancelled(true);
						if (ph.zombie()) {
							ar.msgEveryone(Main.PRFX + "§c" + p.getName() + singleDie());
							ar.respZh(p, ph);
						} else {
							ar.msgEveryone(Main.PRFX + "§a" + p.getName() + singleDie());
							ph.addStat(Stat.ZH_pdths, 1);
							ar.zombifyPl(p, ph);
						}
					}
					return;
				} else {
					if (p.getHealth() - e.getFinalDamage() > 0d) return;
					final String ldn;
					if (ph.zombie()) {
						if (ld instanceof final Player dmgr) {
							final PlHunter oph = PM.getOplayer(dmgr, PlHunter.class);
							if (oph.arena() == null || oph.zombie()) {
								e.setCancelled(true);
								return;
							}
							ldn = dmgr.getName();
							dmgr.playSound(p.getLocation(), Sound.ENTITY_WITHER_BREAK_BLOCK, 20, 1.5f);
							oph.killsI();
						} else ldn = ld.customName() == null ? ApiOstrov.nrmlzStr(ld.getType().name()) : TCUtils.toString(ld.customName());
						ar.msgEveryone(plDie(p.getName(), ldn, false, (int) ld.getHealth() + 1));
						ar.respZh(p, ph);
					} else {
						if (ld instanceof final Player dmgr) {
							final PlHunter oph = PM.getOplayer(dmgr, PlHunter.class);
							if (oph.arena() == null || !oph.zombie()) {
								e.setCancelled(true);
								return;
							}
							ldn = dmgr.getName();
							dmgr.playSound(p.getLocation(), Sound.ENTITY_WITHER_BREAK_BLOCK, 20, 1.5f);
							oph.addStat(Stat.ZH_zklls, 1);
							oph.killsI();
						} else ldn = ld.customName() == null ?
							ApiOstrov.nrmlzStr(ld.getType().name()) : TCUtils.toString(ld.customName());
						ar.msgEveryone(plDie(p.getName(), ldn, true, (int) ld.getHealth() + 1));
						ph.addStat(Stat.ZH_pdths, 1);
						ar.zombifyPl(p, ph);
					}
				}
				break;
			case WAITING:
			case LOBBY_START:
			case BEGINING:
			case END:
				e.setCancelled(true);
				break;
			}
		} else if (e.getEntity() instanceof LivingEntity && ((Damageable) e.getEntity()).getHealth() - e.getFinalDamage() < 0.1) {
			e.getEntity().getWorld().playSound(e.getEntity().getLocation(), Sound.valueOf("ENTITY_" + e.getEntityType().toString() + "_DEATH"), 0.25f, 1);
			e.setCancelled(true);
			e.getEntity().remove();
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onChat(final ChatPrepareEvent e) {
		final Player p = e.getPlayer();
		final PlHunter ph = PM.getOplayer(p, PlHunter.class);
		final Arena ar = ph.arena();
		e.showLocal(false);
		if (ar == null) {
			final Component c = TCUtils.format(Main.bfr('{', TCUtils.P + ApiOstrov.toSigFigs(
				(float) ph.getStat(Stat.ZH_zklls) / (float) ph.getStat(Stat.ZH_pdths), (byte) 2), '}'));
			e.setSenderGameInfo(c);
			e.setViewerGameInfo(c);
		} else {
			switch (ar.getState()) {
				case WAITING, LOBBY_START:
					final Component c = TCUtils.format(Main.bfr('[', TCUtils.P + ar.getName(), ']'));
					e.setSenderGameInfo(c);
					e.setViewerGameInfo(c);
					break;
				default:
					e.sendProxy(false);
					break;
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
	public void onAChat(final AsyncChatEvent e) {
		final Player snd = e.getPlayer();
		final PlHunter ph = PM.getOplayer(snd, PlHunter.class);
		final Arena ar = ph.arena();
		final String msg = TCUtils.toString(e.message());
		//если на арене
		final String knd = Main.bfr('{', TCUtils.P + ApiOstrov.toSigFigs(
			(float) ph.getStat(Stat.ZH_zklls) / (float) ph.getStat(Stat.ZH_pdths), (byte) 2), '}');
		final Component modMsg;
		if (ar == null) {
			modMsg = TCUtils.format(knd + snd.getName() + Main.afr('[', TCUtils.A + "ЛОББИ", ']') + " §7§o≫ " + TCUtils.N + msg);
			for (final Audience au : e.viewers()) {
				au.sendMessage(modMsg);
			}
		} else {
			switch (ar.getState()) {
			case LOBBY_START:
			case WAITING:
				modMsg = TCUtils.format(knd + snd.getName() + Main.afr('[', TCUtils.P + ar.getName(), ']') + " §7§o≫ " + TCUtils.N + msg);
				for (final Audience au : e.viewers()) {
					au.sendMessage(modMsg);
				}
				break;
			case BEGINING:
			case RUNNING:
			case END:
				if (msg.length() > 1 && msg.charAt(0) == '!') {
					modMsg = TCUtils.format(TCUtils.N + "[Всем] "
						+ (ph.zombie() ? Arena.ZOMB_CLR : Arena.SURV_CLR) +
						snd.getName() + " §7§o≫ " + TCUtils.N + msg.substring(1));
					for (final PlHunter ors : ar.getPls()) {
						final Player pl = ors.getPlayer();
						pl.sendMessage(modMsg);
						pl.playSound(pl.getLocation(), Sound.BLOCK_GRINDSTONE_USE, 1f, 1.4f);
					}
					for (final PlHunter ors : ar.getSpcs()) {
						final Player pl = ors.getPlayer();
						pl.sendMessage(modMsg);
						pl.playSound(pl.getLocation(), Sound.BLOCK_GRINDSTONE_USE, 1f, 1.4f);
					}
				} else {
					modMsg = TCUtils.format((ph.zombie() ? Arena.ZOMB_CLR : Arena.SURV_CLR) +
						snd.getName() + " §7§o≫ " + TCUtils.N + msg);
					for (final PlHunter ors : ar.getPls()) {
						if (ors.zombie() == ph.zombie()) {
							final Player pl = ors.getPlayer();
							pl.sendMessage(modMsg);
							pl.playSound(pl.getLocation(), Sound.BLOCK_GRINDSTONE_USE, 1f, 1.2f);
						}
					}
				}
				break;
			}
		}
		e.viewers().clear();
	}
	
	public String singleDie() {
        return switch (Ostrov.random.nextInt(4)) {
            case 0 -> " §7скончался!";
            case 1 -> " §7откинул коньки!";
            case 2 -> " §7сдохся!";
            case 3 -> " §7умер!";
            default -> "";
        };
	}

	public String plDie(final String tgt, final String dmgr, final boolean byZH, final int hlth) {
        return switch (Ostrov.random.nextInt(4)) {
            case 0 -> Main.PRFX + (byZH ? Arena.ZOMB_CLR : Arena.SURV_CLR) + dmgr + " " + Main.bfr('[', "§c❤§6" + hlth, ']')
				+ "раздробил бошку " + (byZH ? Arena.SURV_CLR : Arena.ZOMB_CLR) + tgt + TCUtils.N + "!";
            case 1 -> Main.PRFX + (byZH ? Arena.ZOMB_CLR : Arena.SURV_CLR) + dmgr + " " + Main.bfr('[', "§c❤§6" + hlth, ']')
				+ "лишил " + (byZH ? Arena.SURV_CLR : Arena.ZOMB_CLR) + tgt + TCUtils.N + " конечностей!";
            case 2 -> Main.PRFX + (byZH ? Arena.ZOMB_CLR : Arena.SURV_CLR) + dmgr + " " + Main.bfr('[', "§c❤§6" + hlth, ']')
				+ "провел экзекуцию " + (byZH ? Arena.SURV_CLR : Arena.ZOMB_CLR) + tgt + TCUtils.N + "!";
            case 3 -> Main.PRFX + (byZH ? Arena.ZOMB_CLR : Arena.SURV_CLR) + dmgr + " " + Main.bfr('[', "§c❤§6" + hlth, ']')
				+ "анигилировал " + (byZH ? Arena.SURV_CLR : Arena.ZOMB_CLR) + tgt + TCUtils.N + "!";
            default -> "";
        };
	}
	
	@SuppressWarnings("deprecation")
	public static void sendSpigotMsg(final String msg, final HumanEntity p) {
		p.spigot().sendMessage(new net.md_5.bungee.api.chat.TextComponent(msg));
	}

	public static String getTopGroup(final Collection<String> grps) {
		if (grps.contains("xpanitely")) {
			return "Хранитель";
		} else if (grps.contains("builder")) {
			return "Строитель";
		} else if (grps.contains("supermoder")) {
			return "Архангел";
		} else if (grps.contains("moder-spy")) {
			return "Ангел";
		} else if (grps.contains("moder")) {
			return "Модератор";
		} else if (grps.contains("mchat")) {
			return "Чат-Модер";
		} else {
			return "N";
		}
	}
}
