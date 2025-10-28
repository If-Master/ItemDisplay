package me.kanuunankuulaspluginsItems.ItemDisplay.formatters;

import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Color;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.bukkit.ChatColor.*;

public class Colors {
    // Enchantments
    public static org.bukkit.ChatColor getEnchantmentColor(Enchantment enchant, int level) {
        if (enchant.isCursed()) {
            return RED;
        }

        if (level > enchant.getMaxLevel()) {
            return GOLD;
        }

        return GRAY;
    }

    // Durability Color
    public static org.bukkit.ChatColor getDurabilityColor(double percent) {
        if (percent > 75) return GREEN;
        if (percent > 50) return YELLOW;
        if (percent > 25) return GOLD;
        return RED;
    }

    // Item Rarity
    public static org.bukkit.ChatColor getItemRarityColor(ItemStack item) {
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return AQUA;
        }

        if (!item.getEnchantments().isEmpty()) {
            boolean hasCurse = item.getEnchantments().keySet().stream().anyMatch(Enchantment::isCursed);
            if (hasCurse) {
                return RED;
            }
            return LIGHT_PURPLE;
        }

        String materialName = item.getType().name();
        if (materialName.contains("DIAMOND")) {
            return AQUA;
        } else if (materialName.contains("GOLD") || materialName.contains("GOLDEN")) {
            return YELLOW;
        } else if (materialName.contains("IRON")) {
            return WHITE;
        } else if (materialName.contains("NETHERITE")) {
            return DARK_PURPLE;
        }

        return WHITE;
    }

    public static TextComponent createColoredComponent(String text) {
        if (text == null || text.isEmpty()) {
            return new TextComponent("");
        }

        Pattern hexPattern = Pattern.compile("(&#[A-Fa-f0-9]{6})");
        String[] parts = hexPattern.split(text);
        Matcher matcher = hexPattern.matcher(text);

        TextComponent component = new TextComponent();
        int partIndex = 0;

        if (partIndex < parts.length && !parts[partIndex].isEmpty()) {
            TextComponent part = new TextComponent(translateBasicColors(parts[partIndex]));
            component.addExtra(part);
        }
        partIndex++;

        while (matcher.find()) {
            String hexColor = matcher.group(1);
            String hex = hexColor.substring(2);

            String followingText = "";
            if (partIndex < parts.length) {
                followingText = parts[partIndex];
                partIndex++;
            }

            TextComponent coloredPart = new TextComponent(translateBasicColors(followingText));
            try {
                coloredPart.setColor(net.md_5.bungee.api.ChatColor.of("#" + hex));
            } catch (Exception e) {
                coloredPart.setColor(net.md_5.bungee.api.ChatColor.WHITE);
            }
            component.addExtra(coloredPart);
        }

        return component;
    }


    // Idk
    public static String getColorName(Color color) {
        if (color.equals(Color.RED)) return "Red";
        if (color.equals(Color.BLUE)) return "Blue";
        if (color.equals(Color.GREEN)) return "Green";
        if (color.equals(Color.YELLOW)) return "Yellow";
        if (color.equals(Color.ORANGE)) return "Orange";
        if (color.equals(Color.WHITE)) return "White";
        if (color.equals(Color.BLACK)) return "Black";
        if (color.equals(Color.PURPLE)) return "Purple";
        if (color.equals(Color.LIME)) return "Lime";
        if (color.equals(Color.AQUA)) return "Aqua";
        if (color.equals(Color.FUCHSIA)) return "Magenta";
        if (color.equals(Color.SILVER)) return "Light Gray";
        if (color.equals(Color.GRAY)) return "Gray";
        if (color.equals(Color.MAROON)) return "Maroon";
        if (color.equals(Color.NAVY)) return "Navy";
        if (color.equals(Color.TEAL)) return "Teal";

        return String.format("#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
    }

    // Translate Color
    public static String translateBasicColors(String text) {
        if (text == null) return "";
        return translateAlternateColorCodes('&', text);
    }

}
