package com.bettercontent.betterdiscoveryguides;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;

/** Routes a hovered Thread item through the player's configured EMI controls. */
final class ThreadRecipeItems {
    private ThreadRecipeItems() {}

    static boolean key(ItemStack item, int key, int scan) {
        return ModList.get().isLoaded("emi") && EmiControls.key(item, key, scan);
    }

    static boolean click(ItemStack item, int button) {
        return ModList.get().isLoaded("emi") && EmiControls.click(item, button);
    }

    private static final class EmiControls {
        private static boolean key(ItemStack item, int key, int scan) {
            if (dev.emi.emi.config.EmiConfig.viewRecipes.matchesKey(key, scan)) {
                dev.emi.emi.api.EmiApi.displayRecipes(dev.emi.emi.api.stack.EmiStack.of(item));
                return true;
            }
            if (dev.emi.emi.config.EmiConfig.viewUses.matchesKey(key, scan)) {
                dev.emi.emi.api.EmiApi.displayUses(dev.emi.emi.api.stack.EmiStack.of(item));
                return true;
            }
            return false;
        }

        private static boolean click(ItemStack item, int button) {
            if (dev.emi.emi.config.EmiConfig.viewRecipes.matchesMouse(button)) {
                dev.emi.emi.api.EmiApi.displayRecipes(dev.emi.emi.api.stack.EmiStack.of(item));
                return true;
            }
            if (dev.emi.emi.config.EmiConfig.viewUses.matchesMouse(button)) {
                dev.emi.emi.api.EmiApi.displayUses(dev.emi.emi.api.stack.EmiStack.of(item));
                return true;
            }
            return false;
        }
    }
}
