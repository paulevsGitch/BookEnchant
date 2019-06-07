package paulevs.bookenchant;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

public class BookEnchant extends JavaPlugin implements Listener
{
	private class StorageData
	{
		Inventory[] inventories;
		int index;
		boolean mustOpen;
	}
	
	private static ItemStack[] BOOKS;
	private static int INV_COUNT;
	private static ItemStack ARROW_BACK;
	private static ItemStack ARROW_NEXT;
	private static ItemStack FILLER;
	
	private static String arrowBackName = "§r§2Previous";
	private static String arrowNextName = "§r§aNext";
	private static int[] levelCost = new int[] {5, 6, 7, 8, 9};
	private static int[] lapisCost = new int[] {1, 2, 3, 4, 5};
	
	private static boolean useLapis = true;
	private static boolean useXP = true;
	
	private static String loreXP = "XP:";
	private static String loreLapis = "Lapis:";
	
	private HashMap<Player, StorageData> data = new HashMap<Player, StorageData>();
	
	@Override
	public void onEnable()
	{
		getServer().getPluginManager().registerEvents(this, this);
		loadConfig();
		initBooks();
	}
	
	@EventHandler
	public void onRightClick(PlayerInteractEvent e)
	{
		if (e.getAction() == Action.RIGHT_CLICK_BLOCK && e.getHand() == EquipmentSlot.HAND)
		{
			if (e.getClickedBlock().getType() == Material.ENCHANTING_TABLE && e.getItem() != null && e.getItem().getType() == Material.BOOK)
			{
				e.setCancelled(true);
				StorageData d = new StorageData();
				d.index = 0;
				d.inventories = buildBookInventory();
				d.mustOpen = false;
				data.put(e.getPlayer(), d);
				e.getPlayer().openInventory(d.inventories[0]);
			}
		}
	}
	
	@EventHandler
	public void onInventoryClick(InventoryClickEvent e)
	{
		if (e.getWhoClicked() instanceof Player)
		{
			Player p = (Player) e.getWhoClicked();
			if (data.containsKey(p))
			{
				StorageData data = this.data.get(p);
				int startSlotID = data.inventories[data.index].getSize() - 9;
				int endSlotID = startSlotID + 8;
				e.setCancelled(true);
				if (e.getRawSlot() == startSlotID)
				{
					int index = data.index - 1;
					if (index < 0)
						index += data.inventories.length;
					data.mustOpen = true;
					data.index = index;
					p.openInventory(data.inventories[index]);
				}
				else if (e.getRawSlot() == endSlotID)
				{
					int index = data.index + 1;
					if (index >= data.inventories.length)
						index -= data.inventories.length;
					data.mustOpen = true;
					data.index = index;
					p.openInventory(data.inventories[index]);
				}
				else
				{
					data.mustOpen = false;
				}
				if (e.getRawSlot() < 45)
				{
					int bookSlot = getSlot(p.getInventory());
					if (bookSlot >= 0)
					{
						ItemStack item = e.getInventory().getItem(e.getRawSlot());
						if (item != null)
						{
							item = item.clone();
							ItemStack book = p.getInventory().getItem(bookSlot);
							EnchantmentStorageMeta metaE =(EnchantmentStorageMeta) item.getItemMeta();
							int index = metaE.getStoredEnchants().values().iterator().next() - 1;
							int level = levelCost[index];
							if (!useXP || p.getLevel() > level)
							{
								int lapisSlot = getSlotLapis(p.getInventory());
								if (lapisSlot >= 0 || !useLapis)
								{
									ItemStack lapis = p.getInventory().getItem(lapisSlot);
									if (!useLapis || (lapis != null && lapis.getAmount() > lapisCost[index]))
									{
										ItemMeta meta = item.getItemMeta();
										meta.setLore(new ArrayList<String>());
										item.setItemMeta(meta);
										book.setAmount(book.getAmount() - 1);
										if (useLapis)
											lapis.setAmount(lapis.getAmount() - lapisCost[index]);
										if (useXP)
											p.giveExpLevels(-level);
										int slot = p.getInventory().firstEmpty();
										if (slot < 0)
											p.getWorld().dropItem(p.getLocation(), item);
										else
											p.getInventory().setItem(slot, item);
									}
								}
							}
						}
					}
				}
			}
		}
	}
	
	@EventHandler
	public void onCloseInventory(InventoryCloseEvent e)
	{
		if (e.getPlayer() instanceof Player)
		{
			Player p = (Player) e.getPlayer();
			if (data.containsKey(p))
			{
				StorageData data = this.data.get(p);
				if (!data.mustOpen)
				{
					this.data.remove(p);
				}
				else
					data.mustOpen = false;
			}
		}
	}
	
