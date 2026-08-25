#!/usr/bin/env python3
"""
Find overlapping and clipped controls in the running app, from an accessibility dump.

Why this exists: the settings UI is built for a television and is regularly wrong on a
phone in ways a screenshot does not make obvious — a heading that renders as
"Dashboard Background ListRandomize", or a fourth option clipped to "INSTAN". Both were
shipped and neither was noticed by looking. Comparing pixel bounds finds them
mechanically.

Usage:
    adb -s <serial> shell uiautomator dump /sdcard/ui.xml
    adb -s <serial> shell cat /sdcard/ui.xml > /tmp/ui.xml
    python3 tools/sweep_layout.py /tmp/ui.xml <screen-width-px> "<label>"

Navigate the app yourself between dumps; this only inspects one screen at a time. Note
that `adb shell input keyevent` sends events with input source "unknown", which the
app's D-pad handling ignores — use `input dpad -d 0 keyevent <code>`, and the `-d 0`
matters when a second (virtual) display is attached, e.g. Android Studio mirroring.

Two failure modes are reported:
  CLIPPED  - a node reaches the screen edge, so its text is cut off
  COLLIDE  - two nodes on the same visual row overlap horizontally

Known false positive, filtered out below: an OutlinedTextField's floating label is a
separate node whose bounds sit *inside* the field's, so label and value always appear
to overlap. Containment plus a large size difference identifies that case.
"""
import re
import sys
import pathlib


def nodes(path):
    text = pathlib.Path(path).read_text(errors="replace")
    found = []
    for match in re.finditer(r"<node[^>]*>", text):
        chunk = match.group(0)
        label = re.search(r'text="([^"]*)"', chunk)
        bounds = re.search(r'bounds="\[(-?\d+),(-?\d+)\]\[(-?\d+),(-?\d+)\]"', chunk)
        if label and label.group(1).strip() and bounds:
            x1, y1, x2, y2 = map(int, bounds.groups())
            found.append((label.group(1), x1, y1, x2, y2))
    return found


def contained(inner, outer):
    """True if `inner`'s box sits within `outer`'s — the floating-label shape."""
    return (outer[1] <= inner[1] and outer[3] >= inner[3]
            and outer[2] <= inner[2] and outer[4] >= inner[4])


def issues(ns, screen_width):
    out = []
    for n in ns:
        if n[3] >= screen_width:
            out.append(f"CLIPPED  '{n[0][:34]}' right edge {n[3]} >= screen {screen_width}")
    for i, a in enumerate(ns):
        for b in ns[i + 1:]:
            v_overlap = min(a[4], b[4]) - max(a[2], b[2])
            h_overlap = min(a[3], b[3]) - max(a[1], b[1])
            a_h, b_h = a[4] - a[2], b[4] - b[2]
            if v_overlap <= 0.6 * min(a_h, b_h) or h_overlap <= 4:
                continue
            if contained(a, b) or contained(b, a):
                continue  # text-field label inside its field
            out.append(f"COLLIDE  '{a[0][:24]}' x{a[1]}-{a[3]}  vs  '{b[0][:24]}' x{b[1]}-{b[3]}")
    return sorted(set(out))


def main():
    path, width, label = sys.argv[1], int(sys.argv[2]), sys.argv[3]
    ns = nodes(path)
    found = issues(ns, width)
    if found:
        print(f"  {label}: {len(found)} issue(s)")
        for line in found:
            print(f"    {line}")
    else:
        print(f"  {label}: clean ({len(ns)} text nodes)")
    return 1 if found else 0


if __name__ == "__main__":
    sys.exit(main())
