package com.bettercontent.betterdiscoveryguides;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;

/** Routes the real result stack through configured EMI controls, focusing explicit recipes. */
final class ThreadRecipeItems {
    private ThreadRecipeItems() {}
    interface View {
        void recipes(ItemStack stack);
        void recipe(String id);
        void uses(ItemStack stack);
    }
    static void dispatch(ItemStack item, String recipe, boolean uses, View view) {
        if (uses) view.uses(item);
        else if (!recipe.isEmpty()) view.recipe(recipe);
        else view.recipes(item);
    }
    static boolean key(ItemStack item, String recipe, int key, int scan) {
        return ModList.get().isLoaded("emi") && EmiControls.key(item, recipe, key, scan);
    }
    static boolean click(ItemStack item, String recipe, int button) {
        return ModList.get().isLoaded("emi") && EmiControls.click(item, recipe, button);
    }
    private static final class EmiControls {
        private static final View VIEW = new View() {
            @Override public void recipes(ItemStack item) {
                dev.emi.emi.api.EmiApi.displayRecipes(dev.emi.emi.api.stack.EmiStack.of(item));
            }
            @Override public void recipe(String id) {
                var recipe = dev.emi.emi.api.EmiApi.getRecipeManager().getRecipe(new ResourceLocation(id));
                // A missing explicit recipe is not replaced by a generic tool lookup.
                if (recipe != null) dev.emi.emi.api.EmiApi.displayRecipe(recipe);
            }
            @Override public void uses(ItemStack item) {
                dev.emi.emi.api.EmiApi.displayUses(dev.emi.emi.api.stack.EmiStack.of(item));
            }
        };
        private static boolean key(ItemStack item, String recipe, int key, int scan) {
            if (dev.emi.emi.config.EmiConfig.viewRecipes.matchesKey(key, scan)) { dispatch(item, recipe, false, VIEW); return true; }
            if (dev.emi.emi.config.EmiConfig.viewUses.matchesKey(key, scan)) { dispatch(item, recipe, true, VIEW); return true; }
            return false;
        }
        private static boolean click(ItemStack item, String recipe, int button) {
            if (dev.emi.emi.config.EmiConfig.viewRecipes.matchesMouse(button)) { dispatch(item, recipe, false, VIEW); return true; }
            if (dev.emi.emi.config.EmiConfig.viewUses.matchesMouse(button)) { dispatch(item, recipe, true, VIEW); return true; }
            return false;
        }
    }
}
