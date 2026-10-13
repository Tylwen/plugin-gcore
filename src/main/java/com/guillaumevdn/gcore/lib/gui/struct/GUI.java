package com.guillaumevdn.gcore.lib.gui.struct;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;

import com.guillaumevdn.gcore.ConfigGCore;
import com.guillaumevdn.gcore.GCore;
import com.guillaumevdn.gcore.lib.GPlugin;
import com.guillaumevdn.gcore.lib.collection.CollectionUtils;
import com.guillaumevdn.gcore.lib.compatibility.material.Mat;
import com.guillaumevdn.gcore.lib.concurrency.RWHashMap;
import com.guillaumevdn.gcore.lib.concurrency.RWLowerCaseHashMap;
import com.guillaumevdn.gcore.lib.concurrency.RWWeakHashMap;
import com.guillaumevdn.gcore.lib.gui.element.item.type.types.border.BorderGUIItem;
import com.guillaumevdn.gcore.lib.gui.internal.Handler;
import com.guillaumevdn.gcore.lib.gui.internal.protocol.ProtocolHandler;
import com.guillaumevdn.gcore.lib.compatibility.Version;
import com.guillaumevdn.gcore.lib.gui.internal.vanilla.VanillaHandler;
import com.guillaumevdn.gcore.lib.gui.struct.ClickCall.ClickType;
import com.guillaumevdn.gcore.lib.number.NumberUtils;
import com.guillaumevdn.gcore.lib.object.ObjectUtils;
import com.guillaumevdn.gcore.lib.plugin.PluginUtils;
import com.guillaumevdn.gcore.lib.string.StringUtils;
import com.guillaumevdn.gcore.lib.tuple.IntegerPair;

/**
 * @author GuillaumeVDN
 */
public class GUI {

	// ----- options
	public static enum Option {
		DONT_UNREGISTER_ON_CLOSE, AUTO_BACK_ITEM, RAW_NAME
	}

	private static final int MAX_LENGTH = 25;
	private static final String APPEND_CUT = "...";

	// ----- base
	private GPlugin plugin;
	private final String id;
	private final String name;
	private final GUIType type;
	private int backItemSlot = -1;
	private final Set<Option> options;
	private final List<Integer> regularItemSlots;
	private final RWLowerCaseHashMap<GUIItem> regularItems = new RWLowerCaseHashMap<>(5, 1f);
	private final RWLowerCaseHashMap<GUIItem> persistentItems = new RWLowerCaseHashMap<>(1, 1f); // expecting more regulars than persistents
	private final Handler handler;
	private long lastFilled = 0L;
	private boolean active = false;
	private RWWeakHashMap<Player, ClickCall> fromCall = new RWWeakHashMap<>(1, 1f); // usually expecting one player per GUI

	public GUI(GPlugin plugin, String id, String name, GUIType type, Option... options) {
		this(plugin, id, name, type, NumberUtils.range(0, type.getRegularItemSlotsEnd()), options);
	}

