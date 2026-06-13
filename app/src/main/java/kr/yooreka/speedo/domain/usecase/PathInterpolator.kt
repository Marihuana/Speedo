package kr.yooreka.speedo.domain.usecase

import kr.yooreka.speedo.domain.model.RideTelemetry

/**
 * 위치가 비어 있는(시간주기, F-13b) 텔레메트리 행의 좌표를 채우는 보간 전략(Strategy).
 *
 * GPS 픽스 시점 행만 실좌표를 가지므로, 그 사이 [latitude]/[longitude] 가 null 인 행들을
 * 타임스탬프 비례로 채운다. 선형([LinearPathInterpolator]) 기본, 미관용 Catmull-Rom 등으로 교체 가능.
 */
interface PathInterpolator {
    /**
     * @param points timestamp 오름차순 정렬을 가정한 텔레메트리 목록(DAO가 ORDER BY timestamp ASC 보장).
     * @return 좌표(null)만 보간으로 채우고 그 외 필드(speed/roll/brake 등)는 보존한 동일 길이 목록.
     */
    fun interpolate(points: List<RideTelemetry>): List<RideTelemetry>
}
