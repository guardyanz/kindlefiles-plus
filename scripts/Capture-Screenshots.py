#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Capture the signed app through its real UI on a disposable Android tablet AVD."""
import hashlib
import json
from pathlib import Path
import re
import struct
import subprocess
import time
import xml.etree.ElementTree as ET
import zipfile

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "artifacts" / "screenshots"
PACKAGE = "io.github.guardyanz.kindlefiles"
OUT.mkdir(parents=True, exist_ok=True)


def adb(*args):
    return subprocess.run(["adb", *args], check=True, capture_output=True, text=True, timeout=30).stdout


def screen(name):
    adb("shell", "screencap", "-p", "/sdcard/folderkit-screen.png")
    target = OUT / f"{name}.png"
    adb("pull", "/sdcard/folderkit-screen.png", str(target))
    dimensions = struct.unpack(">II", target.read_bytes()[16:24])
    print(f"Screenshot {target.name}: {dimensions}", flush=True)
    return dimensions


def state():
    for _ in range(6):
        result = adb("shell", "uiautomator", "dump", "/sdcard/folderkit-view.xml")
        if "dumped to" in result:
            xml = adb("shell", "cat", "/sdcard/folderkit-view.xml")
            (OUT / "latest-view.xml").write_text(xml, encoding="utf-8")
            return ET.fromstring(xml)
        print(f"Waiting for UI hierarchy: {result.strip()}", flush=True)
        time.sleep(2)
    raise RuntimeError("UI Automator could not capture a settled hierarchy.")


def find(tree, label, attribute="text", contains=False):
    for node in tree.iter("node"):
        value = node.get(attribute, "")
        if (label in value if contains else value.casefold() == label.casefold()):
            numbers = list(map(int, re.findall(r"\d+", node.get("bounds", ""))))
            if len(numbers) == 4 and numbers[2] > numbers[0] and numbers[3] > numbers[1]:
                return node
    return None