	public GUI(GPlugin plugin, String id, String name, GUIType type, List<Integer> regularItemSlots, Option... options) {
		this.plugin = plugin;
		this.id = id;
		Set<Option> opts = new HashSet<>();
		for (Option option : options) {
			if (option != null) {
				opts.add(option);
			}
		}
		this.options = opts; // Collections.unmodifiableSet(opts);
		String n = StringUtils.unformat(name);
		if (!this.options.contains(Option.RAW_NAME) && n.length() > MAX_LENGTH) {
			/*
			 * Faut compter les couleurs nécessaires pour arriver au cut pour que le nom qu'on a cut, unformatté, ait la même taille
			 * que le cut normal Perso j'ai la flemme
			 * 
			 * int cutAt = n.length() - (MAX_LENGTH - APPEND_CUT.length()) + (name.length() - n.length());
			 * 
			 * int cutAt = n.length() - (MAX_LENGTH - APPEND_CUT.length()) + (name.length() - n.length()); int cutAtNormal =
			 * n.length() - (MAX_LENGTH - APPEND_CUT.length());
			 * 
			 * Bukkit.getLogger().info("name " + name); Bukkit.getLogger().info("n    " + n); Bukkit.getLogger().info("n    " +
			 * name.replace('§', '&')); Bukkit.getLogger().info("= diff " + (name.length() - n.length()));
			 * Bukkit.getLogger().info("- cutAt " + cutAt); Bukkit.getLogger().info("- cutAtNormal " + cutAtNormal);
			 * Bukkit.getLogger().info("- subst " + name.substring(cutAt, name.length()) + ", length " + (name.substring(cutAt,
			 * name.length()).length()));
			 * 
			 * if (cutAt > 0 && name.charAt(cutAt - 1) == '§') { if (cutAt + 1 >= n.length()) { --cutAt; } else { ++cutAt; } }
			 * this.name = StringUtils.getLastColors(name.substring(0, cutAt)) + APPEND_CUT + name.substring(cutAt, name.length());
			 */

			int extra = n.length() - MAX_LENGTH;

			this.name = StringUtils.getLastColors(name.substring(0, extra)) + APPEND_CUT + name.substring(extra, name.length());
		} else {
			this.name = name;
		}
		this.type = type;
		this.regularItemSlots = regularItemSlots;
		if (this.options.contains(Option.AUTO_BACK_ITEM) && type.getBackItemSlot() != -1)
			regularItemSlots.remove((Integer) (this.backItemSlot = type.getBackItemSlot()));
		// packet-based GUIs rely on NMS names that haven't been mapped for versions newer than 1.21.3
		this.handler = ConfigGCore.allowProtocolGUIs && Version.CURRENT.isLessOrEqualsTo(Version.MC_1_21_R3) && PluginUtils.isPluginEnabled("ProtocolLib") ? new ProtocolHandler(this) : new VanillaHandler(this);
	}

	// ----- get
	public final GPlugin getPlugin() {
		return plugin;
	}

	public final String getId() {
		return id;
	}

	public GUIType getType() {
		return type;
	}

	public final String getName() {
		return name;
	}

	public final Set<Option> getOptions() {
		return options;
	}

	public final RWHashMap<String, GUIItem> getRegularItems() {
		return regularItems;
	}

	public final GUIItem getRegularItem(String id) {
		return regularItems.get(id);
	}

	public final RWHashMap<String, GUIItem> getPersistentItems() {
		return persistentItems;
	}

	public final GUIItem getPersistentItem(String id) {
		return persistentItems.get(id);
	}

	public final int getBackItemSlot() {
		return backItemSlot;
	}

	public final boolean isActive() {
		return active;
	}

	public final ClickCall getFromCall(Player player) {
		return fromCall.get(player);
	}

	public final RWHashMap<Player, Integer> getViewers() {
		return handler.getViewers();
	}

	public final int getViewerPage(Player player) {
		return handler.getViewerPage(player);
	}

	public final <G extends GUI> G castThisOrParentFromCall(Player player, Class<G> clazz) {
		G gui = ObjectUtils.castOrNull(this, clazz);
		if (gui == null) {
			ClickCall c = this.getFromCall(player);
			for (int tries = 0; c != null && tries < 25 /* more than enough, but necessary #1897 #1932 */; ++tries) {
				gui = ObjectUtils.castOrNull(c.getGUI(), clazz);
				if (gui != null) {
					break;
				}
				c = c.getGUI().getFromCall(player);
			}
		}
		return gui;
	}

	// ----- set

	protected void setPlugin(GPlugin plugin) { // this can be useful, for the confirm GUI for instance
		this.plugin = plugin;
	}

	public void setFromCall(Player player, ClickCall fromCall) {
		if (fromCall == null) {
			this.fromCall.remove(player);
		} else {
			this.fromCall.put(player, fromCall);
		}
	}

