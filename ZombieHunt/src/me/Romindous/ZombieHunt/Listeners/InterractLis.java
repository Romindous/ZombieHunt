package me.Romindous.ZombieHunt.Listeners;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import me.Romindous.ZombieHunt.Main;
import me.Romindous.ZombieHunt.Game.Arena;
import me.Romindous.ZombieHunt.Game.GameState;
import me.Romindous.ZombieHunt.Messages.EntMeta;
import me.Romindous.ZombieHunt.Messages.TitleManager;
import ru.komiss77.ApiOstrov;

public class InterractLis implements Listener{
	
	@EventHandler
	public void onSwap(final PlayerSwapHandItemsEvent e) {
		final Arena ar = Arena.getPlayerArena(e.getPlayer().getName());
		if (ar != null) {
			e.setCancelled(ar.getState() == GameState.LOBBY_WAIT || ar.getState() == GameState.LOBBY_START || ar.getState() == GameState.BEGINING || ar.getState() == GameState.END);
		} else {
			e.setCancelled(true);
		}
	}
	
	@EventHandler
	public void onDrop(PlayerDropItemEvent e) {
		e.setCancelled(!e.getPlayer().isOp());
	}
	
	@EventHandler
	public void onPickup(EntityPickupItemEvent e) {
		e.setCancelled(e.getEntity() instanceof Player && !e.getEntity().isOp());
	}
	
