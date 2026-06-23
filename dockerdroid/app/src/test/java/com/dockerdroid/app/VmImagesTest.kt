package com.dockerdroid.app

import com.dockerdroid.app.vm.VmImages
import com.dockerdroid.app.vm.VmState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VmImagesTest {

    @Test
    fun `default guest declares a kernel and rootfs`() {
        val assets = VmImages.defaultArm64()
        val names = assets.map { it.name }.toSet()
        assertTrue("kernel" in names)
        assertTrue("rootfs" in names)
        assertTrue("rootfs is gzipped", assets.first { it.name == "rootfs" }.gzipped)
        assertEquals(2375, VmImages.DOCKER_PORT)
    }

    @Test
    fun `vm state active flag`() {
        assertTrue(VmState.Booting.isActive)
        assertTrue(VmState.Running.isActive)
        assertTrue(VmState.Provisioning("x").isActive)
        assertFalse(VmState.Stopped.isActive)
        assertFalse(VmState.Error("x").isActive)
    }
}
