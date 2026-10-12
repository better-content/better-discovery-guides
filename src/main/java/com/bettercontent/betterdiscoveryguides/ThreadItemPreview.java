package com.bettercontent.betterdiscoveryguides;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** A defensive copy of a native result, optionally focused on its exact EMI recipe. */
public record ThreadItemPreview(ItemStack stack, String recipeId) {
    public ThreadItemPreview {
        if (stack == null || stack.isEmpty() || recipeId == null || recipeId.length() > 128
            || !recipeId.isEmpty() && (ResourceLocation.tryParse(recipeId) == null
                || ResourceLocation.tryParse(recipeId).getPath().isEmpty()))
            throw new IllegalArgumentException("invalid item preview");
        stack = stack.copy();
    }
    @Override public ItemStack stack() { return stack.copy(); }
    void encode(FriendlyByteBuf buffer) { buffer.writeItem(stack); buffer.writeUtf(recipeId, 128); }
    static ThreadItemPreview decode(FriendlyByteBuf buffer) { return new ThreadItemPreview(buffer.readItem(), buffer.readUtf(128)); }
}
