package me.Romindous.ZombieHunt.Commands;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.bukkit.Bukkit;
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
import net.kyori.adventure.text.Component;
import ru.komiss77.utils.ItemBuilder;

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
						Inventory inv = Bukkit.createInventory(p, 27, Component.text("§3Создание Набора"));
						inv.setContents(fillAddInv());
						if (args[1].equalsIgnoreCase("player")) {
							inv.setItem(13, new ItemBuilder(Material.VILLAGER_SPAWN_EGG).name(args[2]).build());
						} else {
							inv.setItem(13, new ItemBuilder(Material.ZOMBIE_SPAWN_EGG).name(args[2]).build());
						}
						p.openInventory(inv);
					} else if (args[0].equalsIgnoreCase("edit") && kits.contains("kits." + args[1] + "." + args[2])) {
						Inventory inv = Bukkit.createInventory(p, 27, Component.text("§3Создание Набора"));
						inv.setContents(fillEditInv(kits, "kits." + args[1] + "." + args[2]));
						if (args[1].equalsIgnoreCase("player")) {
							inv.setItem(13, new ItemBuilder(Material.VILLAGER_SPAWN_EGG).name(args[2]).build());
						} else {
							inv.setItem(13, new ItemBuilder(Material.ZOMBIE_SPAWN_EGG).name(args[2]).build());
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
					final Inventory inv = Bukkit.createInventory(p, 27, Component.text("§3Доступные Наборы"));
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
				final Inventory inv = Bukkit.createInventory(p, 27, Component.text("§3Доступные Наборы"));
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
				loot[i] = new ItemBuilder(Material.LIGHT_GRAY_STAINED_GLASS_PANE).name("§8-=-=-=-=-").build();
				break;
			case 1:
				loot[i] = new ItemBuilder(Material.VILLAGER_SPAWN_EGG).name("§6Наборы для §dВыживших").build();
				break;
			case 7:
				loot[i] = new ItemBuilder(Material.ZOMBIE_SPAWN_EGG).name("§6Наборы для §dЗомби").build();
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
					loot[i] = new ItemBuilder(getItemStack(kits.getConfigurationSection("kits.player." + 
							kits.getConfigurationSection("kits").getConfigurationSection("player").getKeys(false).toArray()[pused] + ".0")))
						.name("§a§n" + kits.getConfigurationSection("kits.player").getKeys(false).toArray()[pused])
						.lore(new LinkedList<String>(Arrays.asList("§bЛКМ §7- выбрать набор","§bПКМ §7- посмотреть набор"))).build();
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
					loot[i] = new ItemBuilder(getItemStack(kits.getConfigurationSection("kits.zombie." + 
							kits.getConfigurationSection("kits").getConfigurationSection("zombie").getKeys(false).toArray()[zused] + ".0")))
						.name("§e§n" + kits.getConfigurationSection("kits.zombie").getKeys(false).toArray()[zused])
						.lore(new LinkedList<String>(Arrays.asList("§bЛКМ §7- выбрать набор","§bПКМ §7- посмотреть набор"))).build();
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
				loot[i] = new ItemBuilder(Material.SPIDER_EYE).name("§6Здоровье: §4-1").setAmount(kits.getInt(path + ".hp")).build();
				break;
			case 6:
				loot[i] = new ItemBuilder(Material.BEETROOT).name("§6Здоровье: §2+1").setAmount(kits.getInt(path + ".hp")).build();
				break;
			case 8:
				loot[i] = new ItemBuilder(Material.GREEN_WOOL).name("§aГотово").build();
				break;
			case 17:
				if (kits.getString(path + ".perm").contains("N")) {
					loot[i] = new ItemBuilder(Material.BRICK).name("§6Без привилегий").build();
				} else if (kits.getString(path + ".perm").contains("w")) {
					loot[i] = new ItemBuilder(Material.IRON_INGOT).name("§6Воин и выше").build();
				} else if (kits.getString(path + ".perm").contains("h")) {
					loot[i] = new ItemBuilder(Material.GOLD_INGOT).name("§6Герой и выше").build();
				} else {
					loot[i] = new ItemBuilder(Material.NETHERITE_INGOT).name("§6Легенда и выше").build();
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
				loot[i] = new ItemBuilder(Material.LIGHT_GRAY_STAINED_GLASS_PANE).name("§8-=-=-=-=-").build();
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
				loot[i] = new ItemBuilder(Material.SPIDER_EYE).name("§6Здоровье: §4-1").setAmount(20).build();
				break;
			case 6:
				loot[i] = new ItemBuilder(Material.BEETROOT).name("§6Здоровье: §2+1").setAmount(20).build();
				break;
			case 8:
				loot[i] = new ItemBuilder(Material.GREEN_WOOL).name("§aГотово").setAmount(20).build();
				break;
			case 17:
				loot[i] = new ItemBuilder(Material.BRICK).name("§6Без привилегий").build();
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
				loot[i] = new ItemBuilder(Material.LIGHT_GRAY_STAINED_GLASS_PANE).name("§8-=-=-=-=-").build();
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