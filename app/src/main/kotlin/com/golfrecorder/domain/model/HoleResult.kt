package com.golfrecorder.domain.model

data class HoleResult(
    val holeNumber: Int,
    val par: Int,
    val strokesToGreen: Int,
    val strokesGreenToHoleOut: Int,
    /** strokesGreenToHoleOut 중 퍼팅(그린 위) 타수만 따로 — 숏어프로치는
     * strokesGreenToHoleOut - strokesPutt로 구한다. */
    val strokesPutt: Int = 0,
) {
    val totalStrokes: Int get() = strokesToGreen + strokesGreenToHoleOut
    val scoreToPar: Int get() = totalStrokes - par
    val isGreenInRegulation: Boolean get() = strokesToGreen <= par - 2
    val strokesShortGame: Int get() = strokesGreenToHoleOut - strokesPutt

    /** 일반 GIR은 스코어(strokesToGreen)만으로 판정해서, 그 뒤에 숏어프로치가
     * 있었어도(실제로는 그린에 못 올랐다는 뜻) GIR 성공으로 집계될 수 있다.
     * 엄격 GIR은 숏어프로치가 전혀 없었던 경우만 인정한다. */
    val isStrictGreenInRegulation: Boolean get() = isGreenInRegulation && strokesShortGame == 0
}
