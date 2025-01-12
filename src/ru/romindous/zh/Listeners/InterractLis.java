package ru.romindous.zh.Listeners;

import net.kyori.adventure.key.Key;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.BlockType;
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
import org.bukkit.inventory.ItemType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import ru.komiss77.ApiOstrov;
import ru.komiss77.Ostrov;
import ru.komiss77.modules.items.ItemBuilder;
import ru.komiss77.modules.player.PM;
import ru.komiss77.utils.ItemUtil;
import ru.komiss77.utils.ScreenUtil;
import ru.komiss77.utils.TCUtil;
import ru.romindous.zh.Game.Arena;
import ru.romindous.zh.Game.GameState;
import ru.romindous.zh.Main;
import ru.romindous.zh.PlHunter;

import java.io.File;
import java.util.Arrays;

public class InterractLis implements Listener{
	
	@EventHandler
	public void onSwap(final PlayerSwapHandItemsEvent e) {
		final Arena ar = Arena.getPlayerArena(e.getPlayer());
		if (ar != null) {
			e.setCancelled(ar.getState() == GameState.WAITING || ar.getState() == GameState.BEGINING || ar.getState() == GameState.END);
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
	public void onInter(final PlayerInteractEvent e) {
		final Player p = e.getPlayer();
		final ItemStack it = e.getItem();
		final PlHunter ph = PM.getOplayer(p, PlHunter.class);
		final Arena ar = ph.arena();
		switch (e.getAction()) {
		case PHYSICAL:
			e.setCancelled(BlockType.FARMLAND.equals(e.getClickedBlock().getType().asBlockType()));
			break;
		case RIGHT_CLICK_AIR:
			if (ItemUtil.isBlank(it, false)) {
				break;
			} else if (ar != null && ar.getState() == GameState.RUNNING) {
				if (ph.zombie() && p.getInventory().getHeldItemSlot() == 0) {
					if (p.hasCooldown(p.getInventory().getItemInMainHand().getType())) {
						ScreenUtil.sendActionBarDirect(p, "§cВы еще набираете силы для прыжка");
					} else {
						p.setVelocity(p.getVelocity().add(p.getLocation().getDirection().multiply(1.5)));
						p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_SHOOT, 80, 0.8f);
						p.setCooldown(p.getInventory().getItemInMainHand().getType(), 200);
					}
					break;
				}
				switch (it.getType()) {
				case WRITTEN_BOOK:
					e.setCancelled(true);
					if (p.hasCooldown(it.getType())) {
						ScreenUtil.sendActionBarDirect(p, "§cВаша способность еще не готова");
					} else {
						final Location loc = p.getLocation();
						crtEnt(loc.clone().add(0,0,1), EntityType.ZOMBIE_VILLAGER, (byte) 2, 0.5f, (short) 900, "§cМертвец");
						crtEnt(loc.clone().add(0,0,-1), EntityType.ZOMBIE_VILLAGER, (byte) 2, 0.5f, (short) 900, "§cМертвец");
						crtEnt(loc.clone().add(1,0,0), EntityType.ZOMBIE_VILLAGER, (byte) 2, 0.5f, (short) 900, "§cМертвец");
						crtEnt(loc.clone().add(-1,0,0), EntityType.ZOMBIE_VILLAGER, (byte) 2, 0.5f, (short) 900, "§cМертвец");
						p.setCooldown(it.getType(), 600);
					}
					return;
				case NETHER_STAR:
					e.setCancelled(true);
					if (p.hasCooldown(it.getType())) {
						ScreenUtil.sendActionBarDirect(p, "§cВаша способность еще не готова");
					} else {
						final Location loc = p.getLocation();
						crtEnt(loc.clone().add(0,1,1), EntityType.VEX, (byte) 1, 0.4f, (short) 600, "§eСвятой Дух");
						crtEnt(loc.clone().add(0,1,-1), EntityType.VEX, (byte) 1, 0.4f, (short) 600, "§eСвятой Дух");
						crtEnt(loc.clone().add(1,1,0), EntityType.VEX, (byte) 1, 0.4f, (short) 600, "§eСвятой Дух");
						crtEnt(loc.clone().add(-1,1,0), EntityType.VEX, (byte) 1, 0.4f, (short) 600, "§eСвятой Дух");
						crtEnt(loc.clone().add(-1,1,1), EntityType.VEX, (byte) 1, 0.4f, (short) 600, "§eСвятой Дух");
						crtEnt(loc.clone().add(1,1,-1), EntityType.VEX, (byte) 1, 0.4f, (short) 600, "§eСвятой Дух");
						crtEnt(loc.clone().add(1,1,1), EntityType.VEX, (byte) 1, 0.4f, (short) 600, "§eСвятой Дух");
						crtEnt(loc.clone().add(-1,1,-1), EntityType.VEX, (byte) 1, 0.4f, (short) 600, "§eСвятой Дух");
						p.setCooldown(it.getType(), 600);
					}
					return;
				default:
					break;
				}
				break;
			} else {
				final String inm = it.getItemMeta().hasDisplayName()
						? TCUtil.deform(it.getItemMeta().displayName()) : "";
				if (inm.contains("Карты")) {
					e.setCancelled(true);
					p.playSound(p.getLocation(), Sound.BLOCK_BEEHIVE_EXIT, 80, 1);
					final Inventory inv = Bukkit.createInventory(p, 27 + (9 * (int) (((float) Main.nonactivearenas.size()) / 9.0f)), TCUtil.form("§6Меню выбора Карты"));
					inv.setContents(fillArInv(27 + (9 * (int) (((float) Main.nonactivearenas.size()) / 9.0f))));
					p.openInventory(inv);
				} else if (inm.contains("Набор")) {
					e.setCancelled(true);
					p.playSound(p.getLocation(), Sound.BLOCK_BEEHIVE_EXIT, 80, 1);
					p.performCommand("zkits choose");
				} else if (inm.contains("Выход")) {
					if (ItemUtil.is(it, ItemType.SLIME_BALL)) {
						e.setCancelled(true);
						p.performCommand("zh leave");
					} else if (ItemUtil.is(it, ItemType.MAGMA_CREAM)) {
						e.setCancelled(true);
						ApiOstrov.sendToServer(p, "lobby1", "");
					}
				} else {
					e.setCancelled(!p.isOp() || !(p.getGameMode() == GameMode.CREATIVE));
				}
			}
			break;
		case RIGHT_CLICK_BLOCK:
			if (ItemUtil.isBlank(it, false)) {
				break;
			} else if (ar != null && ar.getState() == GameState.RUNNING) {
				switch (it.getType()) {
				case WRITTEN_BOOK:
					e.setCancelled(true);
					if (p.hasCooldown(it.getType())) {
						ScreenUtil.sendActionBarDirect(p, "§cВаша способность еще не готова");
					} else {
						final Location loc = p.getLocation();
						crtEnt(loc.clone().add(0,0,1), EntityType.ZOMBIE_VILLAGER, (byte) 2, 0.5f, (short) 900, "§cМертвец");
						crtEnt(loc.clone().add(0,0,-1), EntityType.ZOMBIE_VILLAGER, (byte) 2, 0.5f, (short) 900, "§cМертвец");
						crtEnt(loc.clone().add(1,0,0), EntityType.ZOMBIE_VILLAGER, (byte) 2, 0.5f, (short) 900, "§cМертвец");
						crtEnt(loc.clone().add(-1,0,0), EntityType.ZOMBIE_VILLAGER, (byte) 2, 0.5f, (short) 900, "§cМертвец");
						p.setCooldown(it.getType(), 600);
					}
					return;
				case NETHER_STAR:
					e.setCancelled(true);
					if (p.hasCooldown(it.getType())) {
						ScreenUtil.sendActionBarDirect(p, "§cВаша способность еще не готова");
					} else {
						final Location loc = p.getLocation();
						crtEnt(loc.clone().add(0,1,1), EntityType.VEX, (byte) 1, 0.4f, (short) 600, "§eСвятой Дух");
						crtEnt(loc.clone().add(0,1,-1), EntityType.VEX, (byte) 1, 0.4f, (short) 600, "§eСвятой Дух");
						crtEnt(loc.clone().add(1,1,0), EntityType.VEX, (byte) 1, 0.4f, (short) 600, "§eСвятой Дух");
						crtEnt(loc.clone().add(-1,1,0), EntityType.VEX, (byte) 1, 0.4f, (short) 600, "§eСвятой Дух");
						crtEnt(loc.clone().add(-1,1,1), EntityType.VEX, (byte) 1, 0.4f, (short) 600, "§eСвятой Дух");
						crtEnt(loc.clone().add(1,1,-1), EntityType.VEX, (byte) 1, 0.4f, (short) 600, "§eСвятой Дух");
						crtEnt(loc.clone().add(1,1,1), EntityType.VEX, (byte) 1, 0.4f, (short) 600, "§eСвятой Дух");
						crtEnt(loc.clone().add(-1,1,-1), EntityType.VEX, (byte) 1, 0.4f, (short) 600, "§eСвятой Дух");
						p.setCooldown(it.getType(), 600);
					}
					return;
				default:
					break;
				}
				e.setCancelled(Tag.LOGS.isTagged(e.getClickedBlock().getType()));
				break;
			} else {
				final String inm = it.getItemMeta().hasDisplayName()
					? TCUtil.deform(it.getItemMeta().displayName()) : "";
				if (inm.contains("Карты")) {
					e.setCancelled(true);
					p.playSound(p.getLocation(), Sound.BLOCK_BEEHIVE_EXIT, 80, 1);
					final Inventory inv = Bukkit.createInventory(p, 27 + (9 * (int) (((float) Main.nonactivearenas.size()) / 9.0f)), TCUtil.form("§6Меню выбора Карты"));
					inv.setContents(fillArInv(27 + (9 * (int) (((float) Main.nonactivearenas.size()) / 9.0f))));
					p.openInventory(inv);
				} else if (inm.contains("Набор")) {
					e.setCancelled(true);
					p.playSound(p.getLocation(), Sound.BLOCK_BEEHIVE_EXIT, 80, 1);
					p.performCommand("zkits choose");
				} else if (inm.contains("Выход")) {
					if (ItemUtil.is(it, ItemType.SLIME_BALL)) {
						e.setCancelled(true);
						p.performCommand("zh leave");
					} else if (ItemUtil.is(it, ItemType.MAGMA_CREAM)) {
						e.setCancelled(true);
						ApiOstrov.sendToServer(p, "lobby1", "");
					}
				} else {
					e.setCancelled(!p.isOp() || !(p.getGameMode() == GameMode.CREATIVE));
				}
			}
			break;
		default:
			e.setCancelled(!e.getPlayer().isOp() || !(e.getPlayer().getGameMode() == GameMode.CREATIVE));
			break;
		}
	}

	private ItemStack[] fillArInv(int slots) {
		final YamlConfiguration arenas = YamlConfiguration.loadConfiguration(new File(Main.plug.getDataFolder() + File.separator + "arenas.yml"));
		final ItemStack[] loot = new ItemStack[slots];
		int used = 0;
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
				loot[i] = new ItemBuilder(ItemType.LIGHT_GRAY_STAINED_GLASS_PANE).name("§8-=-=-=-=-").build();
				break;
			case 4:
				loot[i] = new ItemBuilder(ItemType.LEATHER).name("§6Выбор Карты").build();
				break;
			default:
				if (i > (slots - 10)) {
					loot[i] = new ItemBuilder(ItemType.LIGHT_GRAY_STAINED_GLASS_PANE).name("§8-=-=-=-=-").build();
				} else if (used < Main.nonactivearenas.size()) {
					if (Arena.getNameArena(Main.nonactivearenas.get(used)) != null) {
						final Arena ar = Arena.getNameArena(Main.nonactivearenas.get(used));
						switch (ar.getState()) {
							case WAITING, BEGINING:
								final int pls = ar.getPlAmount(null);
								loot[i] = new ItemBuilder(ItemType.YELLOW_CONCRETE_POWDER).name("§e" + Main.nonactivearenas.get(used))
									.lore("").lore("§6Игроки: " + (pls < ar.getMin() ? pls + " из " + ar.getMin() : pls + " из " + ar.getMax())).build();
								break;
							default:
								loot[i] = new ItemBuilder(ItemType.RED_CONCRETE_POWDER).name("§c" + Main.nonactivearenas.get(used))
									.lore(Arrays.asList("", "§4Идет Игра", "", "§7Нажмите для наблюдения!")).build();
								break;
						}
					} else {
						loot[i] = new ItemBuilder(ItemType.GREEN_CONCRETE_POWDER).name("§a" + Main.nonactivearenas.get(used))
							.lore("").lore("§2Ожидание (§7" + arenas.getInt("arenas." + Main.nonactivearenas.get(used) + ".min") + "§2)").build();
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
		le.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(spd);
		le.getAttribute(Attribute.MAX_HEALTH).setBaseValue(hlth);
		le.setHealth(hlth);
		le.customName(TCUtil.form(nm));
		le.setCustomNameVisible(false);
		le.setTicksLived(1);
		Arrays.fill(le.getEquipment().getArmorContents(), null);
		le.getEquipment().setItemInMainHand(null);
		le.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 1000000, 1, true, false));
		final Sound snd = Ostrov.registries.SOUNDS.get(Key.key("entity." + et.toString().toLowerCase() + ".hurt"));
		if (snd != null) loc.getWorld().playSound(loc, snd, 0.5f, 0.8f);
		Ostrov.sync(() -> le.remove(), tm);
	}
}
