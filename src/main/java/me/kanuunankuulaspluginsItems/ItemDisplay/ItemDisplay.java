package me.kanuunankuulaspluginsItems.ItemDisplay;

import me.kanuunankuulaspluginsItems.ItemDisplay.Chat.ItemLinkHandler;
import me.kanuunankuulaspluginsItems.ItemDisplay.Inventory.ViewInvAdmin;
import net.milkbowl.vault.chat.Chat;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;


import static me.kanuunankuulaspluginsItems.ItemDisplay.Inventory.ViewInvNormal.*;

import static org.bukkit.ChatColor.*;


public class ItemDisplay extends JavaPlugin implements Listener, CommandExecutor {

    public static Chat vaultChat;
    private FileConfiguration config;
    public static NamespacedKey viewOnlyKey;
    public static NamespacedKey headOwnerKey;
    public static Plugin Itemdisplay;

    public static final String VIEW_ONLY_MARKER = "itemdisplay_viewonly";
    public static final String INVENTORY_TITLE_SUFFIX = "'s Inventory";

    public static final String VIEW_ONLY_MARKET_TWO = "itemdisplay_viewonly";

    private boolean chatCommandIEnabled;
    private boolean chatCommandInvEnabled;


    private boolean isFolia = false;

    private static final EnumMap<Material, String> categoryCache = new EnumMap<>(Material.class);

    public static Map<UUID, UUID> viewingInventories = new HashMap<>();
    public static Map<UUID, Long> sharedInventoryTimestamps = new HashMap<>();
    
    public static Set<UUID> sharedInventories = new HashSet<>();

