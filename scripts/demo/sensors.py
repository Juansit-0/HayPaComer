#!/usr/bin/env python3
"""Send simulated ESP32 events for the demo: door, temperature, scale, and a cold chain break."""

import argparse
import datetime
import json
import pathlib
import sys
import urllib.error
import urllib.request
import uuid

CREDENTIALS = pathlib.Path(__file__).resolve().parents[2] / ".demo-credentials.json"


def envelope(device, kind, at, **fields):
    body = {"eventId": str(uuid.uuid4()), "device": device, "type": kind, "at": at.isoformat()}
    body.update(fields)
    return body


def send(base, key, events):
    request = urllib.request.Request(
        base.rstrip("/") + "/api/v1/device/events",
        method="POST",
        data=json.dumps(events).encode(),
        headers={"Content-Type": "application/json", "X-Device-Key": key},
    )
    try:
        with urllib.request.urlopen(request) as response:
            report = json.loads(response.read() or b"{}")
    except urllib.error.HTTPError as failure:
        raise SystemExit(f"Events answered {failure.code}: {failure.read().decode()}")
    print(
        f"accepted {report.get('accepted')}, rejected {report.get('rejected')},"
        f" duplicates {report.get('duplicates')}, dropped as stale or unstable {report.get('dropped')}"
    )


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    commands = parser.add_subparsers(dest="command", required=True)
    door = commands.add_parser("door-open", help="the door opened N seconds ago and is still open")
    door.add_argument("--seconds", type=int, default=45)
    commands.add_parser("door-close")
    temperature = commands.add_parser("temperature")
    temperature.add_argument("celsius")
    commands.add_parser(
        "cold-break", help="25 minutes above 5 C, still warm; send it before newer temperatures"
    )
    weigh = commands.add_parser("weigh", help="a scale reading in grams")
    weigh.add_argument("grams")
    weigh.add_argument("--mode", choices=["FRIDGE", "COOKING"], default="FRIDGE")
    weigh.add_argument("--unstable", action="store_true")
    args = parser.parse_args()

    saved = json.loads(CREDENTIALS.read_text())
    now = datetime.datetime.now(datetime.timezone.utc).replace(microsecond=0)
    if args.command == "door-open":
        events = [envelope("door-demo", "DOOR", now - datetime.timedelta(seconds=args.seconds), door="OPEN")]
        send(saved["base"], saved["doorKey"], events)
    elif args.command == "door-close":
        send(saved["base"], saved["doorKey"], [envelope("door-demo", "DOOR", now, door="CLOSED")])
    elif args.command == "temperature":
        send(saved["base"], saved["doorKey"], [envelope("door-demo", "TEMPERATURE", now, tempC=args.celsius)])
    elif args.command == "cold-break":
        events = [
            envelope("door-demo", "TEMPERATURE", now - datetime.timedelta(minutes=minutes), tempC=celsius)
            for minutes, celsius in [(30, "4.2"), (25, "6.8"), (15, "8.4"), (5, "9.1"), (0, "8.7")]
        ]
        send(saved["base"], saved["doorKey"], events)
    else:
        events = [
            envelope(
                "scale-demo",
                "WEIGHT",
                now,
                grams=args.grams,
                stable=not args.unstable,
                mode=args.mode,
            )
        ]
        send(saved["base"], saved["scaleKey"], events)


if __name__ == "__main__":
    sys.exit(main())
