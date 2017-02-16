import winapi_helpers as h

MONITOR = h.monitors()[0]
print(MONITOR)

STREAMS = (DRIVER, VISION) = range(2)

TOLERANCE = 100

WIDTHS = {
    DRIVER: 1296,
    VISION: 640
}

POSITIONS = {
    DRIVER: lambda w: (MONITOR.x + MONITOR.w - w, MONITOR.y),
    VISION: lambda w: (MONITOR.x + MONITOR.w - w, MONITOR.y)
}

handled = []

def callback(hWinEventHook, event, hwnd, idObject, idChild, dwEventThread, dwmsEventTime):
    title = h.title(hwnd)
    if title != b'GStreamer D3D video sink (internal window)':
        return

    if hwnd in handled:
        return # Don't reposition again

    handled.append(hwnd) #Append the window id
    width, height = h.size(hwnd)
    print(width, height)

    # Find which stream the window is for
    stream = None
    for s in WIDTHS:
        if abs(WIDTHS[s] - width) < TOLERANCE:
            stream = s

    if stream is None:
        print('Window with width {} not handled'.format(width))
        return

    x, y = POSITIONS[stream](width)
    h.user32.MoveWindow(hwnd, x, y, width, height, True)

h.register_hook(callback)
