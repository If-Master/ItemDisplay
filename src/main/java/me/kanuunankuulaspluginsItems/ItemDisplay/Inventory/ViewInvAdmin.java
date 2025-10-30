package me.kanuunankuulaspluginsItems.ItemDisplay.Inventory;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.io.*;
import java.util.*;

import static org.bukkit.ChatColor.*;

public class ViewInvAdmin {
    private static final String ADMIN_INV_SUFFIX = "'s Inventory (Admin)";
    public static final String ADMIN_ENDER_SUFFIX = "'s Ender Chest (Admin)";
    public static final String InventoryAdminPerm = "itemdisplay.admin.invedit";
    public static final String EchestAdminPerm = "itemdisplay.admin.echest";

    private static int syncTaskId = -1;

    public static HashMap<UUID, UUID> adminViewingPlayer = new HashMap<>();
    public static HashMap<UUID, Boolean> adminViewingType = new HashMap<>();
    private static HashMap<UUID, OfflineInventoryData> offlineInventoryData = new HashMap<>();
    private static HashMap<UUID, ItemStack[]> lastKnownInventoryState = new HashMap<>();
    private static HashMap<UUID, ItemStack[]> lastKnownArmorState = new HashMap<>();
    private static HashMap<UUID, ItemStack> lastKnownOffhandState = new HashMap<>();

    private static Plugin plugin;
    private static boolean isFolia = false;

    public static void initialize(Plugin pluginInstance) {
        plugin = pluginInstance;
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            isFolia = true;
        } catch (ClassNotFoundException e) {
            isFolia = false;
        }

