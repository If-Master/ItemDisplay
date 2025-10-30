package me.kanuunankuulaspluginsItems.ItemDisplay.formatters;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;

import static me.kanuunankuulaspluginsItems.ItemDisplay.ItemDisplay.*;

public class EnchantmentFormatter {

    static {
        initializeCaches();
    }

    public static double getBaseAttackDamage(Material material) {
        return attackDamageCache.getOrDefault(material, 0.0);
    }

    public static double getBaseAttackSpeed(Material material) {
        return attackSpeedCache.getOrDefault(material, 0.0);
    }

    public static void initializeCaches() {
        // Attack Damage - Swords
        attackDamageCache.put(Material.NETHERITE_SWORD, 8.0);
        attackDamageCache.put(Material.DIAMOND_SWORD, 7.0);
        attackDamageCache.put(Material.IRON_SWORD, 6.0);
        attackDamageCache.put(Material.STONE_SWORD, 5.0);
        attackDamageCache.put(Material.GOLDEN_SWORD, 4.0);
        attackDamageCache.put(Material.WOODEN_SWORD, 4.0);

        // Attack Damage - Pickaxes
        attackDamageCache.put(Material.NETHERITE_PICKAXE, 6.0);
        attackDamageCache.put(Material.DIAMOND_PICKAXE, 5.0);
        attackDamageCache.put(Material.IRON_PICKAXE, 4.0);
        attackDamageCache.put(Material.STONE_PICKAXE, 3.0);
        attackDamageCache.put(Material.GOLDEN_PICKAXE, 2.0);
        attackDamageCache.put(Material.WOODEN_PICKAXE, 2.0);

        // Attack Damage - Shovels
        attackDamageCache.put(Material.NETHERITE_SHOVEL, 6.5);
        attackDamageCache.put(Material.DIAMOND_SHOVEL, 5.5);
        attackDamageCache.put(Material.IRON_SHOVEL, 4.5);
        attackDamageCache.put(Material.STONE_SHOVEL, 3.5);
        attackDamageCache.put(Material.GOLDEN_SHOVEL, 2.5);
        attackDamageCache.put(Material.WOODEN_SHOVEL, 2.5);

        // Attack Damage - Hoes (all 1.0)
        for (Material m : new Material[]{Material.NETHERITE_HOE, Material.DIAMOND_HOE, Material.IRON_HOE,
                Material.STONE_HOE, Material.GOLDEN_HOE, Material.WOODEN_HOE}) {
            attackDamageCache.put(m, 1.0);
        }

        // Attack Damage - Axes
        attackDamageCache.put(Material.NETHERITE_AXE, 10.0);
        attackDamageCache.put(Material.DIAMOND_AXE, 9.0);
        attackDamageCache.put(Material.IRON_AXE, 9.0);
        attackDamageCache.put(Material.STONE_AXE, 9.0);
        attackDamageCache.put(Material.GOLDEN_AXE, 7.0);
        attackDamageCache.put(Material.WOODEN_AXE, 7.0);

        // Attack Damage - Special
        attackDamageCache.put(Material.TRIDENT, 9.0);
        attackDamageCache.put(Material.MACE, 6.0);

        // Attack Speed - Swords (all 1.6)
        for (Material m : new Material[]{Material.NETHERITE_SWORD, Material.DIAMOND_SWORD, Material.IRON_SWORD,
                Material.GOLDEN_SWORD, Material.STONE_SWORD, Material.WOODEN_SWORD}) {
            attackSpeedCache.put(m, 1.6);
        }

        // Attack Speed - Pickaxes (all 1.2)
        for (Material m : new Material[]{Material.NETHERITE_PICKAXE, Material.DIAMOND_PICKAXE, Material.IRON_PICKAXE,
                Material.GOLDEN_PICKAXE, Material.STONE_PICKAXE, Material.WOODEN_PICKAXE}) {
            attackSpeedCache.put(m, 1.2);
        }

        // Attack Speed - Shovels (all 1.0)
        for (Material m : new Material[]{Material.NETHERITE_SHOVEL, Material.DIAMOND_SHOVEL, Material.IRON_SHOVEL,
                Material.GOLDEN_SHOVEL, Material.STONE_SHOVEL, Material.WOODEN_SHOVEL}) {
            attackSpeedCache.put(m, 1.0);
        }

        // Attack Speed - Hoes (varied)
        attackSpeedCache.put(Material.NETHERITE_HOE, 4.0);
        attackSpeedCache.put(Material.DIAMOND_HOE, 4.0);
        attackSpeedCache.put(Material.IRON_HOE, 3.0);
        attackSpeedCache.put(Material.GOLDEN_HOE, 1.0);
        attackSpeedCache.put(Material.STONE_HOE, 2.0);
        attackSpeedCache.put(Material.WOODEN_HOE, 1.0);

        // Attack Speed - Axes
        attackSpeedCache.put(Material.NETHERITE_AXE, 1.0);
        attackSpeedCache.put(Material.DIAMOND_AXE, 1.0);
        attackSpeedCache.put(Material.IRON_AXE, 0.9);
        attackSpeedCache.put(Material.GOLDEN_AXE, 1.0);
        attackSpeedCache.put(Material.STONE_AXE, 0.8);
        attackSpeedCache.put(Material.WOODEN_AXE, 0.8);

        // Attack Speed - Special
        attackSpeedCache.put(Material.TRIDENT, 1.1);
        attackSpeedCache.put(Material.MACE, 0.6);

        // Enchantment Order
        String[] enchantOrder = {
                "binding_curse", "vanishing_curse", "riptide", "channeling", "wind_burst",
                "frost_walker", "sharpness", "smite", "bane_of_arthropods", "impaling",
                "power", "density", "breach", "piercing", "sweeping", "multishot",
                "fire_aspect", "flame", "knockback", "punch", "protection",
                "blast_protection", "fire_protection", "projectile_protection", "feather_falling",
                "fortune", "looting", "silk_touch", "luck_of_the_sea", "efficiency",
                "quick_charge", "lure", "respiration", "aqua_affinity", "soul_speed",
                "swift_sneak", "depth_strider", "thorns", "loyalty", "unbreaking",
                "infinity", "mending"
        };

        for (int i = 0; i < enchantOrder.length; i++) {
            enchantmentOrderCache.put(enchantOrder[i], i);
        }
    }

