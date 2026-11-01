package com.kclog;

import static org.junit.Assert.*;
import org.junit.Test;

public class BossEncounterFilterTest
{
    @Test public void ignoresOlmHands()
    {
        assertFalse(BossEncounterFilter.shouldCount(7552, "Great Olm (Left claw)"));
        assertFalse(BossEncounterFilter.shouldCount(7555, "Great Olm (Left claw)"));
        assertFalse(BossEncounterFilter.shouldCount(7550, "Great Olm (Right claw)"));
        assertFalse(BossEncounterFilter.shouldCount(7553, "Great Olm (Right claw)"));
    }

    @Test public void countsOlmHeadAsOneBoss()
    {
        assertTrue(BossEncounterFilter.shouldCount(7551, "Great Olm"));
        assertTrue(BossEncounterFilter.shouldCount(7554, "Great Olm"));
        assertEquals("Great Olm", BossEncounterFilter.displayName(7554, "Great Olm"));
    }

    @Test public void onlyCountsFinalVerzikPhase()
    {
        assertFalse(BossEncounterFilter.shouldCount(10830, "Verzik Vitur"));
        assertFalse(BossEncounterFilter.shouldCount(10831, "Verzik Vitur"));
        assertTrue(BossEncounterFilter.shouldCount(10832, "Verzik Vitur"));
        assertEquals("Verzik Vitur", BossEncounterFilter.displayName(10832, "Verzik Vitur"));
    }

    @Test public void leavesOrdinaryMonstersAlone()
    {
        assertTrue(BossEncounterFilter.shouldCount(1, "Goblin"));
        assertEquals("Goblin", BossEncounterFilter.displayName(1, "Goblin"));
    }
}
