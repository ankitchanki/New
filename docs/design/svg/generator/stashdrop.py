"""Renders every StashDrop screen, sheet, dialog and component as SVG (390×844 dp artboards)."""
import sys
from svgkit import Canvas, icon, write, board, text_w, FONT, MONO

OUT = sys.argv[1]
W, H = 390, 844

# Tokens from ui/theme/Theme.kt
BG, SURF, SURF2, PRIC = "#0C0E15", "#171B26", "#242A39", "#283249"
ICE, MINT, TEXT, MUTED, ONPRI = "#B4C9FF", "#9DE3CF", "#F1F3FA", "#A4AABE", "#15223E"
ERRC, ONERRC, RED = "#8C1D18", "#F9DEDC", "#E5484D"

ITEMS = [
    dict(kind="image", coll="Photos", text=None),
    dict(kind="sticker", coll="Stickers", text=None),
    dict(kind="text", coll="Inbox", text="Good ideas deserve a place to land."),
    dict(kind="link", coll="Reading", text="https://example.com/articles/design-systems-that-scale"),
    dict(kind="otp", coll="Inbox", text="Your verification code is 482913"),
]


# ---------- chrome ----------
def phone(uid, draw, bg=BG, status_fg=TEXT):
    c = Canvas(W, H, uid)
    clip = c.clip_rect(0, 0, W, H, 36)
    c.begin(clip)
    c.rect(0, 0, W, H, 0, bg)
    draw(c)
    status(c, status_fg)
    c.rect(W / 2 - 54, H - 10, 108, 4, 2, status_fg, opacity=.55)
    c.end()
    c.rect(.5, .5, W - 1, H - 1, 36, "none", "#2A2F3D", 1)
    return c


def status(c, fg):
    c.text(28, 25, "9:41", 14, fg, 600)
    icon(c, "signal", 300, 12, 15, fg, fill=True)
    icon(c, "wifi", 320, 11, 16, fg, 1.8)
    c.rect(342, 15, 20, 10, 3, "none", fg, 1.2)
    c.rect(344, 17, 13, 6, 1.5, fg)


def bottom_nav(c, tab):
    y = 760
    c.rect(24, y, 342, 72, 36, SURF2, extra=c.filter_shadow(6, 16, .5))
    for i, (label, ic) in enumerate([("Home", "home"), ("Shelf", "layers"), ("Settings", "tune")]):
        cx = 24 + 342 / 6 * (2 * i + 1)
        on = i == tab
        if on:
            c.rect(cx - 32, y + 10, 64, 32, 16, PRIC)
        icon(c, ic, cx - 12, y + 14, 24, ICE if on else MUTED)
        c.text(cx, y + 60, label, 12, TEXT if on else MUTED, 600 if on else 500, "middle")


def header(c, label, pill=None, logo=False, back=False):
    y = 66
    x = 22
    if back:
        icon(c, "back", 22, y - 12, 24, TEXT)
        x = 62
    if logo:
        stash_logo(c, 22, y - 20, 40)
        x = 70
    c.text(x, y + 4, label, 11, TEXT, 700, ls=2)
    if pill:
        txt, on = pill
        w = text_w(txt, 11) + 26
        c.rect(368 - w, y - 15, w, 30, 15, MINT if on else ICE, opacity=.12)
        c.text(368 - w / 2, y + 4, txt, 11, MINT if on else ICE, 500, "middle")


def stash_logo(c, x, y, s):
    k = s / 40
    for i, col in enumerate([ICE, MINT, ICE]):
        yy = y + (12 + i * 7) * k
        c.path(f"M{x + 8 * k:g} {yy:g} L{x + 20 * k:g} {yy + 6 * k:g} L{x + 32 * k:g} {yy:g}", "none", col, 3.4 * k)
    c.path(f"M{x + 8 * k:g} {y + 12 * k:g} L{x + 20 * k:g} {y + 6 * k:g} L{x + 32 * k:g} {y + 12 * k:g}", "none", ICE, 3.4 * k)


def headline(c, y, title, sub=None):
    c.text(22, y, title, 28, TEXT, 700, ls=-.6)
    if sub:
        c.text(22, y + 26, sub, 14, MUTED)


def section(c, y, title, sub, trailing=None):
    c.text(22, y, title, 16, TEXT, 500)
    c.text(22 + text_w(title, 16) + 6, y, f" ·  {sub}", 12, MUTED, 500)
    if trailing:
        c.text(356, y, trailing, 14, ICE, 500, "end")


def card(c, y, h, x=22, w=346, fill=SURF, r=20, stroke=None):
    c.rect(x, y, w, h, r, fill, stroke)


def action_row(c, y, title, sub, ic):
    card(c, y, 76)
    icon(c, ic, 38, y + 25, 26, ICE)
    c.text(80, y + 33, title, 14, TEXT, 500)
    c.text(80, y + 52, sub, 12, MUTED)
    icon(c, "chev", 334, y + 28, 20, ICE)


def setup_row(c, y, ic, title, done, action, status=None):
    card(c, y, 68)
    icon(c, ic, 38, y + 23, 22, MINT if done else ICE)
    c.text(72, y + 30, title, 14, TEXT)
    c.text(72, y + 48, status or ("On" if done else "Not enabled"), 12, MINT if done else MUTED)
    if done:
        icon(c, "check_circle", 330, y + 22, 24, MINT)
    else:
        c.text(344, y + 39, action, 14, ICE, 500, "end")


