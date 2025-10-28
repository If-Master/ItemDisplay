package me.kanuunankuulaspluginsItems.ItemDisplay.formatters;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class Components {
    // Will later on add this, right now it will not be counted
    private int countItemComponents(ItemStack item) {
        int count = 1;

        if (!item.hasItemMeta()) {
            return addMaterialComponents(item.getType(), count);
        }

        ItemMeta meta = item.getItemMeta();

        count += (meta.hasDisplayName() ? 1 : 0)
                + (meta.hasLore() && !meta.getLore().isEmpty() ? 1 : 0)
                + (meta.hasEnchants() ? meta.getEnchants().size() : 0)
                + (meta.hasAttributeModifiers() ? 1 : 0)
                + (meta.isUnbreakable() ? 1 : 0)
                + (meta.hasCustomModelData() ? 1 : 0)
                + (item.getDurability() > 0 ? 1 : 0)
                + meta.getPersistentDataContainer().getKeys().size();

        return addMaterialComponents(item.getType(), count);
    }

    private int addMaterialComponents(Material type, int count) {
        if (type.getMaxDurability() > 0) count++;
        if (type.isEdible()) count++;
        return count;
    }

}
