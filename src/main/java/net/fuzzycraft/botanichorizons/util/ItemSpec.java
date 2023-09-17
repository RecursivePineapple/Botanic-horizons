package net.fuzzycraft.botanichorizons.util;

import java.util.ArrayList;
import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import cpw.mods.fml.common.registry.GameData;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

public interface ItemSpec {
    
    public boolean matches(@Nullable ItemStack item);

    public @Nullable ItemStack getExample();

    public default boolean hasItem() {
        ItemStack ex = this.getExample();
        return ex != null && ex.getItem() != null;
    }

    public static @Nonnull SpecificItemSpec specific(int itemId) {
        return new SpecificItemSpec(
            String.format("itemId=%d", itemId),
            new ItemStack(GameData.getItemRegistry().getObjectById(itemId))
        );
    }

    public static @Nonnull SpecificItemSpec specific(@Nullable Item item) {
        return new SpecificItemSpec(
            String.format("Item=%s", item),
            item == null ? null : new ItemStack(item)
        );
    }

    public static @Nonnull SpecificItemSpec specific(@Nullable Block block) {
        return new SpecificItemSpec(
            String.format("Block=%s", block),
            block == null ? null : new ItemStack(Item.getItemFromBlock(block))
        );
    }

    public static @Nonnull SpecificItemSpec specific(String name) {
        Item item = GameData.getItemRegistry().getObject(name);

        return new SpecificItemSpec(
            String.format("Name=%s, Item=%s", name, item),
            item == null ? null : new ItemStack(item)
        );
    }

    public static @Nonnull SpecificItemSpec specific(@Nullable ItemStack item) {
        return new SpecificItemSpec(
            String.format("Stack=%s", item),
            item
        );
    }

    public static @Nonnull FuzzyItemSpec fuzzy(@Nullable Item item) {
        return new FuzzyItemSpec(new ItemStack(item), false, false);
    }

    public static @Nonnull FuzzyItemSpec fuzzy(@Nonnull ItemStack item) {
        return new FuzzyItemSpec(item, false, false);
    }

    public static @Nonnull OreDictItemSpec oreDict(@Nonnull String oreDict) {
        return new OreDictItemSpec(oreDict);
    }

    public default @Nonnull ItemSpec or(@Nonnull ItemSpec next) {
        return new OrItemSpec(this).or(next);
    }

    public default @Nonnull ExampleItemSpec withExample(@Nonnull ItemStack example) {
        return new ExampleItemSpec(this, example);
    }

    public default @Nonnull ExampleItemSpec withExample(@Nonnull ItemSpec example) {
        ItemStack example2 = example.getExample();
        Objects.requireNonNull(example2, "example cannot be null");
        return new ExampleItemSpec(this, example2);
    }

    class SpecificItemSpec implements ItemSpec {
        private String name;
        private @Nullable ItemStack item;

        SpecificItemSpec(String name, @Nullable ItemStack item) {
            this.name = name;
            this.item = item == null || item.getItem() == null ? null : item;
        }

        public @Nonnull SpecificItemSpec withDamage(int damage) {
            if(this.item != null) {
                this.item.setItemDamage(damage);
            }

            return this;
        }

        @Override
        public boolean matches(@Nullable ItemStack item) {
            return ItemStack.areItemStacksEqual(this.item, item);
        }

        @Override
        public ItemStack getExample() {
            return this.item;
        }

        @Override
        public String toString() {
            return String.format("SpecificItemSpec[%s, %s]", this.name, this.item);
        }
    }

    class FuzzyItemSpec implements ItemSpec {
        private @Nullable ItemStack item;
        private boolean matchDamage;
        private boolean matchNBT;

        FuzzyItemSpec(@Nullable ItemStack item, boolean matchDamage, boolean matchNBT) {
            this.item = item;
        }

        public FuzzyItemSpec matchDamage(boolean match) {
            this.matchDamage = match;
            return this;
        }

        public FuzzyItemSpec matchNBT(boolean match) {
            this.matchNBT = match;
            return this;
        }

        @Override
        public boolean matches(@Nullable ItemStack other) {
            ItemStack item = this.item;

            if(item == null || other == null) {
                return false;
            }

            if(!Objects.equals(item.getItem(), other.getItem())) {
                return false;
            }

            if(this.matchDamage && item.getItemDamage() != other.getItemDamage()) {
                return false;
            }

            if(this.matchNBT && !ItemStack.areItemStackTagsEqual(item, other)) {
                return false;
            }

            return true;
        }

        @Override
        public ItemStack getExample() {
            return this.item;
        }

        @Override
        public String toString() {
            return String.format("FuzzyItemSpec[%s, matchDamage=%b, matchNBT=%b]", this.item, this.matchDamage, this.matchNBT);
        }
    }

    class OreDictItemSpec implements ItemSpec {

        private String oreDict;
        private ArrayList<ItemStack> items;

        OreDictItemSpec(@NotNull String oreDict) {
            Objects.requireNonNull(oreDict, "oreDict cannot be null");
            this.oreDict = oreDict;
            this.items = OreDictionary.getOres(oreDict);
        }

        @Override
        public boolean matches(@Nullable ItemStack item) {
            for(ItemStack ore : this.items) {
                if(ItemStack.areItemStacksEqual(ore, item)) {
                    return true;
                }
            }

            return false;
        }

        @Override
        public ItemStack getExample() {
            return items.isEmpty() ? null : items.get(0);
        }

        @Override
        public String toString() {
            return String.format("OreDictItemSpec[%s, items=%s]", this.oreDict, this.items);
        }
    }

    class OrItemSpec implements ItemSpec {
        private ArrayList<ItemSpec> specs;

        OrItemSpec(@Nonnull ItemSpec base) {
            Objects.requireNonNull(base, "base cannot be null");
            this.specs = new ArrayList<>();
            this.specs.add(base);
        }

        @Override
        public boolean matches(@Nullable ItemStack item) {
            for(ItemSpec spec : this.specs) {
                if(spec.hasItem() && spec.matches(item)) {
                    return true;
                }
            }

            return false;
        }

        @Override
        public ItemStack getExample() {
            for(ItemSpec spec : this.specs) {
                if(spec.hasItem()) {
                    return spec.getExample();
                }
            }

            return null;
        }

        @Override
        public @Nonnull ItemSpec or(@Nonnull ItemSpec next) {
            Objects.requireNonNull(next, "next cannot be null");
            this.specs.add(next);
            return this;
        }

        @Override
        public String toString() {
            return String.format("OrItemSpec[%s]", this.specs);
        }
    }

    class ExampleItemSpec implements ItemSpec {
        private ItemSpec spec;
        private ItemStack example;

        ExampleItemSpec(@Nonnull ItemSpec spec, @Nonnull ItemStack example) {
            Objects.requireNonNull(spec, "spec cannot be null");
            Objects.requireNonNull(example, "example cannot be null");
            this.spec = spec;
            this.example = example;
        }

        @Override
        public boolean matches(@Nullable ItemStack item) {
            return this.spec.matches(item);
        }

        @Override
        public ItemStack getExample() {
            return this.example;
        }

        @Override
        public String toString() {
            return String.format("ExampleItemSpec[example=%s, %s]", this.example, this.spec);
        }
    }
}