def switch(c, x, y, on):
    c.rect(x, y, 52, 32, 16, ICE if on else SURF2, None if on else "#8E9099", 2)
    if on:
        c.circle(x + 36, y + 16, 12, ONPRI)
        c.path(f"M{x + 30} {y + 16} l4 4 l8 -8", "none", ICE, 2)
    else:
        c.circle(x + 16, y + 16, 8, "#8E9099")


def setting_switch(c, y, title, sub, on):
    card(c, y, 76)
    c.text(38, y + 32, title, 14, TEXT)
    c.para(38, y + 51, sub, 240, 12, MUTED)
    switch(c, 302, y + 22, on)


# ---------- content pieces ----------
def thumb(c, x, y, s, kind, r=12):
    clip = c.clip_rect(x, y, s, s, r)
    c.begin(clip)
    if kind == "image":
        c.rect(x, y, s, s, 0, "#A3CEEE")
        c.circle(x + s * .88, y + s * .25, s * .13, "#FBD27C")
        c.path(f"M{x - 2:g} {y + s:g} L{x + s * .38:g} {y + s * .38:g} L{x + s * .78:g} {y + s:g}Z", "#45786E")
        c.add(f'<ellipse cx="{x + s * .85:g}" cy="{y + s * 1.05:g}" rx="{s * .6:g}" ry="{s * .35:g}" fill="#5E9876"/>')
    elif kind == "sticker":
        c.rect(x, y, s, s, 0, "#302A4A")
        c.circle(x + s / 2, y + s * .62, s * .42, "#EDB0DC")
        c.circle(x + s * .38, y + s * .55, s * .05, "#2A2440")
        c.circle(x + s * .62, y + s * .55, s * .05, "#2A2440")
        c.path(f"M{x + s * .3:g} {y + s * .7:g} Q{x + s / 2:g} {y + s * .88:g} {x + s * .7:g} {y + s * .7:g}", "none", "#2A2440", s * .04)
    else:
        c.rect(x, y, s, s, 0, SURF2)
        ic = {"text": "doc", "link": "link", "otp": "password", "file": "file", "video": "video"}[kind]
        icon(c, ic, x + s / 2 - 12, y + s / 2 - 12, 24, ICE)
    c.end()


def stash_row(c, y, item, chosen=False, swipe=0):
    media = item["kind"] in ("image", "sticker")
    h = 104 if media else 70
    c.begin(c.clip_rect(22, y, 346, h, 20))
    if swipe:
        c.rect(22, y, 346, h, 20, ERRC)
        icon(c, "delete", 282, y + h / 2 - 11, 22, ONERRC)
        c.text(352, y + h / 2 + 5, "Delete", 12, ONERRC, 500, "end")
    x0 = 22 - swipe
    c.rect(x0, y, 346, h, 20, "#1F2840" if chosen else SURF, ICE if chosen else None, 1)
    s = 76 if media else 42
    thumb(c, x0 + 14, y + (h - s) / 2, s, item["kind"])
    tx = x0 + 14 + s + 12
    ty = y + h / 2 + 4
    lines = []
    if item["text"]:
        lines.append(("t", item["text"]))
    if item["coll"] not in ("Inbox", "Images"):
        lines.append(("c", item["coll"]))
    if len(lines) == 2:
        ty -= 9
    for i, (k, s_) in enumerate(lines):
        if k == "t":
            ny = c.para(tx, ty + i * 18, s_, 230 - (s - 42), 14, TEXT, 18, max_lines=1)
        else:
            c.text(tx, ty + i * 18, s_, 12, ICE, 500)
    icon(c, "folder", x0 + 312, y + h / 2 - 9, 18, ICE)
    c.end()
    return h


def bubble(c, cx, cy, count=0, absorbing=False):
    size = 68 if absorbing else 54
    if absorbing:
        c.circle(cx, cy, size / 2 + 14, c.radial([(0, ICE, .45), (.6, MINT, .18), (1, MINT, 0)]))
    fill = c.linear([(0, "#2C3754", None), (1, "#151A27", None)])
    ring = c.linear([(0, "#FFFFFF", .55), (.5, ICE, .12), (1, MINT, .4)])
    c.circle(cx, cy, size / 2, fill, extra=c.filter_shadow(5, 10, .5))
    c.circle(cx, cy, size / 2 - .5, "none", ring, 1)
    if absorbing:
        c.circle(cx - 8, cy + 4, 14, MINT, opacity=.35)
        c.circle(cx + 9, cy - 5, 11, ICE, opacity=.35)
    icon(c, "layers", cx - 12, cy - 12, 24, ICE)
    if count:
        bx, by = cx + 20, cy - 20
        c.circle(bx, by, 11, MINT, "#151A27", 2)
        c.text(bx, by + 4, str(count), 11, BG, 600, "middle")


def playground(c, y, count, progress=0.0):
    card(c, y, 210, r=26)
    clip = c.clip_rect(22, y, 346, 210, 26)
    c.begin(clip)
    if progress:
        c.add(f'<ellipse cx="195" cy="{y + 210:g}" rx="220" ry="{90 + 80 * progress:g}" fill="{c.radial([(0, ICE, .35), (.5, "#9B72CB", .22), (1, MINT, 0)])}"/>')
    c.end()
    bubble(c, 195, y + 92, count, progress > .5)
    c.text(195, y + 152, "A little space. For everything.", 12, MUTED, 500, "middle")
    c.rect(173, y + 194, 44, 4, 2, "#FFFFFF", opacity=.28)