	// ----- items

	public GUIItem getItem(int page, int slot) {
		return persistentItems.streamResultValues(str -> str.filter(it -> it.isInSlot(slot)).findFirst())
				.orElseGet(() -> regularItems.streamResultValues(str -> str.filter(it -> it.isInLocation(page, slot)).findFirst().orElse(null)));
	}

	public GUIItem getItemWithPerformer(int page, int slot, ClickType type) { // some items allow to override them, but they however can still be detected at
																				// their slot (for instance, dynamic borders)
		return persistentItems.streamResultValues(str -> str.filter(it -> it.isInSlot(slot) && it.getClickPerformer(type) != null).findFirst())
				.orElseGet(() -> regularItems.streamResultValues(
						str -> str.filter(it -> it.isInLocation(page, slot) && it.getClickPerformer(type) != null).findFirst().orElse(null)));
	}

	public GUIItem getRegularItem(int pageIndex, int slot) {
		return regularItems.streamResultValues(str -> str.filter(it -> it.isInLocation(pageIndex, slot)).findFirst().orElse(null));
	}

	public GUIItem getPersistentItem(int slot) {
		return persistentItems.streamResultValues(str -> str.filter(it -> it.isInSlot(slot)).findFirst().orElse(null));
	}

	public void setItem(GUIItem item, boolean persistent) {
		if (persistent) {
			setPersistentItem(item);
		} else {
			setRegularItem(item);
		}
	}

	public void setRegularItem(GUIItem item) {
		// invalid item
		if (Mat.isVoid(item.getItem())) {
			throw new IllegalArgumentException("can't set regular item " + item.getId() + " in GUI " + getId() + ", void item");
		}

		// invalid slot
		for (IntegerPair preferredLocation : item.getLocations()) {
			if (preferredLocation.getB() < -1 || preferredLocation.getB() >= type.getSize()) {
				throw new IllegalArgumentException("can't set regular item " + item.getId() + " in GUI " + getId() + ", preferred slot "
						+ preferredLocation.getB() + " is outside bounds (-1 to " + (type.getSize() - 1) + ")");
			}
		}

		// remove if existing
		GUIItem previous = removeRegularItem(item);

		// get locations
		List<IntegerPair> locations;
		if (previous != null && !previous.getLocations().isEmpty()) { // get from previous
			locations = previous.getLocations();
		} else { // recalculate them
			locations = new ArrayList<>();
			if (item.getPreferredLocations().isEmpty()) {
				IntegerPair location = findOrCreateFreeForRegular(-1, -1);
				if (location == null) {
					GCore.inst().getMainLogger().error("can't set regular item " + item.getId() + " in GUI " + getId() + ", no location found");
				} else {
					locations.add(location);
				}
			} else {
				for (IntegerPair preferredLocation : item.getPreferredLocations()) {
					IntegerPair location = findOrCreateFreeForRegular(preferredLocation.getA(), preferredLocation.getB());
					if (location == null) {
						GCore.inst().getMainLogger().error("can't set regular item " + item.getId() + " in GUI " + getId()
								+ ", no location found for preferred " + preferredLocation.toString());
					} else {
						locations.add(location);
					}
				}
			}
		}

		// register it
		regularItems.put(item.getId(), item);

		// set item
		locations.forEach(location -> {
			handler.setPageItem(location, item.getItem());
		});
		item.setLocations(locations);
	}

	@Deprecated
	public void sendPageItem(IntegerPair location, ItemStack item) {
		handler.setPageItem(location, item);
	}

