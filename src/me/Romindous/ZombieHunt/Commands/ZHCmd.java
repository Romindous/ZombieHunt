package me.Romindous.ZombieHunt.Commands;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import me.Romindous.ZombieHunt.Main;
import me.Romindous.ZombieHunt.Game.Arena;
import me.Romindous.ZombieHunt.Game.GameState;

public class ZHCmd implements CommandExecutor, TabCompleter{

	private Main plug;
	
	public ZHCmd(Main plug) {
		this.plug = plug;
	}
	
	@Override
	public List<String> onTabComplete(CommandSender send, Command cmd, String al, String[] args) {
		LinkedList<String> sugg = new LinkedList<String>();
		if (send instanceof Player) {
			YamlConfiguration ars = YamlConfiguration.loadConfiguration(new File(plug.getDataFolder() + File.separator + "arenas.yml"));
			Player p = (Player) send;
			if (p.isOp()) {
				if (args.length == 1) {
					sugg.add("join");
					sugg.add("leave");
					sugg.add("help");
					sugg.add("create");
					sugg.add("addspawn");
					sugg.add("finish");
					sugg.add("delete");
					sugg.add("setlobby");
					sugg.add("reload");
				} else if (args.length == 2 && (args[0].equalsIgnoreCase("join") || args[0].equalsIgnoreCase("delete"))) {
					for (String s : Main.nonactivearenas) {
						sugg.add(s);
					}
				} else if (args.length == 2 && (args[0].equalsIgnoreCase("addspawn") || args[0].equalsIgnoreCase("finish"))) {
					for (String s : ars.getConfigurationSection("arenas").getKeys(false)) {
						if (!ars.contains("arenas." + s + ".fin")) {
							sugg.add(s);
						}
					}
				}
			} else {
				if (args.length == 1) {
					sugg.add("join");
					sugg.add("leave");
					sugg.add("help");
				} else if (args.length == 2 && args[0].equalsIgnoreCase("join")) {
					for (String s : Main.nonactivearenas) {
						sugg.add(s);
					}
				}
			}
		}
		return sugg;
	}

