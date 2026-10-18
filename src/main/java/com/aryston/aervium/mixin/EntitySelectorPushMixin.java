package com.aryston.aervium.mixin;

import com.aryston.aervium.world.entity.PushRules;
import java.util.function.Predicate;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntitySelector.class)
abstract class EntitySelectorPushMixin {
    @Inject(method = "pushableBy(Lnet/minecraft/world/entity/Entity;)Ljava/util/function/Predicate;", at = @At("HEAD"), cancellable = true)
    private static void aervium$pushWithoutTeams(Entity pusher, CallbackInfoReturnable<Predicate<Entity>> callback) {
        if (PushRules.withoutTeams(pusher)) {
            callback.setReturnValue(PushRules.pushableWithoutTeams(pusher));
        }
    }
}