	@EventHandler
	public void onInter(PlayerInteractEvent e) {
		final Player p = e.getPlayer();
		final ItemStack it = e.getItem();
		final Arena ar = Arena.getPlayerArena(p.getName());
		switch (e.getAction()) {
		case PHYSICAL:
			e.setCancelled(e.getClickedBlock().getType() == Material.FARMLAND);
			break;
		case RIGHT_CLICK_AIR:
			
			if (it == null || it.getType() == Material.AIR) {
				break;
			} else if (ar != null && ar.getState() == GameState.RUNNING) {
				if (ar.isZombie(p.getName()) && p.getInventory().getHeldItemSlot() == 0) {
					if (EntMeta.checkBol(p, "jp", true)) {
						p.setVelocity(p.getVelocity().add(p.getLocation().getDirection().multiply(1.5)));
						p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_SHOOT, 80, 0.8f);
						p.setMetadata("jp", new FixedMetadataValue(Main.plug, false));
						Bukkit.getScheduler().scheduleSyncDelayedTask(Main.plug, new Runnable() {
							@Override
							public void run() {
								p.setMetadata("jp", new FixedMetadataValue(Main.plug, true));
								TitleManager.sendAcBr(p, ChatColor.DARK_GREEN + "Вы готовы к прыжку", 30);
							}}, 200);
					} else {
						TitleManager.sendAcBr(p, ChatColor.RED + "Вы еще набираете силы для прыжка", 30);
					}
					break;
				}
				switch (it.getType()) {
				case WRITTEN_BOOK:
					e.setCancelled(true);
					if (EntMeta.checkBol(p, "sp", true)) {
						final Location loc = p.getLocation();
						crtEnt(loc.clone().add(0,0,1), EntityType.ZOMBIE_VILLAGER, (byte) 2, 0.5f, (short) 900, "§cМертвец");
						crtEnt(loc.clone().add(0,0,-1), EntityType.ZOMBIE_VILLAGER, (byte) 2, 0.5f, (short) 900, "§cМертвец");
						crtEnt(loc.clone().add(1,0,0), EntityType.ZOMBIE_VILLAGER, (byte) 2, 0.5f, (short) 900, "§cМертвец");
						crtEnt(loc.clone().add(-1,0,0), EntityType.ZOMBIE_VILLAGER, (byte) 2, 0.5f, (short) 900, "§cМертвец");
						p.setMetadata("sp", new FixedMetadataValue(Main.plug, false));
						Bukkit.getScheduler().scheduleSyncDelayedTask(Main.plug, new Runnable() {
							@Override
							public void run() {
								p.setMetadata("sp", new FixedMetadataValue(Main.plug, true));
								if (Main.activearenas.contains(ar)) {
									TitleManager.sendAcBr(p, ChatColor.LIGHT_PURPLE + "Ваша способность готова", 30);
								}
							}}, 600);
					} else {
						TitleManager.sendAcBr(p, ChatColor.RED + "Ваша способность еще не готова", 30);
					}
					return;
				case NETHER_STAR:
					e.setCancelled(true);
					if (EntMeta.checkBol(p, "sp", true)) {
						final Location loc = p.getLocation();
						crtEnt(loc.clone().add(0,1,1), EntityType.VEX, (byte) 1, 0.4f, (short) 600, "§eСвятой Дух");
						crtEnt(loc.clone().add(0,1,-1), EntityType.VEX, (byte) 1, 0.4f, (short) 600, "§eСвятой Дух");
						crtEnt(loc.clone().add(1,1,0), EntityType.VEX, (byte) 1, 0.4f, (short) 600, "§eСвятой Дух");
						crtEnt(loc.clone().add(-1,1,0), EntityType.VEX, (byte) 1, 0.4f, (short) 600, "§eСвятой Дух");
						crtEnt(loc.clone().add(-1,1,1), EntityType.VEX, (byte) 1, 0.4f, (short) 600, "§eСвятой Дух");
						crtEnt(loc.clone().add(1,1,-1), EntityType.VEX, (byte) 1, 0.4f, (short) 600, "§eСвятой Дух");
						crtEnt(loc.clone().add(1,1,1), EntityType.VEX, (byte) 1, 0.4f, (short) 600, "§eСвятой Дух");
						crtEnt(loc.clone().add(-1,1,-1), EntityType.VEX, (byte) 1, 0.4f, (short) 600, "§eСвятой Дух");
						p.setMetadata("sp", new FixedMetadataValue(Main.plug, false));
						Bukkit.getScheduler().scheduleSyncDelayedTask(Main.plug, new Runnable() {
							@Override
							public void run() {
								p.setMetadata("sp", new FixedMetadataValue(Main.plug, true));
								if (Main.activearenas.contains(ar)) {
									TitleManager.sendAcBr(p, ChatColor.LIGHT_PURPLE + "Ваша способность готова", 30);
								}
							}}, 600);
					} else {
						TitleManager.sendAcBr(p, ChatColor.RED + "Ваша способность еще не готова", 30);
					}
					return;
				default:
					break;
				}
				break;
			} else if (it.getItemMeta().getDisplayName().contains("Карты")) {
				e.setCancelled(true);
				p.playSound(p.getLocation(), Sound.BLOCK_BEEHIVE_EXIT, 80, 1);
				final Inventory inv = Bukkit.createInventory(p, 27 + (9 * (int) (((float) Main.nonactivearenas.size()) / 9.0f)), ChatColor.AQUA + "Меню выбора Карты");
				inv.setContents(fillArInv(27 + (9 * (int) (((float) Main.nonactivearenas.size()) / 9.0f))));
				p.openInventory(inv);
			} else if (it.getItemMeta().getDisplayName().contains("Набор")) {
				e.setCancelled(true);
				p.playSound(p.getLocation(), Sound.BLOCK_BEEHIVE_EXIT, 80, 1);
				p.performCommand("zkits choose");
			} else if (it.getItemMeta().getDisplayName().contains("Выход")) {
				if (p.getInventory().getItemInMainHand().getType() == Material.SLIME_BALL) {
					e.setCancelled(true);
					p.performCommand("zh leave");
				} else if (p.getInventory().getItemInMainHand().getType() == Material.MAGMA_CREAM) {
					e.setCancelled(true);
					ApiOstrov.sendToServer(p, "lobby1", "");
				}
			} else {
				e.setCancelled(!p.isOp() || !(p.getGameMode() == GameMode.CREATIVE));
			}
			break;
		case RIGHT_CLICK_BLOCK:
			if (it == null || it.getType() == Material.AIR) {
				break;
			} else if (ar != null && ar.getState() == GameState.RUNNING) {
				switch (it.getType()) {
				case WRITTEN_BOOK:
					e.setCancelled(true);
					if (EntMeta.checkBol(p, "sp", true)) {
						final Location loc = p.getLocation();
						crtEnt(loc.clone().add(0,0,1), EntityType.ZOMBIE_VILLAGER, (byte) 2, 0.5f, (short) 900, "§cМертвец");
						crtEnt(loc.clone().add(0,0,-1), EntityType.ZOMBIE_VILLAGER, (byte) 2, 0.5f, (short) 900, "§cМертвец");
						crtEnt(loc.clone().add(1,0,0), EntityType.ZOMBIE_VILLAGER, (byte) 2, 0.5f, (short) 900, "§cМертвец");
						crtEnt(loc.clone().add(-1,0,0), EntityType.ZOMBIE_VILLAGER, (byte) 2, 0.5f, (short) 900, "§cМертвец");
						p.setMetadata("sp", new FixedMetadataValue(Main.plug, false));
						Bukkit.getScheduler().scheduleSyncDelayedTask(Main.plug, new Runnable() {
							@Override
							public void run() {
								p.setMetadata("sp", new FixedMetadataValue(Main.plug, true));
								if (Main.activearenas.contains(ar)) {
									TitleManager.sendAcBr(p, ChatColor.LIGHT_PURPLE + "Ваша способность готова", 30);
								}
							}}, 600);
					} else {
						TitleManager.sendAcBr(p, ChatColor.RED + "Ваша способность еще не готова", 30);
					}
					return;
				case NETHER_STAR:
					e.setCancelled(true);
					if (EntMeta.checkBol(p, "sp", true)) {
						final Location loc = p.getLocation();
						crtEnt(loc.clone().add(0,1,1), EntityType.VEX, (byte) 1, 0.4f, (short) 600, "§eСвятой Дух");
						crtEnt(loc.clone().add(0,1,-1), EntityType.VEX, (byte) 1, 0.4f, (short) 600, "§eСвятой Дух");
						crtEnt(loc.clone().add(1,1,0), EntityType.VEX, (byte) 1, 0.4f, (short) 600, "§eСвятой Дух");
						crtEnt(loc.clone().add(-1,1,0), EntityType.VEX, (byte) 1, 0.4f, (short) 600, "§eСвятой Дух");
						crtEnt(loc.clone().add(-1,1,1), EntityType.VEX, (byte) 1, 0.4f, (short) 600, "§eСвятой Дух");
						crtEnt(loc.clone().add(1,1,-1), EntityType.VEX, (byte) 1, 0.4f, (short) 600, "§eСвятой Дух");
						crtEnt(loc.clone().add(1,1,1), EntityType.VEX, (byte) 1, 0.4f, (short) 600, "§eСвятой Дух");
						crtEnt(loc.clone().add(-1,1,-1), EntityType.VEX, (byte) 1, 0.4f, (short) 600, "§eСвятой Дух");
						p.setMetadata("sp", new FixedMetadataValue(Main.plug, false));
						Bukkit.getScheduler().scheduleSyncDelayedTask(Main.plug, new Runnable() {
							@Override
							public void run() {
								p.setMetadata("sp", new FixedMetadataValue(Main.plug, true));
								if (Main.activearenas.contains(ar)) {
									TitleManager.sendAcBr(p, ChatColor.LIGHT_PURPLE + "Ваша способность готова", 30);
								}
							}}, 600);
					} else {
						TitleManager.sendAcBr(p, ChatColor.RED + "Ваша способность еще не готова", 30);
					}
					return;
				default:
					break;
				}
				e.setCancelled(e.getClickedBlock().getType().toString().contains("LOG") || e.getClickedBlock().getType().toString().contains("WOOD"));
				break;
			} else if (it.getItemMeta().getDisplayName().contains("Карты")) {
				e.setCancelled(true);
				p.playSound(p.getLocation(), Sound.BLOCK_BEEHIVE_EXIT, 80, 1);
				final Inventory inv = Bukkit.createInventory(p, 27 + (9 * (int) (((float) Main.nonactivearenas.size()) / 9.0f)), ChatColor.AQUA + "Меню выбора Карты");
				inv.setContents(fillArInv(27 + (9 * (int) (((float) Main.nonactivearenas.size()) / 9.0f))));
				p.openInventory(inv);
			} else if (it.getItemMeta().getDisplayName().contains("Набор")) {
				e.setCancelled(true);
				p.playSound(p.getLocation(), Sound.BLOCK_BEEHIVE_EXIT, 80, 1);
				p.performCommand("zkits choose");
			} else if (it.getItemMeta().getDisplayName().contains("Выход")) {
				if (p.getInventory().getItemInMainHand().getType() == Material.SLIME_BALL) {
					e.setCancelled(true);
					p.performCommand("zh leave");
				} else if (it.getType() == Material.MAGMA_CREAM) {
					e.setCancelled(true);
					ApiOstrov.sendToServer(p, "lobby1", "");
				}
			} else {
				e.setCancelled(!p.isOp() || !(p.getGameMode() == GameMode.CREATIVE));
			}
			break;
		default:
			e.setCancelled(!e.getPlayer().isOp() || !(e.getPlayer().getGameMode() == GameMode.CREATIVE));
			break;
		}
	}

	private ItemStack[] fillArInv(int slots) {
		YamlConfiguration arenas = YamlConfiguration.loadConfiguration(new File(Main.plug.getDataFolder() + File.separator + "arenas.yml"));
		ItemStack[] loot = new ItemStack[slots];
		byte used = 0;
		for (byte i = 0; i < slots; i++) {
			switch (i) {
			case 0:
			case 1:
			case 2:
			case 3:
			case 5:
			case 6:
			case 7:
			case 8:
				ItemStack item = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
				ItemMeta meta = item.getItemMeta();
				meta.setDisplayName(ChatColor.DARK_GRAY + "-=-=-=-=-");
				item.setItemMeta(meta);
				loot[i] = item;
				break;
				
			case 4:
				item = new ItemStack(Material.LEATHER);
				meta = item.getItemMeta();
				meta.setDisplayName(ChatColor.GOLD + "Выбор Карты");
				item.setItemMeta(meta);
				loot[i] = item;
				break;
				
			default:
				if (i > (slots - 10)) {
					item = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
					meta = item.getItemMeta();
					meta.setDisplayName(ChatColor.DARK_GRAY + "-=-=-=-=-");
					item.setItemMeta(meta);
					loot[i] = item;
				} else if (used < Main.nonactivearenas.size()) {
					if (Arena.getNameArena(Main.nonactivearenas.get(used)) != null) {
						Arena ar = Arena.getNameArena(Main.nonactivearenas.get(used));
						if (ar.getState() == GameState.LOBBY_WAIT || ar.getState() == GameState.LOBBY_START) {
							item = new ItemStack(Material.YELLOW_CONCRETE_POWDER);
							meta = item.getItemMeta();
							meta.setDisplayName(ChatColor.YELLOW + Main.nonactivearenas.get(used));
							meta.setLore(new ArrayList<String>(Arrays.asList("", ChatColor.GOLD + "Игроки: " + (ar.getPlAmount() < ar.getMin() ? ar.getPlAmount() + " из " + ar.getMin() : ar.getPlAmount() + " из " + ar.getMax()))));
							item.setItemMeta(meta);
							loot[i] = item;
						} else {
							item = new ItemStack(Material.RED_CONCRETE_POWDER);
							meta = item.getItemMeta();
							meta.setDisplayName(ChatColor.RED + Main.nonactivearenas.get(used));
							meta.setLore(new ArrayList<String>(Arrays.asList("", ChatColor.DARK_RED + "Идет Игра", "", ChatColor.GRAY + "Нажмите для наблюдения!")));
							item.setItemMeta(meta);
							loot[i] = item;
						}
					} else {
						item = new ItemStack(Material.GREEN_CONCRETE_POWDER);
						meta = item.getItemMeta();
						meta.setDisplayName(ChatColor.GREEN + Main.nonactivearenas.get(used));
						meta.setLore(new ArrayList<String>(Arrays.asList("", ChatColor.DARK_GREEN + "Ожидание (" + ChatColor.GRAY + arenas.getInt("arenas." + Main.nonactivearenas.get(used) + ".min") + ChatColor.DARK_GREEN + ")")));
						item.setItemMeta(meta);
						loot[i] = item;
					}
					used++;
				}
				break;
			}
		}
		return loot;
	}

	public void crtEnt(final Location loc, final EntityType et, final byte hlth, final float spd, final short tm, final String nm) {
		final LivingEntity le = (LivingEntity) loc.getWorld().spawnEntity(loc, et);
		le.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(spd);
		le.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(hlth);
		le.setHealth(hlth);
		le.setCustomName(nm);
		le.setCustomNameVisible(false);
		le.setTicksLived(1);
		Arrays.fill(le.getEquipment().getArmorContents(), null);
		le.getEquipment().getItemInMainHand().setType(Material.AIR);
		le.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 1000000, 1, true, false));
		loc.getWorld().playSound(loc, Sound.valueOf("ENTITY_" + et.toString() + "_HURT"), 0.5f, 0.8f);
		Bukkit.getScheduler().scheduleSyncDelayedTask(Main.plug, new Runnable() {
			@Override
			public void run() {
				le.remove();
			}
		}, tm);
	}
}
