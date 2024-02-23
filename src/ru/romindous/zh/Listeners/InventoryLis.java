package ru.romindous.zh.Listeners;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import ru.komiss77.modules.player.PM;
import ru.komiss77.utils.ItemBuilder;
import ru.komiss77.utils.ItemUtils;
import ru.komiss77.utils.TCUtils;
import ru.romindous.zh.Commands.KitsCmd;
import ru.romindous.zh.Game.Arena;
import ru.romindous.zh.Main;
import ru.romindous.zh.PlHunter;

public class InventoryLis implements Listener{

	@EventHandler
	public void onClick(final InventoryClickEvent e) {
		//клик на ничего? и предметы в инвентаре
		final ItemStack it = e.getCurrentItem();
		if (it == null) return;
		
		if (e.getClick() == ClickType.NUMBER_KEY || e.getClick() == ClickType.SWAP_OFFHAND) {
			e.setCancelled(true);
			e.getCursor().setType(Material.AIR);
			return;
		}
		
		if (e.getWhoClicked().getGameMode() == GameMode.SPECTATOR && it.getType() == Material.REDSTONE) {
			e.setCancelled(true);
			for (final Arena ar : Main.activearenas.values()) {
				ar.removeSpec((Player) e.getWhoClicked());
			}
			return;
		}

		final String nm = it.getItemMeta() == null || !it.getItemMeta().hasDisplayName()
			? "" : TCUtils.stripColor(it.getItemMeta().displayName());
		if ((nm.contains("Выбор") || nm.contains("Набор") || nm.contains("Выход"))) {
			e.setCancelled(true);
			e.getCursor().setType(Material.AIR);
			return;
		}
		
		final String inm = TCUtils.toString(e.getView().title());
		if (inm.contains("Карты")) {
			e.setCancelled(true);
			final Player p = (Player) e.getWhoClicked();
			switch (e.getCurrentItem().getType()) {
			case GREEN_CONCRETE_POWDER, YELLOW_CONCRETE_POWDER:
				p.performCommand("zh join " + nm);
				p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 80, 1);
				p.closeInventory();
				break;
			case RED_CONCRETE_POWDER:
				p.sendMessage(Main.PRFX + "§cНа этой карте уже идет игра!");
				p.sendMessage(Main.PRFX + "§7Помещаем вас в качестве зрителя");
				p.getInventory().clear();
				ItemStack item = new ItemStack(Material.REDSTONE);
				ItemMeta meta = item.getItemMeta();
				meta.displayName(TCUtils.format("§4Обратно в лобби"));
				item.setItemMeta(meta);
				p.getInventory().setItem(8, item);
				Arena.getNameArena(nm.substring(2)).addSpec(p);
				break;	
			default:
				break;
			}
		} else if (inm.contains("Наборы")) {
			final Player p = (Player) e.getWhoClicked();
			e.setCancelled(true);
			if (e.getCurrentItem().getType() == Material.LIGHT_GRAY_STAINED_GLASS_PANE) return;

			p.closeInventory();
			final ConfigurationSection plcs = KitsCmd.kits.getConfigurationSection("kits.player." + nm);
			if (plcs != null) {
				if (e.getClick() == ClickType.RIGHT) {
					final Inventory inv = Bukkit.createInventory(p, 27, TCUtils.format("§3Просмотр Набора"));
					inv.setContents(KitsCmd.fillEditInv("kits.player." + nm));
					inv.setItem(8, new ItemBuilder(Material.REDSTONE_TORCH).name("§8Назад").build());
					inv.setItem(5, inv.getItem(4));
					inv.setItem(6, new ItemBuilder(Material.APPLE).setAmount(inv.getItem(6).getAmount()).name("§6Здоровье:").build());
					p.openInventory(inv);
				} else {
					final String prm = plcs.getString("perm");
					if (prm != null && prm.charAt(0) != 'N' && !PM.getOplayer(p).hasGroup(prm)) {
						p.sendMessage(Main.PRFX + "§cВам нужно иметь донат " + TCUtils.A + transDon(prm) + "§c для игры с этим набором!");
						p.playSound(p.getLocation(), Sound.ITEM_BUCKET_EMPTY_LAVA, 2f, 0.8f);
						return;
					}
					p.playSound(p.getLocation(), Sound.ITEM_ARMOR_EQUIP_IRON, 2f, 1f);
					p.sendMessage(Main.PRFX + "Выбран набор " + TCUtils.P + nm + TCUtils.N + " для Игрока");
					PM.getOplayer(p, PlHunter.class).survKit(nm);
				}
				return;
			}

			final ConfigurationSection zbcs = KitsCmd.kits.getConfigurationSection("kits.zombie." + nm);
			if (zbcs != null) {
				e.setCancelled(true);
				if (e.getClick() == ClickType.RIGHT) {
					final Inventory inv = Bukkit.createInventory(p, 27, TCUtils.format("§3Просмотр Набора"));
					inv.setContents(KitsCmd.fillEditInv("kits.zombie." + nm));
					inv.setItem(8, new ItemBuilder(Material.REDSTONE_TORCH).name("§8Назад").build());
					inv.setItem(5, inv.getItem(4));
					inv.setItem(6, new ItemBuilder(Material.APPLE).setAmount(inv.getItem(6).getAmount()).name("§6Здоровье:").build());
					p.openInventory(inv);
				} else {
					final String prm = zbcs.getString("perm");
					if (prm != null && prm.charAt(0) != 'N' && !PM.getOplayer(p).hasGroup(prm)) {
						p.sendMessage(Main.PRFX + "§cВам нужно иметь донат " + TCUtils.A + transDon(prm) + "§c для игры с этим набором!");
						p.playSound(p.getLocation(), Sound.ITEM_BUCKET_EMPTY_LAVA, 2f, 0.8f);
						return;
					}
					p.playSound(p.getLocation(), Sound.ITEM_ARMOR_EQUIP_IRON, 2f, 1f);
					p.sendMessage(Main.PRFX + "Выбран набор " + TCUtils.P + nm + TCUtils.N + " для Зомби");
					PM.getOplayer(p, PlHunter.class).zombKit(nm);
				}
				return;
			}

			p.sendMessage(Main.PRFX + "§cТакого набора не существует!");
		} else if (inm.contains("Просмотр")) {
			if (nm.contains("Назад")) {
				e.getWhoClicked().closeInventory();
				((Player) e.getWhoClicked()).performCommand("zkits choose");
			} else {
				e.setCancelled(true);
			}
		} else if (inm.contains("Создание")) {
			switch (e.getCurrentItem().getType()) {
			case VILLAGER_SPAWN_EGG:
			case ZOMBIE_SPAWN_EGG:
			case LIGHT_GRAY_STAINED_GLASS_PANE:
				e.setCancelled(true);
				break;
			case BRICK:
				if (nm.contains("Без")) {
					e.setCancelled(true);
					e.getInventory().setItem(e.getSlot(), new ItemBuilder(Material.IRON_INGOT).name("§6Воин и выше").build());
				}
				break;
			case IRON_INGOT:
				if (nm.contains("Воин")) {
					e.setCancelled(true);
					e.getInventory().setItem(e.getSlot(), new ItemBuilder(Material.GOLD_INGOT).name("§6Герой и выше").build());
				}
				break;
			case GOLD_INGOT:
				if (nm.contains("Герой")) {
					e.setCancelled(true);
					e.getInventory().setItem(e.getSlot(), new ItemBuilder(Material.NETHERITE_INGOT).name("§6Легенда и выше").build());
				}
				break;
			case NETHERITE_INGOT:
				if (nm.contains("Легенда")) {
					e.setCancelled(true);
					e.getInventory().setItem(e.getSlot(), new ItemBuilder(Material.BRICK).name("§6Без привилегий").build());
				}
				break;
			case SPIDER_EYE:
				if (nm.contains("-1")) {
					e.setCancelled(true);
					if (e.getCurrentItem().getAmount() == 1) break;
					final ItemStack item = e.getCurrentItem();
					e.getClickedInventory().setItem(5, new ItemBuilder(item).setAmount(item.getAmount() - 1).build());
					e.getClickedInventory().setItem(6, new ItemBuilder(e.getClickedInventory().getItem(6)).setAmount(item.getAmount() - 1).build());
				}
				break;
			case BEETROOT:
				if (nm.contains("+1")) {
					e.setCancelled(true);
					final ItemStack item = e.getCurrentItem();
					e.getClickedInventory().setItem(5, new ItemBuilder(e.getClickedInventory().getItem(5)).setAmount(item.getAmount() + 1).build());
					e.getClickedInventory().setItem(6, new ItemBuilder(item).setAmount(item.getAmount() + 1).build());
				}
				break;
			case GREEN_WOOL:
				if (nm.contains("Готово")) {
					e.setCancelled(true);
					createKit(e.getClickedInventory().getContents());
					e.getWhoClicked().closeInventory();
					e.getWhoClicked().sendMessage(Main.PRFX + "§7Набор " + TCUtils.P + TCUtils.toString(
						e.getClickedInventory().getItem(13).getItemMeta().displayName()) + "§7 успешно создан!");
				}
				break;
			default:
				if (e.getClickedInventory().getSize() == 27) {
					switch (e.getSlot()) {
					case 0:
						e.setCancelled(e.getCursor().getType().getEquipmentSlot() != EquipmentSlot.HEAD);
						break;
					case 1:
						e.setCancelled(e.getCursor().getType().getEquipmentSlot() != EquipmentSlot.CHEST);
						break;
					case 2:
						e.setCancelled(e.getCursor().getType().getEquipmentSlot() != EquipmentSlot.LEGS);
						break;
					case 3:
						e.setCancelled(e.getCursor().getType().getEquipmentSlot() != EquipmentSlot.FEET);
						break;
					case 19:
						e.setCancelled(e.getCursor().getType() == Material.AIR);
						break;
					default:
						break;
					}
				}
				break;
			}
		}
	}
	
	private String transDon(final String s) {
        return switch (s) {
            case "warior" -> "Воин";
			case "hero" -> "Герой";
			case "legend" -> "Легенда";
            default -> "-=-=-";
        };
    }

	private static void createKit(final ItemStack[] loot) {
		final String path = loot[13].getType() == Material.VILLAGER_SPAWN_EGG ? 
			"kits.player." + TCUtils.toString(loot[13].getItemMeta().displayName()) 
			: "kits.zombie." + TCUtils.toString(loot[13].getItemMeta().displayName());
		KitsCmd.kits.removeKey(path);
		switch (loot[17].getType()) {
			case NETHERITE_INGOT -> KitsCmd.kits.set(path + ".perm", "legend");
			case GOLD_INGOT -> KitsCmd.kits.set(path + ".perm", "hero");
			case IRON_INGOT -> KitsCmd.kits.set(path + ".perm", "warior");
			default -> KitsCmd.kits.set(path + ".perm", "N");
		}
		KitsCmd.kits.set(path + ".hp", loot[5].getAmount());
		KitsCmd.kits.set(path + ".helm", ItemUtils.toString(loot[0], KitsCmd.split));
		KitsCmd.kits.set(path + ".chest", ItemUtils.toString(loot[1], KitsCmd.split));
		KitsCmd.kits.set(path + ".leggs", ItemUtils.toString(loot[2], KitsCmd.split));
		KitsCmd.kits.set(path + ".boots", ItemUtils.toString(loot[3], KitsCmd.split));
		for (int i = 19; i < 26; i++) {
			if (loot[i] == null) continue;
			KitsCmd.kits.set(path + "." + (i - 19), ItemUtils.toString(loot[i], KitsCmd.split));
		}
		KitsCmd.kits.saveConfig();
	}
}