	public void setPersistentItem(GUIItem item) {
		// invalid item
		if (Mat.isVoid(item.getItem())) {
			throw new IllegalArgumentException("can't set persistent item " + item.getId() + " in GUI " + getId() + ", void item");
		}

		// invalid locations
		if (item.getPreferredLocations().isEmpty()) {
			throw new IllegalArgumentException("can't set persistent item " + item.getId() + " in GUI " + getId() + ", no location configured");
		}
		for (IntegerPair preferredLocation : item.getPreferredLocations()) {
			if (preferredLocation.getA() > 0) {
				throw new IllegalArgumentException("can't set persistent item " + item.getId() + " in GUI " + getId() + ", no preferred page can be specified");
			} else if (preferredLocation.getB() < 0 /* no -1 for persistent items */ || preferredLocation.getB() >= type.getSize()) {
				throw new IllegalArgumentException("can't set persistent item " + item.getId() + " in GUI " + getId() + ", preferred slot "
						+ preferredLocation.getB() + " is outside bounds (-1 to " + (type.getSize() - 1) + ")");
			} else if (preferredLocation.getB() == backItemSlot) {
				throw new IllegalArgumentException("can't set persistent item " + item.getId() + " in GUI " + getId() + ", preferred slot "
						+ preferredLocation.getB() + " is the back item slot");
			}
			if (!(item instanceof BorderGUIItem /* border items will simply be skipped on these slots */)) {
				GUIItem existing = getPersistentItem(preferredLocation.getB());
				if (existing != null && !existing.getId().equals(item.getId())
						&& !(existing instanceof BorderGUIItem) /* existing border items will simply be overriden */) {
					throw new IllegalArgumentException("can't set persistent item " + item.getId() + " in GUI " + getId() + ", preferred slot "
							+ preferredLocation.getB() + " already has persistent item " + existing.getId());
				}
			}
		}

		// remove if existing
		removePersistentItem(item);

		// register it
		persistentItems.put(item.getId(), item);

		// ensure there's at least one page
		// when we call refill() manually, we sometimes end up adding a persistent dynamic border at first, and it's added
		// nowhere because there are no pages
		if (getPageCount() == 0) {
			createPage();
		}

		// add it on all pages
		List<GUIItem> existingRegularItems = new ArrayList<>();
		List<IntegerPair> locations = new ArrayList<>();
		item.getPreferredLocations().forEach(preferredLocation -> {
			// do not override item, if there's already a non-border item
			GUIItem existingPersistent = getPersistentItem(preferredLocation.getB());
			if (existingPersistent != null && !existingPersistent.equals(item) && !(existingPersistent instanceof BorderGUIItem)) {
				return;
			}
			// set item
			for (int pageIndex = 0; pageIndex < handler.getPageCount(); ++pageIndex) {
				// control item
				if ((pageIndex > 0 && preferredLocation.getB() == type.getPreviousPageItemSlot())
						|| (pageIndex + 1 < handler.getPageCount() && preferredLocation.getB() == type.getNextPageItemSlot())
						|| preferredLocation.getB() == backItemSlot) {
					continue;
				}
				// remove existing regular item if any
				GUIItem existingRegular = getRegularItem(pageIndex, preferredLocation.getB());
				if (existingRegular != null) {
					handler.clearPageItem(pageIndex, preferredLocation.getB());
					existingRegularItems.add(existingRegular);
				}
				// set item
				handler.setPageItem(pageIndex, preferredLocation.getB(), item.getItem());
				locations.add(IntegerPair.of(pageIndex, preferredLocation.getB()));
			}
		});

		// re-add existing regular items
		existingRegularItems.forEach(existingItem -> setRegularItem(existingItem));
		item.setLocations(locations);
	}

	public GUIItem removeRegularItem(GUIItem item) {
		return removeRegularItem(item.getId());
	}

	public GUIItem removeRegularItem(String itemId) {
		GUIItem item = regularItems.remove(itemId);
		if (item != null) {
			item.getLocations().forEach(location -> handler.clearPageItem(location));
		}
		return item;
	}

	public boolean removePersistentItem(GUIItem item) {
		return removePersistentItem(item.getId()) != null;
	}

