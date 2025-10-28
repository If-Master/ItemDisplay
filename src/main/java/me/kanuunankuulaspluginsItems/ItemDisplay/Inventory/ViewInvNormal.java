package me.kanuunankuulaspluginsItems.ItemDisplay.Inventory;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

import static me.kanuunankuulaspluginsItems.ItemDisplay.formatters.Colors.createColoredComponent;
import static org.bukkit.ChatColor.*;
import static org.bukkit.ChatColor.GOLD;
import static org.bukkit.ChatColor.GRAY;

import static me.kanuunankuulaspluginsItems.ItemDisplay.ItemDisplay.*;

public class ViewInvNormal {
    public static void CreateTheInv(Player target, Player player) {
        org.bukkit.inventory.Inventory viewInventory = Bukkit.createInventory(null, 54,
                DARK_GRAY + "┃ " + GOLD + target.getName() +
                        DARK_GRAY + INVENTORY_TITLE_SUFFIX + " ┃");

        org.bukkit.inventory.ItemStack[] mainInventory = target.getInventory().getContents();
        for (int i = 9; i < Math.min(mainInventory.length, 36); i++) {
            if (mainInventory[i] != null) {
                ItemStack clonedItem = mainInventory[i].clone();
                markAsViewOnly(clonedItem);
                viewInventory.setItem(i - 9, clonedItem);
            }
        }

        for (int i = 0; i < 9; i++) {
            if (mainInventory[i] != null) {
                ItemStack clonedItem = mainInventory[i].clone();
                markAsViewOnly(clonedItem);
                viewInventory.setItem(27 + i, clonedItem);
            }
        }

        ItemStack separator = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta separatorMeta = separator.getItemMeta();
        separatorMeta.setDisplayName(" ");
        separator.setItemMeta(separatorMeta);

        for (int i = 36; i < 45; i++) {
            viewInventory.setItem(i, separator);
        }

        org.bukkit.inventory.ItemStack[] armorContents = target.getInventory().getArmorContents();

        if (armorContents[3] != null && armorContents[3].getType() != Material.AIR) {
            ItemStack clonedHelmet = armorContents[3].clone();
            markAsViewOnly(clonedHelmet);
            viewInventory.setItem(47, clonedHelmet);
        } else {
            viewInventory.setItem(47, createArmorPlaceholder(Material.IRON_HELMET, "Helmet"));
        }

        if (armorContents[2] != null && armorContents[2].getType() != Material.AIR) {
            ItemStack clonedChestplate = armorContents[2].clone();
            markAsViewOnly(clonedChestplate);
            viewInventory.setItem(48, clonedChestplate);
        } else {
            viewInventory.setItem(48, createArmorPlaceholder(Material.IRON_CHESTPLATE, "Chestplate"));
        }

        if (armorContents[1] != null && armorContents[1].getType() != Material.AIR) {
            ItemStack clonedLeggings = armorContents[1].clone();
            markAsViewOnly(clonedLeggings);
            viewInventory.setItem(49, clonedLeggings);
        } else {
            viewInventory.setItem(49, createArmorPlaceholder(Material.IRON_LEGGINGS, "Leggings"));
        }

        if (armorContents[0] != null && armorContents[0].getType() != Material.AIR) {
            ItemStack clonedBoots = armorContents[0].clone();
            markAsViewOnly(clonedBoots);
            viewInventory.setItem(50, clonedBoots);
        } else {
            viewInventory.setItem(50, createArmorPlaceholder(Material.IRON_BOOTS, "Boots"));
        }

        org.bukkit.inventory.ItemStack offhand = target.getInventory().getItemInOffHand();
        if (offhand != null && offhand.getType() != Material.AIR) {
            ItemStack clonedOffhand = offhand.clone();
            markAsViewOnly(clonedOffhand);
            viewInventory.setItem(52, clonedOffhand);
        } else {
            viewInventory.setItem(52, createArmorPlaceholder(Material.SHIELD, "Offhand"));
        }

        ItemStack border = new ItemStack(Material.LIGHT_BLUE_STAINED_GLASS_PANE);
        ItemMeta borderMeta = border.getItemMeta();
        borderMeta.setDisplayName(" ");
        border.setItemMeta(borderMeta);

        viewInventory.setItem(45, border);
        viewInventory.setItem(46, border);
        viewInventory.setItem(51, border);
        viewInventory.setItem(53, border);

        player.openInventory(viewInventory);
        player.sendMessage(GREEN + "✓ " + GRAY + "Viewing " +
                GOLD + target.getName() + GRAY + INVENTORY_TITLE_SUFFIX );

        viewingInventories.put(player.getUniqueId(), target.getUniqueId());
    }

    public static ItemStack createArmorPlaceholder(Material material, String slotName) {
        ItemStack placeholder = new ItemStack(material);
        ItemMeta meta = placeholder.getItemMeta();
        meta.setDisplayName(DARK_GRAY + "Empty " + slotName + " Slot");
        List<String> lore = new ArrayList<>();
        lore.add(GRAY + "No item equipped");
        meta.setLore(lore);
        placeholder.setItemMeta(meta);
        markAsViewOnly(placeholder);
        return placeholder;
    }

    private static void markAsViewOnly(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        meta.getPersistentDataContainer().set(viewOnlyKey, PersistentDataType.STRING, VIEW_ONLY_MARKER);
        item.setItemMeta(meta);
    }
    public static void handleInventoryDisplay(Player player, String originalMessage) {
        String displayName = getCleanPlayerName(player);
        String prefix = getPlayerPrefix(player);

        String invDisplayText = GREEN + "[View Inventory]";
        String modifiedMessage = originalMessage.replace("[inv]", invDisplayText);

        sharedInventories.add(player.getUniqueId());
        sharedInventoryTimestamps.put(player.getUniqueId(), System.currentTimeMillis());

        TextComponent invComponent = new TextComponent(invDisplayText);
        invComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new Text(YELLOW + "Click to view " + player.getName() + INVENTORY_TITLE_SUFFIX)));
        invComponent.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/viewinv " + player.getName()));

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            TextComponent fullMessage = new TextComponent();

            if (!prefix.isEmpty()) {
                TextComponent prefixComponent = createColoredComponent(prefix);
                fullMessage.addExtra(prefixComponent);
            }

            TextComponent nameComponent = new TextComponent(displayName);
            nameComponent.setColor(net.md_5.bungee.api.ChatColor.WHITE);
            fullMessage.addExtra(nameComponent);

            TextComponent colonComponent = new TextComponent(": ");
            colonComponent.setColor(net.md_5.bungee.api.ChatColor.WHITE);
            fullMessage.addExtra(colonComponent);

            String beforeInv = modifiedMessage.substring(0, modifiedMessage.indexOf(invDisplayText));
            if (!beforeInv.isEmpty()) {
                TextComponent beforeComponent = createColoredComponent(beforeInv);
                fullMessage.addExtra(beforeComponent);
            }

            fullMessage.addExtra(invComponent);

            String afterInv = modifiedMessage.substring(modifiedMessage.indexOf(invDisplayText) + invDisplayText.length());
            if (!afterInv.isEmpty()) {
                TextComponent afterComponent = createColoredComponent(afterInv);
                fullMessage.addExtra(afterComponent);
            }

            onlinePlayer.spigot().sendMessage(fullMessage);
        }
    }

}
