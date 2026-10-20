"""Install the actual APK and exercise its Android UI on an API 33 emulator."""
import re
import subprocess
import time
import xml.etree.ElementTree as ET
from pathlib import Path


def adb(*arguments):
    return subprocess.check_output(["adb", *arguments], text=True, stderr=subprocess.STDOUT, timeout=45)


def screen():
    adb("shell", "uiautomator", "dump", "/sdcard/aimei-window.xml")
    return ET.fromstring(adb("exec-out", "cat", "/sdcard/aimei-window.xml"))


def locate(text, partial=False):
    for attempt in range(8):
        for node in screen().iter("node"):
            value = node.attrib.get("text", "")
            if (text in value if partial else text == value):
                return node
        time.sleep(1)
    raise AssertionError("UI element not found: " + text)


def tap(text, partial=False):
    node = locate(text, partial)
    coordinates = [int(value) for value in re.findall(r"\d+", node.attrib["bounds"])]
    if len(coordinates) != 4:
        raise AssertionError("Invalid UI bounds")
    left, top, right, bottom = coordinates
    adb("shell", "input", "tap", str((left + right) // 2), str((top + bottom) // 2))
    time.sleep(1)


apk = Path("dist/Aimei-0.6.0.apk")
if not apk.is_file():
    raise SystemExit("The built APK was not downloaded")
print(adb("install", "-r", str(apk)))
adb("logcat", "-c")
launch = adb("shell", "am", "start", "-W", "-n", "com.aimei.app/.MainActivity")
print(launch)
if "Status: ok" not in launch or "Error:" in launch:
    raise AssertionError("Android could not start the installed app")
locate("开启实时试衣", True)
print("PASS: app installs and home screen opens")
tap("衣橱")
locate("私人衣橱")
locate("从相册添加衣服", True)
print("PASS: private wardrobe opens")
tap("我的")
locate("试衣指南")
print("PASS: profile and guide entry open")
tap("试衣")
tap("开启实时试衣", True)
locate("刷新")
locate("浏览器")
print("PASS: embedded try-on screen opens")
time.sleep(4)
if not adb("shell", "pidof", "com.aimei.app").strip():
    raise AssertionError("App process exited")
crashes = adb("logcat", "-d", "-s", "AndroidRuntime:E")
if "FATAL EXCEPTION" in crashes:
    print(crashes)
    raise AssertionError("Android runtime crash")
print("PASS: no Android runtime crash")
print("Physical-phone camera permission and live garment rendering are not covered by this smoke check.")
