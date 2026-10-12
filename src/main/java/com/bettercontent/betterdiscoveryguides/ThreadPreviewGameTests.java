package com.bettercontent.betterdiscoveryguides;

import com.google.gson.JsonParser;
import io.netty.buffer.Unpooled;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import java.util.List;

/** Registry, native result, and packet contracts run under Forge, never a mock bootstrap. */
@GameTestHolder("better_discovery_guides")
@PrefixGameTestTemplate(false)
public final class ThreadPreviewGameTests {
    private static ItemStack output(String material) {
        var stack=new ItemStack(Items.STONE);
        var materials=new ListTag(); materials.add(StringTag.valueOf("tconstruct:"+material));
        stack.getOrCreateTag().put("tic_materials",materials);
        stack.getOrCreateTag().putString("test_output",material);
        return stack;
    }
    @GameTest(template="empty") public static void native_recipe_manager_owns_preview_results(GameTestHelper h) {
        var refs=ThreadArt.BY_ID.get("first_hand_axe").recipeItems();
        var manager=new net.minecraft.world.item.crafting.RecipeManager();
        var recipes=new java.util.ArrayList<net.minecraft.world.item.crafting.Recipe<?>>();
        for(var ref:refs) recipes.add(new net.minecraft.world.item.crafting.ShapelessRecipe(
            new net.minecraft.resources.ResourceLocation(ref.id()),"",net.minecraft.world.item.crafting.CraftingBookCategory.EQUIPMENT,
            output(ref.id()),net.minecraft.core.NonNullList.create()));
        manager.replaceRecipes(recipes);
        var previews=ThreadRecipeReference.resolve(refs,manager,h.getLevel().registryAccess());
        equal(6,previews.size());
        for(int i=0;i<6;i++) check(ItemStack.matches(recipes.get(i).getResultItem(h.getLevel().registryAccess()),previews.get(i).stack()),"Native result differs");
        manager.replaceRecipes(recipes.subList(0,5));
        equal(5,ThreadRecipeReference.resolve(refs,manager,h.getLevel().registryAccess()).size());
        h.succeed();
    }
    @GameTest(template="empty") public static void six_preview_stacks_and_nbt_survive_packet(GameTestHelper h) {
        var refs=ThreadArt.BY_ID.get("first_hand_axe").recipeItems();
        var previews=ThreadRecipeReference.resolve(refs,id->output(id.getPath()));
        equal(6,previews.size());
        equal(6L,previews.stream().map(p->p.stack().getTag().toString()).distinct().count());
        var d=ThreadArt.BY_ID.get("first_hand_axe");
        var card=new ThreadNetwork.Card(d.id(),d.conceptId(),d.title(),d.topic().id(),d.order(),"",d.art().toString(),true,true,true,d.event(),d.cause(),d.action(),previews,"","",1,0,0,1,"");
        var buffer=new FriendlyByteBuf(Unpooled.buffer());
        try {
            card.encode(buffer); var decoded=ThreadNetwork.Card.decode(buffer);
            equal(6,decoded.recipeItems().size());
            for(int i=0;i<6;i++) {
                equal(refs.get(i).id(),decoded.recipeItems().get(i).recipeId());
                check(ItemStack.matches(previews.get(i).stack(),decoded.recipeItems().get(i).stack()),"Packet lost result NBT");
            }
            equal(0,buffer.readableBytes());
        } finally { buffer.release(); }
        var original=output("flint");
        var preview=new ThreadItemPreview(original,"kubejs:primitive/flint_hand_axe");
        original.getTag().putString("test_output","changed");
        preview.stack().getTag().putString("test_output","also_changed");
        equal("flint",preview.stack().getTag().getString("test_output"));
        h.succeed();
    }
    @GameTest(template="empty") public static void emi_dispatch_keeps_stack_and_exact_recipe(GameTestHelper h) {
        var item=output("bone"); var called=new java.util.ArrayList<String>();
        var view=new ThreadRecipeItems.View() {
            public void recipes(ItemStack stack) {check(item==stack,"Generic lookup replaced stack");called.add("generic");}
            public void recipe(String id) {called.add(id);}
            public void uses(ItemStack stack) {check(item==stack,"Uses replaced stack");equal("bone",stack.getTag().getString("test_output"));called.add("uses");}
        };
        ThreadRecipeItems.dispatch(item,"kubejs:primitive/bone_butcher_knife",false,view);
        ThreadRecipeItems.dispatch(item,"kubejs:primitive/bone_butcher_knife",true,view);
        ThreadRecipeItems.dispatch(item,"",false,view);
        equal(List.of("kubejs:primitive/bone_butcher_knife","uses","generic"),called);
        h.succeed();
    }
    @GameTest(template="empty") public static void preview_rows_wrap_without_merging_variants(GameTestHelper h) {
        for(int width:List.of(80,128,230)) {
            int columns=ThreadPreviewLayout.columns(width);
            check(columns*20+4<=width,"Preview row exceeds panel");
            var positions=new java.util.HashSet<String>();
            for(int i=0;i<6;i++) positions.add((i/columns)+":"+(i%columns));
            equal(6,positions.size());
        }
        equal(3,ThreadPreviewLayout.columns(80));
        h.succeed();
    }
    @GameTest(template="empty") public static void missing_recipe_never_substitutes_generic_tool(GameTestHelper h) {
        check(ThreadRecipeReference.resolve(ThreadArt.BY_ID.get("first_hand_axe").recipeItems(),id->ItemStack.EMPTY).isEmpty(),"Missing recipe produced generic tool");
        var plain=ThreadRecipeReference.parse(JsonParser.parseString("\"minecraft:stone\""));
        var previews=ThreadRecipeReference.resolve(List.of(plain),id->ItemStack.EMPTY);
        equal(1,previews.size()); equal("",previews.get(0).recipeId());
        h.succeed();
    }
    @GameTest(template="empty") public static void previews_validate_bounds_references_and_privacy(GameTestHelper h) {
        for(String json:List.of("{}","{\"recipe\":7}","{\"recipe\":\"bad id\"}","{\"recipe\":\"\"}","{\"recipe\":\"minecraft:stone\",\"nbt\":\"{}\"}","7"))
            invalid(()->ThreadRecipeReference.parse(JsonParser.parseString(json)));
        invalid(()->new ThreadRecipeReference("x".repeat(129),true));
        var ref=new ThreadRecipeReference("minecraft:stone",false);
        equal(8,ThreadRecipeReference.resolve(java.util.Collections.nCopies(8,ref),id->ItemStack.EMPTY).size());
        invalid(()->ThreadRecipeReference.resolve(java.util.Collections.nCopies(9,ref),id->ItemStack.EMPTY));
        var d=ThreadArt.BY_ID.get("first_hand_axe");
        var preview=new ThreadItemPreview(output("flint"),"kubejs:primitive/flint_hand_axe");
        invalid(()->new ThreadNetwork.Card(d.id(),d.conceptId(),d.title(),d.topic().id(),d.order(),"",d.art().toString(),false,false,false,"","","",List.of(preview),"","",0,-1,-1,0,""));
        invalid(()->new ThreadNetwork.Card(d.id(),d.conceptId(),d.title(),d.topic().id(),d.order(),"",d.art().toString(),true,true,true,d.event(),d.cause(),d.action(),java.util.Collections.nCopies(9,preview),"","",1,0,0,1,""));
        var buffer=new FriendlyByteBuf(Unpooled.buffer());
        try {buffer.writeItem(ItemStack.EMPTY);buffer.writeUtf("");invalid(()->ThreadItemPreview.decode(buffer));}
        finally {buffer.release();}
        h.succeed();
    }
    private static void check(boolean condition,String message) {if(!condition)throw new GameTestAssertException(message);}
    private static void equal(Object expected,Object actual) {check(java.util.Objects.equals(expected,actual),"Expected "+expected+" but got "+actual);}
    private static void invalid(Runnable action) {
        try {action.run();} catch(IllegalArgumentException expected) {return;}
        throw new GameTestAssertException("Invalid preview was accepted");
    }
}