def primary_btn(c, y, label, ic="layers", x=22, w=346, h=52):
    c.rect(x, y, w, h, 16, ICE)
    tw = text_w(label, 14) + 30
    icon(c, ic, x + w / 2 - tw / 2, y + h / 2 - 10, 20, ONPRI)
    c.text(x + w / 2 - tw / 2 + 30, y + h / 2 + 5, label, 14, ONPRI, 500)


def scrim(c, a=.55):
    c.rect(0, 0, W, H, 0, "#000", opacity=a)


def dialog(c, title, body, buttons, extra=None, top=None):
    """Material 3 AlertDialog centered on screen."""
    bw = 312
    x = (W - bw) / 2
    tl = c.para  # measure: draw later
    from svgkit import wrap
    tlines = wrap(title, bw - 48, 24)
    blines = wrap(body, bw - 48, 14) if body else []
    eh = extra[0] if extra else 0
    h = 24 + len(tlines) * 32 + 16 + len(blines) * 20 + (eh + 8 if extra else 0) + 24 + 40 + 24
    y = top if top is not None else (H - h) / 2
    c.rect(x, y, bw, h, 28, "#1D2130", extra=c.filter_shadow(10, 24, .55))
    yy = y + 24 + 24
    for ln in tlines:
        c.text(x + 24, yy, ln, 24, TEXT)
        yy += 32
    yy += 4
    for ln in blines:
        c.text(x + 24, yy, ln, 14, MUTED)
        yy += 20
    if extra:
        extra[1](c, x + 24, yy - 6, bw - 48)
        yy += eh + 8
    bx = x + bw - 24
    for label, enabled in reversed(buttons):
        tw = text_w(label, 14, .55) + 24
        c.text(bx - tw / 2, y + h - 30, label, 14, ICE if enabled else MUTED, 500, "middle", opacity=1 if enabled else .5)
        bx -= tw + 8
    return y, h


def text_field(c, x, y, w, label, value="", h=56, focused=True, support=None, lines=1):
    col = ICE if focused else "#8E9099"
    c.rect(x, y, w, h, 4, "none", col, 2 if focused else 1)
    lw = text_w(label, 12) + 8
    c.rect(x + 12, y - 2, lw, 4, 0, "#1D2130")
    c.text(x + 16, y + 4, label, 12, col)
    if value:
        for i, ln in enumerate(value.split("\n")):
            c.text(x + 16, y + 30 + i * 20, ln, 16, TEXT)
        c.rect(x + 16 + text_w(value.split("\n")[-1], 16), y + 16 + 20 * (len(value.split(chr(10))) - 1), 2, 20, 0, ICE)
    else:
        c.rect(x + 16, y + 18, 2, 20, 0, ICE)
    if support:
        c.text(x + 16, y + h + 16, support, 12, MUTED)


# ---------- screens ----------
def home(c, running=False, empty=False):
    header(c, "StashDrop", ("Edge shelf on" if running else "Edge shelf off", running), logo=True)
    c.text(22, 124, "Everything within reach.", 28, TEXT, 700, ls=-.4)
    playground(c, 140, 0 if empty else len(ITEMS), .65 if running else 0)
    primary_btn(c, 356, "Hide edge shelf" if running else "Show edge shelf")
    action_row(c, 420, "Circle to Search", "Google only · eligibility varies", "crop")
    section(c, 530, "Recent", f"{0 if empty else len(ITEMS)} saved", "See all")
    if empty:
        card(c, 550, 128)
        c.text(38, 584, "Nothing saved yet", 16, TEXT, 500)
        c.text(38, 608, "Share into StashDrop or add your first note.", 14, MUTED)
        c.rect(38, 626, 104, 40, 20, SURF2)
        c.text(90, 651, "Add note", 14, ICE, 500, "middle")
    else:
        y = 550
        for it in ITEMS[:2]:
            y += stash_row(c, y, it) + 12
    c.rect(0, 740, W, 104, 0, c.linear([(0, BG, 0), (.3, BG, 1), (1, BG, 1)], 0, 0, 0, 1))
    bottom_nav(c, 0)


def shelf(c, selected=(), swipe_index=None, filt="All", coll="All", empty=False):
    header(c, "YOUR SPACE")
    c.text(22, 118, "Your shelf.", 28, TEXT, 700, ls=-.4)
    c.rect(276, 92, 92, 40, 16, ICE)
    icon(c, "add", 290, 103, 18, ONPRI, 2)
    c.text(314, 117, "Note", 14, ONPRI, 500)
    n = 0 if empty else len(ITEMS)
    c.text(22, 152, f"{n} saved · tap cards to select", 14, MUTED)
    x = 22
    for name in ["All", "Photos", "Stickers", "Reading"] if not empty else ["All"]:
        w = text_w(name, 14) + 32
        on = name == coll
        c.rect(x, 168, w, 32, 16, "#25304A" if on else SURF)
        c.text(x + w / 2, 189, name, 14, ICE if on else MUTED, 500, "middle")
        x += w + 8
    type_chips(c, 210, filt)
    y = 256
    if empty:
        c.text(22, 290, "Share an item or add a note to get started.", 14, MUTED)
        y = 320
    else:
        for i, it in enumerate(ITEMS):
            if y > 700:
                break
            y += stash_row(c, y, it, i in selected, 90 if i == swipe_index else 0) + 12
    if empty:
        c.para(22, y, "Share text, images or files to StashDrop from another app's Share menu.", 340, 12, MUTED)
    c.rect(0, 690 if selected else 740, W, 160, 0, c.linear([(0, BG, 0), (.25, BG, 1), (1, BG, 1)], 0, 0, 0, 1))
    if selected:
        selection_bar(c, 692, len(selected))
    bottom_nav(c, 1)


