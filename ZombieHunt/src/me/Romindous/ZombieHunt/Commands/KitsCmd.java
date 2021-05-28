package me.Romindous.ZombieHunt.Commands;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

import me.Romindous.ZombieHunt.Main;
import me.Romindous.ZombieHunt.Game.Arena;
import me.Romindous.ZombieHunt.Game.GameState;

public class KitsCmd implements CommandExecutor, TabCompleter{
	
	private Main plug;
	
	public KitsCmd(Main plug) {
		this.plug = plug;
	}
	
	@Override
	public List<String> onTabComplete(CommandSender send, Command cmd, String al, String[] args) {
		LinkedList<String> sugg = new LinkedList<String>();
		if (send instanceof Player) {
			if (send.isOp()) {
				YamlConfiguration kits = YamlConfiguration.loadConfiguration(new File(plug.getDataFolder() + File.separator + "kits.yml"));
				if (args.length == 1) {
					sugg.add("add");
					sugg.add("edit");
					sugg.add("del");
					sugg.add("choose");
				} else if (args.length == 2 && (args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("edit") || args[0].equalsIgnoreCase("del"))) {
					sugg.add("zombie");
					sugg.add("player");
				} else if (args.length == 3 && (args[0].equalsIgnoreCase("edit") || args[0].equalsIgnoreCase("del"))) {
					if (args[1].equalsIgnoreCase("zombie")) {
						for (String s : kits.getConfigurationSection("kits").getConfigurationSection("zombie").getKeys(false)) {
							sugg.add(s);
						}
					} else if (args[1].equalsIgnoreCase("player")) {
						for (String s : kits.getConfigurationSection("kits").getConfigurationSection("player").getKeys(false)) {
							sugg.add(s);
						}
					}
				}
			} else {
				if (args.length == 1) {
					sugg.add("choose");
				}
			}
		}
		return sugg;
	}
	
	@Override
	public boolean onCommand(CommandSender send, Command cmd, String label, String[] args) {
		if (label.equalsIgnoreCase("zkits") && send instanceof Player) {
			final Player p = (Player) send;
			final YamlConfiguration kits = YamlConfiguration.loadConfiguration(new File(plug.getDataFolder() + File.separator + "kits.yml"));
			if (send.isOp()) {
				if (args.length == 3 && (args[1].equalsIgnoreCase("player") || args[1].equalsIgnoreCase("zombie"))) {
					if (args[0].equalsIgnoreCase("add")) {
						//есть ли уже набор?
						if (kits.contains("kits." + args[1] + "." + args[2])) {
							p.sendMessage("§cТакой набор уже есть");
							return true;
						}
						Inventory inv = Bukkit.createInventory(p, 27, "§3Создание Набора");
						inv.setContents(fillAddInv());
						if (args[1].equalsIgnoreCase("player")) {
							ItemStack item = new ItemStack(Material.VILLAGER_SPAWN_EGG);
							ItemMeta meta = item.getItemMeta();
							meta.setDisplayName(args[2]);
							item.setItemMeta(meta);
							inv.setItem(13, item);
						} else {
							ItemStack item = new ItemStack(Material.ZOMBIE_SPAWN_EGG);
							ItemMeta meta = item.getItemMeta();
							meta.setDisplayName(args[2]);
							item.setItemMeta(meta);
							inv.setItem(13, item);
						}
						p.openInventory(inv);
					} else if (args[0].equalsIgnoreCase("edit") && kits.contains("kits." + args[1] + "." + args[2])) {
						Inventory inv = Bukkit.createInventory(p, 27, "§3Создание Набора");
						inv.setContents(fillEditInv(kits, "kits." + args[1] + "." + args[2]));
						if (args[1].equalsIgnoreCase("player")) {
							ItemStack item = new ItemStack(Material.VILLAGER_SPAWN_EGG);
							ItemMeta meta = item.getItemMeta();
							meta.setDisplayName(args[2]);
							item.setItemMeta(meta);
							inv.setItem(13, item);
						} else {
							ItemStack item = new ItemStack(Material.ZOMBIE_SPAWN_EGG);
							ItemMeta meta = item.getItemMeta();
							meta.setDisplayName(args[2]);
							item.setItemMeta(meta);
							inv.setItem(13, item);
						}
						p.openInventory(inv);
					} else if (args[0].equalsIgnoreCase("del") && kits.contains("kits." + args[1] + "." + args[2])) {
						kits.set("kits." + args[1] + "." + args[2], null);
						try {
							kits.save(new File(plug.getDataFolder() + File.separator + "kits.yml"));
						} catch (IOException e) {
							e.printStackTrace();
						}
						p.sendMessage(Main.pref() + "§7Набор §6" + args[2] + "§7 успешно удален!");
						return true;
					} else {
						p.sendMessage(Main.pref() + "§cИспользование: /zkits add | edit | del  zombie | player  название");
						return true;
					}
				} else if (args.length == 1 && args[0].equalsIgnoreCase("choose")) {
					if (Arena.getPlayerArena(p.getName()) != null && Arena.getPlayerArena(p.getName()).getState() == GameState.RUNNING) {
						p.sendMessage(Main.pref() + "§cВы не можете изпользовать эту комманду во время игры!");
						return true;
					}
					final Inventory inv = Bukkit.createInventory(p, 27, "§3Доступные Наборы");
					inv.setContents(fillChooseInv(kits));
					p.openInventory(inv);
				} else {
					if (Arena.getPlayerArena(p.getName()) != null && Arena.getPlayerArena(p.getName()).getState() == GameState.RUNNING) {
						p.sendMessage(Main.pref() + "§cВы не можете изпользовать эту комманду во время игры!");
						return true;
					}
					p.sendMessage(Main.pref() + "§cИспользование: /zkits add | edit | del  zombie | player  название");
					return true;
				}
			} else if (args.length == 1 && args[0].equalsIgnoreCase("choose")) {
				final Inventory inv = Bukkit.createInventory(p, 27, "§3Доступные Наборы");
				inv.setContents(fillChooseInv(kits));
				p.openInventory(inv);
			} else {
				p.sendMessage(Main.pref() + "§cИспользование: /zkits add | edit | del  zombie | player  название");
				return true;
			}
		}
		return true;
	}

