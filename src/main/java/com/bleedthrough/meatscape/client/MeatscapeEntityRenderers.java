package com.bleedthrough.meatscape.client;

import com.bleedthrough.meatscape.Meatscape;
import com.bleedthrough.meatscape.core.registry.MeatscapeEntities;
import net.minecraft.client.model.CowModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Meatscape.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class MeatscapeEntityRenderers {
    private MeatscapeEntityRenderers() { }
    @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(MeatscapeEntities.MAW_GRAZER.get(), context ->
                new MobRenderer<>(context, new CowModel<>(context.bakeLayer(ModelLayers.COW)), 0.5F) {
                    @Override public ResourceLocation getTextureLocation(com.bleedthrough.meatscape.ecology.MawGrazer entity) {
                        return ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/cow/cow.png");
                    }
                });
        event.registerEntityRenderer(MeatscapeEntities.IMMUNE_ORGANISM.get(), context ->
                new MobRenderer<>(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE)), 0.5F) {
                    @Override public ResourceLocation getTextureLocation(com.bleedthrough.meatscape.ecology.MawImmuneOrganism entity) {
                        return ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/zombie/zombie.png");
                    }
                });
    }
}
