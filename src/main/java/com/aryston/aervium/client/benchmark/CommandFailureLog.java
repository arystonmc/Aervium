package com.aryston.aervium.client.benchmark;

import com.aryston.aervium.Aervium;
import net.minecraft.commands.CommandSource;
import net.minecraft.network.chat.Component;

final class CommandFailureLog implements CommandSource {
    static final CommandFailureLog INSTANCE = new CommandFailureLog();

    private CommandFailureLog() {
    }

    @Override
    public void sendSystemMessage(Component message) {
        Aervium.LOGGER.warn("Aervium benchmark command failed: {}", message.getString());
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
