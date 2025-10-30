package me.kanuunankuulaspluginsItems.ItemDisplay.formatters;

import org.bukkit.Color;
import org.bukkit.FireworkEffect;

import static me.kanuunankuulaspluginsItems.ItemDisplay.formatters.Colors.*;

public class FireworkFormatter {

    public static String formatFireworkEffect(FireworkEffect effect) {
        StringBuilder effectText = new StringBuilder();

        String type = formatFireworkType(effect.getType());
        effectText.append(type);

        if (!effect.getColors().isEmpty()) {
            effectText.append(" (");
            boolean first = true;
            for (Color color : effect.getColors()) {
                if (!first) effectText.append(", ");
                effectText.append(getColorName(color));
                first = false;
            }
            effectText.append(")");
        }

        if (effect.hasTrail()) {
            effectText.append(" §7Trail");
        }
        if (effect.hasFlicker()) {
            effectText.append(" §7Twinkle");
        }

        return effectText.toString();
    }

    private static String formatFireworkType(FireworkEffect.Type type) {
        switch (type) {
            case BALL: return "Small Ball";
            case BALL_LARGE: return "Large Ball";
            case STAR: return "Star";
            case BURST: return "Burst";
            case CREEPER: return "Creeper";
            default: return type.name();
        }
    }

}
