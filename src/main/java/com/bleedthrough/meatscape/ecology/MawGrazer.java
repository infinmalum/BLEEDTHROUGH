package com.bleedthrough.meatscape.ecology;

import com.bleedthrough.meatscape.core.registry.MeatscapeItems;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/** Passive grazer whose one signature behaviour is following processed collagen. */
public final class MawGrazer extends Animal {
    public MawGrazer(EntityType<? extends MawGrazer> type, Level level) { super(type, level); }

    public static AttributeSupplier.Builder attributes() {
        return Animal.createLivingAttributes().add(Attributes.MAX_HEALTH, 16).add(Attributes.MOVEMENT_SPEED, 0.22)
                .add(Attributes.FOLLOW_RANGE, 16);
    }

    @Override protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new PanicGoal(this, 1.3));
        goalSelector.addGoal(2, new TemptGoal(this, 1.1, Ingredient.of(MeatscapeItems.COLLAGEN.get()), false));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.9));
    }

    @Override public boolean isFood(net.minecraft.world.item.ItemStack stack) { return stack.is(MeatscapeItems.COLLAGEN.get()); }
    @Override @Nullable public MawGrazer getBreedOffspring(net.minecraft.server.level.ServerLevel level, net.minecraft.world.entity.AgeableMob mate) {
        return (MawGrazer) getType().create(level);
    }
}