def type_chips(c, y, filt, x=22, maxx=368, surf=None):
    for name in ["All", "Images", "Videos", "Stickers", "URLs", "OTP", "Text", "Files"]:
        w = text_w(name, 14) + 32
        if x > maxx:
            break
        on = name == filt
        if on:
            c.rect(x, y, w + 18, 32, 16, "#2E3A57")
            c.path(f"M{x + 12} {y + 16} l3.5 3.5 l7 -7", "none", ICE, 1.8)
            c.text(x + 30 + (w - 32) / 2 + 2, y + 21, name, 14, ICE, 500, "middle")
            x += w + 26
        else:
            c.rect(x, y, w, 32, 16, "none", "#8E9099", 1)
            c.text(x + w / 2, y + 21, name, 14, MUTED, 500, "middle")
            x += w + 8
    c.rect(maxx - 20, y - 2, 30, 36, 0, c.linear([(0, surf or BG, 0), (1, surf or BG, 1)], 0, 0, 1, 0))


def selection_bar(c, y, n, menu=False):
    c.rect(16, y, 358, 56, 20, SURF2)
    icon(c, "close", 32, y + 16, 24, TEXT)
    c.text(76, y + 33, f"{n} selected", 12, TEXT, 500)
    icon(c, "share", 236, y + 16, 24, TEXT)
    icon(c, "send", 284, y + 16, 24, TEXT)
    icon(c, "more", 332, y + 16, 24, TEXT, 3)
    if menu:
        mx, my = 196, y - 112
        c.rect(mx, my, 176, 104, 4, "#262B3B", extra=c.filter_shadow(6, 14, .5))
        icon(c, "folder", mx + 12, my + 16, 22, MUTED)
        c.text(mx + 48, my + 32, "Move to collection", 14, TEXT)
        icon(c, "delete", mx + 12, my + 64, 22, MUTED)
        c.text(mx + 48, my + 80, "Delete selected", 14, TEXT)


def settings(c, expanded=False, ready=False, shizuku=False):
    header(c, "MAKE IT YOURS")
    headline(c, 120, "Settings.", "Choose the access you need.")
    section(c, 186, "Setup", "Ready" if ready else "Tap to review")
    y = 202
    if shizuku and not ready:
        c.rect(22, y, 346, 44, 16, ICE)
        icon(c, "autofix", 122, y + 13, 18, ONPRI)
        c.text(148, y + 27, "Set up with Shizuku", 14, ONPRI, 500)
        y += 56
    setup_row(c, y, "bubble", "Edge shelf", ready, "Allow"); y += 80
    setup_row(c, y, "bell", "Notifications", True, "Allow"); y += 80
    setup_row(c, y, "hub", "Shizuku", shizuku, "Connect", "Bridge connected" if shizuku else "Shizuku is not running"); y += 92
    section(c, y, "Search & assistant", "Google only"); y += 16
    h = 232
    card(c, y, h)
    c.text(38, y + 28, "Google Circle to Search", 14, TEXT, 500)
    c.text(38, y + 48, "Google voice service active · eligibility varies", 12, MUTED)
    ny = c.para(38, y + 80, "All search buttons and gestures request Google's interface. Google may show Gemini on unsupported devices. Enable Circle to Search in your phone's navigation settings; StashDrop cannot unlock eligibility.", 314, 12, MUTED, 17)
    c.text(38, ny + 18, "Choose Google assistant", 14, ICE, 500)
    c.text(38, ny + 56, "Try Google search", 14, ICE, 500)
    y += h + 12
    action_row(c, y, "Digital assistant", "StashDrop selected" if expanded else "System picker and optional Shizuku", "assistant"); y += 88
    if expanded:
        card(c, y, 150)
        ny = c.para(38, y + 28, "Assistant gestures and Google's voice service are separate settings. Google decides native search eligibility.", 314, 12, MUTED, 17)
        c.text(38, ny + 18, "Open assistant picker", 14, ICE, 500)
        c.text(38, ny + 56, "Restore previous" if shizuku else "Connect Shizuku", 14, ICE, 500)
        y += 162
    action_row(c, y, "Gestures, clipboard & feel", "Customize interactions and motion", "tune")
    c.rect(0, 740, W, 104, 0, c.linear([(0, BG, 0), (.3, BG, 1), (1, BG, 1)], 0, 0, 0, 1))
    bottom_nav(c, 2)