    public static void Log(String Message, String type) {
        if (type == "warn") {
            Itemdisplay.getLogger().warning(Message);
        } else if (type == "error") {
            Itemdisplay.getLogger().warning("ERROR: "+ Message);
        } else {
            Itemdisplay.getLogger().info(Message);
        }
    }
    @Override
    public void onEnable() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            isFolia = true;
            getLogger().info("Folia detected! Using region-aware scheduling.");
        } catch (ClassNotFoundException e) {
            isFolia = false;
            getLogger().info("Running on standard Paper/Spigot.");
        }
        ViewInvAdmin.initialize(this);

        saveDefaultConfig();
        config = getConfig();

        if (!config.contains("debug")) {
            config.set("debug", false);
            saveConfig();
        }

        if (!config.contains("use-minecraft-format")) {
            config.set("use-minecraft-format", true);
            saveConfig();
        }

        if (!config.contains("show-advanced-tooltips")) {
            config.set("show-advanced-tooltips", true);
            saveConfig();
        }
        if (!config.contains("chat-commands-enabled.i")) {
            config.set("chat-commands-enabled.i", true);
            saveConfig();
        }

        if (!config.contains("chat-commands-enabled.inv")) {
            config.set("chat-commands-enabled.inv", true);
            saveConfig();
        }

        loadConfigCache();

        getServer().getPluginManager().registerEvents(this, this);

        initializeCategories();
        Itemdisplay = this;

        if (!setupVaultChat()) {
            getLogger().warning("Vault Chat not found! Prefixes will not work.");
        } else {
            getLogger().info("Vault Chat successfully hooked!");
        }
        viewOnlyKey = new NamespacedKey(this, VIEW_ONLY_MARKER);
        viewOnlyKey = new NamespacedKey(this, VIEW_ONLY_MARKET_TWO);

        Plugin headHunterPlugin = getServer().getPluginManager().getPlugin("HeadHunterPlugin");
        if (headHunterPlugin != null) {
            headOwnerKey = new NamespacedKey(headHunterPlugin, "head_owner");
        }

        if (getCommand("viewinv") != null) {
            getCommand("viewinv").setExecutor(this);
        }

        scheduleRepeatingTask(null, this::cleanupViewOnlyItems, 600L, 600L);
        getLogger().info("ItemDisplay plugin enabled!");
    }

    private boolean setupVaultChat() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            getLogger().warning("Vault plugin not found!");
            return false;
        }

        RegisteredServiceProvider<Chat> rsp = getServer().getServicesManager().getRegistration(Chat.class);
        if (rsp != null) {
            vaultChat = rsp.getProvider();
            getLogger().info("Vault Chat provider: " + vaultChat.getClass().getSimpleName());
            return true;
        }

        getLogger().warning("No Vault Chat provider found!");
        return false;
    }

    private void initializeCategories() {
        for (Material mat : Material.values()) {
            String name = mat.name();
            if (Stream.of("SWORD", "AXE", "ARROW", "MACE", "TRIDENT").anyMatch(name::contains)) {
                categoryCache.put(mat, "Combat");
            }
            else if (Stream.of("PICKAXE", "SHOVEL", "HOE", "ROCKET").anyMatch(name::contains)) {
                categoryCache.put(mat, "Tools");
            }
            else if (Stream.of("HELMET", "CHESTPLATE", "LEGGINGS", "BOOTS", "SHIELD", "ELYTRA").anyMatch(name::contains)) {
                categoryCache.put(mat, "Combat");
            }
            else if (mat.isEdible()) {
                categoryCache.put(mat,"Food & Drinks");
            }
            else if (mat.isBlock() && !name.contains("SPAWN_EGG")) {
                categoryCache.put(mat,"Building Blocks");
            } else if (name.contains("SPAWN") && !(name.contains("ANCHOR") && !(name.contains("FROG")) )) {
                categoryCache.put(mat,"Spawn Eggs");
            }
            else {
                categoryCache.put(mat, "Miscellaneous");
            }
        }
    }
    private void loadConfigCache() {
        this.chatCommandIEnabled = config.getBoolean("chat-commands-enabled.i", true);
        this.chatCommandInvEnabled = config.getBoolean("chat-commands-enabled.inv", true);

    }

    @Override
    public void onDisable() {
        getLogger().info("ItemDisplay plugin disabled!");
    }

    private void scheduleTask(Entity entity, Runnable task, long delay) {
        if (isFolia) {
            if (entity != null) {
                entity.getScheduler().run(this, (t) -> task.run(), null);
            } else {
                Bukkit.getGlobalRegionScheduler().runDelayed(this, (t) -> task.run(), delay);
            }
        } else {
            Bukkit.getScheduler().runTaskLater(this, task, delay);
        }
    }

    private void scheduleRepeatingTask(Entity entity, Runnable task, long delay, long period) {
        if (isFolia) {
            if (entity != null) {
                entity.getScheduler().runAtFixedRate(this, (t) -> task.run(), null, delay, period);
            } else {
                Bukkit.getGlobalRegionScheduler().runAtFixedRate(this, (t) -> task.run(), delay, period);
            }
        } else {
            Bukkit.getScheduler().runTaskTimer(this, task, delay, period);
        }
    }

    private void runEntityTask(Entity entity, Runnable task) {
        if (isFolia) {
            entity.getScheduler().run(this, (t) -> task.run(), null);
        } else {
            Bukkit.getScheduler().runTask(this, task);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("itemdisplay")) {
            if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                if (!sender.hasPermission("itemdisplay.reload")) {
                    sender.sendMessage(RED + "You don't have permission to reload the plugin!");
                    return true;
                }

                reloadConfig();
                config = getConfig();
                loadConfigCache();
                sender.sendMessage(GREEN + "Item descriptions reloaded!");
                return true;
            }
        }

        if (command.getName().equalsIgnoreCase("viewinv")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(RED + "Only players can use this command!");
                return true;
            }

            if (args.length == 0) {
                sender.sendMessage(RED + "Usage: /viewinv <player>");
                return true;
            }

            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(RED + "Player not found!");
                return true;
            }

            Player player = (Player) sender;

            if (!player.hasPermission("itemdisplay.viewinv")) {
                player.sendMessage(RED + "You don't have permission to view inventories!");
                return true;
            }

            if (!sharedInventories.contains(target.getUniqueId()) || isInventoryExpired(target.getUniqueId())) {
                if (isInventoryExpired(target.getUniqueId())) {
                    sharedInventories.remove(target.getUniqueId());
                    sharedInventoryTimestamps.remove(target.getUniqueId());
                }
                player.sendMessage(RED + target.getName() + " hasn't shared their inventory or it has expired!");
                return true;
            }

            CreateTheInv(target, player);

            return true;
        }

        if (command.getName().equalsIgnoreCase("invedit")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(RED + "Only players can use this command!");
                return true;
            }

            Player admin = (Player) sender;

            if (!admin.hasPermission("itemdisplay.admin.invedit")) {
                admin.sendMessage(RED + "✗ " + GRAY + "You don't have permission to use this command!");
                return true;
            }

            if (args.length == 0) {
                admin.sendMessage(RED + "Usage: /invedit <player>");
                return true;
            }

            String targetName = args[0];

            @SuppressWarnings("deprecation")
            OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);

            if (target == null || (!target.isOnline() && !target.hasPlayedBefore())) {
                admin.sendMessage(RED + "✗ " + GRAY + "Player " + GOLD + targetName + GRAY + " not found!");
                return true;
            }

            ViewInvAdmin.openPlayerInventory(admin, target);

            return true;
        }

        if (command.getName().equalsIgnoreCase("echestedit")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(RED + "Only players can use this command!");
                return true;
            }

            Player admin = (Player) sender;

            if (!admin.hasPermission("itemdisplay.admin.echest")) {
                admin.sendMessage(RED + "✗ " + GRAY + "You don't have permission to use this command!");
                return true;
            }

            if (args.length == 0) {
                admin.sendMessage(RED + "Usage: /echestedit <player>");
                return true;
            }

            String targetName = args[0];

            @SuppressWarnings("deprecation")
            OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);

            if (target == null || (!target.isOnline() && !target.hasPlayedBefore())) {
                admin.sendMessage(RED + "✗ " + GRAY + "Player " + GOLD + targetName + GRAY + " not found!");
                return true;
            }

            ViewInvAdmin.openPlayerEnderChest(admin, target);

            return true;
        }

        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (command.getName().equalsIgnoreCase("viewinv") ||
                command.getName().equalsIgnoreCase("invedit") ||
                command.getName().equalsIgnoreCase("echestedit")) {

            if (args.length == 1) {
                String partial = args[0].toLowerCase();
                completions = Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase().startsWith(partial))
                        .collect(Collectors.toList());
            }
        }

        return completions;
    }

    private boolean isInventoryExpired(UUID playerUUID) {
        if (!sharedInventoryTimestamps.containsKey(playerUUID)) {
            return true;
        }

        long sharedTime = sharedInventoryTimestamps.get(playerUUID);
        long currentTime = System.currentTimeMillis();
        long fiveMinutesInMillis = 5 * 60 * 1000;

        return (currentTime - sharedTime) > fiveMinutesInMillis;
    }

    private boolean isViewOnly(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        return meta.getPersistentDataContainer().has(viewOnlyKey, PersistentDataType.STRING);
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();
        if (isPlayerMuted(player)) {
            if ((!chatCommandIEnabled) && (!chatCommandInvEnabled)) {
                return;

            }
                event.setCancelled(true);
            player.sendMessage(RED + "You are muted and cannot send messages!");
            return;
        }

        if (message.contains("[i]") || message.contains("[item]")) {
            if (!chatCommandIEnabled) {
                return;
            }

            event.setCancelled(true);
            runEntityTask(player, () -> ItemLinkHandler.handleItemLinkMessage(player, message));
            return;
        }

        if (message.contains("[inv]")) {
            if (!chatCommandInvEnabled) {
                return;
            }

            event.setCancelled(true);
            runEntityTask(player, () -> handleInventoryDisplay(player, message));
        }
    }

    private boolean isPlayerMuted(Player player) {
        if (checkLibertyBansMute(player)) {
            return true;
        }

        try {
            org.bukkit.plugin.Plugin essentials = null;
            for (org.bukkit.plugin.Plugin plugin : getServer().getPluginManager().getPlugins()) {
                if (plugin.getName().startsWith("Essentials")) {
                    essentials = plugin;
                    break;
                }
            }

            if (essentials == null) {
                return false;
            }

            try {
                com.earth2me.essentials.Essentials ess = (com.earth2me.essentials.Essentials) essentials;
                com.earth2me.essentials.User user = ess.getUser(player);

                if (user == null) {
                    return false;
                }

                return user.isMuted();

            } catch (Exception castError) {
                try {
                    Class<?> essentialsClass = essentials.getClass();
                    Object essInstance = essentialsClass.cast(essentials);

                    java.lang.reflect.Method getUserMethod = essentialsClass.getMethod("getUser", Player.class);
                    Object user = getUserMethod.invoke(essInstance, player);

                    if (user == null) {
                        return false;
                    }

                    java.lang.reflect.Method isMutedMethod = user.getClass().getMethod("isMuted");
                    return (Boolean) isMutedMethod.invoke(user);

                } catch (Exception reflectionError) {
                    return false;
                }
            }

        } catch (Exception e) {
            return false;
        }
    }

    private boolean checkLibertyBansMute(Player player) {
        try {
            org.bukkit.plugin.Plugin libertyBans = getServer().getPluginManager().getPlugin("LibertyBans");

            if (libertyBans == null) {
                return false;
            }

            try {
                Class.forName("space.arim.libertybans.core.Part");
                Class<?> apiProviderClass = Class.forName("space.arim.libertybans.api.LibertyBans");
                java.lang.reflect.Method getMethod = apiProviderClass.getMethod("get");
                Object libertyBansApi = getMethod.invoke(null);

                if (libertyBansApi != null) {
                    java.lang.reflect.Method getSelectorMethod = libertyBansApi.getClass().getMethod("getSelector");
                    Object selector = getSelectorMethod.invoke(libertyBansApi);

                    Class<?> victimClass = Class.forName("space.arim.libertybans.api.Victim");
                    Class<?> playerVictimClass = Class.forName("space.arim.libertybans.api.PlayerVictim");
                    java.lang.reflect.Method ofMethod = playerVictimClass.getMethod("of", java.util.UUID.class);
                    Object victim = ofMethod.invoke(null, player.getUniqueId());

                    java.lang.reflect.Method selectionBuilderMethod = selector.getClass().getMethod("selectionBuilder");
                    Object selectionBuilder = selectionBuilderMethod.invoke(selector);

                    Class<?> punishmentTypeClass = Class.forName("space.arim.libertybans.api.PunishmentType");
                    Object muteType = punishmentTypeClass.getField("MUTE").get(null);

                    java.lang.reflect.Method typeMethod = selectionBuilder.getClass().getMethod("type", punishmentTypeClass);
                    selectionBuilder = typeMethod.invoke(selectionBuilder, muteType);

                    java.lang.reflect.Method victimMethod = selectionBuilder.getClass().getMethod("victim", victimClass);
                    selectionBuilder = victimMethod.invoke(selectionBuilder, victim);

                    java.lang.reflect.Method buildMethod = selectionBuilder.getClass().getMethod("build");
                    Object selectionOrder = buildMethod.invoke(selectionBuilder);

                    java.lang.reflect.Method getFirstSpecificPunishmentMethod = selectionOrder.getClass().getMethod("getFirstSpecificPunishment");
                    Object futureOptional = getFirstSpecificPunishmentMethod.invoke(selectionOrder);

                    java.lang.reflect.Method joinMethod = futureOptional.getClass().getMethod("join");
                    Object optional = joinMethod.invoke(futureOptional);

                    java.lang.reflect.Method isPresentMethod = optional.getClass().getMethod("isPresent");
                    return (Boolean) isPresentMethod.invoke(optional);
                }

            } catch (ClassNotFoundException e) {
            }

        } catch (Exception e) {
        }

        return false;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        UUID playerUUID = player.getUniqueId();

        if (viewingInventories.containsKey(playerUUID)) {
            String inventoryTitle = event.getView().getTitle();
            if (inventoryTitle.contains(INVENTORY_TITLE_SUFFIX)) {
                event.setCancelled(true);
                return;
            }
        }

        ItemStack clickedItem = event.getCurrentItem();
        if (isViewOnly(clickedItem)) {
            event.setCancelled(true);
            return;
        }

        ItemStack cursorItem = event.getCursor();
        if (isViewOnly(cursorItem)) {
            event.setCancelled(true);
            return;
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        UUID playerUUID = player.getUniqueId();

        if (viewingInventories.containsKey(playerUUID)) {
            String inventoryTitle = event.getView().getTitle();
            if (inventoryTitle.contains(INVENTORY_TITLE_SUFFIX)) {
                event.setCancelled(true);
                return;
            }
        }

        ItemStack oldCursor = event.getOldCursor();
        if (isViewOnly(oldCursor)) {
            event.setCancelled(true);
            return;
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        ItemStack droppedItem = event.getItemDrop().getItemStack();
        if (isViewOnly(droppedItem)) {
            event.setCancelled(true);
            scheduleTask(event.getPlayer(), () -> removeViewOnlyItems(event.getPlayer()), 1L);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;

        Player player = (Player) event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        if (viewingInventories.containsKey(playerUUID)) {
            String inventoryTitle = event.getView().getTitle();
            if (inventoryTitle.contains(INVENTORY_TITLE_SUFFIX )) {
                viewingInventories.remove(playerUUID);
                scheduleTask(player, () -> removeViewOnlyItems(player), 1L);
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        scheduleTask(event.getPlayer(), () -> removeViewOnlyItems(event.getPlayer()), 20L);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerUUID = event.getPlayer().getUniqueId();
        viewingInventories.remove(playerUUID);
        removeViewOnlyItems(event.getPlayer());
    }

    @EventHandler
    public void onAdminInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player admin = (Player) event.getWhoClicked();
        UUID adminUUID = admin.getUniqueId();
        String invTitle = event.getView().getTitle();

        if (ViewInvAdmin.isAdminViewInventory(invTitle)) {

            int slot = event.getRawSlot();
            ItemStack clickedItem = event.getCurrentItem();
            ItemStack cursorItem = event.getCursor();

            boolean isEnderChest = invTitle.contains(ViewInvAdmin.ADMIN_ENDER_SUFFIX);
            String requiredPerm = isEnderChest ? ViewInvAdmin.EchestAdminPerm : ViewInvAdmin.InventoryAdminPerm;

            if (!admin.hasPermission(requiredPerm)) {
                event.setCancelled(true);
                admin.closeInventory();
                admin.sendMessage(RED + "✗ " + GRAY + "You don't have permission to edit inventories!");
                ViewInvAdmin.cleanupAdminView(adminUUID);
                return;
            }

            int inventorySize = event.getInventory().getSize();

            if (slot >= inventorySize) {
                return;
            }

            if (!isEnderChest && inventorySize == 54) {
                if (slot >= 36 && slot <= 44) {
                    event.setCancelled(true);
                    return;
                }

                if (slot == 45 || slot == 46 || slot == 51 || slot == 53) {
                    event.setCancelled(true);
                    return;
                }
            }

            if (clickedItem != null && ViewInvAdmin.isPlaceholderItem(clickedItem)) {
                if (cursorItem != null && cursorItem.getType() != Material.AIR) {
                    event.setCancelled(false);

                    scheduleTask(admin, () -> {
                        event.getInventory().setItem(slot, cursorItem.clone());
                        admin.setItemOnCursor(null);
                    }, 1L);

                    event.setCancelled(true);
                    return;
                } else {
                    event.setCancelled(true);
                    return;
                }
            }

            if (cursorItem != null && ViewInvAdmin.isPlaceholderItem(cursorItem)) {
                event.setCancelled(true);
                return;
            }

            if (!isEnderChest && inventorySize == 54) {
                if ((slot >= 47 && slot <= 50) || slot == 52) {
                    if (clickedItem != null && !ViewInvAdmin.isPlaceholderItem(clickedItem)) {
                        scheduleTask(admin, () -> {
                            ItemStack currentItem = event.getInventory().getItem(slot);
                            if (currentItem == null || currentItem.getType() == Material.AIR) {
                                Material placeholderMat;
                                String slotName;

                                switch (slot) {
                                    case 47:
                                        placeholderMat = Material.IRON_HELMET;
                                        slotName = "Helmet";
                                        break;
                                    case 48:
                                        placeholderMat = Material.IRON_CHESTPLATE;
                                        slotName = "Chestplate";
                                        break;
                                    case 49:
                                        placeholderMat = Material.IRON_LEGGINGS;
                                        slotName = "Leggings";
                                        break;
                                    case 50:
                                        placeholderMat = Material.IRON_BOOTS;
                                        slotName = "Boots";
                                        break;
                                    case 52:
                                        placeholderMat = Material.SHIELD;
                                        slotName = "Offhand";
                                        break;
                                    default:
                                        return;
                                }

                                event.getInventory().setItem(slot, ViewInvAdmin.createArmorPlaceholder(placeholderMat, slotName));
                            }
                        }, 1L);
                    }
                }
            }

            return;
        }

        if (viewingInventories.containsKey(adminUUID)) {
            String inventoryTitle = event.getView().getTitle();
            if (inventoryTitle.contains(INVENTORY_TITLE_SUFFIX)) {
                event.setCancelled(true);
                return;
            }
        }

        ItemStack clickedItem2 = event.getCurrentItem();
        if (isViewOnly(clickedItem2)) {
            event.setCancelled(true);
            return;
        }

        ItemStack cursorItem2 = event.getCursor();
        if (isViewOnly(cursorItem2)) {
            event.setCancelled(true);
            return;
        }
    }

    @EventHandler
    public void onAdminInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player admin = (Player) event.getWhoClicked();
        String invTitle = event.getView().getTitle();

        if (ViewInvAdmin.isAdminViewInventory(invTitle)) {
            for (int slot : event.getRawSlots()) {
                if ((slot >= 36 && slot <= 44) || slot == 45 || slot == 46 || slot == 51 || slot == 53) {
                    event.setCancelled(true);
                    return;
                }
            }

            if (ViewInvAdmin.isPlaceholderItem(event.getOldCursor())) {
                event.setCancelled(true);
                return;
            }

            return;
        }

        UUID adminUUID = admin.getUniqueId();
        if (viewingInventories.containsKey(adminUUID)) {
            String inventoryTitle = event.getView().getTitle();
            if (inventoryTitle.contains(INVENTORY_TITLE_SUFFIX)) {
                event.setCancelled(true);
                return;
            }
        }

        ItemStack oldCursor = event.getOldCursor();
        if (isViewOnly(oldCursor)) {
            event.setCancelled(true);
            return;
        }
    }

    @EventHandler
    public void onPlayerJoinWhileBeingEdited(PlayerJoinEvent event) {
        UUID joinedPlayerUUID = event.getPlayer().getUniqueId();

        for (Map.Entry<UUID, UUID> entry : ViewInvAdmin.adminViewingPlayer.entrySet()) {
            UUID adminUUID = entry.getKey();
            UUID targetUUID = entry.getValue();

            if (targetUUID.equals(joinedPlayerUUID)) {
                Player admin = Bukkit.getPlayer(adminUUID);

                if (admin != null && admin.isOnline()) {
                    scheduleTask(admin, () -> {
                        admin.closeInventory();
                        admin.sendMessage(YELLOW + "⚠ " + GOLD + event.getPlayer().getName() +
                                GRAY + " came online!");
                        admin.sendMessage(YELLOW + "⚠ " + GRAY + "Their offline inventory editor has been closed to prevent data loss.");
                        admin.sendMessage(GRAY + "Use " + GOLD + "/invedit " + event.getPlayer().getName() +
                                GRAY + " to edit their live inventory.");

                        ViewInvAdmin.cleanupAdminView(adminUUID);
                    }, 1L);

                    scheduleTask(event.getPlayer(), () -> {
                        event.getPlayer().sendMessage(YELLOW + "⚠ " + GRAY + "An admin was viewing your offline inventory.");
                        event.getPlayer().sendMessage(GRAY + "The editor has been closed to protect your items.");
                    }, 20L);
                }
            }
        }
    }

    @EventHandler
    public void onAdminInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;

        Player admin = (Player) event.getPlayer();
        UUID adminUUID = admin.getUniqueId();
        String invTitle = event.getView().getTitle();

        if (ViewInvAdmin.isAdminViewInventory(invTitle)) {
            boolean isEnderChest = invTitle.contains(ViewInvAdmin.ADMIN_ENDER_SUFFIX);
            String requiredPerm = isEnderChest ? ViewInvAdmin.EchestAdminPerm : ViewInvAdmin.InventoryAdminPerm;

            if (!admin.hasPermission(requiredPerm)) {
                admin.closeInventory();
                admin.sendMessage(RED + "✗ " + GRAY + "You don't have permission to edit inventories!");
                ViewInvAdmin.cleanupAdminView(admin.getUniqueId());
                return;
            }


            ViewInvAdmin.saveInventoryChanges(admin, event.getInventory());
            ViewInvAdmin.cleanupAdminView(adminUUID);
            return;
        }

        if (viewingInventories.containsKey(adminUUID)) {
            String inventoryTitle = event.getView().getTitle();
            if (inventoryTitle.contains(INVENTORY_TITLE_SUFFIX)) {
                viewingInventories.remove(adminUUID);
                scheduleTask(admin, () -> removeViewOnlyItems(admin), 1L);
            }
        }
    }

    @EventHandler
    public void onPlayerQuitWithAdminView(PlayerQuitEvent event) {
        UUID playerUUID = event.getPlayer().getUniqueId();

        viewingInventories.remove(playerUUID);
        removeViewOnlyItems(event.getPlayer());

        ViewInvAdmin.cleanupAdminView(playerUUID);
    }

    private void removeViewOnlyItems(Player player) {
        org.bukkit.inventory.PlayerInventory inv = player.getInventory();

        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack item = inv.getItem(i);
            if (isViewOnly(item)) {
                inv.setItem(i, null);
            }
        }

        ItemStack[] armor = inv.getArmorContents();
        for (int i = 0; i < armor.length; i++) {
            if (isViewOnly(armor[i])) {
                armor[i] = null;
            }
        }
        inv.setArmorContents(armor);

        if (isViewOnly(inv.getItemInOffHand())) {
            inv.setItemInOffHand(null);
        }
    }

    private void cleanupViewOnlyItems() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            removeViewOnlyItems(player);
        }
    }

    public static String getCleanPlayerName(Player player) {
        return player.getName();
    }

    public static String getPlayerPrefix(Player player) {
        if (vaultChat == null) {
            return "";
        }

        try {
            String world = player.getWorld().getName();
            String prefix = vaultChat.getPlayerPrefix(world, player);

            if (prefix == null || prefix.isEmpty()) {
                prefix = vaultChat.getPlayerPrefix(player);
            }

            if (prefix != null && !prefix.isEmpty()) {
                if (!prefix.endsWith(" ")) {
                    prefix += " ";
                }
                return prefix;
            }

        } catch (Exception e) {
        }

        if (player.hasMetadata("prefix")) {
            String prefix = player.getMetadata("prefix").get(0).asString();
            return prefix.endsWith(" ") ? prefix : prefix + " ";
        }

        return "";
    }
}