    public static String formatEnchantmentName(Enchantment enchant) {
        String name = enchant.getKey().getKey();

        switch (name) {
            case "protection": return "Protection";
            case "fire_protection": return "Fire Protection";
            case "feather_falling": return "Feather Falling";
            case "blast_protection": return "Blast Protection";
            case "projectile_protection": return "Projectile Protection";
            case "respiration": return "Respiration";
            case "aqua_affinity": return "Aqua Affinity";
            case "thorns": return "Thorns";
            case "depth_strider": return "Depth Strider";
            case "frost_walker": return "Frost Walker";
            case "binding_curse": return "Curse of Binding";
            case "soul_speed": return "Soul Speed";
            case "swift_sneak": return "Swift Sneak";
            case "sharpness": return "Sharpness";
            case "smite": return "Smite";
            case "bane_of_arthropods": return "Bane of Arthropods";
            case "knockback": return "Knockback";
            case "fire_aspect": return "Fire Aspect";
            case "looting": return "Looting";
            case "sweeping": return "Sweeping Edge";
            case "efficiency": return "Efficiency";
            case "silk_touch": return "Silk Touch";
            case "unbreaking": return "Unbreaking";
            case "fortune": return "Fortune";
            case "power": return "Power";
            case "punch": return "Punch";
            case "flame": return "Flame";
            case "infinity": return "Infinity";
            case "luck_of_the_sea": return "Luck of the Sea";
            case "lure": return "Lure";
            case "loyalty": return "Loyalty";
            case "impaling": return "Impaling";
            case "riptide": return "Riptide";
            case "channeling": return "Channeling";
            case "multishot": return "Multishot";
            case "quick_charge": return "Quick Charge";
            case "piercing": return "Piercing";
            case "mending": return "Mending";
            case "vanishing_curse": return "Curse of Vanishing";
            default:
                StringBuilder result = new StringBuilder();
                for (String word : name.split("_")) {
                    if (!word.isEmpty()) {
                        result.append(Character.toUpperCase(word.charAt(0)))
                                .append(word.substring(1).toLowerCase())
                                .append(" ");
                    }
                }
                return result.toString().trim();
        }
    }
}
