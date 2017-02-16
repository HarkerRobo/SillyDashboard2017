from collections import namedtuple
import sys
import ctypes
import ctypes.wintypes

EVENT_SYSTEM_FOREGROUND = 0x0003
WINEVENT_OUTOFCONTEXT = 0x0000

user32 = ctypes.windll.user32
ole32 = ctypes.windll.ole32

user32.SetProcessDPIAware()
ole32.CoInitialize(0)

WinEventProcType = ctypes.WINFUNCTYPE(
    None,
    ctypes.wintypes.HANDLE,
    ctypes.wintypes.DWORD,
    ctypes.wintypes.HWND,
    ctypes.wintypes.LONG,
    ctypes.wintypes.LONG,
    ctypes.wintypes.DWORD,
    ctypes.wintypes.DWORD
)

def register_hook(callback):
    WinEventProc = WinEventProcType(callback)

    user32.SetWinEventHook.restype = ctypes.wintypes.HANDLE
    hook = user32.SetWinEventHook(
        EVENT_SYSTEM_FOREGROUND,
        EVENT_SYSTEM_FOREGROUND,
        0,
        WinEventProc,
        0,
        0,
        WINEVENT_OUTOFCONTEXT
    )
    if hook == 0:
        print('SetWinEventHook failed')
        sys.exit(1)

    msg = ctypes.wintypes.MSG()
    while user32.GetMessageW(ctypes.byref(msg), 0, 0, 0) != 0:
        user32.TranslateMessageW(msg)
        user32.DispatchMessageW(msg)

    user32.UnhookWinEvent(hook)
    ole32.CoUninitialize()

def title(hwnd):
    length = user32.GetWindowTextLengthA(hwnd)
    buff = ctypes.create_string_buffer(length + 1)
    user32.GetWindowTextA(hwnd, buff, length + 1)
    return buff.value

def size(hwnd):
    rect = ctypes.wintypes.RECT()
    user32.GetWindowRect(hwnd, ctypes.byref(rect))
    width = rect.right - rect.left
    height = rect.bottom - rect.top
    return (width, height)

def screenres():
    return (user32.GetSystemMetrics(0), user32.GetSystemMetrics(1))

Monitor = namedtuple('Monitor', ['x', 'y', 'w', 'h'])

def monitors():
    monitors = []

    def callback(monitor, dc, rect, data):
        rct = rect.contents
        monitors.append(Monitor(
            rct.left,
            rct.top,
            rct.right - rct.left,
            rct.bottom - rct.top))
        return 1

    MonitorEnumProc = ctypes.WINFUNCTYPE(
        ctypes.c_int,
        ctypes.c_ulong,
        ctypes.c_ulong,
        ctypes.POINTER(ctypes.wintypes.RECT),
        ctypes.c_double)

    ctypes.windll.user32.EnumDisplayMonitors(
        0, 0, MonitorEnumProc(callback), 0)

    return monitors