	private ItemStack[] fillChooseInv(YamlConfiguration kits) {
		ItemStack[] loot = new ItemStack[27];
		byte pused = 0;
		byte zused = 0;
		for (byte i = 0; i < 27; i++) {
			switch (i) {
			case 0:
			case 2:
			case 3:
			case 4:
			case 5:
			case 6:
			case 8:
			case 13:
			case 22:
				ItemStack item = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
				ItemMeta meta = item.getItemMeta();
				meta.setDisplayName(ChatColor.DARK_GRAY + "-=-=-=-=-");
				item.setItemMeta(meta);
				loot[i] = item;
				break;
			case 1:
				item = new ItemStack(Material.VILLAGER_SPAWN_EGG);
				meta = item.getItemMeta();
				meta.setDisplayName("§6Наборы для §dВыживших");
				item.setItemMeta(meta);
				loot[i] = item;
				break;
			case 7:
				item = new ItemStack(Material.ZOMBIE_SPAWN_EGG);
				meta = item.getItemMeta();
				meta.setDisplayName("§6Наборы для §dЗомби");
				item.setItemMeta(meta);
				loot[i] = item;
				break;
			case 9:
			case 10:
			case 11:
			case 12:
			case 18:
			case 19:
			case 20:
			case 21:
				if (pused < kits.getConfigurationSection("kits.player").getKeys(false).toArray().length) {
					item = getItemStack(kits.getConfigurationSection("kits.player." + kits.getConfigurationSection("kits").getConfigurationSection("player").getKeys(false).toArray()[pused] + ".0"));
					meta = item.getItemMeta();
					meta.setDisplayName("§a" + ChatColor.UNDERLINE + kits.getConfigurationSection("kits.player").getKeys(false).toArray()[pused]);
					meta.setLore(new LinkedList<String>(Arrays.asList("§bЛКМ §7- выбрать набор","§bПКМ §7- посмотреть набор")));
					item.setItemMeta(meta);
					loot[i] = item;
					pused ++;
				}
				break;
			case 14:
			case 15:
			case 16:
			case 17:
			case 23:
			case 24:
			case 25:
			case 26:
				if (zused < kits.getConfigurationSection("kits.zombie").getKeys(false).toArray().length) {
					item = getItemStack(kits.getConfigurationSection("kits.zombie." + kits.getConfigurationSection("kits").getConfigurationSection("zombie").getKeys(false).toArray()[zused] + ".0"));
					meta = item.getItemMeta();
					meta.setDisplayName("§e" + ChatColor.UNDERLINE + kits.getConfigurationSection("kits.zombie").getKeys(false).toArray()[zused]);
					meta.setLore(new LinkedList<String>(Arrays.asList("§bЛКМ §7- выбрать набор","§bПКМ §7- посмотреть набор")));
					item.setItemMeta(meta);
					loot[i] = item;
					zused ++;
				}
				break;
			default:
				break;
			}
		}
		return loot;
	}