	public boolean removePersistentItem(int slot) {
		GUIItem item = getPersistentItem(slot);
		return item == null ? false : removePersistentItem(item);
	}

	public GUIItem removePersistentItem(String itemId) {
		GUIItem item = persistentItems.remove(itemId);
		if (item != null) {
			item.getLocations().forEach(location -> handler.clearPageItem(location));
		}
		return item;
	}

	public void removeItem(GUIItem item, boolean persistent) {
		if (persistent) {
			removePersistentItem(item);
		} else {
			removeRegularItem(item);
		}
	}

	public void removeItem(String itemId, boolean persistent) {
		if (persistent) {
			removePersistentItem(itemId);
		} else {
			removeRegularItem(itemId);
		}
	}

	public void clear() {
		handler.clear();
		persistentItems.clear();
		regularItems.clear();
	}

	// ----- page
	public final int getPageCount() {
		return handler.getPageCount();
	}

	public final int createPage() {
		// create page
		handler.createPage();
		int pageIndex = handler.getPageCount() - 1;
		// update page controllers
		List<GUIItem> existing = new ArrayList<>();
		if (pageIndex != 0) {
			// add previous page item on this page
			if (type.getPreviousPageItemSlot() != -1) {
				handler.setPageItem(pageIndex, type.getPreviousPageItemSlot(), ConfigGCore.previousPageItem);
			}
			// add next page item on previous page
			if (type.getNextPageItemSlot() != -1) {
				GUIItem existingItem = getRegularItem(pageIndex - 1, type.getNextPageItemSlot());
				handler.setPageItem(pageIndex - 1, type.getNextPageItemSlot(), ConfigGCore.nextPageItem);
				if (existingItem != null) {
					existing.add(existingItem);
				}
			}
		}
		// add back item
		if (backItemSlot != -1) {
			handler.setPageItem(pageIndex, backItemSlot, ConfigGCore.backItem);
		}
		// add persistent items
		persistentItems.forEach((__, persistentItem) -> {
			persistentItem.getPreferredLocations().forEach(preferredLocation -> {
				// do not set if it's a previous page item slot
				if (pageIndex != 0 && preferredLocation.getB() == type.getPreviousPageItemSlot()) {
					return;
				}
				// do not override item, if there's already a non-border item
				GUIItem existingPersistent = getPersistentItem(preferredLocation.getB());
				if (existingPersistent != null && !existingPersistent.equals(persistentItem) && !(existingPersistent instanceof BorderGUIItem)) {
					return;
				}
				// set item
				handler.setPageItem(pageIndex, preferredLocation.getB(), persistentItem.getItem());
				// update locations
				List<IntegerPair> locations = CollectionUtils.asList(persistentItem.getLocations());
				locations.add(IntegerPair.of(pageIndex, preferredLocation.getB()));
				persistentItem.setLocations(locations);
			});
		});
		// update existing items
		existing.forEach(existingItem -> {
			existingItem.setLocations(new ArrayList<>());
			setRegularItem(existingItem);
		});
		// we good
		return pageIndex;
	}

	private final IntegerPair findOrCreateFreeForRegular(int preferredPageIndex, int preferredSlot) {
		return doFindOrCreateFreeForRegular(preferredPageIndex, preferredSlot, false);
	}

