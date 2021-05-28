package me.Romindous.ZombieHunt.Listeners;

import java.util.Iterator;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Damageable;
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
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import me.Romindous.ZombieHunt.Main;
import me.Romindous.ZombieHunt.Game.Arena;
import me.Romindous.ZombieHunt.Game.GameState;
import me.clip.deluxechat.events.DeluxeChatEvent;
import net.md_5.bungee.api.chat.TextComponent;
import ru.komiss77.Enums.Data;
import ru.komiss77.Events.BungeeStatRecieved;
import ru.komiss77.Managers.PM;
import ru.komiss77.Objects.Oplayer;

public class MainLis implements Listener{
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onBungee(BungeeStatRecieved e) {
        final String wantArena = PM.getOplayer(e.getPlayer().getName()).getBungeeData(Data.WANT_ARENA_JOIN);
        if (!wantArena.isEmpty()) {
            if (Arena.getNameArena(wantArena) != null && Arena.getNameArena(wantArena).getState() == GameState.LOBBY_WAIT) {
            	Arena.getNameArena(wantArena).addPl(e.getPlayer().getName());
            }
        }
        Bukkit.getScheduler().runTaskLater(Main.plug, new Runnable() {
			@Override
			public void run() {
		        Main.data.setString(e.getPlayer().getName(), "prm", getTopGroup(e.getOplayer()), "pls");
			}
		}, 2);
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onJoin(final PlayerJoinEvent e) {
		final Player p = e.getPlayer();
		Main.data.chckIfExsts(p.getName(), "pls");
		Main.lobbyPlayer(p);
		e.setJoinMessage(Main.pref() + "§6" + p.getName() + " §7зашел на под-сервер!");
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
		p.setPlayerListHeaderFooter("§7<§6Инфекция§7>\n" + title, "§7Сейчас в игре: §6" + getPlaying() + " §7человек!");
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
		e.setQuitMessage(null);
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
								ar.msgEveryone(plDie(p.getName(), dg.getCustomName(), false, (byte) Math.round(dg.getHealth())));
								cntKll("", p.getName(), true);
								ar.respZh(p);
							} else {
								ar.msgEveryone(plDie(p.getName(), dg.getCustomName(), true, (byte) Math.round(dg.getHealth())));
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
	public void onChat(final AsyncPlayerChatEvent e) {
		if (e.getMessage().startsWith("/")) {
			return;
		}
		final Player send = e.getPlayer();
		if (Arena.getPlayerArena(send.getName()) == null) {
			for (final Player rec : e.getRecipients()) {
	        	if (Arena.getPlayerArena(rec.getName()) != null && Arena.getPlayerArena(rec.getName()).getState() == GameState.RUNNING) {
	        		e.getRecipients().remove(rec);
	        	}
	        }
			return;
		} else {
			final Arena ar = Arena.getPlayerArena(send.getName());
			if (e.getMessage().startsWith("!") && ar.getState() == GameState.RUNNING) {
				for (final Player rec : e.getRecipients()) {
					if (Arena.getPlayerArena(rec.getName()) == null) {
						continue;
					} else if (Arena.getPlayerArena(rec.getName()).getName().equalsIgnoreCase(ar.getName())) {
						sendSpigotMsg(Main.pref().replace('[', '<').replace(']', '>') + (ar.isZombie(send.getName()) ? ChatColor.DARK_RED : ChatColor.DARK_GREEN) + send.getName() + ChatColor.GRAY + " ≫ " + e.getMessage().replaceFirst("!", ""), rec);
					}
				}
			} else {
				for (final Player rec : e.getRecipients()) {
					if (Arena.getPlayerArena(rec.getName()) == null) {
						if (ar.getState() != GameState.RUNNING) {
							sendSpigotMsg(Main.pref().replace('[', '<').replace(']', '>') + ChatColor.DARK_GREEN + send.getName() + ChatColor.GRAY + " [" + ChatColor.GOLD + ar.getName() + ChatColor.GRAY + "] ≫ " + e.getMessage(), rec);
						}
						continue;
					}
					switch (ar.getState()) {
					case LOBBY_WAIT:
					case LOBBY_START:
					case BEGINING:
						sendSpigotMsg(Main.pref().replace('[', '<').replace(']', '>') + ChatColor.DARK_GREEN + send.getName() + ChatColor.GRAY + " [" + ChatColor.GOLD + ar.getName() + ChatColor.GRAY + "] ≫ " + e.getMessage(), rec);
						break;
					case RUNNING:
						if (Arena.getPlayerArena(rec.getName()).getName().equalsIgnoreCase(ar.getName()) && ar.isZombie(rec.getName()) == ar.isZombie(send.getName())) {
							sendSpigotMsg(Main.pref().replace('[', '<').replace(']', '>').replace(ChatColor.GOLD + "", (ar.isZombie(send.getName()) ? ChatColor.DARK_RED + "" : ChatColor.DARK_GREEN) + "") + ChatColor.GOLD + send.getName() + ChatColor.GRAY + " ≫ " + e.getMessage(), rec);
						}
						break;
					case END:
						if (Arena.getPlayerArena(rec.getName()).getName().equalsIgnoreCase(ar.getName())) {
							sendSpigotMsg(Main.pref().replace('[', '<').replace(']', '>') + ChatColor.DARK_GREEN + send.getName() + ChatColor.GRAY + " [" + ChatColor.GOLD + ar.getName() + ChatColor.GRAY + "] ≫ " + e.getMessage(), rec);
						}
						break;
					default:
						break;
					}
		        }
			}
		}
        e.getRecipients().clear();
    }
	
	@EventHandler
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
    }
	
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
	
	public static void sendSpigotMsg(String msg, Player p) {
		p.spigot().sendMessage(new TextComponent(msg));
	}

	public static String getTopGroup(final Oplayer op) {
		if (op.groups.contains("xpanitely")) {
			return "Хранитель";
		} else if (op.groups.contains("builder")) {
			return "Строитель";
		} else if (op.groups.contains("supermoder")) {
			return "Архангел";
		} else if (op.groups.contains("moder-spy")) {
			return "Ангел";
		} else if (op.groups.contains("moder")) {
			return "Модератор";
		} else if (op.groups.contains("mchat")) {
			return "Чат-Модер";
		} else {
			return "N";
		}
	}
}