	@Override
	public boolean onCommand(CommandSender send, Command cmd, String label, String[] args) {
		if (label.equalsIgnoreCase("zh") && send instanceof Player) {
			Player p = (Player) send;
			YamlConfiguration ars = YamlConfiguration.loadConfiguration(new File(plug.getDataFolder() + File.separator + "arenas.yml"));
			//админ комманды
			if (p.isOp()) {
				//создание карты
				if (args.length == 4 && args[0].equalsIgnoreCase("create") && !ars.contains("arenas." + args[1] + ".world")) {
					p.sendMessage(Main.pref() + "Начинаем cоздание арены §e" + args[1] + "§7:");
					byte min;
					byte max;
					//проверка на число
					try {
						min = Byte.parseByte(args[2]);
						max = Byte.parseByte(args[3]);
					} catch (NumberFormatException e) {
						p.sendMessage(Main.pref() + "После названия надо вписать 2 числа!");
						return true;
					}
					if (min >= 2 && min <= max) {
						//добавляем арену
						ars.set("arenas." + args[1] + ".min", min);
						ars.set("arenas." + args[1] + ".max", max);
						ars.set("arenas." + args[1] + ".world", p.getWorld().getName());
						try {
							ars.save(new File(plug.getDataFolder() + File.separator + "arenas.yml"));
						} catch (IOException e) {
							e.printStackTrace();
						}
						p.sendMessage(Main.pref() + "Минимальное кол-во игроков: §9" + min);
						p.sendMessage(Main.pref() + "Максимальное кол-во игроков: §9" + max);
						return true;
					} else {
						p.sendMessage(Main.pref() + "§cПервое число должно быть меньше или равно второму и быть более 2!");
						return true;
					}
				} else if (args.length == 2) {
					//добавление спавнпоинтов
					if (args[0].equalsIgnoreCase("addspawn") && ars.contains("arenas." + args[1] + ".world") && !ars.contains("arenas." + args[1] + ".fin")) {
						if (ars.contains("arenas." + args[1] + ".spawns.x")) {
							if (ars.getString("arenas." + args[1] + ".spawns.x").split(":").length < ars.getInt("arenas." + args[1] + ".max")) {
								ars.set("arenas." + args[1] + ".spawns.x", ars.getString("arenas." + args[1] + ".spawns.x") + ":" + p.getLocation().getBlockX());
								ars.set("arenas." + args[1] + ".spawns.y", ars.getString("arenas." + args[1] + ".spawns.y") + ":" + (p.getLocation().getBlockY() + 1));
								ars.set("arenas." + args[1] + ".spawns.z", ars.getString("arenas." + args[1] + ".spawns.z") + ":" + p.getLocation().getBlockZ());
								p.sendMessage(Main.pref() + "Точка спавна для карты §e" + args[1] + " §7сохранена на " + 
										"§9( " + p.getLocation().getBlockX() + " " + p.getLocation().getBlockY() + " " + p.getLocation().getBlockZ() + " )");
								try {
									ars.save(new File(plug.getDataFolder() + File.separator + "arenas.yml"));
								} catch (IOException e) {
	 
									e.printStackTrace();
								}
								return true;
							} else {
								p.sendMessage(Main.pref() + "§cВы уже создали достаточно точек для этой карты!");
								return true;
							}
						} else {
							ars.set("arenas." + args[1] + ".spawns.x", p.getLocation().getBlockX());
							ars.set("arenas." + args[1] + ".spawns.y", (p.getLocation().getBlockY() + 1));
							ars.set("arenas." + args[1] + ".spawns.z", p.getLocation().getBlockZ());
							p.sendMessage(Main.pref() + "§7Точка спавна для карты §e" + args[1] + " §7сохранена на " + 
									"§9( " + p.getLocation().getBlockX() + " " + p.getLocation().getBlockY() + " " + p.getLocation().getBlockZ() + " )");
							try {
								ars.save(new File(plug.getDataFolder() + File.separator + "arenas.yml"));
							} catch (IOException e) {
 
								e.printStackTrace();
							}
							return true;
						}
						//окончание разработки карты
					} else if (args[0].equalsIgnoreCase("finish") && ars.contains("arenas." + args[1] + ".world") && !ars.contains("arenas." + args[1] + ".fin")) {
						if (ars.getString("arenas." + args[1] + ".spawns.x").split(":").length == ars.getInt("arenas." + args[1] + ".max")) {
							ars.set("arenas." + args[1] + ".fin", 1);
							Main.nonactivearenas.add(args[1]);
							p.sendMessage(Main.pref() + "Карта §e" + args[1] + " §7успешно создана!");
							try {
								ars.save(new File(plug.getDataFolder() + File.separator + "arenas.yml"));
							} catch (IOException e) {
								e.printStackTrace();
							}
							return true;
						} else {
							p.sendMessage(Main.pref() + "§cСоздайте еще §9" + (ars.getInt("arenas." + args[1] + ".max") - 
								ars.getString("arenas." + args[1] + ".spawns.x").split(":").length) + "§c точек для этой карты!");
							return true;
						}
						//удаление карты
					} else if (args[0].equalsIgnoreCase("delete") && ars.contains("arenas." + args[1] + ".world")) {
						ars.set("arenas." + args[1], null);
						if (Main.nonactivearenas.contains(args[1])) {
							Main.nonactivearenas.remove(args[1]);
						}
						p.sendMessage(Main.pref() + "Карта §e" + args[1] + "§7 успешно удалена!");
						try {
							ars.save(new File(plug.getDataFolder() + File.separator + "arenas.yml"));
						} catch (IOException e) {
							e.printStackTrace();
						}
						return true;
					} else if (!args[0].equalsIgnoreCase("join")) {
						p.sendMessage(Main.pref() + "§cНеправельный синтакс комманды, все комманды - §6/zh help");
						return true;
					}
					//установка лобби
				} else if (args.length == 1) {
					if (args[0].equalsIgnoreCase("setlobby")) {
						Main.lobby = p.getLocation();
						ars.set("lobby.world", p.getWorld().getName());
						ars.set("lobby.x", p.getLocation().getBlockX());
						ars.set("lobby.y", p.getLocation().getBlockY());
						ars.set("lobby.z", p.getLocation().getBlockZ());
						p.sendMessage(Main.pref() + "Точка лобби сохранена на " + 
								"§9( " + p.getLocation().getBlockX() + " " + p.getLocation().getBlockY() + " " + p.getLocation().getBlockZ() + " )");
						try {
							ars.save(new File(plug.getDataFolder() + File.separator + "arenas.yml"));
						} catch (IOException e) {
							e.printStackTrace();
						}
						return true;
						//перезапуск конфига
					} else if (args[0].equalsIgnoreCase("clear")) {
						Main.data.delTbl("pls");
						Main.data.mkTbl("pls", "name", "zkit", "pkit", "zkls", "zdths", "pkls", "pdths", "gms", "prm");
						p.sendMessage(Main.pref() + "Done!");
						return true;
					} else if (args[0].equalsIgnoreCase("reload")) {
						plug.loadConfigs();
						Main.dataConn();
						p.sendMessage(Main.pref() + "Конфиги плагина успешно перезагружены!");
						return true;
					} else if (!args[0].equalsIgnoreCase("join") && !args[0].equalsIgnoreCase("leave") && !args[0].equalsIgnoreCase("help")) {
						p.sendMessage(Main.pref() + "§cНеправельный синтакс комманды, все комманды - §6/zh help");
						return true;
					}
				} else {
					p.sendMessage(Main.pref() + "§cНеправельный синтакс комманды, все комманды - §6/zh help");
					return true;
				}
			}
			
			//общие комманды
			YamlConfiguration kits = YamlConfiguration.loadConfiguration(new File(plug.getDataFolder() + File.separator + "kits.yml"));
			if (ars.contains("lobby") && kits.getConfigurationSection("kits").getConfigurationSection("zombie").getKeys(false).size() > 0 && kits.getConfigurationSection("kits").getConfigurationSection("player").getKeys(false).size() > 0) {
				//добавление на карту
				if (args.length == 2 && args[0].equalsIgnoreCase("join")) {
					if (Arena.getPlayerArena(p.getName()) == null) {
						for (final Arena ar : Main.activearenas) {
							if (ar.getName().equalsIgnoreCase(args[1])) {
								if (ar.getState() == GameState.LOBBY_WAIT || ar.getState() == GameState.LOBBY_START) {
									ar.addPl(p.getName());
								} else {
									p.sendMessage(Main.pref() + "§cНа этой арене уже идет игра!");
								}
								return true;
							}
						}
						if (Main.nonactivearenas.contains(args[1])) {
							final Arena ar = plug.createArena(args[1]);
							Main.activearenas.add(ar);
							ar.addPl(p.getName());
							return true;
						} else {
							p.sendMessage(Main.pref() + "§cТакой карты не существует!");
							return true;
						}
					} else {
						p.sendMessage(Main.pref() + "§cВы уже на карте, используйте §9/zh leave §cдля выхода!");
						return true;
					}
				} else if (args.length == 1) {
					if (args[0].equalsIgnoreCase("join")) {
						if (Arena.getPlayerArena(p.getName()) == null) {
							final Arena ar = biggestArena(Main.activearenas);
							if (ar != null) {
								if (ar.getState() == GameState.LOBBY_WAIT || ar.getState() == GameState.LOBBY_START) {
									ar.addPl(p.getName());
								} else {
									p.sendMessage(Main.pref() + "§cНа этой арене уже идет игра!");
								}
								return true;
							} else {
								if (Main.nonactivearenas.size() > 0) {
									final Arena a = plug.createArena(Main.nonactivearenas.get(0));
									Main.activearenas.add(a);
									a.addPl(p.getName());
									return true;
								} else {
									p.sendMessage(Main.pref() + "§cНи одной карты еще не создано!");
									return true;
								}
							}
						} else {
							p.sendMessage(Main.pref() + "§cВы уже на карте, используйте §9/zh leave §cдля выхода!");
							return true;
						}
						//выход с карты
					} else if (args[0].equalsIgnoreCase("leave")) {
						if (Arena.getPlayerArena(p.getName()) != null) {
							Arena.getPlayerArena(p.getName()).removePl(p.getName());
							return true;
						} else {
							p.sendMessage(Main.pref() + "§cВы не находитесь в игре!");
							return true;
						}
						//помощь
					} else if (args[0].equalsIgnoreCase("help")) {
						if (p.isOp()) {
							p.sendMessage("§8-=-=-=-=-=-=-=-=-=-=-=-=-=-=-");
							p.sendMessage("§6Помощь по коммандам:");
							p.sendMessage("§6/zh join (название) §7- присоединится к игре");
							p.sendMessage("§6/zh leave §7- выход из игры");
							p.sendMessage("§6/zh help §7- этот текст");
							p.sendMessage("§6/zh create название [мин. кол-во игроков] [макс. кол-во игроков] §7- создание карты");
							p.sendMessage("§6/zh addspawn название §7- добавить точку спавна на карту");
							p.sendMessage("§6/zh finish название §7- окончание разработки карты");
							p.sendMessage("§6/zh delete название §7- удвление карты");
							p.sendMessage("§6/zh setlobby §7- установка лобби");
							p.sendMessage("§6/zh reload §7- перезагрузка конфигов");
							p.sendMessage("§8-=-=-=-=-=-=-=-=-=-=-=-=-=-=-");
							return true;
						}
						p.sendMessage("§8-=-=-=-=-=-=-=-=-=-=-=-=-=-=-");
						p.sendMessage("§6Помощь по коммандам:");
						p.sendMessage("§6/zh join (название) §7- присоединится к игре");
						p.sendMessage("§6/zh leave §7- выход из игры");
						p.sendMessage("§6/zh help §7- этот текст");
						p.sendMessage("§8-=-=-=-=-=-=-=-=-=-=-=-=-=-=-");
						return true;
					}
				}
			} else {
				p.sendMessage(Main.pref() + "§cСначала поставте точку лобби с помощью §6/zh setlobby");
				p.sendMessage(Main.pref() + "§cи добавьте наборы используя §6/zkits add");
				return true;
			}
		}
		return true;
	}

	//арена на которой больше всего игроков
	public Arena biggestArena (final ArrayList<Arena> list) {
		Arena ret = null;

		boolean one = true;
		for (final Arena ar : list) {
			if (ar.getState() == GameState.LOBBY_WAIT || ar.getState() == GameState.LOBBY_START) {
				if (one) {
					ret = ar;
					one = false;
				} else {
					ret = ar.getPlAmount() > ret.getPlAmount() ? ar : ret;
				}
			}
		}
		
		return ret;
	}

}