	private final IntegerPair doFindOrCreateFreeForRegular(int preferredPageIndex, int preferredSlot, boolean createdPage) {
		// has a preferred page
		if (preferredPageIndex >= 0) {
			// force create pages until count is reached
			while (preferredPageIndex >= handler.getPageCount()) {
				createPage();
			}
			// has preferred slot
			if (preferredSlot >= 0) {
				return isSlotFreeForRegular(preferredPageIndex, preferredSlot) ? IntegerPair.of(preferredPageIndex, preferredSlot) : null;
			}
			// no preferred slot
			else {
				int firstEmpty = handler.firstEmpty(preferredPageIndex);
				return firstEmpty != -1 && regularItemSlots.contains(firstEmpty) ? IntegerPair.of(preferredPageIndex, firstEmpty) : null;
			}
		}
		// no preferred page
		else {
			// has preferred slot
			if (preferredSlot >= 0 && preferredSlot < type.getSize()) {
				for (int page = 0; page < handler.getPageCount(); ++page) {
					if (isSlotFreeForRegular(page, preferredSlot)) {
						return IntegerPair.of(page, preferredSlot);
					}
				}
			}
			// no preferred slot
			else {
				for (int page = 0; page < handler.getPageCount(); ++page) {
					int firstEmpty = handler.firstEmpty(page);
					if (firstEmpty != -1 && regularItemSlots.contains(firstEmpty)) {
						return IntegerPair.of(page, firstEmpty);
					}
				}
			}
		}
		// couldn't find any, eventually return a new page
		if (!createdPage) {
			createPage();
			return doFindOrCreateFreeForRegular(preferredPageIndex, preferredSlot, true);
		}
		return null;
	}

	public final boolean isSlotFreeForRegular(int pageIndex, int slot) {
		// no page
		if (pageIndex >= handler.getPageCount()) {
			return false;
		}
		// not in regular slots
		if (!regularItemSlots.contains(slot)) {
			return false;
		}
		// has page control item
		if (type.getPreviousPageItemSlot() != -1 && slot == type.getPreviousPageItemSlot() && pageIndex > 0) {
			return false;
		}
		if (type.getNextPageItemSlot() != -1 && slot == type.getNextPageItemSlot() && pageIndex + 1 < handler.getPageCount()) {
			return false;
		}
		if (backItemSlot != -1 && slot == backItemSlot) {
			return false;
		}
		// has a persistent item there
		if (getPersistentItem(slot) != null) {
			return false;
		}
		// there's already an item there
		ItemStack item = handler.getPageItem(pageIndex, slot);
		if (!Mat.isVoid(item)) {
			return false;
		}
		// we good
		return true;
	}

	// ----- open and fill
	/** @return true if the GUI must be opened after refilling it */
	public boolean refill() {
		final Map<Player, Integer> viewers = getViewers();

		// clear/fill
		clear();
		boolean open = doFill();
		lastFilled = System.currentTimeMillis();

		// open new pages for viewers ; old pages are cleared, but kept open, so the cursor doesn't recenter when we reopen new
		// pages
		if (open) {
			if (handler.getPageCount() == 0) {
				createPage();
			}
			viewers.forEach((player, pageIndex) -> {
				openFor(player, pageIndex < handler.getPageCount() ? pageIndex : handler.getPageCount() - 1, getFromCall(player));
			});
		}
		// or maybe just close pages for viewers
		else {
			viewers.keySet().forEach(player -> handler.close(player));
		}

		// done
		return open;
	}

	/** @return true if the GUI must be opened after filling it */
	protected boolean doFill() {
		return true;
	}

	/** @return true if the GUI was opened */
	public final boolean refillAndOpenFor(Player player, ClickCall fromCall) {
		int currentPage = getViewerPage(player);
		return refill() && openFor(player, currentPage == -1 ? 0 : currentPage, fromCall);
	}

	/** @return true if the GUI was opened */
	public final boolean openFor(Player player, ClickCall fromCall) {
		return openFor(player, 0, fromCall);
	}

	/** @return true if the GUI was opened ; this method can be overriden for extra monitoring but must call super */
	public boolean openFor(Player player, int pageIndex, ClickCall fromCall) {
		return doOpenFor(player, pageIndex, fromCall);
	}