def interactions(c, auto=True):
    header(c, "INTERACTIONS", back=True)
    headline(c, 120, "Your way.", "One search strip. One movable edge shelf.")
    setting_switch(c, 166, "Gesture strip", "Swipe up above the bottom navigation bar. No persistent markers.", True)
    action_row(c, 254, "System corner swipe", "Enable Google's assistant corner gesture in…", "assistant")
    ny = c.para(22, 352, "Android handles physical corner swipes. Availability and Google search eligibility depend on your phone. Drag the shelf handle to either edge; its position is remembered.", 346, 12, MUTED, 17)
    y = ny + 4
    setting_switch(c, y, "Auto-stash copied text", "May retain passwords, codes or private text", auto); y += 88
    if auto:
        c.text(22, y + 2, "Watching the clipboard while the edge shelf runs", 12, MINT); y += 18
    setting_switch(c, y, "Haptic feedback", "Feel drags, stacks and circles", True); y += 88
    card(c, y, 92)
    c.text(38, y + 30, "Springiness", 16, TEXT)
    c.text(352, y + 30, "1.0×", 14, ICE, 400, "end")
    c.rect(38, y + 58, 314, 4, 2, "#3A4256")
    c.rect(38, y + 58, 157, 4, 2, ICE)
    c.rect(193, y + 46, 4, 28, 2, ICE)
    c.rect(0, 740, W, 104, 0, c.linear([(0, BG, 0), (.3, BG, 1), (1, BG, 1)], 0, 0, 0, 1))
    bottom_nav(c, 2)


def fake_app(c):
    """A neutral host app behind the floating overlays."""
    c.rect(0, 0, W, H, 0, "#F4F5F8")
    c.rect(0, 0, W, 104, 0, "#FFFFFF")
    c.text(22, 84, "Messages", 24, "#14151A", 600)
    for i in range(7):
        y = 124 + i * 82
        c.circle(46, y + 30, 22, ["#C9D8FF", "#CDEBE0", "#F6D7E6", "#FBE4C3", "#DDD5FA", "#C9D8FF", "#CDEBE0"][i])
        c.rect(84, y + 14, 140 + (i * 37) % 80, 12, 6, "#2A2D36", opacity=.75)
        c.rect(84, y + 36, 200 + (i * 23) % 70, 10, 5, "#9A9EAB", opacity=.6)
        c.text(366, y + 26, f"{9 - i % 3}:{(i * 7) % 60:02d}", 11, "#9A9EAB", 400, "end")


def edge_handle(c, x, y, active=False):
    c.rect(x, y, 40, 96, 0, "none")
    c.rect(x + 18 - (2 if active else 0), y + 26, 8 if active else 4, 44, 4, ICE, opacity=.9 if active else .4)


def floating_shelf(c, selected=(), filt="All", empty=False, delete=False):
    fake_app(c)
    c.rect(0, 0, W, H, 0, "#000", opacity=.24)
    sx, sy, sw, sh = 12, 330, 366, 506
    c.rect(sx, sy, sw, sh, 24, SURF, extra=c.filter_shadow(-4, 20, .45))
    c.text(sx + 12, sy + 34, "Hold a card to drag", 10, ICE, 500)
    c.circle(sx + sw - 76, sy + 30, 20, SURF2)
    icon(c, "search", sx + sw - 88, sy + 18, 24, ICE)
    icon(c, "close", sx + sw - 40, sy + 18, 24, TEXT)
    type_chips(c, sy + 62, filt, sx + 12, sx + sw - 12, SURF)
    clip = c.clip_rect(sx, sy + 104, sw, sh - 104 - (102 if selected else 8))
    c.begin(clip)
    y = sy + 108
    if empty:
        icon(c, "inventory", W / 2 - 12, sy + 220, 24, ICE)
        c.text(W / 2, sy + 270, "No videos here", 16, TEXT, 500, "middle")
        c.text(W / 2, sy + 292, "Share to StashDrop from another app", 12, MUTED, 400, "middle")
    else:
        for i, it in enumerate(ITEMS):
            chosen = i in selected
            media = it["kind"] in ("image", "sticker")
            h = 116 if media else 84
            rot = [-4, 2, 5][i % 3] if chosen else 0
            cx, cy = sx + 12 + (sw - 24) / 2, y + h / 2
            sc = .96 if chosen else 1
            c.begin(transform=f"rotate({rot} {cx:g} {cy:g}) translate({cx:g} {cy:g}) scale({sc}) translate({-cx:g} {-cy:g})")
            c.rect(sx + 12, y, sw - 24, h, 12, PRIC if chosen else SURF2, extra=c.filter_shadow(6, 12, .5) if chosen else "")
            if media:
                clip2 = c.clip_rect(sx + 18, y + 6, sw - 36, h - 12, 12)
                c.begin(clip2)
                thumb(c, sx + 18, y + 6 - (sw - 36 - h + 12) / 2, sw - 36, it["kind"], 0)
                c.end()
                if chosen:
                    c.circle(sx + sw - 36, y + 24, 12, PRIC)
                    icon(c, "check_circle", sx + sw - 48, y + 12, 24, ICE)
            else:
                thumb(c, sx + 20, y + 8, 68, it["kind"])
                c.para(sx + 98, y + 38, it["text"], sw - 150 - (24 if chosen else 0), 14, TEXT, 19, max_lines=2)
                if chosen:
                    icon(c, "check_circle", sx + sw - 50, y + h / 2 - 12, 24, ICE)
            c.end()
            y += h + 10
    c.end()
    if selected:
        by = sy + sh - 96
        c.rect(sx, by - 6, sw, 6, 0, SURF)
        stack = [ITEMS[i] for i in selected][:3]
        for j, it in enumerate(reversed(stack)):
            rot = [-4, 2, 5][j % 3]
            px_, py_ = sx + 24, by + 14 - j * 5
            c.begin(transform=f"rotate({rot} {px_ + 46:g} {py_ + 32:g})")
            c.rect(px_, py_, 92, 64, 12, PRIC, extra=c.filter_shadow(4, 10, .55))
            if it["kind"] in ("image", "sticker"):
                clip3 = c.clip_rect(px_, py_, 92, 64, 12)
                c.begin(clip3)
                thumb(c, px_, py_ - 14, 92, it["kind"], 0)
                c.end()
            else:
                c.para(px_ + 10, py_ + 22, it["text"], 72, 11, TEXT, 14, max_lines=2)
            c.end()
        c.rect(sx + 104, by + 2, 26, 26, 12, ICE)
        c.text(sx + 117, by + 20, str(len(selected)), 12, ONPRI, 500, "middle")
        c.rect(sx + 148, by + 22, 104, 40, 20, ICE)
        c.text(sx + 200, by + 47, "WhatsApp", 14, ONPRI, 500, "middle")
        c.rect(sx + 260, by + 22, 86, 40, 20, "none", "#8E9099")
        c.text(sx + 303, by + 47, "Share", 14, ICE, 500, "middle")
    edge_handle(c, W - 40, 230, True)
    if delete:
        dy = 440
        c.rect(24, dy, W - 48, 196, 24, SURF2, extra=c.filter_shadow(10, 24, .6))
        c.text(48, dy + 44, "Delete saved item?", 22, TEXT)
        c.para(48, dy + 76, "This removes the item and its cached file permanently.", W - 96, 14, TEXT, 20)
        c.text(186, dy + 160, "Keep item", 14, ICE, 500, "middle")
        c.text(296, dy + 160, "Delete permanently", 14, ICE, 500, "middle")


