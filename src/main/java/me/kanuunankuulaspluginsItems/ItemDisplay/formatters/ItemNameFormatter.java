package me.kanuunankuulaspluginsItems.ItemDisplay.formatters;

import org.bukkit.inventory.ItemStack;

public class ItemNameFormatter {
    public static String formatItemName(String name) {
        String formatted = name.replace("_", " ").toLowerCase();
        String[] words = formatted.split(" ");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1))
                        .append(" ");
            }
        }
        return result.toString().trim();
    }

    public static String getItemDisplayName(ItemStack item) {
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return item.getItemMeta().getDisplayName();
        }
        return formatItemName(item.getType().name());
    }

}
