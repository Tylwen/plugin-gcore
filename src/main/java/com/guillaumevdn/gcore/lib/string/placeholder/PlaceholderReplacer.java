package com.guillaumevdn.gcore.lib.string.placeholder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

import org.bukkit.entity.Player;

import com.guillaumevdn.gcore.ConfigGCore;
import com.guillaumevdn.gcore.GCore;
import com.guillaumevdn.gcore.lib.collection.CollectionUtils;
import com.guillaumevdn.gcore.lib.collection.SortedHashMap;
import com.guillaumevdn.gcore.lib.exception.CalculationError;
import com.guillaumevdn.gcore.lib.integration.Integration;
import com.guillaumevdn.gcore.lib.location.Point;
import com.guillaumevdn.gcore.lib.logic.LogicUtils;
import com.guillaumevdn.gcore.lib.number.NumberUtils;
import com.guillaumevdn.gcore.lib.player.PlayerUtils;
import com.guillaumevdn.gcore.lib.serialization.Serializer;
import com.guillaumevdn.gcore.lib.string.StringUtils;

/**
 * @author GuillaumeVDN
 */
public class PlaceholderReplacer implements Comparable<PlaceholderReplacer> {

	private final String id;
	private final int priority;
	private final boolean needPlayer;
	private final List<String> description;
	private final BiFunction<String, Player, String> replacer;

	public PlaceholderReplacer(String id, int priority, boolean needPlayer, List<String> description, BiFunction<String, Player, String> replacer) {
		this.id = id.toLowerCase();
		this.priority = priority;
		this.needPlayer = needPlayer;
		this.description = description;
		this.replacer = replacer;
	}

	// ----- get
	public String getId() {
		return id;
	}

	public int getPriority() {
		return priority;
	}

	public boolean needPlayer() {
		return needPlayer;
	}

	public List<String> getDescription() {
		return description;
	}

	// ----- methods
	private String replace(String placeholder, Player player, boolean silenceMathErrors) {
		try {
			if (!needPlayer || player != null)
				return replacer.apply(placeholder, player);
		} catch (Throwable exception) {
			if (!(exception instanceof CalculationError) || !silenceMathErrors) {
				GCore.inst().getMainLogger().error("Couldn't parse '" + placeholder + "' for placeholder container " + id + " (invalid placeholder ?)",
						exception);
			}
		}
		return null;
	}

	@Override
	public String toString() {
		return id + "/" + priority;
	}

	@Override
	public int compareTo(PlaceholderReplacer other) {
		return priority - other.priority;
	}

	// ----- static methods
	public static String parseAll(String line, Player player) {
		return parseAll(line, player, false);
	}

	public static String parseAll(String line, Player player, boolean silenceMathErrors) {
		if (line == null) {
			return line;
		}

		// Bukkit.getLogger().info(">>>>>>>> Must parse " + line);

		// bracket placeholders
		if (StringUtils.hasBracketPlaceholders(line)) {
			// final Stack<Integer> starts = new Stack<>();
			int start = -1;
			int mustIgnoreEnds = 0;
			boolean hasNested = false;

			for (int i = 0; i < line.length(); ++i) {
				char c = line.charAt(i);
				if (c == '{') {
					// starts.push(i);
					if (start == -1)
						start = i;
					else {
						++mustIgnoreEnds;
						hasNested = true;
					}

					// Bukkit.getLogger().info("> Start, i = " + i + " ; start " + start + " ; mustIgnoreEnds " + mustIgnoreEnds);
				} else if (c == '}') {
					/*
					 * if (starts.isEmpty()) break; // invalid placeholder string
					 */
					if (start == -1)
						continue; // stray closing bracket, ignore it
					if (mustIgnoreEnds != 0) {
						--mustIgnoreEnds;
						continue;
					}

					// Bukkit.getLogger().info("> End, i = " + i + " ; start " + start + " ; mustIgnoreEnds " + mustIgnoreEnds + " ; going
					// to try to parse with " + registered.keySet());

					// find replacement
					// int start = starts.pop();
					String placeholder = line.substring(start + 1, i);
					String replacement = null;
					// PlaceholderReplacer replacementRep = null;

					for (PlaceholderReplacer repl : registered.values()) {
						if ((replacement = repl.replace(placeholder, player, silenceMathErrors)) != null) {
							// replacementRep = repl;
							break;
						}
					}

					// Bukkit.getLogger().info("Placeholder " + placeholder + ", replacement " + replacement + " by " + (replacementRep !=
					// null ? replacementRep.getId() : "/"));

					// found match, replace and continue
					if (replacement != null) {
						line = line.substring(0, start) + replacement + line.substring(i + 1); // this will not contain the placeholder
						i = start - 1; // restart at start (-1 because loop++), maybe we replaced a new placeholder

						// Bukkit.getLogger().info(">> Restart, i = " + i + ", line = '" + line + "' ; start " + start + " ; mustIgnoreEnds " +
						// mustIgnoreEnds);
					}
					// no match ; if it contained nested placeholders, rescan right after its opening bracket so they can be parsed on their own
					else if (hasNested) {
						i = start;
					}

					// this placeholder is done, the next opening bracket starts a new one
					start = -1;
					mustIgnoreEnds = 0;
					hasNested = false;
				}
			}
		}

		// placeholderAPI
		if (StringUtils.hasPercentagePlaceholders(line) && player != null) { // #2749 no longer parse PAPI without a player ; when player is null, some
																				// placeholder expansions return "" instead of null as they should, which
																				// removes the placeholder completely
			final Integration integration = GCore.inst().getIntegration("PlaceholderAPI");
			if (integration.getInstance() != null) {
				line = ((com.guillaumevdn.gcore.integration.placeholderapi.IntegrationInstancePlaceholderAPI) integration.getInstance()).parse(player, line);
			}
		}

		// Bukkit.getLogger().info(">>>> Result " + line);

		return line;
	}

