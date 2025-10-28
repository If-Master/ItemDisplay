package me.kanuunankuulaspluginsItems.ItemDisplay.formatters;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;

import static me.kanuunankuulaspluginsItems.ItemDisplay.ItemDisplay.*;

public class EnchantmentFormatter {
    public static double getBaseAttackDamage(Material material) {
        return attackDamageCache.getOrDefault(material, 0.0);
    }
    public static double getBaseAttackSpeed(Material material) {
        return attackSpeedCache.getOrDefault(material, 0.0);
    }

    public static void initializeCaches() {
        // Attack Damage
        attackDamageCache.put(Material.NETHERITE_SWORD, 8.0);
        attackDamageCache.put(Material.DIAMOND_SWORD, 7.0);
        attackDamageCache.put(Material.IRON_SWORD, 6.0);
        attackDamageCache.put(Material.STONE_SWORD, 5.0);
        attackDamageCache.put(Material.GOLDEN_SWORD, 4.0);
        attackDamageCache.put(Material.WOODEN_SWORD, 4.0);

        attackDamageCache.put(Material.NETHERITE_PICKAXE, 6.0);
        attackDamageCache.put(Material.DIAMOND_PICKAXE, 5.0);
        attackDamageCache.put(Material.IRON_PICKAXE, 4.0);
        attackDamageCache.put(Material.STONE_PICKAXE, 3.0);
        attackDamageCache.put(Material.GOLDEN_PICKAXE, 2.0);
        attackDamageCache.put(Material.WOODEN_PICKAXE, 2.0);

        attackDamageCache.put(Material.NETHERITE_SHOVEL, 6.5);
        attackDamageCache.put(Material.DIAMOND_SHOVEL, 5.5);
        attackDamageCache.put(Material.IRON_SHOVEL, 4.5);
        attackDamageCache.put(Material.STONE_SHOVEL, 3.5);
        attackDamageCache.put(Material.GOLDEN_SHOVEL, 2.5);
        attackDamageCache.put(Material.WOODEN_SHOVEL, 2.5);

        attackDamageCache.put(Material.NETHERITE_HOE, 1.0);
        attackDamageCache.put(Material.DIAMOND_HOE, 1.0);
        attackDamageCache.put(Material.IRON_HOE, 1.0);
        attackDamageCache.put(Material.STONE_HOE, 1.0);
        attackDamageCache.put(Material.GOLDEN_HOE, 1.0);
        attackDamageCache.put(Material.WOODEN_HOE, 1.0);

        attackDamageCache.put(Material.NETHERITE_AXE, 10.0);
        attackDamageCache.put(Material.DIAMOND_AXE, 9.0);
        attackDamageCache.put(Material.IRON_AXE, 9.0);
        attackDamageCache.put(Material.STONE_AXE, 9.0);
        attackDamageCache.put(Material.GOLDEN_AXE, 7.0);
        attackDamageCache.put(Material.WOODEN_AXE, 7.0);

        attackDamageCache.put(Material.TRIDENT, 9.0);
        attackDamageCache.put(Material.MACE, 6.0);

        // Attack speed
        attackSpeedCache.put(Material.NETHERITE_SWORD, 1.6);
        attackSpeedCache.put(Material.DIAMOND_SWORD, 1.6);
        attackSpeedCache.put(Material.IRON_SWORD, 1.6);
        attackSpeedCache.put(Material.GOLDEN_SWORD, 1.6);
        attackSpeedCache.put(Material.STONE_SWORD, 1.6);
        attackSpeedCache.put(Material.WOODEN_SWORD, 1.6);

        attackSpeedCache.put(Material.NETHERITE_PICKAXE, 1.2);
        attackSpeedCache.put(Material.DIAMOND_PICKAXE, 1.2);
        attackSpeedCache.put(Material.IRON_PICKAXE, 1.2);
        attackSpeedCache.put(Material.GOLDEN_PICKAXE, 1.2);
        attackSpeedCache.put(Material.STONE_PICKAXE, 1.2);
        attackSpeedCache.put(Material.WOODEN_PICKAXE, 1.2);

        attackSpeedCache.put(Material.NETHERITE_SHOVEL, 1.0);
        attackSpeedCache.put(Material.DIAMOND_SHOVEL, 1.0);
        attackSpeedCache.put(Material.IRON_SHOVEL, 1.0);
        attackSpeedCache.put(Material.GOLDEN_SHOVEL, 1.0);
        attackSpeedCache.put(Material.STONE_SHOVEL, 1.0);
        attackSpeedCache.put(Material.WOODEN_SHOVEL, 1.0);

        attackSpeedCache.put(Material.NETHERITE_HOE, 4.0);
        attackSpeedCache.put(Material.DIAMOND_HOE, 4.0);
        attackSpeedCache.put(Material.IRON_HOE, 3.0);
        attackSpeedCache.put(Material.GOLDEN_HOE, 1.0);
        attackSpeedCache.put(Material.STONE_HOE, 2.0);
        attackSpeedCache.put(Material.WOODEN_HOE, 1.0);

        attackSpeedCache.put(Material.NETHERITE_AXE, 1.0);
        attackSpeedCache.put(Material.DIAMOND_AXE, 1.0);
        attackSpeedCache.put(Material.IRON_AXE, 0.9);
        attackSpeedCache.put(Material.GOLDEN_AXE, 1.0);
        attackSpeedCache.put(Material.STONE_AXE, 0.8);
        attackSpeedCache.put(Material.WOODEN_AXE, 0.8);

        attackSpeedCache.put(Material.TRIDENT, 1.1);
        attackSpeedCache.put(Material.MACE, 0.6);

        // Enchantments
        enchantmentOrderCache.put("binding_curse", 0);
        enchantmentOrderCache.put("vanishing_curse", 1);
        enchantmentOrderCache.put("riptide", 2);
        enchantmentOrderCache.put("channeling", 3);
        enchantmentOrderCache.put("wind_burst", 4);
        enchantmentOrderCache.put("frost_walker", 5);
        enchantmentOrderCache.put("sharpness", 6);
        enchantmentOrderCache.put("smite", 7);
        enchantmentOrderCache.put("bane_of_arthropods", 8);
        enchantmentOrderCache.put("impaling", 9);
        enchantmentOrderCache.put("power", 10);
        enchantmentOrderCache.put("density", 11);
        enchantmentOrderCache.put("breach", 12);
        enchantmentOrderCache.put("piercing", 13);
        enchantmentOrderCache.put("sweeping", 14);
        enchantmentOrderCache.put("multishot", 15);
        enchantmentOrderCache.put("fire_aspect", 16);
        enchantmentOrderCache.put("flame", 17);
        enchantmentOrderCache.put("knockback", 18);
        enchantmentOrderCache.put("punch", 19);
        enchantmentOrderCache.put("protection", 20);
        enchantmentOrderCache.put("blast_protection", 21);
        enchantmentOrderCache.put("fire_protection", 22);
        enchantmentOrderCache.put("projectile_protection", 23);
        enchantmentOrderCache.put("feather_falling", 24);
        enchantmentOrderCache.put("fortune", 25);
        enchantmentOrderCache.put("looting", 26);
        enchantmentOrderCache.put("silk_touch", 27);
        enchantmentOrderCache.put("luck_of_the_sea", 28);
        enchantmentOrderCache.put("efficiency", 29);
        enchantmentOrderCache.put("quick_charge", 30);
        enchantmentOrderCache.put("lure", 31);
        enchantmentOrderCache.put("respiration", 32);
        enchantmentOrderCache.put("aqua_affinity", 33);
        enchantmentOrderCache.put("soul_speed", 34);
        enchantmentOrderCache.put("swift_sneak", 35);
        enchantmentOrderCache.put("depth_strider", 36);
        enchantmentOrderCache.put("thorns", 37);
        enchantmentOrderCache.put("loyalty", 38);
        enchantmentOrderCache.put("unbreaking", 39);
        enchantmentOrderCache.put("infinity", 40);
        enchantmentOrderCache.put("mending", 41);
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
    }

}