        startRealTimeSync();
    }

    private static void startRealTimeSync() {
        if (syncTaskId != -1) {
            try {
                Bukkit.getScheduler().cancelTask(syncTaskId);
            } catch (Exception e) {
            }
        }

        if (isFolia) {
            Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, (t) -> syncAllAdminViews(), 1, 10);
        } else {
            syncTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin,
                    ViewInvAdmin::syncAllAdminViews, 1L, 10L);
        }
    }

    private static void syncAllAdminViews() {
        Iterator<Map.Entry<UUID, UUID>> iterator = adminViewingPlayer.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, UUID> entry = iterator.next();
            UUID adminUUID = entry.getKey();
            UUID targetUUID = entry.getValue();

            Player admin = Bukkit.getPlayer(adminUUID);
            Player target = Bukkit.getPlayer(targetUUID);

            if (admin == null || !admin.isOnline() || target == null || !target.isOnline()) {
                continue;
            }

            if (!hasRequiredPermission(admin, adminUUID)) {
                admin.closeInventory();
                admin.sendMessage(RED + "✗ " + GRAY + "Your permission to edit inventories has been revoked!");
                iterator.remove();
                adminViewingType.remove(adminUUID);
                offlineInventoryData.remove(adminUUID);
                lastKnownInventoryState.remove(adminUUID);
                lastKnownArmorState.remove(adminUUID);
                lastKnownOffhandState.remove(adminUUID);
                continue;
            }

            if (offlineInventoryData.containsKey(adminUUID)) {
                continue;
            }

            try {
                String invTitle = admin.getOpenInventory().getTitle();
                if (!isAdminViewInventory(invTitle)) {
                    continue;
                }
                if (invTitle.contains(ADMIN_ENDER_SUFFIX)) {
                    if (!admin.hasPermission(EchestAdminPerm)) return;
                } else {
                if (!admin.hasPermission(InventoryAdminPerm)) return;
}
                Inventory adminView = admin.getOpenInventory().getTopInventory();
                boolean isInventory = adminViewingType.getOrDefault(adminUUID, true);

                if (isInventory) {
                    syncInventoryBidirectional(admin, adminView, target);
                } else {
                    syncEnderChestBidirectional(admin, adminView, target);
                }
            } catch (Exception e) {
            }
        }
    }

    private static void syncInventoryBidirectional(Player admin, Inventory adminView, Player target) {
        UUID adminUUID = admin.getUniqueId();

        ItemStack[] targetInventory = target.getInventory().getContents();
        ItemStack[] targetArmor = target.getInventory().getArmorContents();
        ItemStack targetOffhand = target.getInventory().getItemInOffHand();

        ItemStack[] lastInventory = lastKnownInventoryState.get(adminUUID);
        ItemStack[] lastArmor = lastKnownArmorState.get(adminUUID);
        ItemStack lastOffhand = lastKnownOffhandState.get(adminUUID);

        if (lastInventory == null) {
            storeCurrentState(adminUUID, targetInventory, targetArmor, targetOffhand);
            syncPlayerInventoryToAdminView(adminView, targetInventory, targetArmor, targetOffhand);
            return;
        }

        for (int i = 0; i < 27; i++) {
            ItemStack adminItem = adminView.getItem(i);
            ItemStack lastItem = (lastInventory.length > i + 9) ? lastInventory[i + 9] : null;

            if (!itemsEqualIgnorePlaceholder(adminItem, lastItem)) {
                if (adminItem != null && adminItem.getType() != Material.AIR && !isPlaceholderItem(adminItem)) {
                    target.getInventory().setItem(i + 9, adminItem.clone());
                } else {
                    target.getInventory().setItem(i + 9, null);
                }
            }
        }

        for (int i = 0; i < 9; i++) {
            ItemStack adminItem = adminView.getItem(27 + i);
            ItemStack lastItem = (lastInventory.length > i) ? lastInventory[i] : null;

            if (!itemsEqualIgnorePlaceholder(adminItem, lastItem)) {
                if (adminItem != null && adminItem.getType() != Material.AIR && !isPlaceholderItem(adminItem)) {
                    target.getInventory().setItem(i, adminItem.clone());
                } else {
                    target.getInventory().setItem(i, null);
                }
            }
        }

        checkAndApplyArmorChange(adminView, 47, lastArmor, 3, target, 39);
        checkAndApplyArmorChange(adminView, 48, lastArmor, 2, target, 38);
        checkAndApplyArmorChange(adminView, 49, lastArmor, 1, target, 37);
        checkAndApplyArmorChange(adminView, 50, lastArmor, 0, target, 36);

        ItemStack adminOffhand = adminView.getItem(52);
        if (!itemsEqualIgnorePlaceholder(adminOffhand, lastOffhand)) {
            if (adminOffhand != null && adminOffhand.getType() != Material.AIR && !isPlaceholderItem(adminOffhand)) {
                target.getInventory().setItemInOffHand(adminOffhand.clone());
            } else {
                target.getInventory().setItemInOffHand(null);
            }
        }

        syncPlayerInventoryToAdminView(adminView, target.getInventory().getContents(),
                target.getInventory().getArmorContents(),
                target.getInventory().getItemInOffHand());

        storeCurrentState(adminUUID, target.getInventory().getContents(),
                target.getInventory().getArmorContents(),
                target.getInventory().getItemInOffHand());
    }

    private static void checkAndApplyArmorChange(Inventory adminView, int adminSlot,
                                                 ItemStack[] lastArmor, int armorIndex,
                                                 Player target, int targetSlot) {
        ItemStack adminItem = adminView.getItem(adminSlot);
        ItemStack lastItem = (lastArmor != null && lastArmor.length > armorIndex) ? lastArmor[armorIndex] : null;

        if (!itemsEqualIgnorePlaceholder(adminItem, lastItem)) {
            if (adminItem != null && adminItem.getType() != Material.AIR && !isPlaceholderItem(adminItem)) {
                target.getInventory().setItem(targetSlot, adminItem.clone());
            } else {
                target.getInventory().setItem(targetSlot, null);
            }
        }
    }

    private static void storeCurrentState(UUID adminUUID, ItemStack[] inventory, ItemStack[] armor, ItemStack offhand) {
        lastKnownInventoryState.put(adminUUID, cloneItemArray(inventory));
        lastKnownArmorState.put(adminUUID, cloneItemArray(armor));
        lastKnownOffhandState.put(adminUUID, offhand != null ? offhand.clone() : null);
    }

    private static ItemStack[] cloneItemArray(ItemStack[] items) {
        if (items == null) return null;
        ItemStack[] cloned = new ItemStack[items.length];
        for (int i = 0; i < items.length; i++) {
            cloned[i] = items[i] != null ? items[i].clone() : null;
        }
        return cloned;
    }

    private static void syncPlayerInventoryToAdminView(Inventory adminView, ItemStack[] inventory,
                                                       ItemStack[] armor, ItemStack offhand) {
        for (int i = 9; i < Math.min(inventory.length, 36); i++) {
            ItemStack actual = inventory[i];
            if (actual != null && actual.getType() != Material.AIR) {
                adminView.setItem(i - 9, actual.clone());
            } else {
                adminView.setItem(i - 9, null);
            }
        }

        for (int i = 0; i < 9; i++) {
            ItemStack actual = inventory[i];
            if (actual != null && actual.getType() != Material.AIR) {
                adminView.setItem(27 + i, actual.clone());
            } else {
                adminView.setItem(27 + i, null);
            }
        }

        syncArmorSlot(adminView, 47, armor[3], Material.IRON_HELMET, "Helmet");
        syncArmorSlot(adminView, 48, armor[2], Material.IRON_CHESTPLATE, "Chestplate");
        syncArmorSlot(adminView, 49, armor[1], Material.IRON_LEGGINGS, "Leggings");
        syncArmorSlot(adminView, 50, armor[0], Material.IRON_BOOTS, "Boots");

        syncArmorSlot(adminView, 52, offhand, Material.SHIELD, "Offhand");
    }

    private static void syncArmorSlot(Inventory adminView, int slot, ItemStack actual,
                                      Material placeholderMaterial, String slotName) {
        if (actual != null && actual.getType() != Material.AIR) {
            adminView.setItem(slot, actual.clone());
        } else {
            adminView.setItem(slot, createArmorPlaceholder(placeholderMaterial, slotName));
        }
    }

    private static void syncEnderChestBidirectional(Player admin, Inventory adminView, Player target) {
        UUID adminUUID = admin.getUniqueId();

        ItemStack[] targetEnder = target.getEnderChest().getContents();
        ItemStack[] lastEnder = lastKnownInventoryState.get(adminUUID);

        if (lastEnder == null || lastEnder.length != 27) {
            initializeEnderChestState(adminUUID, targetEnder);
            lastEnder = lastKnownInventoryState.get(adminUUID);

            for (int i = 0; i < 27; i++) {
                if (i < targetEnder.length && targetEnder[i] != null) {
                    adminView.setItem(i, targetEnder[i].clone());
                } else {
                    adminView.setItem(i, null);
                }
            }
            return;
        }

        for (int i = 0; i < 27; i++) {
            ItemStack adminItem = adminView.getItem(i);
            ItemStack lastItem = (i < lastEnder.length) ? lastEnder[i] : null;

            if (!itemsEqualIgnorePlaceholder(adminItem, lastItem)) {
                if (adminItem != null && !isPlaceholderItem(adminItem)) {
                    target.getEnderChest().setItem(i, adminItem.clone());
                } else {
                    target.getEnderChest().setItem(i, null);
                }
            }
        }

        ItemStack[] currentEnder = target.getEnderChest().getContents();
        for (int i = 0; i < 27; i++) {
            ItemStack current = adminView.getItem(i);
            ItemStack actual = (i < currentEnder.length) ? currentEnder[i] : null;

            if (!itemsEqual(current, actual)) {
                if (actual != null) {
                    adminView.setItem(i, actual.clone());
                } else {
                    adminView.setItem(i, null);
                }
            }
        }

        ItemStack[] newState = new ItemStack[27];
        for (int i = 0; i < 27; i++) {
            if (i < currentEnder.length) {
                newState[i] = currentEnder[i] != null ? currentEnder[i].clone() : null;
            } else {
                newState[i] = null;
            }
        }
        lastKnownInventoryState.put(adminUUID, newState);
    }

    private static boolean itemsEqual(ItemStack item1, ItemStack item2) {
        if ((item1 == null || item1.getType() == Material.AIR) &&
                (item2 == null || item2.getType() == Material.AIR)) {
            return true;
        }

        if ((item1 == null || item1.getType() == Material.AIR) ||
                (item2 == null || item2.getType() == Material.AIR)) {
            return false;
        }

        if (isPlaceholderItem(item1) || isPlaceholderItem(item2)) {
            return false;
        }

        return item1.isSimilar(item2) && item1.getAmount() == item2.getAmount();
    }

    private static boolean itemsEqualIgnorePlaceholder(ItemStack item1, ItemStack item2) {
        boolean item1Empty = (item1 == null || item1.getType() == Material.AIR || isPlaceholderItem(item1));
        boolean item2Empty = (item2 == null || item2.getType() == Material.AIR || isPlaceholderItem(item2));

        if (item1Empty && item2Empty) {
            return true;
        }

        if (item1Empty || item2Empty) {
            return false;
        }

        return item1.isSimilar(item2) && item1.getAmount() == item2.getAmount();
    }

    public static void cleanupAdminView(UUID adminUUID) {
        adminViewingPlayer.remove(adminUUID);
        adminViewingType.remove(adminUUID);
        offlineInventoryData.remove(adminUUID);
        lastKnownInventoryState.remove(adminUUID);
        lastKnownArmorState.remove(adminUUID);
        lastKnownOffhandState.remove(adminUUID);
    }

    public static void openPlayerInventory(Player admin, OfflinePlayer target) {
        if (!hasInventoryPermission(admin)) {
            admin.sendMessage(RED + "✗ " + GRAY + "You don't have permission to edit inventories!");
            return;
        }

        if (target.isOnline()) {
            openOnlinePlayerInventory(admin, target.getPlayer());
        } else {
            openOfflinePlayerInventory(admin, target);
        }
    }

    private static void openOnlinePlayerInventory(Player admin, Player target) {
        Inventory adminView = Bukkit.createInventory(null, 54,
                DARK_GRAY + "┃ " + GOLD + target.getName() + ADMIN_INV_SUFFIX + " ┃");

        ItemStack[] mainInventory = target.getInventory().getContents();
        for (int i = 9; i < Math.min(mainInventory.length, 36); i++) {
            if (mainInventory[i] != null) {
                adminView.setItem(i - 9, mainInventory[i].clone());
            }
        }

        for (int i = 0; i < 9; i++) {
            if (mainInventory[i] != null) {
                adminView.setItem(27 + i, mainInventory[i].clone());
            }
        }

        ItemStack separator = createSeparator();
        for (int i = 36; i < 45; i++) {
            adminView.setItem(i, separator);
        }

        ItemStack[] armorContents = target.getInventory().getArmorContents();

        if (armorContents[3] != null && armorContents[3].getType() != Material.AIR) {
            adminView.setItem(47, armorContents[3].clone());
        } else {
            adminView.setItem(47, createArmorPlaceholder(Material.IRON_HELMET, "Helmet"));
        }

        if (armorContents[2] != null && armorContents[2].getType() != Material.AIR) {
            adminView.setItem(48, armorContents[2].clone());
        } else {
            adminView.setItem(48, createArmorPlaceholder(Material.IRON_CHESTPLATE, "Chestplate"));
        }

        if (armorContents[1] != null && armorContents[1].getType() != Material.AIR) {
            adminView.setItem(49, armorContents[1].clone());
        } else {
            adminView.setItem(49, createArmorPlaceholder(Material.IRON_LEGGINGS, "Leggings"));
        }

        if (armorContents[0] != null && armorContents[0].getType() != Material.AIR) {
            adminView.setItem(50, armorContents[0].clone());
        } else {
            adminView.setItem(50, createArmorPlaceholder(Material.IRON_BOOTS, "Boots"));
        }

        ItemStack offhand = target.getInventory().getItemInOffHand();
        if (offhand != null && offhand.getType() != Material.AIR) {
            adminView.setItem(52, offhand.clone());
        } else {
            adminView.setItem(52, createArmorPlaceholder(Material.SHIELD, "Offhand"));
        }

        ItemStack border = createBorder();
        adminView.setItem(45, border);
        adminView.setItem(46, border);
        adminView.setItem(51, border);
        adminView.setItem(53, border);

        admin.openInventory(adminView);
        admin.sendMessage(GREEN + "✓ " + GRAY + "Editing " + GOLD + target.getName() +
                GRAY + "'s inventory " + RED + "(Admin Mode)");

        adminViewingPlayer.put(admin.getUniqueId(), target.getUniqueId());
        adminViewingType.put(admin.getUniqueId(), true);
    }

    private static void openOfflinePlayerInventory(Player admin, OfflinePlayer target) {
        admin.sendMessage(YELLOW + "⚠ " + GRAY + "Loading offline player data for " + GOLD + target.getName() + GRAY + "...");

        runAsync(() -> {
            try {
                File playerDataFolder = new File(Bukkit.getWorlds().get(0).getWorldFolder(), "playerdata");
                File playerFile = new File(playerDataFolder, target.getUniqueId().toString() + ".dat");

                if (!playerFile.exists()) {
                    runSync(() -> admin.sendMessage(RED + "✗ " + GRAY + "Could not find player data file for " + GOLD + target.getName()));
                    return;
                }

                OfflineInventoryData data = OfflinePlayerDataReader.readPlayerInventory(playerFile);

                runSync(() -> {
                    Inventory adminView = Bukkit.createInventory(null, 54,
                            DARK_GRAY + "┃ " + GOLD + target.getName() + ADMIN_INV_SUFFIX + " ┃");

                    for (int i = 9; i < 36; i++) {
                        if (data.inventory[i] != null) {
                            adminView.setItem(i - 9, data.inventory[i].clone());
                        }
                    }

                    for (int i = 0; i < 9; i++) {
                        if (data.inventory[i] != null) {
                            adminView.setItem(27 + i, data.inventory[i].clone());
                        }
                    }

                    ItemStack separator = createSeparator();
                    for (int i = 36; i < 45; i++) {
                        adminView.setItem(i, separator);
                    }

                    if (data.armor[3] != null && data.armor[3].getType() != Material.AIR) {
                        adminView.setItem(47, data.armor[3].clone());
                    } else {
                        adminView.setItem(47, createArmorPlaceholder(Material.IRON_HELMET, "Helmet"));
                    }

                    if (data.armor[2] != null && data.armor[2].getType() != Material.AIR) {
                        adminView.setItem(48, data.armor[2].clone());
                    } else {
                        adminView.setItem(48, createArmorPlaceholder(Material.IRON_CHESTPLATE, "Chestplate"));
                    }

                    if (data.armor[1] != null && data.armor[1].getType() != Material.AIR) {
                        adminView.setItem(49, data.armor[1].clone());
                    } else {
                        adminView.setItem(49, createArmorPlaceholder(Material.IRON_LEGGINGS, "Leggings"));
                    }

                    if (data.armor[0] != null && data.armor[0].getType() != Material.AIR) {
                        adminView.setItem(50, data.armor[0].clone());
                    } else {
                        adminView.setItem(50, createArmorPlaceholder(Material.IRON_BOOTS, "Boots"));
                    }

                    if (data.offhand != null && data.offhand.getType() != Material.AIR) {
                        adminView.setItem(52, data.offhand.clone());
                    } else {
                        adminView.setItem(52, createArmorPlaceholder(Material.SHIELD, "Offhand"));
                    }

                    ItemStack border = createBorder();
                    adminView.setItem(45, border);
                    adminView.setItem(46, border);
                    adminView.setItem(51, border);
                    adminView.setItem(53, border);

                    admin.openInventory(adminView);
                    admin.sendMessage(GREEN + "✓ " + GRAY + "Editing " + GOLD + target.getName() +
                            GRAY + "'s inventory " + RED + "(Admin Mode - Offline)");

                    adminViewingPlayer.put(admin.getUniqueId(), target.getUniqueId());
                    adminViewingType.put(admin.getUniqueId(), true);
                    offlineInventoryData.put(admin.getUniqueId(), data);
                });

            } catch (Exception e) {
                runSync(() -> {
                    admin.sendMessage(RED + "✗ " + GRAY + "Failed to load player data: " + e.getMessage());
                    e.printStackTrace();
                });
            }
        });
    }

    public static void openPlayerEnderChest(Player admin, OfflinePlayer target) {
        if (!hasEnderChestPermission(admin)) {
            admin.sendMessage(RED + "✗ " + GRAY + "You don't have permission to edit ender chests!");
            return;
        }

        if (target.isOnline()) {
            openOnlinePlayerEnderChest(admin, target.getPlayer());
        } else {
            openOfflinePlayerEnderChest(admin, target);
        }
    }

    private static void openOnlinePlayerEnderChest(Player admin, Player target) {
        Inventory enderView = Bukkit.createInventory(null, 27,
                DARK_GRAY + "┃ " + GOLD + target.getName() + ADMIN_ENDER_SUFFIX + " ┃");

        ItemStack[] enderContents = target.getEnderChest().getContents();

        for (int i = 0; i < Math.min(27, enderContents.length); i++) {
            if (enderContents[i] != null) {
                enderView.setItem(i, enderContents[i].clone());
            }
        }

        admin.openInventory(enderView);
        admin.sendMessage(GREEN + "✓ " + GRAY + "Editing " + GOLD + target.getName() +
                GRAY + "'s ender chest " + RED + "(Admin Mode)");

        adminViewingPlayer.put(admin.getUniqueId(), target.getUniqueId());
        adminViewingType.put(admin.getUniqueId(), false);

        initializeEnderChestState(admin.getUniqueId(), enderContents);
    }

    private static void initializeEnderChestState(UUID adminUUID, ItemStack[] enderContents) {
        ItemStack[] initialState = new ItemStack[27];
        for (int i = 0; i < Math.min(27, enderContents.length); i++) {
            initialState[i] = enderContents[i] != null ? enderContents[i].clone() : null;
        }
        lastKnownInventoryState.put(adminUUID, initialState);
    }

    private static void openOfflinePlayerEnderChest(Player admin, OfflinePlayer target) {
        admin.sendMessage(YELLOW + "⚠ " + GRAY + "Loading offline ender chest data for " + GOLD + target.getName() + GRAY + "...");

        runAsync(() -> {
            try {
                File playerDataFolder = new File(Bukkit.getWorlds().get(0).getWorldFolder(), "playerdata");
                File playerFile = new File(playerDataFolder, target.getUniqueId().toString() + ".dat");

                if (!playerFile.exists()) {
                    runSync(() -> admin.sendMessage(RED + "✗ " + GRAY + "Could not find player data file for " + GOLD + target.getName()));
                    return;
                }

                OfflineInventoryData data = OfflinePlayerDataReader.readPlayerInventory(playerFile);

                runSync(() -> {
                    Inventory enderView = Bukkit.createInventory(null, 27,
                            DARK_GRAY + "┃ " + GOLD + target.getName() + ADMIN_ENDER_SUFFIX + " ┃");

                    for (int i = 0; i < Math.min(27, data.enderChest.length); i++) {
                        if (data.enderChest[i] != null) {
                            enderView.setItem(i, data.enderChest[i].clone());
                        }
                    }

                    admin.openInventory(enderView);
                    admin.sendMessage(GREEN + "✓ " + GRAY + "Editing " + GOLD + target.getName() +
                            GRAY + "'s ender chest " + RED + "(Admin Mode - Offline)");

                    adminViewingPlayer.put(admin.getUniqueId(), target.getUniqueId());
                    adminViewingType.put(admin.getUniqueId(), false);
                    offlineInventoryData.put(admin.getUniqueId(), data);

                    initializeEnderChestState(admin.getUniqueId(), data.enderChest);
                });

            } catch (Exception e) {
                runSync(() -> {
                    admin.sendMessage(RED + "✗ " + GRAY + "Failed to load ender chest data: " + e.getMessage());
                    e.printStackTrace();
                });
            }
        });
    }

    public static void saveInventoryChanges(Player admin, Inventory inv) {
        UUID adminUUID = admin.getUniqueId();
        if (!adminViewingPlayer.containsKey(adminUUID)) return;
        if (!hasRequiredPermission(admin, adminUUID)) {
            admin.sendMessage(RED + "✗ " + GRAY + "You don't have permission to save inventory changes!");
            return;
        }

        UUID targetUUID = adminViewingPlayer.get(adminUUID);
        Player target = Bukkit.getPlayer(targetUUID);

        boolean isInventory = adminViewingType.getOrDefault(adminUUID, true);

        if (target != null && target.isOnline()) {
            if (isInventory) {
                saveInventoryChangesToPlayer(target, inv);
                admin.sendMessage(GREEN + "✓ " + GRAY + "Saved changes to " + GOLD + target.getName() +
                        GRAY + "'s inventory");
            } else {
                saveEnderChestChangesToPlayer(target, inv);
                admin.sendMessage(GREEN + "✓ " + GRAY + "Saved changes to " + GOLD + target.getName() +
                        GRAY + "'s ender chest");
            }
            offlineInventoryData.remove(adminUUID);
        } else {
            saveOfflineInventoryChanges(admin, inv, targetUUID, isInventory);
        }
    }

    private static void saveOfflineInventoryChanges(Player admin, Inventory inv, UUID targetUUID, boolean isInventory) {
        if (isInventory && !hasInventoryPermission(admin)) {
            admin.sendMessage(RED + "✗ " + GRAY + "You don't have permission to save inventory changes!");
            return;
        }
        if (!isInventory && !hasEnderChestPermission(admin)) {
            admin.sendMessage(RED + "✗ " + GRAY + "You don't have permission to save ender chest changes!");
            return;
        }

        if (!offlineInventoryData.containsKey(admin.getUniqueId())) {
            admin.sendMessage(RED + "✗ " + GRAY + "User left, before you saving, please run the command again.");
            return;
        }

        OfflineInventoryData data = offlineInventoryData.get(admin.getUniqueId());
        String targetName = Bukkit.getOfflinePlayer(targetUUID).getName();

        Player onlineTarget = Bukkit.getPlayer(targetUUID);
        if (onlineTarget != null && onlineTarget.isOnline()) {
            admin.sendMessage(YELLOW + "⚠ " + GRAY + "Player " + GOLD + targetName + GRAY + " came online during editing.");
            admin.sendMessage(YELLOW + "⚠ " + GRAY + "Saving changes to online player instead...");

            if (isInventory) {
                saveInventoryChangesToPlayer(onlineTarget, inv);
                admin.sendMessage(GREEN + "✓ " + GRAY + "Saved changes to " + GOLD + targetName +
                        GRAY + "'s inventory (now online)");
            } else {
                saveEnderChestChangesToPlayer(onlineTarget, inv);
                admin.sendMessage(GREEN + "✓ " + GRAY + "Saved changes to " + GOLD + targetName +
                        GRAY + "'s ender chest (now online)");
            }

            offlineInventoryData.remove(admin.getUniqueId());
            return;
        }

        admin.sendMessage(YELLOW + "⚠ " + GRAY + "Saving offline changes for " + GOLD + targetName + GRAY + "...");

        runAsync(() -> {
            try {
                if (isInventory) {
                    for (int i = 0; i < 27; i++) {
                        ItemStack item = inv.getItem(i);
                        data.inventory[i + 9] = (item != null && !isPlaceholderItem(item)) ? item : null;
                    }

                    for (int i = 0; i < 9; i++) {
                        ItemStack item = inv.getItem(27 + i);
                        data.inventory[i] = (item != null && !isPlaceholderItem(item)) ? item : null;
                    }

                    ItemStack helmet = inv.getItem(47);
                    ItemStack chestplate = inv.getItem(48);
                    ItemStack leggings = inv.getItem(49);
                    ItemStack boots = inv.getItem(50);

                    data.armor[3] = (helmet != null && !isPlaceholderItem(helmet)) ? helmet : null;
                    data.armor[2] = (chestplate != null && !isPlaceholderItem(chestplate)) ? chestplate : null;
                    data.armor[1] = (leggings != null && !isPlaceholderItem(leggings)) ? leggings : null;
                    data.armor[0] = (boots != null && !isPlaceholderItem(boots)) ? boots : null;

                    ItemStack offhand = inv.getItem(52);
                    data.offhand = (offhand != null && !isPlaceholderItem(offhand)) ? offhand : null;
                } else {
                    for (int i = 0; i < 27; i++) {
                        ItemStack item = inv.getItem(i);
                        data.enderChest[i] = (item != null && !isPlaceholderItem(item)) ? item : null;
                    }
                }

                Player stillOffline = Bukkit.getPlayer(targetUUID);
                if (stillOffline != null && stillOffline.isOnline()) {
                    runSync(() -> {
                        admin.sendMessage(YELLOW + "⚠ " + GRAY + "Player " + GOLD + targetName +
                                GRAY + " came online during save!");
                        admin.sendMessage(RED + "✗ " + GRAY + "Changes not saved to prevent data loss.");
                        admin.sendMessage(GRAY + "Please use " + GOLD + "/invedit " + targetName +
                                GRAY + " again to edit their online inventory.");
                    });
                    return;
                }

                File playerDataFolder = new File(Bukkit.getWorlds().get(0).getWorldFolder(), "playerdata");
                File playerFile = new File(playerDataFolder, targetUUID.toString() + ".dat");

                OfflinePlayerDataReader.writePlayerInventory(playerFile, data);

                runSync(() -> {
                    if (isInventory) {
                        admin.sendMessage(GREEN + "✓ " + GRAY + "Saved offline changes to " + GOLD + targetName +
                                GRAY + "'s inventory");
                    } else {
                        admin.sendMessage(GREEN + "✓ " + GRAY + "Saved offline changes to " + GOLD + targetName +
                                GRAY + "'s ender chest");
                    }
                });

            } catch (Exception e) {
                runSync(() -> {
                    admin.sendMessage(RED + "✗ " + GRAY + "Failed to save offline changes: " + e.getMessage());
                    e.printStackTrace();
                });
            } finally {
                offlineInventoryData.remove(admin.getUniqueId());
            }
        });
    }

    private static void saveInventoryChangesToPlayer(Player target, Inventory adminView) {
        for (int i = 0; i < 27; i++) {
            ItemStack item = adminView.getItem(i);
            if (item != null && isPlaceholderItem(item)) {
                target.getInventory().setItem(i + 9, null);
            } else {
                target.getInventory().setItem(i + 9, item);
            }
        }

        for (int i = 0; i < 9; i++) {
            ItemStack item = adminView.getItem(27 + i);
            if (item != null && isPlaceholderItem(item)) {
                target.getInventory().setItem(i, null);
            } else {
                target.getInventory().setItem(i, item);
            }
        }

        ItemStack helmet = adminView.getItem(47);
        ItemStack chestplate = adminView.getItem(48);
        ItemStack leggings = adminView.getItem(49);
        ItemStack boots = adminView.getItem(50);

        target.getInventory().setItem(39, (helmet != null && !isPlaceholderItem(helmet)) ? helmet : null);
        target.getInventory().setItem(38, (chestplate != null && !isPlaceholderItem(chestplate)) ? chestplate : null);
        target.getInventory().setItem(37, (leggings != null && !isPlaceholderItem(leggings)) ? leggings : null);
        target.getInventory().setItem(36, (boots != null && !isPlaceholderItem(boots)) ? boots : null);

        ItemStack offhand = adminView.getItem(52);
        target.getInventory().setItemInOffHand((offhand != null && !isPlaceholderItem(offhand)) ? offhand : null);

        target.updateInventory();
    }

    private static void saveEnderChestChangesToPlayer(Player target, Inventory adminView) {
        for (int i = 0; i < 27; i++) {
            ItemStack item = adminView.getItem(i);
            if (item != null && isPlaceholderItem(item)) {
                target.getEnderChest().setItem(i, null);
            } else {
                target.getEnderChest().setItem(i, item);
            }
        }
    }

    public static boolean isAdminViewInventory(String title) {
        return title.contains(ADMIN_INV_SUFFIX) || title.contains(ADMIN_ENDER_SUFFIX);
    }

    private static ItemStack createSeparator() {
        ItemStack separator = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = separator.getItemMeta();
        meta.setDisplayName(" ");
        separator.setItemMeta(meta);
        return separator;
    }

    private static ItemStack createBorder() {
        ItemStack border = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta meta = border.getItemMeta();
        meta.setDisplayName(RED + "" + BOLD + "ADMIN MODE");
        List<String> lore = new ArrayList<>();
        lore.add(GRAY + "You are editing this inventory");
        meta.setLore(lore);
        border.setItemMeta(meta);
        return border;
    }

    public static ItemStack createArmorPlaceholder(Material material, String slotName) {
        ItemStack placeholder = new ItemStack(material);
        ItemMeta meta = placeholder.getItemMeta();
        meta.setDisplayName(DARK_GRAY + "Empty " + slotName + " Slot");
        List<String> lore = new ArrayList<>();
        lore.add(GRAY + "No item equipped");
        lore.add(YELLOW + "You can place items here");
        meta.setLore(lore);
        placeholder.setItemMeta(meta);
        return placeholder;
    }

    public static boolean isPlaceholderItem(ItemStack item) {
        if (item == null) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return false;

        String displayName = meta.getDisplayName();
        return displayName.contains("Empty") && displayName.contains("Slot");
    }

    private static void runAsync(Runnable task) {
        if (plugin == null) {
            throw new IllegalStateException("ViewInvAdmin not initialized! Call initialize() first.");
        }

        if (isFolia) {
            Bukkit.getAsyncScheduler().runNow(plugin, (t) -> task.run());
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
        }
    }

    private static void runSync(Runnable task) {
        if (plugin == null) {
            throw new IllegalStateException("ViewInvAdmin not initialized! Call initialize() first.");
        }

        if (isFolia) {
            Bukkit.getGlobalRegionScheduler().run(plugin, (t) -> task.run());
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    static class OfflineInventoryData {
        ItemStack[] inventory = new ItemStack[41];
        ItemStack[] armor = new ItemStack[4];
        ItemStack offhand;
        ItemStack[] enderChest = new ItemStack[27];
        byte[] rawData;

        Object nbtCompound;
        File playerFile;
    }
    private static boolean hasInventoryPermission(Player admin) {
        return admin.hasPermission(InventoryAdminPerm);
    }
    private static boolean hasEnderChestPermission(Player admin) {
        return admin.hasPermission(EchestAdminPerm);
    }
    private static boolean hasRequiredPermission(Player admin, UUID adminUUID) {
        boolean isInventory = adminViewingType.getOrDefault(adminUUID, true);
        return isInventory ? hasInventoryPermission(admin) : hasEnderChestPermission(admin);
    }

}