	public static List<String> parseAll(List<String> list, Player player) {
		if (list == null) {
			return null;
		}
		List<String> result = new ArrayList<>();
		list.forEach(line -> result.add(parseAll(line, player)));
		return result;
	}

	public static void parseAllNoClone(List<String> list, Player player) {
		if (list == null) {
			return;
		}
		for (int i = 0; i < list.size(); ++i) {
			list.set(i, parseAll(list.get(i), player));
		}
	}

	public static List<String> parseAll(String[] array, Player player) {
		if (array == null) {
			return null;
		}
		List<String> result = new ArrayList<>();
		for (String line : array) {
			result.add(parseAll(line, player));
		}
		return result;
	}

	public static List<String> describeAll() {
		List<String> desc = new ArrayList<>();
		for (PlaceholderReplacer replacer : registered.values()) {
			desc.addAll(replacer.getDescription());
		}
		return desc;
	}

	// ----- registration
	private static SortedHashMap<String, PlaceholderReplacer> registered = SortedHashMap.valueSorted();

	public static SortedHashMap<String, PlaceholderReplacer> values() {
		return registered;
	}

	public static <T extends PlaceholderReplacer> T register(T container) {
		registered.put(container.getId(), container);
		return container;
	}

	public static void unregister(String id) {
		registered.remove(id);
	}

	// ----- values
	public static final PlaceholderReplacer PLAYER_LOCATION = register(new PlaceholderReplacer("player_location", 1, true,
			CollectionUtils.asList("§7Player location : §8{player_location}"), (placeholderContent, player) -> {
				if (placeholderContent.equalsIgnoreCase("player_location")) {
					return Serializer.POINT.serialize(new Point(player.getLocation()));
				}
				return null; // no match;
			}));

	public static final PlaceholderReplacer PLAYER_DISPLAYNAME = register(new PlaceholderReplacer("player_displayname", 1, true,
			CollectionUtils.asList("§7Player display name : §8{player_displayname}"), (placeholderContent, player) -> {
				if (placeholderContent.equalsIgnoreCase("player_displayname")) {
					return player.getDisplayName();
				}
				return null; // no match;
			}));

	public static final PlaceholderReplacer PLAYER = register(
			new PlaceholderReplacer("player", 1, true, CollectionUtils.asList("§7Player name : §8{player}"), (placeholderContent, player) -> {
				if (placeholderContent.equalsIgnoreCase("player")) {
					return player.getName();
				}
				return null; // no match;
			}));

	public static final PlaceholderReplacer ONLINE_PLAYERS_COUNT = register(
			new PlaceholderReplacer("online_players_cont", 1, false, CollectionUtils.asList("§7Player name : §8{player}"), (placeholderContent, player) -> {
				if (placeholderContent.equalsIgnoreCase("online_players_cont")) {
					return StringUtils.formatNumber((int) PlayerUtils.getOnlineStream().count());
				}
				return null; // no match;
			}));

	public static final PlaceholderReplacer UNKNOWN_PLACEHOLDER = register(new PlaceholderReplacer("unknown_placeholder", 1, false,
			CollectionUtils.asList("§7General unknown placeholder text : §8{unknown_placeholder}"), (placeholderContent, player) -> {
				if (placeholderContent.equalsIgnoreCase("unknown_placeholder")) {
					return ConfigGCore.unknownPlaceholderResult;
				}
				return null; // no match;
			}));

	public static final PlaceholderReplacer RANDOM_IN_LIST = register(new PlaceholderReplacer("random_in_list", 1, false,
			CollectionUtils.asList("§7Random in list : §8{random_in_list:value1,value2,value3,...}"), (placeholderContent, player) -> {
				if (placeholderContent.toLowerCase().startsWith("random_in_list:")) {
					String[] split = placeholderContent.substring("random_in_list:".length()).split(",");
					return split.length == 0 ? "0" : split[NumberUtils.random(0, split.length - 1)];
				}
				return null; // no match;
			}));

