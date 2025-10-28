package me.kanuunankuulaspluginsItems.ItemDisplay.formatters;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Collection;

import static me.kanuunankuulaspluginsItems.ItemDisplay.formatters.EnchantmentFormatter.*;

public class AttributeFormatter {
    public static String getItemAttributes(ItemStack item) {
        StringBuilder attributes = new StringBuilder();

        double attackDamage = getBaseAttackDamage(item.getType());
        double attackSpeed = getBaseAttackSpeed(item.getType());

        if (item.hasItemMeta() && item.getItemMeta().hasAttributeModifiers()) {
            ItemMeta meta = item.getItemMeta();

            if (meta.hasAttributeModifiers()) {
                Collection<AttributeModifier> damageModifiers = meta.getAttributeModifiers(Attribute.ATTACK_DAMAGE);
                if (damageModifiers != null) {
                    for (AttributeModifier mod : damageModifiers) {
                        attackDamage += mod.getAmount();
                    }
                }

                Collection<AttributeModifier> speedModifiers = meta.getAttributeModifiers(Attribute.ATTACK_SPEED);
                if (speedModifiers != null) {
                    for (AttributeModifier mod : speedModifiers) {
                        attackSpeed += mod.getAmount();
                    }
                }
            }
        }

        if (attackDamage > 0 || attackSpeed != 4.0) {
            if (attackDamage > 0) {
                attributes.append("\n§aWhen in Main Hand:");

                if (attackDamage > 0) {
                    attributes.append("\n§a ").append(String.format("%.0f", attackDamage)).append(" Attack Damage");
                }

                if (attackSpeed != 4.0) {
                    attributes.append("\n§a ").append(String.format("%.1f", attackSpeed)).append(" Attack Speed");
                }
            }
        }
        return attributes.toString();
    }

}
