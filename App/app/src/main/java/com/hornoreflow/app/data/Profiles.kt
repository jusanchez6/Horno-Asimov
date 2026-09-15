package com.hornoreflow.app.data

/**
 * Curvas de referencia aproximadas (no certificadas contra IPC/J-STD-020),
 * suficientes para desarrollar y probar la app y el firmware antes de
 * calibrar contra el horno fisico real.
 */
object Profiles {

    val LEADED = SolderProfile(
        id = 1,
        name = "Con plomo (Sn63/Pb37)",
        segments = listOf(
            ProfileSegment(OvenState.PREHEAT, 0, 90, 25f, 150f),
            ProfileSegment(OvenState.SOAK, 90, 180, 150f, 180f),
            ProfileSegment(OvenState.REFLOW, 180, 225, 180f, 225f),
            ProfileSegment(OvenState.COOLING, 225, 300, 225f, 50f)
        )
    )

    val LEAD_FREE = SolderProfile(
        id = 2,
        name = "Sin plomo (SAC305)",
        segments = listOf(
            ProfileSegment(OvenState.PREHEAT, 0, 90, 25f, 150f),
            ProfileSegment(OvenState.SOAK, 90, 180, 150f, 200f),
            ProfileSegment(OvenState.REFLOW, 180, 230, 200f, 245f),
            ProfileSegment(OvenState.COOLING, 230, 320, 245f, 50f)
        )
    )

    val ALL = listOf(LEADED, LEAD_FREE)
}
