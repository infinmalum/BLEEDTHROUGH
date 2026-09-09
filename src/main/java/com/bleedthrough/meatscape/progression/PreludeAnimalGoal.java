package com.bleedthrough.meatscape.progression;

import com.bleedthrough.meatscape.Meatscape;
import com.bleedthrough.meatscape.core.config.MeatscapeConfig;
import com.bleedthrough.meatscape.world.data.MeatscapeWorldData;
import java.util.EnumSet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Temporarily owns movement/look goals. Never writes NoAI, health, effects or entity NBT. */
@Mod.EventBusSubscriber(modid = Meatscape.MOD_ID)
public final class PreludeAnimalGoal extends Goal {
    private final Animal animal;
    public PreludeAnimalGoal(Animal animal) {
        this.animal = animal;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }
    @Override public boolean canUse() {
        if (!(animal.level() instanceof ServerLevel level) || !level.dimension().equals(Level.OVERWORLD)
                || !MeatscapeConfig.BLEEDING_ENABLED.get()) return false;
        var data = MeatscapeWorldData.get(level.getServer());
        return !data.isPaused() && PreludeTimeline.anomaly(data.preludeElapsed())
                && data.bleedingOrigin().filter(origin -> animal.blockPosition().distSqr(origin) <= 32.0D * 32.0D
                        && level.players().stream().anyMatch(player -> !player.isSpectator()
                        && player.blockPosition().distSqr(origin) <= 128.0D * 128.0D)).isPresent();
    }
    @Override public boolean canContinueToUse() { return canUse(); }
    @Override public void start() { animal.getNavigation().stop(); }
    @Override public void tick() { animal.getNavigation().stop(); }
    @SubscribeEvent public static void join(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof Animal animal) {
            // An entity object can rejoin after a dimension transfer; avoid duplicate goals.
            if (animal.goalSelector.getAvailableGoals().stream().noneMatch(goal -> goal.getGoal() instanceof PreludeAnimalGoal)) {
                animal.goalSelector.addGoal(0, new PreludeAnimalGoal(animal));
            }
        }
    }
}
