package com.aryston.aervium.world.collision;

import java.lang.reflect.Method;
import net.minecraft.world.entity.Entity;

/**
 * Which entity classes another entity can bump into. Vanilla only boats, shulkers and happy ghasts say yes in
 * {@code canBeCollidedWith}; a class counts when it or a parent below {@link Entity} overrides that method.
 */
public final class CollidableEntities {
    private static final ClassValue<Boolean> OVERRIDES_CAN_BE_COLLIDED_WITH = overrides("canBeCollidedWith", Entity.class);
    private static final ClassValue<Boolean> OVERRIDES_CAN_COLLIDE_WITH = overrides("canCollideWith", Entity.class);

    private CollidableEntities() {
    }

    public static boolean mayBeCollidedWith(Object entity) {
        return entity instanceof Entity && OVERRIDES_CAN_BE_COLLIDED_WITH.get(entity.getClass());
    }

    /** True when the source uses the default rule, so only entities that may be collided with can stop it. */
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

    /** Implemented by entity sections: how many entities in the section may be collided with. */
    public interface Counter {
        int aervium$collidable();
    }
}
