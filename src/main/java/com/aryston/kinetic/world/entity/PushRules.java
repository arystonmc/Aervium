package com.aryston.kinetic.world.entity;

import java.util.function.Predicate;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public final class PushRules {
    private PushRules() {
    }

    public static boolean withoutTeams(Entity pusher) {
        return pusher.level().getScoreboard().getPlayerTeams().isEmpty();
    }

    public static Predicate<Entity> pushableWithoutTeams(Entity pusher) {
        boolean clientSide = pusher.level().isClientSide();
        return input -> !input.isSpectator() && input.isPushable() && (!clientSide || input instanceof Player player && player.isLocalPlayer());
    }
}