	public static ItemStack[] fillEditInv(YamlConfiguration kits, String path) {
		ItemStack[] loot = new ItemStack[27];
		for (byte i = 0; i < 27; i++) {
			switch (i) {
			case 0:
				loot[i] = getItemStack(kits.getConfigurationSection(path + ".helm"));
				break;
			case 1:
				loot[i] = getItemStack(kits.getConfigurationSection(path + ".chest"));
				break;
			case 2:
				loot[i] = getItemStack(kits.getConfigurationSection(path + ".leggs"));;
				break;
			case 3:
				loot[i] = getItemStack(kits.getConfigurationSection(path + ".boots"));;
				break;
			case 5:
				ItemStack item = new ItemStack(Material.SPIDER_EYE);
				ItemMeta meta = item.getItemMeta();
				meta.setDisplayName("§6Здоровье: §4-1");
				item.setItemMeta(meta);
				item.setAmount(kits.getInt(path + ".hp"));
				loot[i] = item;
				break;
			case 6:
				item = new ItemStack(Material.BEETROOT);
				meta = item.getItemMeta();
				meta.setDisplayName("§6Здоровье: §2+1");
				item.setItemMeta(meta);
				item.setAmount(kits.getInt(path + ".hp"));
				loot[i] = item;
				break;
			case 8:
				item = new ItemStack(Material.GREEN_WOOL);
				meta = item.getItemMeta();
				meta.setDisplayName("§aГотово");
				item.setItemMeta(meta);
				loot[i] = item;
				break;
			case 17:
				if (kits.getString(path + ".perm").contains("N")) {
					item = new ItemStack(Material.BRICK);
					meta = item.getItemMeta();
					meta.setDisplayName("§6Без привилегий");
					item.setItemMeta(meta);
					loot[i] = item;
				} else if (kits.getString(path + ".perm").contains("g")) {
					item = new ItemStack(Material.IRON_INGOT);
					meta = item.getItemMeta();
					meta.setDisplayName("§6Игроман и выше");
					item.setItemMeta(meta);
					loot[i] = item;
				} else if (kits.getString(path + ".perm").contains("v")) {
					item = new ItemStack(Material.GOLD_INGOT);
					meta = item.getItemMeta();
					meta.setDisplayName("§6Вип и выше");
					item.setItemMeta(meta);
					loot[i] = item;
				} else {
					item = new ItemStack(Material.NETHERITE_INGOT);
					meta = item.getItemMeta();
					meta.setDisplayName("§6Премиум и выше");
					item.setItemMeta(meta);
					loot[i] = item;
				}
				break;
			case 19:
			case 20:
			case 21:
			case 22:
			case 23:
			case 24:
			case 25:
				if (kits.contains(path + "." + (i - 19) + ".mat")) {
					loot[i] = getItemStack(kits.getConfigurationSection(path + "." + (i - 19)));
				}
				break;
			default:
				item = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
				meta = item.getItemMeta();
				meta.setDisplayName("§8-=-=-=-=-");
				item.setItemMeta(meta);
				loot[i] = item;
				break;
			}
		}
		return loot;
	}

