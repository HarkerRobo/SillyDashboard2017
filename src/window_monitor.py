import winapi_helpers as h

MONITOR = h.monitors()[-1]
print(MONITOR)

STREAMS = (DRIVER, VISION) = range(2)

TOLERANCE = 0

WIDTHS = {
    DRIVER: 970, #1296, # The laptop's screen resolution is limitted, so the window will be limited to 970 px across.
    VISION: 640
}

POSITIONS = {
    DRIVER: lambda w: (MONITOR.x + MONITOR.w - w, MONITOR.y),
    VISION: lambda w: (MONITOR.x, MONITOR.y)
}

handled = []

def callback(hWinEventHook, event, hwnd, idObject, idChild, dwEventThread, dwmsEventTime):
    title = h.title(hwnd)
    if title != b'GStreamer D3D video sink (internal window)':
        return

    if hwnd in handled:
        return # Don't reposition again

    handled.append(hwnd) #Append the window id
    width, height = h.windowsize(hwnd)

    clientw, _ = h.clientsize(hwnd)

    # Find which stream the window is for
    stream = None
    for s in WIDTHS:
        if abs(WIDTHS[s] - clientw) <= TOLERANCE:
            stream = s

    if stream is None:
        print('Window with width {} not handled'.format(clientw))
        return

    # Reposition the window
    x, y = POSITIONS[stream](width)
    h.user32.MoveWindow(hwnd, x, y, width, height, True)

h.register_hook(callback)
