package baritone.utils;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.client.player.LocalPlayer;

public class CustomEnchantmentHelper {

    public static int getTunnelLevel(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0;
        }

        try {
            ItemEnchantments enchantments = stack.get(DataComponents.ENCHANTMENTS);

            if (enchantments != null) {
                for (Holder<Enchantment> enchantmentHolder : enchantments.keySet()) {
                    try {
                        String holderString = enchantmentHolder.toString().toLowerCase();

                        if (holderString.contains("tunnel")) {
                            return enchantments.getLevel(enchantmentHolder);
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        } catch (Exception e) {
        }

        return 0;
    }

    public static boolean hasTunnelEnchantment(ItemStack stack) {
        return getTunnelLevel(stack) > 0;
    }

    /**
     * Check if Tunnel enchantment is ACTIVE (works only when NOT sneaking)
     * @param stack The item to check
     * @param player The player (to check if sneaking)
     * @return true if tunnel will mine 3x3
     */
    public static boolean isTunnelActive(ItemStack stack, LocalPlayer player) {
        if (player == null) {
            return false;
        }
        // Tunnel only works when NOT sneaking
        return hasTunnelEnchantment(stack) && !player.isShiftKeyDown();
    }

    public static double getTunnelEfficiencyMultiplier(ItemStack stack) {
        int level = getTunnelLevel(stack);
        if (level == 0) {
            return 1.0;
        }

        switch (level) {
            case 1: return 2.0;
            case 2: return 2.5;
            case 3: return 3.0;
            default: return 1.0 + (level * 0.5);
        }
    }

    /**
     * Get efficiency multiplier considering if player is sneaking
     */
    public static double getTunnelEfficiencyMultiplier(ItemStack stack, LocalPlayer player) {
        if (!isTunnelActive(stack, player)) {
            return 1.0; // No bonus if sneaking or no tunnel
        }
        return getTunnelEfficiencyMultiplier(stack);
    }
}