	private ItemStack[] fillAddInv() {
		ItemStack[] loot = new ItemStack[27];
		for (byte i = 0; i < 27; i++) {
			switch (i) {
			case 0:
				loot[i] = new ItemStack(Material.LEATHER_HELMET);
				break;
			case 1:
				loot[i] = new ItemStack(Material.LEATHER_CHESTPLATE);
				break;
			case 2:
				loot[i] = new ItemStack(Material.LEATHER_LEGGINGS);
				break;
			case 3:
				loot[i] = new ItemStack(Material.LEATHER_BOOTS);
				break;
			case 5:
				ItemStack item = new ItemStack(Material.SPIDER_EYE);
				ItemMeta meta = item.getItemMeta();
				meta.setDisplayName("§6Здоровье: §4-1");
				item.setItemMeta(meta);
				item.setAmount(20);
				loot[i] = item;
				break;
			case 6:
				item = new ItemStack(Material.BEETROOT);
				meta = item.getItemMeta();
				meta.setDisplayName("§6Здоровье: §2+1");
				item.setItemMeta(meta);
				item.setAmount(20);
				loot[i] = item;
				break;
			case 8:
				item = new ItemStack(Material.GREEN_WOOL);
				meta = item.getItemMeta();
				meta.setDisplayName("§aГотово");
				item.setItemMeta(meta);
				loot[i] = item;
				break;
			case 17:
				item = new ItemStack(Material.BRICK);
				meta = item.getItemMeta();
				meta.setDisplayName("§6Без привилегий");
				item.setItemMeta(meta);
				loot[i] = item;
				break;
			case 19:
				loot[i] = new ItemStack(Material.WOODEN_SWORD);
				break;
			case 20:
			case 21:
			case 22:
			case 23:
			case 24:
			case 25:
				break;
			default:
				item = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
				meta = item.getItemMeta();
				meta.setDisplayName("§8-=-=-=-=-");
				item.setItemMeta(meta);
				loot[i] = item;
				break;
			}
		}
		return loot;
	}
	
	@SuppressWarnings("deprecation")
	public static ItemStack getItemStack(final ConfigurationSection cs) {
		//результат
		final ItemStack result = new ItemStack(Material.getMaterial(cs.getString("mat")));
		final ItemMeta meta = result.getItemMeta();
		//имя предметв
		if (cs.getString("name").length() > 1) {
			meta.setDisplayName(cs.getString("name"));
		}
		//неразрушимость
		meta.setUnbreakable(true);
		meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
		//кол-во предмета
		result.setAmount(cs.getInt("amt"));
		//описание предмета
		if (cs.contains("lore")) {
			meta.setLore(Arrays.asList(cs.getString("lore").split(":")));
		}
		//зачарования предмета
		if (cs.contains("enchs")) {
			if (!cs.getString("lvls").contains(":")) {
				meta.addEnchant(Enchantment.getByName(cs.getString("enchs")), 
						Integer.parseInt(cs.getString("lvls")), true);
			} else {
				for (byte i = 0; i < cs.getString("lvls").split(":").length; i++) {
					meta.addEnchant(Enchantment.getByName(cs.getString("enchs").split(":")[i]), 
							Integer.parseInt(cs.getString("lvls").split(":")[i]), true);
				}
			}
		}
		//цвет предмета
		if (cs.contains("color")) {
			LeatherArmorMeta lmeta = (LeatherArmorMeta) meta;
			lmeta.setColor(Color.fromRGB(cs.getInt("color")));
			result.setItemMeta(lmeta);
		} else if (cs.contains("pot")) {
			PotionMeta pmeta = (PotionMeta) meta;
			pmeta.setBasePotionData(new PotionData(PotionType.valueOf(cs.getString("pot.base"))));
			if (cs.contains("pot.color")) {
				pmeta.setColor(Color.fromRGB(Integer.parseInt(cs.getString("pot.color"))));
			}
			if (cs.contains("pot.types")) {
				if (!cs.getString("pot.durs").contains(":")) {
					pmeta.addCustomEffect(new PotionEffect(PotionEffectType.getByName(cs.getString("pot.types")), 
						Integer.parseInt(cs.getString("pot.durs")), 
						Integer.parseInt(cs.getString("pot.amps"))), true);
				} else {
					for (byte i = 0; i < cs.getString("pot.durs").split(":").length; i++) {
						pmeta.addCustomEffect(new PotionEffect(PotionEffectType.getByName(cs.getString("pot.types").split(":")[i]), 
							Integer.parseInt(cs.getString("pot.durs").split(":")[i]), 
							Integer.parseInt(cs.getString("pot.amps").split(":")[i])), true);
					}
				}
			}
			result.setItemMeta(pmeta);
		} else {
			result.setItemMeta(meta);
		}
		return result;
	}
}