	/** @return true if the GUI was opened */
	private final boolean doOpenFor(Player player, int pageIndex, ClickCall fromCall) {

		final ClickCall previousFromCall = getFromCall(player);
		setFromCall(player, fromCall); // some GUIs will use this as early as the items building step, so do it first

		// create a page if has none
		if (handler.getPageCount() == 0) {
			createPage();
		}

		// not active ; fill it (it might have been unregistered on close, or it's a new GUI)
		if (!isActive()) {
			activate();
			lastFilled = System.currentTimeMillis();
			if (!doFill()) {
				setFromCall(player, previousFromCall); // revert from call, since we didn't open it
				return false;
			}
		}
		// active
		else {
			// maybe fill if it was never filled (don't know when this could happen, but it maybe could)
			if (lastFilled == 0L) {
				lastFilled = System.currentTimeMillis();
				if (!doFill()) {
					setFromCall(player, previousFromCall); // revert from call, since we didn't open it
					return false;
				}
			}
		}

		// invalid page
		/*
		 * if (pageIndex >= handler.getPageCount()) { throw new IllegalArgumentException("can't open GUI " + getId() +
		 * ", page index " + pageIndex + " is outside bounds"); }
		 */
		if (pageIndex >= handler.getPageCount()) {
			pageIndex = handler.getPageCount() - 1;
		}

		// remove player from all other GUIs if ProtocolLib ; because the window close packet isn't sent when opening a window
		// while another is already opened, and this creates messy issues (player is considered to be on multiple pages at the
		// same time)
		if (PluginUtils.isPluginEnabled("ProtocolLib")) {
			PluginUtils.getGPlugins().forEach(plugin -> plugin.getGUIs().forEach(gui -> {
				ProtocolHandler handler = ObjectUtils.castOrNull(((GUI) gui).handler, ProtocolHandler.class);
				if (handler != null) {
					handler.removeViewer(player);
					if (gui.equals(this)) { // same GUI ? ignore it, we're reopening a page so he'll be added to the new viewers list
					} else {
						// otherwise, call remove on other handler
						handler.onClose(player);
					}
				}
			}));
		}

		// open page
		handler.openPage(player, pageIndex);
		return true;
	}

	// ----- registration and listeners
	public GUI activate() {
		// already active
		if (active) {
			return this;
		}
		// set active
		active = true;
		plugin.registerGUI(this);
		handler.activate();
		onActivate();
		return this;
	}

	public void onActivate() {
	}

	public GUI deactivate(boolean clear) {
		// not active
		if (!active) {
			return this;
		}

		// set inactive
		active = false;
		plugin.unregisterGUI(this);
		handler.deactivate();

		// close
		getViewers().forEach((player, __) -> handler.close(player));

		// clear
		if (clear) {
			clear();
		}
		onDeactivate();
		return this;
	}

	public void onDeactivate() {
	}

	public void onPlayerInventoryClick(ClickCall call, ItemStack item) {
	}

	public void onClose(Player clicker) {
	}

	public void onBack(Player clicker) {
		ClickCall fromCall = getFromCall(clicker);
		if (fromCall != null) {
			if (fromCall.getGUI() != null) {
				fromCall.getGUI().openFor(clicker, fromCall.getPageIndex(), fromCall.getGUI().getFromCall(clicker));
				return;
			}
		}
		clicker.closeInventory();
	}

	// ----- obj

	@Override
	public String toString() {
		return getId();
	}

	@Override
	public int hashCode() {
		return getId().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		return ObjectUtils.ifCanBeCastedDo(obj, getClass(), other -> other.getId().equals(getId())).orElse(false);
	}

	// ----- static
	public static GUI getOpenGUI(Player player) {
		return PluginUtils.getGPlugins().stream().flatMap(pl -> (Stream<GUI>) pl.getGUIs().stream()).filter(gui -> gui.handler.isViewer(player)).findFirst()
				.orElse(null);
	}

	public static boolean hasOpenGUI(Player player) {
		if (!player.getOpenInventory().getTopInventory().getType().equals(InventoryType.CRAFTING)) // default opened view is the player's crafting inventory
			return true;
		if (getOpenGUI(player) != null)
			return true;
		return false;
	}

}
