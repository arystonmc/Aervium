package com.aryston.kinetic.core.parity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

class StateHashTest {
    @Test
    void sameInputGivesTheSameHash() {
        assertEquals(new StateHash().add(7).add(9L).add("stone").value(), new StateHash().add(7).add(9L).add("stone").value());
    }

    @Test
    void orderAndEveryValueMatter() {
        long base = new StateHash().add(1).add(2).value();
        assertNotEquals(base, new StateHash().add(2).add(1).value());
        assertNotEquals(base, new StateHash().add(1).add(3).value());
        assertNotEquals(new StateHash().add("ab").add("c").value(), new StateHash().add("a").add("bc").value());
    }

    @Test
    void emptyHashIsTheFnvOffsetBasis() {
        assertEquals("cbf29ce484222325", new StateHash().hex());
    }
}
