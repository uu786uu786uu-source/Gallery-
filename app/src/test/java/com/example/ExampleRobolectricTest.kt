package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.GalleryRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Gallery", appName)
  }

  @Test
  fun `repository loads initial showcase photos and albums`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repository = GalleryRepository(context)
    val photos = repository.photos.value
    assertTrue("Showcase photos should be preloaded", photos.isNotEmpty())
    assertTrue("Should contain sample mountains", photos.any { it.id == "sample_mountains" })
  }
}
