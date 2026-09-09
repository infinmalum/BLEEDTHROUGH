package com.bleedthrough.meatscape.ecology;

import com.bleedthrough.meatscape.world.data.MeatscapeWorldData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** Local immune response: territorial around its habitat, not a global base-aware simulation. */
public final class MawImmuneOrganism extends PathfinderMob {
    public MawImmuneOrganism(EntityType<? extends MawImmuneOrganism> type, Level level) { super(type, level); }

    public static AttributeSupplier.Builder attributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 24).add(Attributes.ATTACK_DAMAGE, 4)
                .add(Attributes.MOVEMENT_SPEED, 0.25).add(Attributes.FOLLOW_RANGE, 16);
    }

    @Override protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.05, true));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                target -> target instanceof Player player && immuneResponseActive()
                        && !player.isCreative() && !player.isSpectator()
                        && (!hasRestriction() || isWithinRestriction(player.blockPosition()))
                        && distanceToSqr(player) < 144));
    }

    @Override public void aiStep() {
        if (!level().isClientSide && !immuneResponseActive()) {
            setTarget(null);
            getNavigation().stop();
        }
        super.aiStep();
    }

    public boolean immuneResponseActive() {
        return level().getDifficulty() != Difficulty.PEACEFUL
                && (!(level() instanceof ServerLevel level)
                || !MeatscapeWorldData.get(level.getServer()).isPaused());
    }
}