def edge_handle_screen(c):
    fake_app(c)
    edge_handle(c, W - 40, 300)
    c.circle(W - 20, 348, 38, "none", ICE, 1.2, extra='stroke-dasharray="3 5" opacity="0.7"')
    callout(c, 120, 330, W - 46, 348, "Tap · open the shelf", "Hold · Google Circle to Search", "Drag · move to either edge")
    # notification shade peek
    c.rect(12, 44, W - 24, 80, 22, "#FFFFFF", extra=c.filter_shadow(4, 14, .25))
    stash_logo(c, 24, 58, 26)
    c.text(58, 72, "StashDrop · now", 11, "#5A5E6B")
    c.text(58, 92, "StashDrop shelf is active", 14, "#14151A", 600)
    c.text(58, 110, "Tap the edge handle for your shelf; hold for Google search", 11, "#5A5E6B")
    # gesture strip hint
    c.rect(0, H - 54, W, 40, 0, ICE, opacity=.08)
    c.text(W / 2, H - 30, "Gesture strip · swipe up above the nav bar (invisible on device)", 10, "#5A5E6B", 400, "middle")


def callout(c, x, y, ax, ay, *lines):
    c.rect(x - 100, y - 30, 196, 18 + 20 * len(lines), 14, "#14151A", opacity=.92)
    for i, ln in enumerate(lines):
        c.text(x - 86, y - 6 + i * 20, ln, 12, "#FFFFFF")
    c.line(x + 96, y, ax - 8, ay, ICE, 1.2)


def dismiss_screen(c):
    fake_app(c)
    c.rect(0, H - 300, W, 300, 0, c.linear([(0, "#000", 0), (1, "#000", .55)], 0, 0, 0, 1))
    c.circle(W / 2, H - 120, 37.5, RED, "#FFFFFF", 1.5, opacity=1)
    icon(c, "close", W / 2 - 17, H - 137, 34, "#FFFFFF", 2.2)
    c.text(W / 2, H - 60, "Drop here to hide the bubble", 12, "#FFFFFF", 500, "middle")
    bubble(c, W / 2 + 6, H - 186, 5)
    c.path(f"M{W - 30} 340 C 300 520, 240 560, {W / 2 + 10} {H - 220}", "none", ICE, 1.5, 'stroke-dasharray="4 6"')


def drag_screen(c):
    fake_app(c)
    c.rect(0, 0, W, H, 0, "#000", opacity=.08)
    # cards flying as one stack under the finger
    for j, rot in enumerate([-4, 2, 5]):
        c.begin(transform=f"rotate({rot} 190 470)")
        c.rect(144, 438 - j * 5, 92, 64, 12, PRIC, extra=c.filter_shadow(8, 16, .5))
        if j == 2:
            clip = c.clip_rect(144, 438 - j * 5, 92, 64, 12)
            c.begin(clip)
            thumb(c, 144, 410, 92, "image", 0)
            c.end()
        c.end()
    c.rect(222, 420, 26, 26, 12, ICE)
    c.text(235, 438, "3", 12, ONPRI, 500, "middle")
    c.circle(200, 500, 22, "#FFFFFF", opacity=.25)
    c.rect(20, H - 120, W - 40, 56, 28, "#FFFFFF", ICE, 2)
    c.text(44, H - 86, "Drop into the message box", 14, "#5A5E6B")


