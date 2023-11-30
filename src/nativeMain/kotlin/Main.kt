import kotlinx.cinterop.*
import platform.windows.*
import kotlin.system.exitProcess

fun main() = WindowCreator.createWindow("Native window for the Windows OS", 600, 400, false)

@OptIn(ExperimentalForeignApi::class)
object WindowCreator {
    private const val WINDOW_CLASS_NAME = "NativeWindowClass"

    fun createWindow(title: String, windowWidth: Int, windowHeight: Int, isResizable: Boolean) {
        val (centerPosX, centerPosY) = calculateCenterPosition(windowWidth, windowHeight)
        val instanceHandle = (GetModuleHandle!!)(null)

        memScoped {
            registerWindowClass(instanceHandle).run {
                if ((RegisterClassEx!!)(this.ptr) == 0u.toUShort()) {
                    displayErrorMessage("Failed to register window procedure.")
                    return
                }
            }

            createAndShowWindow(
                title,
                centerPosX,
                centerPosY,
                windowWidth,
                windowHeight,
                instanceHandle,
                if (!isResizable) (WS_OVERLAPPED or WS_CAPTION or WS_SYSMENU or WS_MINIMIZEBOX).toUInt() else WS_OVERLAPPEDWINDOW.toUInt()
            ) ?: displayErrorMessage("Window creation failed.")
        }
    }

    private fun calculateCenterPosition(width: Int, height: Int) = Pair(
        (GetSystemMetrics(SM_CXSCREEN) - width) / 2,
        (GetSystemMetrics(SM_CYSCREEN) - height) / 2
    )

    private fun registerWindowClass(instanceHandle: HINSTANCE?) = cValue<WNDCLASSEX> {
        memScoped {
            cbSize = sizeOf<WNDCLASSEX>().toUInt()
            lpfnWndProc = staticCFunction(::handleWindowMessage)
            cbClsExtra = 0
            cbWndExtra = 0
            hInstance = instanceHandle
            lpszMenuName = null
            hIconSm = null
            lpszClassName = WINDOW_CLASS_NAME.wcstr.ptr
        }
    }

    private fun createAndShowWindow(title: String, posX: Int, posY: Int, width: Int, height: Int, instanceHandle: HINSTANCE?, windowStyle: UInt): HWND? =
        CreateWindowExA(
            WS_EX_CLIENTEDGE.toUInt(), WINDOW_CLASS_NAME, title, windowStyle,
            posX, posY, width, height, null, null, instanceHandle, null
        )?.also { windowInstance ->
            ShowWindow(windowInstance, SW_SHOWDEFAULT)
            UpdateWindow(windowInstance)
            processWindowMessages(windowInstance)
        }

    private fun processWindowMessages(windowInstance: HWND) = memScoped {
        alloc<MSG>().run {
            while (GetMessageA(this.ptr, windowInstance, 0u, 0u) != 0) {
                TranslateMessage(this.ptr)
                DispatchMessageA(this.ptr)
            }
        }
    }

    private fun displayErrorMessage(message: String) = MessageBoxA(null, message, "Error!", (MB_ICONERROR or MB_OK).toUInt())
}

@OptIn(ExperimentalForeignApi::class)
private fun handleWindowMessage(windowHandle: HWND?, message: UINT, wParam: WPARAM, lParam: LPARAM): LRESULT {
    when (message.toInt()) {
        WM_CLOSE -> DestroyWindow(windowHandle)
        WM_DESTROY -> {
            PostQuitMessage(0)
            exitProcess(0)
        }
        WM_ERASEBKGND -> return 1
        WM_SIZE -> {
            InvalidateRect(windowHandle, null, TRUE)
            RedrawWindow(windowHandle, null, null, (RDW_INVALIDATE or RDW_UPDATENOW or RDW_ERASE).toUInt())
        }
        else -> return (DefWindowProc!!)(windowHandle, message, wParam, lParam)
    }
    return 0
}