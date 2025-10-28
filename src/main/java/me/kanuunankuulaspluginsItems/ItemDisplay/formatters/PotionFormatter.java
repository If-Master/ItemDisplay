package me.kanuunankuulaspluginsItems.ItemDisplay.formatters;

import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.bukkit.potion.PotionData;

public class PotionFormatter {

    public static String formatPotionEffect(PotionEffect effect) {
        StringBuilder effectText = new StringBuilder();

        String effectName = formatPotionEffectName(effect.getType());
        effectText.append(effectName);

        if (effect.getAmplifier() > 0) {
            effectText.append(" ").append(toRomanNumeral(effect.getAmplifier() + 1));
        }

        int seconds = effect.getDuration() / 20;
        int minutes = seconds / 60;
        seconds = seconds % 60;
        effectText.append(" (").append(minutes).append(":").append(String.format("%02d", seconds)).append(")");

        return effectText.toString();
    }

    public static String formatPotionDataWithDuration(PotionData data) {
        StringBuilder effectText = new StringBuilder();
        PotionType type = data.getType();

        String effectName = getPotionEffectName(type);
        if (effectName.isEmpty()) {
            return formatPotionData(data);
        }

        effectText.append(effectName);

        if (data.isUpgraded()) {
            effectText.append(" II");
        }

        String duration = getPotionDuration(type, data.isExtended(), data.isUpgraded());
        if (!duration.isEmpty()) {
            effectText.append(" (").append(duration).append(")");
        }

        return effectText.toString();
    }

    public static String formatPotionTypeWithDuration(PotionType type) {
        StringBuilder effectText = new StringBuilder();

        String keyName = type.getKey().getKey();
        boolean isStrong = keyName.startsWith("strong_");
        boolean isLong = keyName.startsWith("long_");

        String effectName = getPotionEffectName(type);
        if (effectName.isEmpty()) {
            return formatPotionTypeName(type);
        }

        effectText.append(effectName);

        if (isStrong) {
            effectText.append(" II");
        }

        String duration = getPotionDuration(type, isLong, isStrong);
        if (!duration.isEmpty()) {
            effectText.append(" (").append(duration).append(")");
        }

        return effectText.toString();
    }

    public static String formatPotionTypeName(PotionType type) {
        if (type == PotionType.WATER) return "Water Bottle";
        if (type == PotionType.AWKWARD) return "Awkward Potion";
        if (type == PotionType.MUNDANE) return "Mundane Potion";
        if (type == PotionType.THICK) return "Thick Potion";

        String name = type.getKey().getKey();
        boolean isStrong = name.startsWith("strong_");
        boolean isLong = name.startsWith("long_");

        if (isStrong) {
            name = name.substring(7);
        } else if (isLong) {
            name = name.substring(5);
        }

        name = name.replace("_", " ");
        String[] words = name.split(" ");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1))
                        .append(" ");
            }
        }

        String formatted = result.toString().trim();

        if (isStrong) {
            formatted += " II";
        } else if (isLong) {
            formatted += " (Extended)";
        }

        return formatted;
    }

    private static String formatPotionData(PotionData data) {
        StringBuilder effectText = new StringBuilder();

        String typeName = formatPotionTypeName(data.getType());
        effectText.append(typeName);

        if (data.isUpgraded()) {
            effectText.append(" II");
        }

        if (data.isExtended()) {
            effectText.append(" (Extended)");
        }

        return effectText.toString();
    }

    private static String getPotionEffectName(PotionType type) {
        String keyName = type.getKey().getKey();

        if (keyName.startsWith("strong_")) {
            keyName = keyName.substring(7);
        } else if (keyName.startsWith("long_")) {
            keyName = keyName.substring(5);
        }

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
        String name = type.getName().toLowerCase().replace("_", " ");
        String[] words = name.split(" ");
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

    private static String getPotionDuration(PotionType type, boolean extended, boolean upgraded) {
        String keyName = type.getKey().getKey();

        if (keyName.startsWith("strong_")) {
            keyName = keyName.substring(7);
        } else if (keyName.startsWith("long_")) {
            keyName = keyName.substring(5);
        }

        if (keyName.equals("healing") || keyName.equals("harming")) {
            return "";
        }

        int duration = 0;

        switch (keyName) {
            case "regeneration":
                duration = upgraded ? 22 : (extended ? 90 : 45);
                break;
            case "swiftness":
                duration = extended ? 480 : 180;
                break;
            case "fire_resistance":
                duration = extended ? 480 : 180;
                break;
            case "poison":
                duration = upgraded ? 21 : (extended ? 90 : 45);
                break;
            case "night_vision":
                duration = extended ? 480 : 180;
                break;
            case "weakness":
                duration = extended ? 240 : 90;
                break;
            case "strength":
                duration = extended ? 480 : 180;
                break;
            case "slowness":
                duration = upgraded ? 20 : (extended ? 240 : 90);
                break;
            case "leaping":
                duration = extended ? 480 : 180;
                break;
            case "water_breathing":
                duration = extended ? 480 : 180;
                break;
            case "invisibility":
                duration = extended ? 480 : 180;
                break;
            case "slow_falling":
                duration = extended ? 240 : 90;
                break;
            case "turtle_master":
                duration = upgraded ? 20 : (extended ? 40 : 20);
                break;
            default:
                return "";
        }

        int minutes = duration / 60;
        int seconds = duration % 60;
        return minutes + ":" + String.format("%02d", seconds);
    }

    private static String toRomanNumeral(int number) {
        if (number <= 0) return "";
        String[] romanNumerals = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
        if (number < romanNumerals.length) {
            return romanNumerals[number];
        }
        return String.valueOf(number);
    }
}