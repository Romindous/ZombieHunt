package me.Romindous.ZombieHunt.Listeners;

import java.io.File;
import java.io.IOException;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;

import me.Romindous.ZombieHunt.Main;
import me.Romindous.ZombieHunt.Commands.KitsCmd;
import me.Romindous.ZombieHunt.Game.Arena;
import me.Romindous.ZombieHunt.Messages.TitleManager;
import ru.komiss77.ApiOstrov;

public class InventoryLis implements Listener{
	
	@EventHandler
	public void onClick(InventoryClickEvent e) {
		//клик на ничего? и предметы в инвентаре
		if (e.getCurrentItem() == null) {
			return;
		}
		if (e.getClick() == ClickType.NUMBER_KEY || e.getClick() == ClickType.SWAP_OFFHAND) {
			e.setCancelled(true);
			e.getCursor().setType(Material.AIR);
			return;
		}
		
		if (e.getWhoClicked().getGameMode() == GameMode.SPECTATOR && e.getCurrentItem().getType() == Material.REDSTONE) {
			e.setCancelled(true);
			for (Arena ar : Main.activearenas) {
				ar.getSpcs().remove(e.getWhoClicked().getName());
			}
			Main.lobbyPlayer((Player) e.getWhoClicked());
			e.getWhoClicked().sendMessage(Main.pref() + "Перемещаем вас обратно в лобби!");
			return;
		}
		
		if (e.getCurrentItem().getItemMeta() != null && (e.getCurrentItem().getItemMeta().getDisplayName().contains("Выбор") || e.getCurrentItem().getItemMeta().getDisplayName().contains("Набор") || e.getCurrentItem().getItemMeta().getDisplayName().contains("Выход"))) {
			e.setCancelled(true);
			e.getCursor().setType(Material.AIR);
			return;
		}
		
		if (e.getView().getTitle().contains("выбор")) {
			e.setCancelled(true);
			Player p = (Player) e.getWhoClicked();
			switch (e.getCurrentItem().getType()) {
			case LIGHT_GRAY_STAINED_GLASS_PANE:
			case LEATHER:
				break;
			case GREEN_CONCRETE_POWDER:
				p.performCommand("zh join " + arenaFromString(e.getCurrentItem().getItemMeta().getDisplayName()));
				p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 80, 1);
				p.closeInventory();
				break;
			case YELLOW_CONCRETE_POWDER:
				p.performCommand("zh join " + arenaFromString(e.getCurrentItem().getItemMeta().getDisplayName()));
				p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 80, 1);
				p.closeInventory();
				break;
			case RED_CONCRETE_POWDER:
				p.sendMessage(Main.pref() + ChatColor.RED + "На этой карте уже идет игра!");
				p.sendMessage(Main.pref() + ChatColor.GRAY + "Помещаем вас в качестве зрителя");
				p.getInventory().clear();
				ItemStack item = new ItemStack(Material.REDSTONE);
				ItemMeta meta = item.getItemMeta();
				meta.setDisplayName(ChatColor.DARK_RED + "Обратно в лобби");
				item.setItemMeta(meta);
				p.getInventory().setItem(8, item);
				p.teleport(Arena.getNameArena(arenaFromString(e.getCurrentItem().getItemMeta().getDisplayName())).getRandSpawn());
				Arena.getNameArena(arenaFromString(e.getCurrentItem().getItemMeta().getDisplayName())).getSpcs().add(p.getName());
				p.setGameMode(GameMode.SPECTATOR);
				for (Player ing : Bukkit.getOnlinePlayers()) {
					if (Arena.getPlayerArena(ing.getName()) != null && e.getCurrentItem().getItemMeta().getDisplayName().contains(Arena.getPlayerArena(ing.getName()).getName())) {
						p.showPlayer(Main.plug, ing);
					}
				}
				break;	
			default:
				break;
			}
		} else if (e.getView().getTitle().contains("Наборы")) {
			final Player p = (Player) e.getWhoClicked();
			final YamlConfiguration kits = YamlConfiguration.loadConfiguration(new File(Main.plug.getDataFolder() + File.separator + "kits.yml"));
			if (e.getCurrentItem().getType() == Material.LIGHT_GRAY_STAINED_GLASS_PANE && e.getCurrentItem().getItemMeta().getDisplayName().contains("-=")) {
				e.setCancelled(true);
				return;
			} else if (e.getCurrentItem().getItemMeta().getDisplayName().contains(ChatColor.GREEN + "")) {
				e.setCancelled(true);
				if (e.getClick() == ClickType.RIGHT) {
					final Inventory inv = Bukkit.createInventory(p, 27, ChatColor.DARK_AQUA + "Просмотр Набора");
					inv.setContents(KitsCmd.fillEditInv(kits, "kits.player." + e.getCurrentItem().getItemMeta().getDisplayName().split("" + ChatColor.UNDERLINE)[1]));
					ItemStack item = new ItemStack(Material.REDSTONE_TORCH);
					ItemMeta meta = item.getItemMeta();
					meta.setDisplayName(ChatColor.DARK_GRAY + "Назад");
					item.setItemMeta(meta);
					inv.setItem(8, item);
					inv.setItem(5, inv.getItem(4));
					item = new ItemStack(Material.APPLE, inv.getItem(6).getAmount());
					meta = item.getItemMeta();
					meta.setDisplayName(ChatColor.GOLD + "Здоровье:");
					item.setItemMeta(meta);
					inv.setItem(6, item);
					p.closeInventory();
					p.openInventory(inv);
				} else {
					if (!kits.getString("kits.player." + e.getCurrentItem().getItemMeta().getDisplayName().split("" + ChatColor.UNDERLINE)[1] + ".perm").contains("N") && !ApiOstrov.hasGroup(p.getName(), kits.getString("kits.player." + e.getCurrentItem().getItemMeta().getDisplayName().split("" + ChatColor.UNDERLINE)[1] + ".perm"))) {
						p.sendMessage(Main.pref() + ChatColor.RED + "Вам нужно иметь донат §6" + transDon(kits.getString("kits.player." + e.getCurrentItem().getItemMeta().getDisplayName().split("" + ChatColor.UNDERLINE)[1] + ".perm")) + "§c для игры с этим набором!");
						p.playSound(p.getLocation(), Sound.ITEM_BUCKET_EMPTY_LAVA, 20, 0.8f);
						p.closeInventory();
						return;
					}
					p.playSound(p.getLocation(), Sound.ITEM_ARMOR_EQUIP_IRON, 200, 1);
					final String kit = e.getCurrentItem().getItemMeta().getDisplayName().split(String.valueOf(ChatColor.UNDERLINE))[1];
					Main.data.setString(p.getName(), "pkit", kit, "pls");
					p.closeInventory();
					p.sendMessage(Main.pref() + ChatColor.GRAY + "Выбран набор " + ChatColor.GOLD + e.getCurrentItem().getItemMeta().getDisplayName().split("" + ChatColor.UNDERLINE)[1] + ChatColor.GRAY + " для игры за выжившего");
					Main.chgSbdTm(p.getScoreboard(), "pkit", "", ChatColor.GOLD + kit);
				}
			} else if (e.getCurrentItem().getItemMeta().getDisplayName().contains(ChatColor.YELLOW + "")) {
				e.setCancelled(true);
				if (e.getClick() == ClickType.RIGHT) { 
					Inventory inv = Bukkit.createInventory(p, 27, ChatColor.DARK_AQUA + "Просмотр Набора");
					inv.setContents(KitsCmd.fillEditInv(kits, "kits.zombie." + e.getCurrentItem().getItemMeta().getDisplayName().split("" + ChatColor.UNDERLINE)[1]));
					ItemStack item = new ItemStack(Material.REDSTONE_TORCH);
					ItemMeta meta = item.getItemMeta();
					meta.setDisplayName(ChatColor.DARK_GRAY + "Назад");
					item.setItemMeta(meta);
					inv.setItem(8, item);
					inv.setItem(5, inv.getItem(4));
					item = new ItemStack(Material.APPLE, inv.getItem(6).getAmount());
					meta = item.getItemMeta();
					meta.setDisplayName(ChatColor.GOLD + "Здоровье:");
					item.setItemMeta(meta);
					inv.setItem(6, item);
					p.closeInventory();
					p.openInventory(inv);
				} else {;
					if (!kits.getString("kits.zombie." + e.getCurrentItem().getItemMeta().getDisplayName().split("" + ChatColor.UNDERLINE)[1] + ".perm").contains("N") && !ApiOstrov.hasGroup(p.getName(), kits.getString("kits.zombie." + e.getCurrentItem().getItemMeta().getDisplayName().split("" + ChatColor.UNDERLINE)[1] + ".perm"))) {
						p.sendMessage(Main.pref() + ChatColor.RED + "Вам нужно иметь донат §6" + transDon(kits.getString("kits.zombie." + e.getCurrentItem().getItemMeta().getDisplayName().split("" + ChatColor.UNDERLINE)[1] + ".perm")) + "§c для игры с этим набором!");
						p.playSound(p.getLocation(), Sound.ITEM_BUCKET_EMPTY_LAVA, 20, 0.8f);
						p.closeInventory();
						return;
					}
					p.playSound(p.getLocation(), Sound.ITEM_ARMOR_EQUIP_IRON, 200, 1);
					final String kit = e.getCurrentItem().getItemMeta().getDisplayName().split(String.valueOf(ChatColor.UNDERLINE))[1];
					Main.data.setString(p.getName(), "zkit", kit, "pls");
					p.closeInventory();
					p.sendMessage(Main.pref() + ChatColor.GRAY + "Выбран набор " + ChatColor.GOLD + e.getCurrentItem().getItemMeta().getDisplayName().split("" + ChatColor.UNDERLINE)[1] + ChatColor.GRAY + " для игры за зомби");
					Main.chgSbdTm(p.getScoreboard(), "zkit", "", ChatColor.GOLD + kit);
				}
			}
		} else if (e.getView().getTitle().contains("Просмотр")) {
			if (e.getCurrentItem().getItemMeta().getDisplayName().contains("Назад")) {
				e.getWhoClicked().closeInventory();
				((Player) e.getWhoClicked()).performCommand("zkits choose");
				TitleManager.sendBack(((Player) e.getWhoClicked()));
			} else {
				e.setCancelled(true);
			}
		} else if (e.getView().getTitle().contains("Создание")) {
			switch (e.getCurrentItem().getType()) {
			case VILLAGER_SPAWN_EGG:
			case ZOMBIE_SPAWN_EGG:
				e.setCancelled(true);
				break;
			case LIGHT_GRAY_STAINED_GLASS_PANE:
				if (e.getCurrentItem().getItemMeta().getDisplayName().contains("-=") || e.getCurrentItem().getItemMeta().getDisplayName().contains("Игрок") || e.getCurrentItem().getItemMeta().getDisplayName().contains("Зомби")) {
					e.setCancelled(true);
				}
				break;
			case BRICK:
				if (e.getCurrentItem().getItemMeta().getDisplayName().contains("Без")) {
					e.setCancelled(true);
					ItemStack item = new ItemStack(Material.IRON_INGOT);
					ItemMeta meta = item.getItemMeta();
					meta.setDisplayName("§6Игроман и выше");
					item.setItemMeta(meta);
					e.getInventory().setItem(e.getSlot(), item);
				}
				break;
			case IRON_INGOT:
				if (e.getCurrentItem().getItemMeta().getDisplayName().contains("Игроман")) {
					e.setCancelled(true);
					ItemStack item = new ItemStack(Material.GOLD_INGOT);
					ItemMeta meta = item.getItemMeta();
					meta.setDisplayName("§6Вип и выше");
					item.setItemMeta(meta);
					e.getInventory().setItem(e.getSlot(), item);
				}
				break;
			case GOLD_INGOT:
				if (e.getCurrentItem().getItemMeta().getDisplayName().contains("Вип")) {
					e.setCancelled(true);
					ItemStack item = new ItemStack(Material.NETHERITE_INGOT);
					ItemMeta meta = item.getItemMeta();
					meta.setDisplayName("§6Премиум и выше");
					item.setItemMeta(meta);
					e.getInventory().setItem(e.getSlot(), item);
				}
				break;
			case NETHERITE_INGOT:
				if (e.getCurrentItem().getItemMeta().getDisplayName().contains("Премиум")) {
					e.setCancelled(true);
					ItemStack item = new ItemStack(Material.BRICK);
					ItemMeta meta = item.getItemMeta();
					meta.setDisplayName("§6Без привилегий");
					item.setItemMeta(meta);
					e.getInventory().setItem(e.getSlot(), item);
				}
				break;
			case GREEN_WOOL:
				if (e.getCurrentItem().getItemMeta().getDisplayName().contains("Готово")) {
					e.setCancelled(true);
					createKit(e.getClickedInventory().getContents());
					e.getWhoClicked().closeInventory();
					e.getWhoClicked().sendMessage(Main.pref() + ChatColor.GRAY + "Набор " + ChatColor.GOLD + e.getClickedInventory().getItem(13).getItemMeta().getDisplayName() + ChatColor.GRAY + " успешно создан!");
				}
				break;
			case SPIDER_EYE:
				if (e.getCurrentItem().getItemMeta().getDisplayName().contains("-1")) {
					e.setCancelled(true);
					if (e.getCurrentItem().getAmount() == 1) {
						break;
					}
					ItemStack item = e.getCurrentItem();
					item.setAmount(e.getCurrentItem().getAmount() - 1);
					e.getClickedInventory().setItem(5, item);
					item = new ItemStack(Material.BEETROOT, e.getCurrentItem().getAmount());
					ItemMeta meta = item.getItemMeta();
					meta.setDisplayName(ChatColor.GOLD + "Здоровье: " + ChatColor.DARK_GREEN + "+1");
					item.setItemMeta(meta);
					e.getClickedInventory().setItem(6, item);
				}
				break;
			case BEETROOT:
				if (e.getCurrentItem().getItemMeta().getDisplayName().contains("+1")) {
					e.setCancelled(true);
					ItemStack item = e.getCurrentItem();
					item.setAmount(e.getCurrentItem().getAmount() + 1);
					e.getClickedInventory().setItem(6, item);
					item = new ItemStack(Material.SPIDER_EYE, e.getCurrentItem().getAmount());
					ItemMeta meta = item.getItemMeta();
					meta.setDisplayName(ChatColor.GOLD + "Здоровье: " + ChatColor.DARK_RED + "-1");
					item.setItemMeta(meta);
					e.getClickedInventory().setItem(5, item);
				}
				break;
			default:
				if (e.getClickedInventory().getSize() == 27) {
					switch (e.getSlot()) {
					case 0:
						e.setCancelled(!e.getCursor().getType().toString().contains("HELMET") && !e.getCursor().getType().toString().contains("HEAD") && !e.getCursor().getType().toString().contains("SKULL"));
						break;
					case 1:
						e.setCancelled(!e.getCursor().getType().toString().contains("CHESTPLATE"));
						break;
					case 2:
						e.setCancelled(!e.getCursor().getType().toString().contains("LEGGINGS"));
						break;
					case 3:
						e.setCancelled(!e.getCursor().getType().toString().contains("BOOTS"));
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
	
	private String transDon(String s) {
		switch (s.length()) {
		case 5:
			return "Игроман";
		case 3:
			return "Вип";
		case 7:
			return "Премиум";
		}
		return null;
	}

	private void createKit(ItemStack[] loot) {
		YamlConfiguration kits = YamlConfiguration.loadConfiguration(new File(Main.folder + File.separator + "kits.yml"));
		String path = loot[13].getType() == Material.VILLAGER_SPAWN_EGG ? "kits.player." + loot[13].getItemMeta().getDisplayName() : "kits.zombie." + loot[13].getItemMeta().getDisplayName();
		if (loot[17].getType() == Material.BRICK) {
			kits.set(path + ".perm", "N");
		} else if (loot[17].getType() == Material.IRON_INGOT) {
			kits.set(path + ".perm", "gamer");
		} else if (loot[17].getType() == Material.GOLD_INGOT) {
			kits.set(path + ".perm", "vip");
		} else {
			kits.set(path + ".perm", "premium");
		}
		kits.set(path + ".hp", loot[5].getAmount());
		kits = rememberItem(kits, path + ".helm", loot[0]);
		kits = rememberItem(kits, path + ".chest", loot[1]);
		kits = rememberItem(kits, path + ".leggs", loot[2]);
		kits = rememberItem(kits, path + ".boots", loot[3]);
		for (byte i = 19; i < 26; i++) {
			kits.set(path + "." + (i - 19), null);
			if (loot[i] != null) {
				kits = rememberItem(kits, path + "." + (i - 19), loot[i]);
			}
		}
		try {
			kits.save(new File(Main.folder + File.separator + "kits.yml"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@SuppressWarnings("deprecation")
	private YamlConfiguration rememberItem(YamlConfiguration kits, String path, ItemStack item) {
		kits.set(path, null);
		//запоминаем материал предмета
		kits.set(path + ".mat", item.getType().toString());
		//имя предмета
		kits.set(path + ".name", item.getItemMeta().getDisplayName());
		//кол-во предмета
		kits.set(path + ".amt", item.getAmount());
		//цвета?
		if (item.getItemMeta() instanceof LeatherArmorMeta) {
			LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();
			kits.set(path + ".color", meta.getColor().asRGB());
		}
		//эффекты?
		if (item.getItemMeta() instanceof PotionMeta) {
			PotionMeta meta = (PotionMeta) item.getItemMeta();
			kits.set(path + ".pot.base", meta.getBasePotionData().getType().toString());
			kits.set(path + ".pot.color", meta.hasColor() ? meta.getColor().asRGB() : null);
			for (PotionEffect pef : meta.getCustomEffects()) {
				if (!kits.contains(path + ".pot.types")) {
					kits.set(path + ".pot.types", pef.getType().getName());
					kits.set(path + ".pot.durs", pef.getDuration() + "");
					kits.set(path + ".pot.amps", pef.getAmplifier() + "");
				} else {
					kits.set(path + ".pot.types", kits.get(path + ".pot.types") + ":" + pef.getType().getName());
					kits.set(path + ".pot.durs", kits.get(path + ".pot.durs") + ":" + pef.getDuration());
					kits.set(path + ".pot.amps", kits.get(path + ".pot.amps") + ":" + pef.getAmplifier());
				}
			}
		}
		//описание предмета
		if (item.getItemMeta().hasLore()) {
			for (String l : item.getItemMeta().getLore()) {
				if (!kits.contains(path + ".lore")) {
					kits.set(path + ".lore", l);
				} else {
					kits.set(path + ".lore", kits.get(path + ".lore") + ":" + l);
				}
			}
		}
		//зачарования предмета
		if (item.getItemMeta().getEnchants() != null) {
			for (Enchantment en : item.getItemMeta().getEnchants().keySet()) {
				if (!kits.contains(path + ".enchs")) {
					kits.set(path + ".enchs", 
							en.getName());
					kits.set(path + ".lvls", 
							"" + item.getItemMeta().getEnchantLevel(en));
				} else {
					kits.set(path + ".enchs", 
							kits.get(path + ".enchs") + ":" + en.getName());
					kits.set(path + ".lvls", 
							kits.get(path + ".lvls") + ":" + item.getItemMeta().getEnchantLevel(en));
				}
			}
		}
		return kits;
	}

	private String arenaFromString(String name) {
		for (String s : Main.nonactivearenas) {
			if (name.contains(s)) {
				return s;
			}
		}
		return null;
	}
}
