import winapi_helpers as h

MONITOR = h.monitors()[-1]
print(MONITOR)

STREAMS = (DRIVER, VISION) = range(2)

TOLERANCE = 0

EXPECTED_WIDTHS = {
    DRIVER: 640,
    VISION: 928
}

DESIRED_SIZES = {
    DRIVER: (480, 360),
    VISION: (800, 600)
}

POSITIONS = {
    DRIVER: lambda w: (MONITOR.x + MONITOR.w - w, MONITOR.y),
    VISION: lambda w: (MONITOR.x, MONITOR.y)
}

handled = []
detected_streams = [s for s in STREAMS]

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
    for s in detected_streams:
        if abs(EXPECTED_WIDTHS[s] - clientw) <= TOLERANCE:
            stream = s

    if stream is None:
        print('Window with width {} not handled'.format(clientw))
        return

    detected_streams.remove(stream)

    # Reposition and possibly resize the window
    vpad = height - clienth
    hpad = width - clientw

    newwidth = DESIRED_SIZES[stream][0] + hpad
    newheight = DESIRED_SIZES[stream][1] + vpad

    x, y = POSITIONS[stream](newwidth)
    h.user32.MoveWindow(hwnd, x, y, newwidth, newheight, True)

h.register_hook(callback)
