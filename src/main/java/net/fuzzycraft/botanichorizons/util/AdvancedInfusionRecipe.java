package net.fuzzycraft.botanichorizons.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

import cpw.mods.fml.common.FMLLog;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.crafting.InfusionRecipe;

public class AdvancedInfusionRecipe extends InfusionRecipe {

    private ItemSpec input;
    private ItemSpec[] recipe;

    public AdvancedInfusionRecipe(String research, Object output, int inst, AspectList aspects, ItemSpec input, ItemSpec... recipe) {
        super(
            research,
            output,
            inst,
            aspects,
            input.getExample(),
            Arrays.stream(recipe)
                .map(spec -> spec.getExample())
                .filter(item -> item != null)
                .toArray(ItemStack[]::new)
        );

        String prefix = "[infusion recipe " + research + "] ";

        Objects.requireNonNull(research, prefix + "research cannot be null");
        Objects.requireNonNull(output, prefix + "output cannot be null");
        Objects.requireNonNull(aspects, prefix + "aspects cannot be null");
        Objects.requireNonNull(input, prefix + "input cannot be null");
        Objects.requireNonNull(input.getExample(), prefix + "input " + input + " must refer to a real item");
        Objects.requireNonNull(recipe, prefix + "recipe cannot be null");
        for(ItemSpec spec : recipe) {
            Objects.requireNonNull(spec, prefix + "recipe cannot contain invalid specs");
            if(!spec.hasItem()) {
                FMLLog.bigWarning("%sinput could not be found and will not be added to the recipe: %s", prefix, spec);
            }
        }

        this.input = input;
        this.recipe = Arrays.stream(recipe)
            .filter(spec -> spec.getExample() != null)
            .toArray(ItemSpec[]::new);
    }

    @Override
    public boolean matches(ArrayList<ItemStack> input, ItemStack central, World world, EntityPlayer player) {
        if (this.getRecipeInput() == null) {
            return false;
        } else if (this.research.length() > 0 && !ThaumcraftApiHelper.isResearchComplete(player.getCommandSenderName(), this.research)) {
            return false;
        } else {
            ItemStack i2 = central.copy();

            if (!this.input.matches(i2)) {
                return false;
            }

            ArrayList<ItemStack> denormalized = new ArrayList<>();

            for (ItemStack is : input) {
                if(is.stackSize == 1) {
                    denormalized.add(is.copy());
                } else {
                    for(int i = 0; i < is.stackSize; i++) {
                        ItemStack copy = is.copy();
                        copy.stackSize = 1;
                        denormalized.add(copy);
                    }
                }
            }

            for (ItemSpec spec : this.recipe) {
                boolean found = false;

                for(int i = 0; i < denormalized.size(); i++) {
                    if(spec.matches(denormalized.get(i))) {
                        found = true;
                        denormalized.remove(i);
                        break;
                    }
                }

                if(!found) {
                    return false;
                }
            }

            return denormalized.isEmpty();
        }
    }
}