# ---------- dialogs ----------
def dlg_note(c):
    shelf(c)
    scrim(c, .6)
    dialog(c, "Keep a thought", None, [("Cancel", True), ("Save note", True)],
           (96, lambda c, x, y, w: text_field(c, x, y + 4, w, "Text or link", "Pick up film for the\ncamera on Friday", 88)))


def dlg_move(c):
    shelf(c, selected=(0, 2))
    scrim(c, .6)
    dialog(c, "Move to collection", None, [("Cancel", True), ("Move selected", True)],
           (82, lambda c, x, y, w: text_field(c, x, y + 4, w, "Collection name", "Ideas", 56, support="Leave blank for Inbox")))


def dlg_delete(c):
    shelf(c, selected=(0, 2))
    scrim(c, .6)
    dialog(c, "Delete 2 saved item(s)?", "The selected items and their cached files will be permanently removed. There is no trash or restore.",
           [("Keep items", True), ("Delete permanently", True)])


def dlg_clip(c):
    interactions(c, auto=False)
    scrim(c, .6)
    dialog(c, "Save copied text?", "Auto-stash can retain passwords, codes and private text on your shelf. Turning it off later does not remove earlier saved items.",
           [("Keep it off", True), ("Enable auto-stash", True)])


def dlg_crash(c):
    home(c)
    scrim(c, .6)
    report = ["StashDrop 0.7.0 (70) · Android 14", "java.lang.IllegalStateException:", "  Overlay window was detached", "  at OneUiThumbDockService.show", "  at OneUiThumbDockService.onStart", "  at android.app.ActivityThread…"]

    def extra(c, x, y, w):
        c.rect(x, y, w, 140, 8, BG)
        for i, ln in enumerate(report):
            c.text(x + 10, y + 22 + i * 19, ln, 11, MUTED, family=MONO)
    dialog(c, "StashDrop closed unexpectedly", "Review this local report before sharing it.", [("Dismiss", True), ("Copy report", True)], (140, extra))


def snackbar_screen(c):
    shelf(c, swipe_index=2)
    c.rect(16, 676, W - 32, 48, 4, "#E2E5F0", extra=c.filter_shadow(4, 10, .4))
    c.text(32, 705, "Crash report copied", 14, "#2B2F3A")


