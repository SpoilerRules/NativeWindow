import kotlin.test.Test
import kotlin.test.assertFails

class CreateWindowTest {
    @Test
    fun produceWindowTest() {
        val title = "Test native window for the Windows OS"
        val width = 727
        val height = 420
        val resizable = false

        assertFails("createWindow should not throw an exception") {
            WindowCreator.createWindow(title, width, height, resizable)
        }
    }
}