package me.kanuunankuulaspluginsItems.ItemDisplay.Chat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static me.kanuunankuulaspluginsItems.ItemDisplay.ItemDisplay.vaultChat;
import static org.bukkit.ChatColor.*;

public class ItemLinkHandler {

    private static final Pattern ITEM_TAG_PATTERN = Pattern.compile("\\[(i|item)]", Pattern.CASE_INSENSITIVE);

    public static void handleItemLinkMessage(Player player, String originalMessage) {
        if (!containsItemTags(originalMessage)) {
            return;
        }

        ItemStack handItem = player.getInventory().getItemInMainHand();
        if (handItem == null || handItem.getType() == Material.AIR) {
            player.sendMessage(RED + "✗ " + GRAY + "You must be holding an item to use " +
                    YELLOW + "[i]" + GRAY + " or " + YELLOW + "[item]");
            return;
        }

        String displayName = getCleanPlayerName(player);
        String prefix = getPlayerPrefix(player);

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            Component fullMessage = buildFullMessage(prefix, displayName, originalMessage, handItem);
            onlinePlayer.sendMessage(fullMessage);
        }
    }

    private static boolean containsItemTags(String message) {
        return ITEM_TAG_PATTERN.matcher(message).find();
    }

    private static Component buildFullMessage(String prefix, String displayName,
                                              String originalMessage, ItemStack item) {
        Component message = Component.empty();

        if (!prefix.isEmpty()) {
            message = message.append(parseLegacyText(prefix));
        }

        message = message.append(Component.text(displayName, NamedTextColor.WHITE));

        message = message.append(Component.text(": ", NamedTextColor.WHITE));

        message = message.append(parseMessageWithItemLinks(originalMessage, item));

        return message;
    }

    private static Component parseMessageWithItemLinks(String message, ItemStack item) {
        Component result = Component.empty();
        Matcher matcher = ITEM_TAG_PATTERN.matcher(message);

        int lastEnd = 0;

        while (matcher.find()) {
            if (matcher.start() > lastEnd) {
                String textBefore = message.substring(lastEnd, matcher.start());
                if (!textBefore.isEmpty()) {
                    result = result.append(parseLegacyText(textBefore));
                }
            }

            result = result.append(createItemComponent(item));

            lastEnd = matcher.end();
        }

        if (lastEnd < message.length()) {
            String remainingText = message.substring(lastEnd);
            if (!remainingText.isEmpty()) {
                result = result.append(parseLegacyText(remainingText));
            }
        }

        return result;
    }

    private static Component createItemComponent(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return Component.empty();
        }

        Component displayName = item.displayName();

        if (item.getAmount() > 1) {
            displayName = displayName.append(
                    Component.text(" x" + item.getAmount(), NamedTextColor.GRAY)
            );
        }

        Component itemComponent = Component.text("[", NamedTextColor.AQUA)
                .append(displayName)
                .append(Component.text("]", NamedTextColor.AQUA))
                .decoration(TextDecoration.BOLD, false)
                .decoration(TextDecoration.ITALIC, false);

        itemComponent = itemComponent.hoverEvent(item.asHoverEvent());

        return itemComponent;
    }

    private static Component parseLegacyText(String legacyText) {
        if (legacyText == null || legacyText.isEmpty()) {
            return Component.empty();
        }

        return net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                .legacySection()
                .deserialize(legacyText);
    }

    private static String getCleanPlayerName(Player player) {
        // TODO: Implement a actual method for getting display name
        return player.getName();
    }

    private static String getPlayerPrefix(Player player) {
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