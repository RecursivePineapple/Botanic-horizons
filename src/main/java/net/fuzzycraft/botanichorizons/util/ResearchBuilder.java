package net.fuzzycraft.botanichorizons.util;

import net.fuzzycraft.botanichorizons.mod.ForgeMod;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.crafting.CrucibleRecipe;
import thaumcraft.api.crafting.InfusionRecipe;
import thaumcraft.api.crafting.ShapedArcaneRecipe;
import thaumcraft.api.crafting.ShapelessArcaneRecipe;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchItem;
import thaumcraft.api.research.ResearchPage;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedList;

public class ResearchBuilder {

    public static final String category = ForgeMod.MOD_ID;
    public static final String prefix = "BH_";

    public final String key;
    public int row = 0;
    public int col = 0;
    public int researchDifficulty = 1;
    public int warp = 0;
    public boolean mainline = false;
    public String[] dependencies = new String[0];
    public String[] hiddenDependencies = new String[0];
    public AspectList researchAspects;
    public LinkedList<ResearchPage> content = new LinkedList<>();
    public ResourceLocation researchIcon = new ResourceLocation("botania", "textures/items/grassSeeds0.png");
    public ItemStack researchItemIcon = null;

    public ResearchBuilder(String key) {
        this.key = prefix + key;
    }

    public ResearchBuilder setResearchAspects(Aspect... aspects) {
        AspectList list = new AspectList();
        for (Aspect aspect: aspects) {
            list.add(aspect, 1);
        }
        this.researchAspects = list;
        return this;
    }

    // goes in units of whole tiles - have >= 2 difference to see lines
    public ResearchBuilder setBookLocation(int x, int y) {
        this.row = y;
        this.col = x;
        return this;
    }

    public ResearchBuilder setDifficulty(int difficulty) {
        this.researchDifficulty = difficulty;
        return this;
    }

    public ResearchBuilder setResearchIconItem(String mod, String filename) {
        this.researchIcon = new ResourceLocation(mod, "textures/items/" + filename);
        return this;
    }

    public ResearchBuilder setResearchIconBlock(String mod, String filename) {
        this.researchIcon = new ResourceLocation(mod, "textures/blocks/" + filename);
        return this;
    }

    public ResearchBuilder setResearchIconItemStack(ItemStack render) {
        this.researchItemIcon = render;
        return this;
    }

    public ResearchBuilder addSingleTextPage() {
        content.add(new ResearchPage(this.key, category + "." + key + ".body"));
        return this;
    }

    public ResearchBuilder addTextPages(int i18n_start, int count) {
        for (int i = i18n_start; i < i18n_start + count; i++) {
            content.add(new ResearchPage(this.key, category + "." + key + ".body_" + i));
        }
        return this;
    }

    public ResearchBuilder addCraftingRecipe(ItemStack out, AspectList aspects, Object... craftingRecipe) {
        ShapedArcaneRecipe recipe = ThaumcraftApi.addArcaneCraftingRecipe(key, out, aspects, craftingRecipe);
        content.add(new ResearchPage(recipe));
        return this;
    }

    public ResearchBuilder addShapelessCraftingRecipe(ItemStack out, AspectList aspects, Object... craftingRecipe) {
        ShapelessArcaneRecipe recipe = ThaumcraftApi.addShapelessArcaneCraftingRecipe(key, out, aspects, craftingRecipe);
        content.add(new ResearchPage(recipe));
        return this;
    }

    public ResearchBuilder addCrucibleRecipe(AspectList aspects, ItemStack out, ItemStack in) {
        CrucibleRecipe recipe = ThaumcraftApi.addCrucibleRecipe(key, out, in, aspects);
        content.add(new ResearchPage(recipe));
        return this;
    }

    public ResearchBuilder addInfusionRecipe(AspectList aspects, ItemStack out, int instability, ItemStack centerItem, ItemStack... inputs) {
        InfusionRecipe recipe = new NonFuzzyInfusionRecipe(key,out, instability, aspects, centerItem, inputs);
        this.addInfusionRecipe(recipe);
        return this;
    }

    public ResearchBuilder addAdvancedInfusionRecipe(AspectList aspects, ItemStack out, int instability, ItemSpec centerItem, ItemSpec... inputs) {
        InfusionRecipe recipe = new AdvancedInfusionRecipe(key,out, instability, aspects, centerItem, inputs);
        this.addInfusionRecipe(recipe);
        return this;
    }

    public ResearchBuilder addInfusionRecipe(InfusionRecipe recipe) {
        try {
            Field reflectionTarget = ThaumcraftApi.class.getDeclaredField("craftingRecipes");
            reflectionTarget.setAccessible(true);
            ArrayList<Object> craftingArray = (ArrayList<Object>) reflectionTarget.get(ThaumcraftApi.class);
            craftingArray.add(recipe);
        } catch (Exception e) {
            throw new RuntimeException("Cannot apply better infusion pattern", e);
        }

        content.add(new ResearchPage(recipe));
        return this;
    }

    public ResearchBuilder setWarp(int warp) {
        this.warp = warp;
        return this;
    }

    public ResearchBuilder setMainlineResearch() {
        this.mainline = true;
        return this;
    }

    public ResearchBuilder setDependencies(String... dependencies) {
        this.dependencies = dependencies;
        for (int i = 0; i < this.dependencies.length; i++) {
            this.dependencies[i] = prefix + this.dependencies[i];
        }
        return this;
    }

    public ResearchBuilder setExternalDependencies(String... dependencies) {
        this.hiddenDependencies = dependencies;
        return this;
    }

    public ResearchBuilder apply(WithResearchBuilder lambda) {
        lambda.apply(this);
        return this;
    }

    public void commit() {
        ResearchItem research;
        if (researchItemIcon != null) {
            research = new ResearchItem(
                    key, category, researchAspects,
                    col, row, researchDifficulty,
                    researchItemIcon
            );
        } else {
            research = new ResearchItem(
                    key, category, researchAspects,
                    col, row, researchDifficulty,
                    researchIcon
            );
        }
        research.setPages(content.toArray(new ResearchPage[0]));
        research.parents = dependencies;
        research.parentsHidden = hiddenDependencies;
        research.setConcealed();
        if (mainline) {
            research.setSpecial();
        }

        ResearchCategories.addResearch(research);
        if (warp > 0) {
            ThaumcraftApi.addWarpToResearch(key, warp);
        }
    }

    public interface WithResearchBuilder {
        void apply(ResearchBuilder builder);
    }
}
