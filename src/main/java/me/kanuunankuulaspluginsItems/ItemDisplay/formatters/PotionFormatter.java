package me.kanuunankuulaspluginsItems.ItemDisplay.formatters;

import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.bukkit.potion.PotionData;

public class PotionFormatter {
    private static final String[] ROMAN_NUMERALS = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};

    public static String formatPotionEffect(PotionEffect effect) {
        StringBuilder sb = new StringBuilder();
        sb.append(formatPotionEffectName(effect.getType()));

        if (effect.getAmplifier() > 0) {
            sb.append(" ").append(toRomanNumeral(effect.getAmplifier() + 1));
        }

        int seconds = effect.getDuration() / 20;
        sb.append(" (").append(seconds / 60).append(":").append(String.format("%02d", seconds % 60)).append(")");

        return sb.toString();
    }

    public static String formatPotionDataWithDuration(PotionData data) {
        String effectName = getPotionEffectName(data.getType());
        if (effectName.isEmpty()) {
            return formatPotionData(data);
        }

        StringBuilder sb = new StringBuilder(effectName);
        if (data.isUpgraded()) sb.append(" II");

        String duration = getPotionDuration(data.getType(), data.isExtended(), data.isUpgraded());
        if (!duration.isEmpty()) {
            sb.append(" (").append(duration).append(")");
        }

        return sb.toString();
    }

    public static String formatPotionTypeWithDuration(PotionType type) {
        String keyName = type.getKey().getKey();
        boolean isStrong = keyName.startsWith("strong_");
        boolean isLong = keyName.startsWith("long_");

        String effectName = getPotionEffectName(type);
        if (effectName.isEmpty()) {
            return formatPotionTypeName(type);
        }

        StringBuilder sb = new StringBuilder(effectName);
        if (isStrong) sb.append(" II");

        String duration = getPotionDuration(type, isLong, isStrong);
        if (!duration.isEmpty()) {
            sb.append(" (").append(duration).append(")");
        }

        return sb.toString();
    }

    public static String formatPotionTypeName(PotionType type) {
        if (type == PotionType.WATER) return "Water Bottle";
        if (type == PotionType.AWKWARD) return "Awkward Potion";
        if (type == PotionType.MUNDANE) return "Mundane Potion";
        if (type == PotionType.THICK) return "Thick Potion";

        String name = type.getKey().getKey();
        boolean isStrong = name.startsWith("strong_");
        boolean isLong = name.startsWith("long_");

        if (isStrong) name = name.substring(7);
        else if (isLong) name = name.substring(5);

        StringBuilder sb = new StringBuilder();
        for (String word : name.split("_")) {
            if (!word.isEmpty()) {
                sb.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1))
                        .append(" ");
            }
        }

        String formatted = sb.toString().trim();
        if (isStrong) return formatted + " II";
        if (isLong) return formatted + " (Extended)";
        return formatted;
    }

    private static String formatPotionData(PotionData data) {
        StringBuilder sb = new StringBuilder(formatPotionTypeName(data.getType()));
        if (data.isUpgraded()) sb.append(" II");
        if (data.isExtended()) sb.append(" (Extended)");
        return sb.toString();
    }

    private static String getPotionEffectName(PotionType type) {
        String keyName = type.getKey().getKey();
        if (keyName.startsWith("strong_")) keyName = keyName.substring(7);
        else if (keyName.startsWith("long_")) keyName = keyName.substring(5);

        switch (keyName) {
            case "regeneration": return "Regeneration";
            case "swiftness": return "Speed";
            case "fire_resistance": return "Fire Resistance";
            case "poison": return "Poison";
            case "healing": return "Instant Health";
            case "night_vision": return "Night Vision";
            case "weakness": return "Weakness";
            case "strength": return "Strength";
            case "slowness": return "Slowness";
            case "leaping": return "Jump Boost";
            case "harming": return "Instant Damage";
            case "water_breathing": return "Water Breathing";
            case "invisibility": return "Invisibility";
            case "slow_falling": return "Slow Falling";
            case "turtle_master": return "Turtle Master";
            default: return "";
        }
    }

    private static String formatPotionEffectName(PotionEffectType type) {
        StringBuilder sb = new StringBuilder();
        for (String word : type.getName().toLowerCase().split("_")) {
            if (!word.isEmpty()) {
                sb.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1))
                        .append(" ");
            }
        }
        return sb.toString().trim();
    }

    private static String getPotionDuration(PotionType type, boolean extended, boolean upgraded) {
        String keyName = type.getKey().getKey();
        if (keyName.startsWith("strong_")) keyName = keyName.substring(7);
        else if (keyName.startsWith("long_")) keyName = keyName.substring(5);

        if (keyName.equals("healing") || keyName.equals("harming")) return "";

        int duration = getDurationSeconds(keyName, extended, upgraded);
        if (duration == 0) return "";

        return (duration / 60) + ":" + String.format("%02d", duration % 60);
    }

    private static int getDurationSeconds(String keyName, boolean extended, boolean upgraded) {
        switch (keyName) {
            case "regeneration": return upgraded ? 22 : (extended ? 90 : 45);
            case "swiftness", "invisibility", "water_breathing", "leaping", "strength", "night_vision",
                 "fire_resistance": return extended ? 480 : 180;
            case "poison": return upgraded ? 21 : (extended ? 90 : 45);
            case "weakness", "slow_falling": return extended ? 240 : 90;
            case "slowness": return upgraded ? 20 : (extended ? 240 : 90);
            case "turtle_master": return upgraded ? 20 : (extended ? 40 : 20);
            default: return 0;
        }
    }

    private static String toRomanNumeral(int number) {
        return (number > 0 && number < ROMAN_NUMERALS.length) ? ROMAN_NUMERALS[number] : String.valueOf(number);
    }
}
