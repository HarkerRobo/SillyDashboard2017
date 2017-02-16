import winapi_helpers as h

MONITOR = h.monitors()[-1]
print(MONITOR)

STREAMS = (DRIVER, VISION) = range(2)

TOLERANCE = 0

EXPECTED_WIDTHS = {
    DRIVER: 970, # The laptop's screen resolution is limitted, so the window will be limited to 970 px across.
    VISION: 640
}

DESIRED_SIZES = {
    DRIVER: (1296, 972),
    VISION: (640, 480)
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

    clientw, clienth = h.clientsize(hwnd)

    # Find which stream the window is for
    stream = None
    for s in EXPECTED_WIDTHS:
        if abs(EXPECTED_WIDTHS[s] - clientw) <= TOLERANCE:
            stream = s

    if stream is None:
        print('Window with width {} not handled'.format(clientw))
        return

    # Reposition and possibly resize the window
    vpad = height - clienth
    hpad = width - clientw

    w = DESIRED_SIZES[stream][0] + hpad
    h = DESIRED_SIZES[stream][1] + vpad

    x, y = POSITIONS[stream](w)
    h.user32.MoveWindow(hwnd, x, y, w, h, True)

h.register_hook(callback)
