package com.aryston.kinetic.client.benchmark;

import com.aryston.kinetic.Kinetic;
import net.minecraft.commands.CommandSource;
import net.minecraft.network.chat.Component;

final class CommandFailureLog implements CommandSource {
    static final CommandFailureLog INSTANCE = new CommandFailureLog();

    private CommandFailureLog() {
    }

    @Override
    public void sendSystemMessage(Component message) {
        Kinetic.LOGGER.warn("Kinetic benchmark command failed: {}", message.getString());
    }

    @Override
    public boolean acceptsSuccess() {
        return false;
    }

    @Override
    public boolean acceptsFailure() {
        return true;
    }

    @Override
    public boolean shouldInformAdmins() {
        return false;
    }
}