# ---------- components ----------
def components():
    c = Canvas(1200, 980, "cmp")
    c.rect(0, 0, 1200, 980, 0, BG)
    c.text(48, 70, "StashDrop · components & tokens", 30, TEXT, 700)
    c.text(48, 98, "Material 3 dark scheme from ui/theme/Theme.kt · Jetpack Compose", 14, MUTED)
    toks = [("Background", BG), ("Surface", SURF), ("Surface variant", SURF2), ("Primary container", PRIC), ("Ice · primary", ICE), ("Mint · secondary", MINT), ("On surface", TEXT), ("On surface variant", MUTED), ("On primary", ONPRI), ("Danger (dismiss)", RED)]
    for i, (n, col) in enumerate(toks):
        x, y = 48 + (i % 5) * 222, 130 + (i // 5) * 100
        c.rect(x, y, 56, 56, 14, col, "#3A4256")
        c.text(x + 68, y + 24, n, 14, TEXT, 500)
        c.text(x + 68, y + 44, col, 12, MUTED, family=MONO)
    y0 = 360
    c.text(48, y0, "Floating bubble", 16, TEXT, 600)
    bubble(c, 90, y0 + 70, 0); c.text(90, y0 + 130, "Idle", 12, MUTED, 400, "middle")
    bubble(c, 190, y0 + 70, 5); c.text(190, y0 + 130, "With count", 12, MUTED, 400, "middle")
    bubble(c, 300, y0 + 70, 12, True); c.text(300, y0 + 130, "Absorbing drop", 12, MUTED, 400, "middle")
    c.text(400, y0, "Edge handle", 16, TEXT, 600)
    c.rect(400, y0 + 20, 40, 96, 8, SURF); edge_handle(c, 400, y0 + 20)
    c.rect(470, y0 + 20, 40, 96, 8, SURF); edge_handle(c, 470, y0 + 20, True)
    c.text(420, y0 + 136, "Rest", 12, MUTED, 400, "middle"); c.text(490, y0 + 136, "Hover", 12, MUTED, 400, "middle")
    c.text(560, y0, "Dismiss target", 16, TEXT, 600)
    c.circle(600, y0 + 66, 30, SURF2, "#FFFFFF", 1.5, .9); icon(c, "close", 586, y0 + 52, 28, "#FFFFFF", 2)
    c.circle(690, y0 + 66, 37.5, RED, "#FFFFFF", 1.5); icon(c, "close", 673, y0 + 49, 34, "#FFFFFF", 2.2)
    c.text(600, y0 + 130, "Shown", 12, MUTED, 400, "middle"); c.text(690, y0 + 130, "Hot", 12, MUTED, 400, "middle")
    c.text(780, y0, "Switch", 16, TEXT, 600)
    switch(c, 780, y0 + 50, True); switch(c, 850, y0 + 50, False)
    c.text(960, y0, "Status pill", 16, TEXT, 600)
    c.rect(960, y0 + 40, 104, 30, 15, MINT, opacity=.12); c.text(1012, y0 + 60, "Edge shelf on", 11, MINT, 500, "middle")
    c.rect(960, y0 + 80, 104, 30, 15, ICE, opacity=.1); c.text(1012, y0 + 100, "Edge shelf off", 11, ICE, 500, "middle")
    y1 = 540
    c.text(48, y1, "Buttons & chips", 16, TEXT, 600)
    c.begin(transform=f"translate(26 {y1 + 18})"); primary_btn(c, 0, "Show edge shelf", w=300); c.end()
    c.rect(350, y1 + 24, 104, 40, 20, SURF2); c.text(402, y1 + 49, "Add note", 14, ICE, 500, "middle")
    c.rect(470, y1 + 24, 92, 40, 16, ICE); icon(c, "add", 484, y1 + 35, 18, ONPRI, 2); c.text(508, y1 + 49, "Note", 14, ONPRI, 500)
    c.rect(580, y1 + 24, 104, 40, 20, ICE); c.text(632, y1 + 49, "WhatsApp", 14, ONPRI, 500, "middle")
    c.rect(700, y1 + 24, 86, 40, 20, "none", "#8E9099"); c.text(743, y1 + 49, "Share", 14, ICE, 500, "middle")
    type_chips(c, y1 + 90, "Images", 48, 760)
    c.text(48, y1 + 170, "Rows", 16, TEXT, 600)
    c.begin(transform=f"translate(26 {y1 + 186})")
    stash_row(c, 0, ITEMS[0]); stash_row(c, 116, ITEMS[3], True)
    c.end()
    c.begin(transform=f"translate(400 {y1 + 186})")
    stash_row(c, 0, ITEMS[2], swipe=90)
    action_row(c, 84, "Circle to Search", "Google only · eligibility varies", "crop")
    c.end()
    c.begin(transform=f"translate(780 {y1 + 186})")
    setup_row(c, 0, "bubble", "Edge shelf", False, "Allow"); setup_row(c, 80, "bell", "Notifications", True, "Allow")
    c.end()
    c.begin(transform=f"translate(780 {y1 + 60})"); selection_bar(c, 0, 3); c.end()
    c.text(48, 950, "Icons: Material Symbols Outlined (Compose material-icons-extended). Motion: IosSpringSpecs follow 0.78/650 · landing · liquid.", 12, MUTED)
    return c


SCREENS = [
    ("01-home", "Home", lambda c: home(c)),
    ("02-home-shelf-on", "Home · edge shelf on", lambda c: home(c, running=True)),
    ("03-home-empty", "Home · nothing saved", lambda c: home(c, empty=True)),
    ("04-shelf", "Shelf", lambda c: shelf(c)),
    ("05-shelf-selection", "Shelf · selection bar", lambda c: shelf(c, selected=(0, 2))),
    ("06-shelf-more-menu", "Shelf · more actions", lambda c: (shelf(c, selected=(0, 2)), selection_bar(c, 692, 2, True))),
    ("07-shelf-swipe-delete", "Shelf · swipe to delete + snackbar", snackbar_screen),
    ("08-shelf-empty", "Shelf · empty", lambda c: shelf(c, empty=True)),
    ("09-settings", "Settings", lambda c: settings(c)),
    ("10-settings-shizuku-assistant", "Settings · Shizuku + assistant expanded", lambda c: settings(c, expanded=True, shizuku=True)),
    ("11-interactions", "Interactions", lambda c: interactions(c)),
    ("12-floating-shelf", "Floating shelf", lambda c: floating_shelf(c)),
    ("13-floating-shelf-selected", "Floating shelf · stack selected", lambda c: floating_shelf(c, selected=(0, 2, 3))),
    ("14-floating-shelf-empty", "Floating shelf · empty filter", lambda c: floating_shelf(c, filt="Videos", empty=True)),
    ("15-floating-shelf-delete", "Floating shelf · delete confirm", lambda c: floating_shelf(c, delete=True)),
    ("16-edge-handle", "Edge handle + notification", edge_handle_screen),
    ("17-bubble-dismiss", "Bubble drag · dismiss target", dismiss_screen),
    ("18-cross-app-drag", "Cross-app drag", drag_screen),
    ("19-dialog-note", "Dialog · Keep a thought", dlg_note),
    ("20-dialog-move", "Dialog · Move to collection", dlg_move),
    ("21-dialog-delete", "Dialog · Delete items", dlg_delete),
    ("22-dialog-clipboard", "Dialog · Auto-stash consent", dlg_clip),
    ("23-dialog-crash", "Dialog · Crash report", dlg_crash),
]

if __name__ == "__main__":
    frags = []
    for slug, label, fn in SCREENS:
        c = phone(slug, fn, bg="#F4F5F8" if slug in ("16-edge-handle", "17-bubble-dismiss", "18-cross-app-drag") else BG,
                  status_fg="#14151A" if slug in ("16-edge-handle", "17-bubble-dismiss", "18-cross-app-drag", "12-floating-shelf", "13-floating-shelf-selected", "14-floating-shelf-empty", "15-floating-shelf-delete") else TEXT)
        write(f"{OUT}/screens/{slug}.svg", c.svg(f"StashDrop · {label}"))
        frags.append((label, c.body(), W, H))
    comp = components()
    write(f"{OUT}/components.svg", comp.svg("StashDrop · components & tokens"))
    board(frags, 6, W, H, "StashDrop · UI design", f"{len(frags)} screens · 390 × 844 dp · generated from the current Compose source", f"{OUT}/stashdrop-all-screens.svg",
          bg="#05060A", fg=TEXT, muted=MUTED)
    print(len(frags), "screens")
