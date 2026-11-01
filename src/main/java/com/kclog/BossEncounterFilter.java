package com.kclog;

/**
 * Filters NPCs that are parts or intermediate forms of a multi-phase boss.
 * Keep this class independent of RuneLite so its rules can be unit tested.
 */
final class BossEncounterFilter
{
    private static final int[] OLM_HAND_IDS = {7550, 7552, 7553, 7555};
    private static final int[] VERZIK_INTERMEDIATE_IDS = {10830, 10831, 10833, 10834};
    private static final int[] VERZIK_FINAL_IDS = {10832, 10835, 10836};

    private BossEncounterFilter()
    {
    }

    /**
     * Returns true only for an NPC death that should produce a kill-count entry.
     * Olm's head represents the encounter; its two hands are ignored.
     * Verzik is counted only when her final phase is defeated.
     */
    static boolean shouldCount(int npcId, String npcName)
    {
        if (contains(OLM_HAND_IDS, npcId))
        {
            return false;
        }

        if (contains(VERZIK_INTERMEDIATE_IDS, npcId))
        {
            return false;
        }

        if (contains(VERZIK_FINAL_IDS, npcId))
        {
            return true;
        }

        // Name fallback for Olm hands in case IDs change or a variant is added.
        if (npcName != null)
        {
            String normalized = npcName.toLowerCase();
            if (normalized.contains("great olm") &&
                (normalized.contains("left claw") || normalized.contains("right claw")))
            {
                return false;
            }
        }

        return true;
    }

    /** Use one stable display name for the Great Olm encounter. */
    static String displayName(int npcId, String npcName)
    {
        if (isOlm(npcId, npcName))
        {
            return "Great Olm";
        }
        if (isVerzik(npcId, npcName))
        {
            return "Verzik Vitur";
        }
        return npcName;
    }

    private static boolean isOlm(int npcId, String npcName)
    {
        return npcId == 7551 || npcId == 7554 ||
            (npcName != null && npcName.toLowerCase().contains("great olm"));
    }

    private static boolean isVerzik(int npcId, String npcName)
    {
        return contains(VERZIK_INTERMEDIATE_IDS, npcId) ||
            contains(VERZIK_FINAL_IDS, npcId) ||
            (npcName != null && npcName.toLowerCase().contains("verzik vitur"));
    }

    private static boolean contains(int[] values, int value)
    {
        for (int candidate : values)
        {
            if (candidate == value)
            {
                return true;
            }
        }
        return false;
    }
}
