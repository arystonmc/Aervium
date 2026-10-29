package com.aryston.kinetic.world.collision;

import java.lang.reflect.Method;
import net.minecraft.world.entity.Entity;

public final class CollidableEntities {
    private static final ClassValue<Boolean> OVERRIDES_CAN_BE_COLLIDED_WITH = overrides("canBeCollidedWith", Entity.class);
    private static final ClassValue<Boolean> OVERRIDES_CAN_COLLIDE_WITH = overrides("canCollideWith", Entity.class);

    private CollidableEntities() {
    }

    public static boolean mayBeCollidedWith(Object entity) {
        return entity instanceof Entity && OVERRIDES_CAN_BE_COLLIDED_WITH.get(entity.getClass());
    }

    public static boolean defaultSource(Entity source) {
        return !OVERRIDES_CAN_COLLIDE_WITH.get(source.getClass());
    }

    private static ClassValue<Boolean> overrides(String name, Class<?>... parameters) {
        return new ClassValue<>() {
            @Override
            protected Boolean computeValue(Class<?> type) {
                for (Class<?> current = type; current != null && current != Entity.class; current = current.getSuperclass()) {
                    for (Method method : current.getDeclaredMethods()) {
                        if (method.getName().equals(name) && method.getParameterCount() == parameters.length) {
                            return true;
                        }
                    }
                }
                return false;
            }
        };
    }

    public interface Counter {
        int kinetic$collidable();
    }
}