	private void loadConfig()
	{
		getConfig().options().copyDefaults(true);
		
		if (!getConfig().contains("title.next"))
			getConfig().set("title.next", arrowNextName);
		arrowNextName = getConfig().getString("title.next", arrowNextName);
		
		if (!getConfig().contains("title.back"))
			getConfig().set("title.back", arrowBackName);
		arrowBackName = getConfig().getString("title.back", arrowBackName);
		
		if (!getConfig().contains("lore.xp"))
			getConfig().set("lore.xp", loreXP);
		loreXP = getConfig().getString("lore.xp", loreXP);
		
		if (!getConfig().contains("lore.lapis"))
			getConfig().set("lore.lapis", loreLapis);
		loreLapis = getConfig().getString("lore.lapis", loreLapis);
		
		for (int i = 0; i < levelCost.length; i++)
		{
			String name = "cost.levels-" + i;
			if (!getConfig().contains(name))
				getConfig().set(name, levelCost[i]);
			levelCost[i] = getConfig().getInt(name, levelCost[i]);
		}
		
		for (int i = 0; i < levelCost.length; i++)
		{
			String name = "cost.lapis-" + i;
			if (!getConfig().contains(name))
				getConfig().set(name, lapisCost[i]);
			lapisCost[i] = getConfig().getInt(name, lapisCost[i]);
		}
		
		if (!getConfig().contains("use.xp"))
			getConfig().set("use.xp", useXP);
		useXP = getConfig().getBoolean("use.xp", useXP);
		
		if (!getConfig().contains("use.lapis"))
			getConfig().set("use.lapis", useLapis);
		useLapis = getConfig().getBoolean("use.lapis", useLapis);
		
		saveConfig();
	}
	
	private void initBooks()
	{
		ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
		EnchantmentStorageMeta metaBook;
		ArrayList<ItemStack> items = new ArrayList<ItemStack>();
		for (Enchantment ench: Enchantment.values())
		{
			for (int i = 1; i <= ench.getMaxLevel(); i++)
			{
				ItemStack bookE = book.clone();
				if (useXP || useLapis)
				{
					metaBook = (EnchantmentStorageMeta) bookE.getItemMeta();
					metaBook.addStoredEnchant(ench, i, true);
					List<String> lore = new ArrayList<String>();
					if (useXP)
						lore.add(loreXP + " " + levelCost[i - 1]);
					if (useLapis)
						lore.add(loreLapis + " " + lapisCost[i - 1]);
					metaBook.setLore(lore);
					bookE.setItemMeta(metaBook);
				}
				items.add(bookE);
			}
		}
		BOOKS = items.toArray(new ItemStack[] {});
		INV_COUNT = (int) Math.ceil(BOOKS.length / 45.0);
		
		ARROW_BACK = new ItemStack(Material.ARROW);
		ItemMeta meta = ARROW_BACK.getItemMeta();
		meta.setDisplayName(arrowBackName);
		ARROW_BACK.setItemMeta(meta);
		
		ARROW_NEXT = new ItemStack(Material.ARROW);
		meta = ARROW_NEXT.getItemMeta();
		meta.setDisplayName(arrowNextName);
		ARROW_NEXT.setItemMeta(meta);
		
		FILLER = new ItemStack(Material.WHITE_STAINED_GLASS_PANE);
		meta = FILLER.getItemMeta();
		meta.setDisplayName(" ");
		FILLER.setItemMeta(meta);
	}
	
	private Inventory[] buildBookInventory()
	{
		Inventory[] inv = new Inventory[INV_COUNT];
		for (int i = 0; i < INV_COUNT; i++)
		{
			inv[i] = Bukkit.createInventory(null, 54);
			fillControls(inv[i]);
		}
		int index = 0;
		for (ItemStack book: BOOKS)
		{
			inv[index / 45].setItem(index % 45, book.clone());
			index++;
		}
		return inv;
	}
	
	private void fillControls(Inventory inv)
	{
		inv.setItem(45, ARROW_BACK.clone());
		inv.setItem(53, ARROW_NEXT.clone());
		for (int i = 46; i < 53; i++)
			inv.setItem(i, FILLER.clone());
	}
	
	private int getSlot(Inventory inv)
	{
		for (int i = 0; i < inv.getSize(); i++)
		{
			if (inv.getItem(i) != null && inv.getItem(i).getType() == Material.BOOK)
				return i;
		}
		return -1;
	}
	
	private int getSlotLapis(Inventory inv)
	{
		for (int i = 0; i < inv.getSize(); i++)
		{
			if (inv.getItem(i) != null && inv.getItem(i).getType() == Material.LAPIS_LAZULI)
				return i;
		}
		return -1;
	}
}
