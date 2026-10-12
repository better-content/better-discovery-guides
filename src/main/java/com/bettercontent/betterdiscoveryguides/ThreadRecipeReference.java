package com.bettercontent.betterdiscoveryguides;

import com.google.gson.JsonElement;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import java.util.List;
import java.util.function.Function;

/** Fixed catalogue references; recipe results remain owned by the native recipe manager. */
public record ThreadRecipeReference(String id, boolean recipe) {
    public ThreadRecipeReference {
        if (id == null || id.isBlank() || id.length() > 128 || ResourceLocation.tryParse(id) == null
            || ResourceLocation.tryParse(id).getPath().isEmpty())
            throw new IllegalArgumentException("invalid preview reference");
    }
    static ThreadRecipeReference parse(JsonElement value) {
        if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString())
            return new ThreadRecipeReference(value.getAsString(), false);
        if (!value.isJsonObject() || value.getAsJsonObject().size() != 1 || !value.getAsJsonObject().has("recipe")
            || !value.getAsJsonObject().get("recipe").isJsonPrimitive()
            || !value.getAsJsonObject().get("recipe").getAsJsonPrimitive().isString())
            throw new IllegalArgumentException("invalid recipe reference");
        return new ThreadRecipeReference(value.getAsJsonObject().get("recipe").getAsString(), true);
    }
    static List<ThreadItemPreview> resolve(List<ThreadRecipeReference> refs, RecipeManager recipes, RegistryAccess access) {
        return resolve(refs, id -> recipes.byKey(id).map(r -> r.getResultItem(access)).orElse(ItemStack.EMPTY));
    }
    static List<ThreadItemPreview> resolve(List<ThreadRecipeReference> refs, Function<ResourceLocation, ItemStack> recipes) {
        if (refs.size() > 8) throw new IllegalArgumentException("too many previews");
        return refs.stream().map(ref -> {
            ItemStack stack = ref.recipe ? recipes.apply(new ResourceLocation(ref.id))
                : new ItemStack(BuiltInRegistries.ITEM.get(new ResourceLocation(ref.id)));
            return stack.isEmpty() ? null : new ThreadItemPreview(stack, ref.recipe ? ref.id : "");
        }).filter(java.util.Objects::nonNull).toList();
    }
}