	/*
	 * public static final PlaceholderReplacer PLACEHOLDER_API = register(new PlaceholderReplacer("placeholderapi", 998,
	 * true, CollectionUtils.asList("§7Supports Placeholder API placeholders"), (string, player) -> { Integration
	 * integration = GCore.inst().getIntegration("PlaceholderAPI"); if (integration.getInstance() != null) { return
	 * ((com.guillaumevdn.gcore.integration.placeholderapi.IntegrationInstancePlaceholderAPI)
	 * integration.getInstance()).parse(player, string); } return string; }));
	 */

	public static final PlaceholderReplacer RANDOM_INTEGER = register(new PlaceholderReplacer("random_integer", 999, false,
			CollectionUtils.asList("§7Random number : §8{random_integer:MIN,MAX}"), (placeholderContent, player) -> {
				if (placeholderContent.toLowerCase().startsWith("random_integer:")) {
					String[] split = placeholderContent.substring("random_integer:".length()).split(",");
					return new BigDecimal(String.valueOf(NumberUtils.random(Integer.parseInt(split[0]), Integer.parseInt(split[1])))).toPlainString();
				}
				return null; // no match;
			}));

	public static final PlaceholderReplacer RANDOM_DOUBLE = register(new PlaceholderReplacer("random_double", 999, false,
			CollectionUtils.asList("§7Random number : §8{random_double:MIN,MAX}"), (placeholderContent, player) -> {
				if (placeholderContent.toLowerCase().startsWith("random_double:")) {
					String[] split = placeholderContent.substring("random_double:".length()).split(",");
					return new BigDecimal(String.valueOf(NumberUtils.random(Double.parseDouble(split[0]), Double.parseDouble(split[1])))).toPlainString();
				}
				return null; // no match;
			}));

	public static final PlaceholderReplacer MATH = register(
			new PlaceholderReplacer("math", 1000, false, CollectionUtils.asList("§7Math expression : §8{math:EXPRESSION}"), (placeholderContent, player) -> {
				if (placeholderContent.toLowerCase().startsWith("math:")) {
					String expression = placeholderContent.substring("math:".length());
					if (StringUtils.hasPercentagePlaceholders(expression) && player == null) {
						return null; // if still has PlaceholderAPI placeholders, but no player, do not parse at all ; it's probably because we're parsing
										// something with GENERIC for items, we'll reparse them later correctly using the player
					}
					return new BigDecimal(NumberUtils.calculateExpression(expression)).toPlainString();
				}
				return null; // no match;
			}));

	public static final PlaceholderReplacer ROUND = register(new PlaceholderReplacer("mathround", 1001, false,
			CollectionUtils.asList("§7Math round : §8{mathround:places,EXPRESSION}"), (placeholderContent, player) -> {
				if (placeholderContent.toLowerCase().startsWith("mathround:")) {
					String content = placeholderContent.substring("mathround:".length());
					if (StringUtils.hasPercentagePlaceholders(content) && player == null) {
						return null; // if still has PlaceholderAPI placeholders, but no player, do not parse at all ; it's probably because we're parsing
										// something with GENERIC for items, we'll reparse them later correctly using the player
					}
					int index = content.indexOf(',');
					if (index != -1) {
						int places = NumberUtils.integerOrElse(content.substring(0, index), 0);
						double calc = NumberUtils.calculateExpression(content.substring(index + 1));
						return "" + NumberUtils.round(calc, places);
					}
				}
				return null; // no match;
			}));

	public static final PlaceholderReplacer TRIENAL_CONDITIONS = register(new PlaceholderReplacer("trienal_conditions", 1002, true,
			CollectionUtils.asList("§7Permission ternary : §8{permission:PERM,IF_HAS,IF_HASNT}"), (placeholderContent, player) -> {
				if (placeholderContent.toLowerCase().startsWith("permission:")) {
					final String[] split = placeholderContent.substring("permission:".length()).split(",");
					if (PlayerUtils.hasPermission(player, split[0])) {
						return split[1];
					} else {
						return split[2];
					}
				}
				return null; // no match;
			}));

	public static final PlaceholderReplacer LOGIC_TERNARY = register(new PlaceholderReplacer("logic_ternary", 1002, false,
			CollectionUtils.asList("§7Logic ternary : §8{logic:LOGIC,IF_MATCH,IF_DOESNT_MATCH}"), (placeholderContent, player) -> {
				if (placeholderContent.toLowerCase().startsWith("logic:")) {
					final String[] split = placeholderContent.substring("logic:".length()).split(",");
					if (LogicUtils.match(split[0], Replacer.GENERIC))
						return split[1];
					else
						return split[2];
				}
				return null; // no match;
			}));

}
