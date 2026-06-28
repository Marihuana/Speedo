package kr.yooreka.speedo

import kr.yooreka.speedo.data.sensor.datasource.YawRateMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class YawRateMathTest {
    @Test
    fun `rotation about up axis returns full angular velocity`() {
        // 위 방향이 +z, 자이로가 z축 1 rad/s → yaw 요율 1 rad/s
        val yaw = YawRateMath.aboutUpAxis(0f, 0f, 1f, 0f, 0f, 9.81f)
        assertEquals(1f, yaw, 0.0001f)
    }

    @Test
    fun `rotation about horizontal axis contributes no yaw`() {
        // 위 방향이 +z 인데 자이로는 x축(roll) 회전 → yaw 성분 0
        val yaw = YawRateMath.aboutUpAxis(2f, 0f, 0f, 0f, 0f, 9.81f)
        assertEquals(0f, yaw, 0.0001f)
    }

    @Test
    fun `projection follows tilted gravity direction`() {
        // 위 방향이 +y(거치 회전), 자이로 y축 2 rad/s → yaw 2 rad/s
        val yaw = YawRateMath.aboutUpAxis(0f, 2f, 0f, 0f, 9.81f, 0f)
        assertEquals(2f, yaw, 0.0001f)
    }

    @Test
    fun `zero acceleration magnitude yields nan`() {
        val yaw = YawRateMath.aboutUpAxis(1f, 1f, 1f, 0f, 0f, 0f)
        assertTrue(yaw.isNaN())
    }
}