def tap_node(node):
    x1, y1, x2, y2 = map(int, re.findall(r"\d+", node.get("bounds", "")))
    adb("shell", "input", "tap", str((x1 + x2) // 2), str((y1 + y2) // 2))
    time.sleep(1)


def tap(label, attribute="text"):
    node = find(state(), label, attribute)
    if node is None:
        raise RuntimeError(f"UI element missing: {attribute}={label}")
    tap_node(node)


def wait(label, attribute="text", seconds=20):
    end = time.monotonic() + seconds
    while time.monotonic() < end:
        tree = state()
        if find(tree, label, attribute) is not None:
            return tree
        time.sleep(1)
    raise RuntimeError(f"Timed out waiting for {label}")


def samples():
    folder = OUT / "sample-files" / "Reading"
    (folder / "Books").mkdir(parents=True, exist_ok=True)
    (folder / "Documents").mkdir(exist_ok=True)
    (folder / "Photos").mkdir(exist_ok=True)
    packing = "Weekend packing list\n\nA book\nA notebook\nHeadphones\nA charging cable\n"
    (folder / "Packing list.txt").write_text(packing)
    (folder / "Documents" / "Packing list copy.txt").write_text(packing)
    (folder / "Travel notes.txt").write_text("A quiet weekend\n\nRead, take a walk, and make time for the things you enjoy.\n")
    (folder / "Books" / "A quiet weekend.txt").write_text("An original sample document for FolderKit screenshots.\n")
    with zipfile.ZipFile(folder / "Weekend books.zip", "w", zipfile.ZIP_DEFLATED) as archive:
        archive.writestr("Reading list.txt", "Books to bring\nOne favorite story\nOne new discovery\n")
        archive.writestr("Notes/Ideas.txt", "Keep a page for ideas.\n")
    with zipfile.ZipFile(folder / "Reading guide.epub", "w") as epub:
        epub.writestr("mimetype", "application/epub+zip")
        epub.writestr("META-INF/container.xml", '<container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container"><rootfiles><rootfile full-path="content.opf" media-type="application/oebps-package+xml"/></rootfiles></container>')
        epub.writestr("content.opf", '<package xmlns="http://www.idpf.org/2007/opf" version="3.0" unique-identifier="id"><metadata xmlns:dc="http://purl.org/dc/elements/1.1/"><dc:identifier id="id">folderkit-sample</dc:identifier><dc:title>Reading guide</dc:title><dc:language>en</dc:language></metadata><manifest><item id="book" href="book.xhtml" media-type="application/xhtml+xml"/></manifest><spine><itemref idref="book"/></spine></package>')
        epub.writestr("book.xhtml", '<html xmlns="http://www.w3.org/1999/xhtml"><head><title>Reading guide</title></head><body><h1>Make time to read</h1><p>An original sample for FolderKit.</p></body></html>')
    adb("push", str(folder), "/sdcard/Download/Reading")


def connect_sample_folder():
    tap("Connect a folder")
    model = adb("shell", "getprop", "ro.product.model").strip()
    for _ in range(10):
        tree = state()
        labels = [n.get("text") or n.get("content-desc") for n in tree.iter("node")]
        print("Picker:", [x for x in labels if x], flush=True)
        node = find(tree, "Reading")
        if node is not None:
            tap_node(node)
            break
        for label in ("Download", "Downloads", "Internal storage", model):
            node = find(tree, label)
            if node is not None:
                tap_node(node)
                break
        else:
            node = find(tree, "Show roots", "content-desc")
            if node is None:
                raise RuntimeError("Could not find internal storage in the folder picker.")
            tap_node(node)
    else:
        raise RuntimeError("Could not navigate to the sample Reading folder.")
    tree = state()
    for label in ("Use this folder", "Select", "Open"):
        node = find(tree, label)
        if node is not None:
            tap_node(node)
            break
    else:
        raise RuntimeError("Could not find the folder picker confirmation button.")
    tree = state()
    for label in ("Allow", "OK"):
        node = find(tree, label)
        if node is not None:
            tap_node(node)
            break
    wait("Search this folder")


def restart():
    adb("shell", "am", "force-stop", PACKAGE)
    adb("shell", "am", "start", "-n", f"{PACKAGE}/.MainActivity")
    time.sleep(4)
    wait("Connect a folder")


def motion_preview():
    for setting in ("window_animation_scale", "transition_animation_scale", "animator_duration_scale"):
        adb("shell", "settings", "put", "global", setting, "1")
    time.sleep(2)
    tree = state()
    nodes = [find(tree, label) for label in ("My places", "Favorites", "File tools", "Browse files")]
    if any(node is None for node in nodes):
        raise RuntimeError("Navigation controls missing before motion recording.")
    recorder = subprocess.Popen(["adb", "shell", "screenrecord", "--size", "1280x800", "--bit-rate", "3000000",
                                 "--time-limit", "12", "/sdcard/folderkit-motion.mp4"], stdout=subprocess.DEVNULL, stderr=subprocess.PIPE, text=True)
    time.sleep(1)
    for node in nodes:
        tap_node(node)
        time.sleep(.6)
    tap("Remove favorite", "content-desc")
    tap("Save favorite", "content-desc")
    _, error = recorder.communicate(timeout=20)
    if recorder.returncode:
        raise RuntimeError(f"Motion recording failed: {error}")
    adb("pull", "/sdcard/folderkit-motion.mp4", str(OUT / "folderkit-motion.mp4"))
    for setting in ("window_animation_scale", "transition_animation_scale", "animator_duration_scale"):
        adb("shell", "settings", "put", "global", setting, "0")


def verify_variants():
    adb("shell", "cmd", "uimode", "night", "yes")
    restart()
    screen("qa-dark-places")
    tap("Reading")
    wait("Search this folder")
    screen("qa-dark-browser")
    adb("shell", "cmd", "uimode", "night", "no")
    adb("shell", "settings", "put", "system", "user_rotation", "1")
    time.sleep(3)
    tap("My places")
    screen("qa-light-portrait")
    tap("Favorites")
    wait("Books")
    adb("shell", "wm", "density", "640")
    adb("shell", "settings", "put", "system", "font_scale", "2.0")
    restart()
    tap("Favorites")
    wait("Books")
    screen("qa-compact-large-text")
    tap("File tools")
    wait("Find duplicates")
    screen("qa-compact-tools")


def main():
    apk = next((ROOT / "artifacts" / "apk").glob("*.apk"))
    print(adb("shell", "wm", "size"), flush=True)
    adb("shell", "wm", "density", "320")
    adb("shell", "settings", "put", "system", "accelerometer_rotation", "0")
    adb("shell", "settings", "put", "system", "user_rotation", "0")
    adb("shell", "settings", "put", "global", "sysui_demo_allowed", "1")
    adb("shell", "am", "broadcast", "-a", "com.android.systemui.demo", "-e", "command", "enter")
    adb("shell", "am", "broadcast", "-a", "com.android.systemui.demo", "-e", "command", "clock", "-e", "hhmm", "0941")
    adb("shell", "am", "broadcast", "-a", "com.android.systemui.demo", "-e", "command", "battery", "-e", "level", "100", "-e", "plugged", "false")
    samples()
    print(adb("install", "-r", str(apk)), flush=True)
    adb("shell", "am", "start", "-n", f"{PACKAGE}/.MainActivity")
    time.sleep(5)
    wait("Connect a folder")
    time.sleep(2)
    if screen("01-places") != (2560, 1600):
        raise RuntimeError("Tablet screenshot resolution is incorrect.")
    connect_sample_folder()
    screen("02-browser")
    tap("Save favorite", "content-desc")
    tap("Books")
    tap("Save favorite", "content-desc")
    tap("Parent folder", "content-desc")
    tap("Favorites")
    wait("Reading")
    screen("03-favorites")
    tap("Browse files")
    tap("Actions for Weekend books.zip", "content-desc")
    wait("Extract ZIP")
    screen("04-archive-actions")
    adb("shell", "input", "keyevent", "4")
    tap("File tools")
    wait("Scan current folder")
    screen("05-file-tools")
    tap("Scan current folder")
    wait("1 duplicate group(s)")
    time.sleep(4)  # Let the completion snackbar leave before capture.
    screen("06-duplicates")
    tap("Close")
    tap("Browse files")
    tap("Select Packing list.txt", "content-desc")
    wait("1 selected")
    screen("07-selection")
    tap("Clear selection", "content-desc")
    motion_preview()
    verify_variants()
    metadata = {
        "device": "AOSP Android emulator API 28, Nexus 10 profile",
        "resolution": [2560, 1600],
        "densityDpi": 320,
        "apk": apk.name,
        "apkSha256": hashlib.sha256(apk.read_bytes()).hexdigest(),
        "screenshots": sorted(p.name for p in OUT.glob("0*.png")),
        "verificationScreens": sorted(p.name for p in OUT.glob("qa-*.png")),
        "motionPreview": "folderkit-motion.mp4",
        "sampleContent": "Original disposable documents; no personal files.",
    }
    (OUT / "capture.json").write_text(json.dumps(metadata, indent=2) + "\n")


if __name__ == "__main__":
    try:
        main()
    except Exception:
        screen("failure")
        raise
