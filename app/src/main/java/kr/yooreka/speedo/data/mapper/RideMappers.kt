package kr.yooreka.speedo.data.mapper

import kr.yooreka.speedo.data.local.entity.RideEntity
import kr.yooreka.speedo.data.local.entity.TelemetryEntity
import kr.yooreka.speedo.domain.model.Ride
import kr.yooreka.speedo.domain.model.RideTelemetry

/** Room 영속 타입 ↔ 도메인 모델 매핑. 데이터 레이어 경계에서만 사용한다. */

fun RideEntity.toDomain(): Ride =
    Ride(
        id = id,
        title = title,
        startTime = startTime,
        endTime = endTime,
        totalDistance = totalDistance,
        maxLean = maxLean,
        maxSpeed = maxSpeed,
        duration = duration,
    )

fun TelemetryEntity.toDomain(): RideTelemetry =
    RideTelemetry(
        id = id,
        rideId = rideId,
        timestamp = timestamp,
        speed = speed,
        roll = roll,
        brakeEvent = brakeEvent,
        brakeForce = brakeForce,
        latitude = latitude,
        longitude = longitude,
    )
