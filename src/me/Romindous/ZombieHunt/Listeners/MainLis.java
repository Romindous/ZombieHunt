package me.Romindous.ZombieHunt.Listeners;

import java.util.Collection;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import io.papermc.paper.event.player.AsyncChatEvent;
import me.Romindous.ZombieHunt.Main;
import me.Romindous.ZombieHunt.Game.Arena;
import me.Romindous.ZombieHunt.Game.GameState;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import ru.komiss77.ApiOstrov;
import ru.komiss77.enums.Data;
import ru.komiss77.enums.Stat;
import ru.komiss77.events.BungeeDataRecieved;
import ru.komiss77.modules.player.PM;
import ru.komiss77.utils.TCUtils;

public class MainLis implements Listener{
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onBungee(final BungeeDataRecieved e) {
		final Player p = e.getPlayer();
        final String wantArena = e.getOplayer().getDataString(Data.WANT_ARENA_JOIN);
        if (!wantArena.isEmpty()) {
            if (Arena.getNameArena(wantArena) != null && Arena.getNameArena(wantArena).getState() == GameState.LOBBY_WAIT) {
            	Arena.getNameArena(wantArena).addPl(p.getName());
            }
        }
        Bukkit.getScheduler().runTaskLater(Main.plug, new Runnable() {
			@Override
			public void run() {
		        Main.data.setString(p.getName(), "prm", getTopGroup(e.getOplayer().getGroups()), "pls");
				for (final Player other : Bukkit.getOnlinePlayers()) {
					final Arena oar = Arena.getPlayerArena(other.getName());
					if (oar == null || oar.getState() == GameState.LOBBY_WAIT || oar.getState() == GameState.LOBBY_START) {
						final String prm = Main.data.getString(other.getName(), "prm", "pls");
						PM.getOplayer(p).tag("§7[" + (oar == null ? "§5ЛОББИ" : "§6" + oar.getName()) + "§7] ", "§2", (prm.length() > 1 ? " §7(§e" + prm + "§7)" : ""));
						p.showPlayer(Main.plug, other);
						other.showPlayer(Main.plug, p);
					} else {
						p.hidePlayer(Main.plug, other);
						other.hidePlayer(Main.plug, p);
					}
				}
			}
		}, 2);
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onJoin(final PlayerJoinEvent e) {
		final Player p = e.getPlayer();
		Main.data.chckIfExsts(p.getName(), "pls");
		Main.lobbyPlayer(p);
		e.joinMessage(Component.text(Main.pref() + "§6" + p.getName() + " §7зашел на под-сервер!"));
		final String title;
		switch (new Random().nextInt(4)) {
		case 0:
			title = "Добро пожаловать!";
			break;
		case 1:
			title = "Приятной игры!";
			break;
		case 2:
			title = "Желаем удачи!";
			break;
		case 3:
			title = "Развлекайтесь!";
			break;
		default:
			title = "";
			break;
		}
		
		p.sendPlayerListHeaderAndFooter(Component.text("§7<§6Инфекция§7>\n" + title), Component.text("§7Сейчас в игре: §6" + getPlaying() + " §7человек!"));
	}

	@EventHandler
	public void onTarget(final EntityTargetLivingEntityEvent e) {
		if (e.getTarget() instanceof Player) {
			final Arena ar = Arena.getPlayerArena(e.getTarget().getName());
			switch (e.getEntityType()) {
			case ZOMBIE_VILLAGER:
				e.setCancelled(ar == null || ar.isZombie(e.getTarget().getName()));
				break;
			case VEX:
				e.setCancelled(ar == null || !ar.isZombie(e.getTarget().getName()));
				break;
			default:
				break;
			}
		}
	}

	@EventHandler
	public void onOpen(InventoryOpenEvent e) {
		if (Arena.getPlayerArena(e.getPlayer().getName()) != null && Arena.getPlayerArena(e.getPlayer().getName()).getState() == GameState.RUNNING) {
			e.setCancelled(!e.getPlayer().isOp());
			e.getPlayer().closeInventory();
		}
	}
	
	@EventHandler
	public void onLeave(PlayerQuitEvent e) {
		e.quitMessage(null);
		if (Arena.getPlayerArena(e.getPlayer().getName()) != null) {
			Arena.getPlayerArena(e.getPlayer().getName()).removePl(e.getPlayer().getName());
		}
	}
	
	@EventHandler
	public void onFood(FoodLevelChangeEvent e) {
		e.setFoodLevel(19);
	}
	
	@EventHandler
	public void onDeath(PlayerRespawnEvent e) {
		if (Main.lobby != null) {
			if (Arena.getPlayerArena(e.getPlayer().getName()) == null) {
				e.setRespawnLocation(Main.lobby);
			} else {
				e.setRespawnLocation(Arena.getPlayerArena(e.getPlayer().getName()).getRandSpawn());
			}
			e.setRespawnLocation(Main.lobby);
			return;
		}
	}
	
	@EventHandler(ignoreCancelled = false, priority = EventPriority.LOW)
	public void onDamage(final EntityDamageEvent e) {
		if (e.getEntity() instanceof Player) {
			final Player p = (Player) e.getEntity();
			if (Arena.getPlayerArena(p.getName()) == null) {
				e.setCancelled(!(e instanceof EntityDamageByEntityEvent && ((EntityDamageByEntityEvent) e).getDamager() instanceof Player && ((EntityDamageByEntityEvent) e).getDamager().isOp()));
				return;
			}
			final Arena ar = Arena.getPlayerArena(p.getName());
			switch (ar.getState()) {
			case RUNNING:
				if (e instanceof EntityDamageByEntityEvent) {
					final EntityDamageByEntityEvent ee = (EntityDamageByEntityEvent) e;
					//hit by player
					if (ee.getDamager() instanceof Player) {
						final Player dmgr = (Player) ee.getDamager();
						final Arena dar = Arena.getPlayerArena(dmgr.getName());
						if (dar == null || !dar.getName().equalsIgnoreCase(ar.getName())) {
							e.setCancelled(true);
							return;
						}
						//if at low hp
						if (p.getHealth() - e.getFinalDamage() < 0.1) {
							e.setCancelled(true);
							//zombie kills player
							if (ar.isZombie(dmgr.getName()) && !ar.isZombie(p.getName())) {
								ar.msgEveryone(plDie(p.getName(), dmgr.getName(), true, (byte) Math.round(dmgr.getHealth())));
								ar.zombifyPl(p);
								ApiOstrov.addStat(dmgr, Stat.ZH_zklls);
								ar.addKls('z' + dmgr.getName());
								cntKll(dmgr.getName(), p.getName(), false);
								dmgr.playSound(p.getLocation(), Sound.ENTITY_WITHER_BREAK_BLOCK, 20, 1.5f);
							//player kills zombie
							} else if (!ar.isZombie(dmgr.getName()) && ar.isZombie(p.getName())) {
								ar.msgEveryone(plDie(p.getName(), dmgr.getName(), false, (byte) Math.round(dmgr.getHealth())));
								ar.respZh(p);
								ar.addKls('p' + dmgr.getName());
								cntKll(dmgr.getName(), p.getName(), true);
								dmgr.playSound(p.getLocation(), Sound.ENTITY_BLAZE_DEATH, 20, 1.5f);
							}
							return;
						}
						e.setCancelled(ar.getList(dmgr.getName()).contains(p.getName()));
					//hit by shot fired by player
					} else if (ee.getDamager() instanceof Projectile && ((Projectile) ee.getDamager()).getShooter() instanceof Player) {
						final Player dmgr = (Player) ((Projectile) ee.getDamager()).getShooter();
						final Arena dar = Arena.getPlayerArena(dmgr.getName());
						if (dar == null || !dar.getName().equalsIgnoreCase(ar.getName())) {
							e.setCancelled(true);
							return;
						}
						//if at low hp
						if (p.getHealth() - e.getFinalDamage() < 0.1) {
							e.setCancelled(true);
							//zombie kills player
							if (ar.isZombie(dmgr.getName()) && !ar.isZombie(p.getName())) {
								ar.msgEveryone(plDie(p.getName(), dmgr.getName(), true, (byte) Math.round(dmgr.getHealth())));
								ar.zombifyPl(p);
								ApiOstrov.addStat(dmgr, Stat.ZH_zklls);
								ar.addKls('z' + dmgr.getName());
								cntKll(dmgr.getName(), p.getName(), false);
								dmgr.playSound(p.getLocation(), Sound.ENTITY_WITHER_BREAK_BLOCK, 20, 1.5f);
							//player kills zombie
							} else if (!ar.isZombie(dmgr.getName()) && ar.isZombie(p.getName())) {
								ar.msgEveryone(plDie(p.getName(), dmgr.getName(), false, (byte) Math.round(dmgr.getHealth())));
								ar.respZh(p);
								ar.addKls('p' + dmgr.getName());
								cntKll(dmgr.getName(), p.getName(), true);
								dmgr.playSound(p.getLocation(), Sound.ENTITY_BLAZE_DEATH, 20, 1.5f);
							}
							return;
						}
						e.setCancelled(ar.getList(dmgr.getName()).contains(p.getName()));
					} else if (ee.getDamager() instanceof Damageable) {
						if (p.getHealth() - e.getFinalDamage() < 0.1) {
							e.setCancelled(true);
							final Damageable dg = (Damageable) ee.getDamager();
							if (ar.isZombie(p.getName())) {
								ar.msgEveryone(plDie(p.getName(), TCUtils.toString(dg.customName()), false, (byte) Math.round(dg.getHealth())));
								cntKll("", p.getName(), true);
								ar.respZh(p);
							} else {
								ar.msgEveryone(plDie(p.getName(), TCUtils.toString(dg.customName()), true, (byte) Math.round(dg.getHealth())));
								cntKll("", p.getName(), false);
								ar.zombifyPl(p);
							}
						}
					}
				} else if (p.getHealth() - e.getFinalDamage() < 0.1) {
					e.setCancelled(true);
					if (ar.isZombie(p.getName())) {
						ar.msgEveryone(Main.pref() + "§c" + p.getName() + singleDie());
						cntKll("", p.getName(), true);
						ar.respZh(p);
					} else {
						ar.msgEveryone(Main.pref() + "§a" + p.getName() + singleDie());
						cntKll("", p.getName(), false);
						ar.zombifyPl(p);
					}
				}
				break;
			case LOBBY_WAIT:
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
	
	public void cntKll(final String dmgr, final String ent, boolean isDmgrPl) {
		Bukkit.getScheduler().runTaskAsynchronously(Main.plug, new Runnable() {
			@Override
			public void run() {
				if (!dmgr.isEmpty()) {
					Main.data.chngNum(dmgr, isDmgrPl ? "pkls" : "zkls", 1, "pls");
				}
				Main.data.chngNum(ent, isDmgrPl ? "pdths" : "zdths", 1, "pls");
			}
		});
	}

	@EventHandler
	public void onChat(final AsyncChatEvent e) {
		final String m = ((TextComponent) e.message()).content();
		if (m.startsWith("/")) {
			return;
		}
		final Player send = e.getPlayer();
		if (Arena.getPlayerArena(send.getName()) == null) {
			for (final Audience rec : e.viewers()) {
	        	if (rec instanceof HumanEntity && Arena.getPlayerArena(((HumanEntity) rec).getName()) != null && Arena.getPlayerArena(((HumanEntity) rec).getName()).getState() == GameState.RUNNING) {
	        		e.viewers().remove(rec);
	        	}
	        }
			return;
		} else {
			final Arena ar = Arena.getPlayerArena(send.getName());
			if (m.startsWith("!") && ar.getState() == GameState.RUNNING) {
				for (final Audience rec : e.viewers()) {
					if (rec instanceof HumanEntity) {
						if (Arena.getPlayerArena(((HumanEntity) rec).getName()) == null) {
							continue;
						} else if (Arena.getPlayerArena(((HumanEntity) rec).getName()).getName().equalsIgnoreCase(ar.getName())) {
							sendSpigotMsg(Main.pref().replace('[', '<').replace(']', '>') + (ar.isZombie(send.getName()) ? "§4" : "§2") + send.getName() + "§7 ≫ " + m.replaceFirst("!", ""), (HumanEntity) rec);
						}
					}
				}
			} else {
				for (final Audience rec : e.viewers()) {
					if (rec instanceof HumanEntity) {
						if (Arena.getPlayerArena(((HumanEntity) rec).getName()) == null) {
							if (ar.getState() != GameState.RUNNING) {
								sendSpigotMsg(Main.pref().replace('[', '<').replace(']', '>') + "§2" + send.getName() + " §7[§6" + ar.getName() + "§7] ≫ " + m, (HumanEntity) rec);
							}
							continue;
						}
						switch (ar.getState()) {
						case LOBBY_WAIT:
						case LOBBY_START:
						case BEGINING:
							sendSpigotMsg(Main.pref().replace('[', '<').replace(']', '>') + "§2" + send.getName() + " §7[§6" + ar.getName() + "§7] ≫ " + m, (HumanEntity) rec);
							break;
						case RUNNING:
							if (Arena.getPlayerArena(((HumanEntity) rec).getName()).getName().equalsIgnoreCase(ar.getName()) && ar.isZombie(((HumanEntity) rec).getName()) == ar.isZombie(send.getName())) {
								sendSpigotMsg(Main.pref().replace('[', '<').replace(']', '>').replace("§6", (ar.isZombie(send.getName()) ? "§4" : "§2")) + "§6" + send.getName() + "§7 ≫ " + m, (HumanEntity) rec);
							}
							break;
						case END:
							if (Arena.getPlayerArena(((HumanEntity) rec).getName()).getName().equalsIgnoreCase(ar.getName())) {
								sendSpigotMsg(Main.pref().replace('[', '<').replace(']', '>') + "§2" + send.getName() + " §7[§6" + ar.getName() + "§7] ≫ " + m, (HumanEntity) rec);
							}
							break;
						default:
							break;
						}
					}
		        }
			}
		}
        e.viewers().clear();
    }
	
	/*@EventHandler
    public void Dchat(final DeluxeChatEvent e) {
        final Player p = e.getPlayer();
        final Arena ar = Arena.getPlayerArena(p.getName());
        if (ar != null && ar.getState() == GameState.RUNNING) {
            e.setCancelled(true);
            return;
        }
        final Iterator<Player> recipients = e.getRecipients().iterator();
        while (recipients.hasNext()) {
            final Player recipient = recipients.next();
            if (!recipient.getWorld().getName().equalsIgnoreCase(p.getWorld().getName())) {
                recipients.remove();
            }
        }
        if (ar != null) {
            e.getDeluxeFormat().setPrefix(Main.pref() + "§7<§6" + ar.getName() + "§7> ");
        }
    }*/
	
	public static short getPlaying() {
		short in = 0;
		for (final Arena ar : Main.activearenas) {
			in += ar.getPlAmount();
		}
		return in;
	}
	
	public String singleDie() {
		switch (new Random().nextInt(4)) {
		case 0:
			return " §7скончался!";
		case 1:
			return " §7откинул коньки!";
		case 2:
			return " §7сдохся!";
		case 3:
			return " §7умер!";
		default:
			return "";
		}
	}

	public String plDie(final String p, final String dmgr, final boolean byZH, final byte hlth) {
		switch (new Random().nextInt(4)) {
		case 0:
			return Main.pref() + (byZH ? "§c" : "§a") + dmgr + " §7[§c❤§6" + hlth + "§7] раздробил бошку " + (byZH ? "§a" : "§c") + p + "§7!";
		case 1:
			return Main.pref() + (byZH ? "§c" : "§a") + dmgr + " §7[§c❤§6" + hlth + "§7] лишил " + (byZH ? "§a" : "§c") + p + " §7конечностей!";
		case 2:
			return Main.pref() + (byZH ? "§c" : "§a") + dmgr + " §7[§c❤§6" + hlth + "§7] провел экзекуцию " + (byZH ? "§a" : "§c") + p + "§7!";
		case 3:
			return Main.pref() + (byZH ? "§c" : "§a") + dmgr + " §7[§c❤§6" + hlth + "§7] анигилировал " + (byZH ? "§a" : "§c") + p + "§7!";
		default:
			return "";
		}
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