#!/usr/bin/env python3
"""Prepare the HayPaComer demo household: people, fridge, food, devices, and settings."""

import argparse
import datetime
import json
import pathlib
import re
import secrets
import sys
import urllib.error
import urllib.request

CREDENTIALS = pathlib.Path(__file__).resolve().parents[2] / ".demo-credentials.json"


class Api:
    def __init__(self, base):
        self.base = base.rstrip("/") + "/api/v1"

    def call(self, method, path, body=None, token=None, key=None, expected=(200, 201, 202, 204)):
        request = urllib.request.Request(
            self.base + path,
            method=method,
            data=json.dumps(body).encode() if body is not None else None,
            headers={"Content-Type": "application/json", "Accept": "application/json"},
        )
        if token:
            request.add_header("Authorization", "Bearer " + token)
        if key:
            request.add_header("X-Device-Key", key)
        try:
            with urllib.request.urlopen(request) as response:
                text = response.read()
                status = response.status
        except urllib.error.HTTPError as failure:
            text = failure.read()
            status = failure.code
        payload = json.loads(text) if text else None
        if status not in expected:
            raise SystemExit(f"{method} {path} answered {status}: {payload}")
        return status, payload


def person(api, people, email, name):
    password = people.get(email) or secrets.token_urlsafe(14)
    api.call(
        "POST",
        "/auth/register",
        {"email": email, "password": password, "displayName": name},
        expected=(201, 409),
    )
    people[email] = password
    _, tokens = api.call("POST", "/auth/login", {"email": email, "password": password})
    return tokens["accessToken"]


def invitation_token(log_path, email):
    pattern = re.compile(r"Email to " + re.escape(email) + r": .*#token=([^\s]+)")
    tokens = pattern.findall(pathlib.Path(log_path).read_text(errors="ignore"))
    return urllib.request.unquote(tokens[-1]) if tokens else None


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--base", default="http://localhost:8080")
    parser.add_argument("--log", help="app log with MAIL_LOG_LINKS=true, to accept Ana's invitation")
    args = parser.parse_args()
    api = Api(args.base)
    saved = json.loads(CREDENTIALS.read_text()) if CREDENTIALS.exists() else {}
    people = saved.get("people", {})
    juan_email, ana_email = "juan@demo.haypacomer.test", "ana@demo.haypacomer.test"
    juan = person(api, people, juan_email, "Juan")
    ana = person(api, people, ana_email, "Ana")

    _, household = api.call(
        "POST",
        "/households",
        {"name": "Apartment 402", "currency": "COP", "timezone": "America/Bogota"},
        token=juan,
    )
    base = "/households/" + household["id"]
    _, fridge = api.call("POST", base + "/fridges", {"name": "Kitchen"}, token=juan)
    tray = fridge["children"][0]["children"][0]["id"]
    tomorrow = (datetime.date.today() + datetime.timedelta(days=1)).isoformat()

    def stock(food, grams, **extra):
        body = {"fridgeId": fridge["id"], "trayId": tray, "food": food, "grams": grams, **extra}
        return api.call("POST", base + "/items", body, token=juan)[1]["itemId"]

    milk = stock("Milk", 892, tareGrams=50)
    yogurt = stock("Yogurt", 125, visibility="PRIVATE")
    stock("Chicken breast", 80, expiresOn=tomorrow)
    stock("Tomato", 360, expiresOn=tomorrow)
    stock("Tuna", 300)
    stock("Rice", 900)
    stock("Egg", 300)

    def device(name):
        body = {"fridgeId": fridge["id"], "name": name, "kind": "SIMULATOR"}
        return api.call("POST", base + "/devices", body, token=juan)[1]

    door = device("Door sensor")
    scale = device("Counter scale")
    api.call(
        "PUT",
        base + "/devices/" + scale["device"]["id"] + "/scale/item",
        {"itemId": milk},
        token=juan,
    )
    api.call(
        "POST",
        base + "/recipes",
        {
            "name": "Rice with chicken",
            "servings": 2,
            "minutes": 35,
            "requirements": [
                {"food": "Chicken breast", "grams": 200},
                {"food": "Rice", "quantity": "150 g"},
                {"food": "Tomato", "quantity": "120 g", "optional": True},
            ],
            "steps": [
                {"instruction": "Weigh the rice", "weigh": {"food": "Rice", "grams": 150}},
                {"instruction": "Brown the chicken", "timerSeconds": 480},
                {"instruction": "Cook rice and chicken together", "timerSeconds": 1200},
            ],
        },
        token=juan,
    )
    api.call("PUT", base + "/market-budget", {"monthly": 200000}, token=juan)
    api.call("POST", base + "/invitations", {"email": ana_email, "role": "MEMBER"}, token=juan)

    joined = False
    if args.log:
        token = invitation_token(args.log, ana_email)
        if token:
            api.call("POST", "/invitations/accept", {"token": token}, token=ana)
            api.call(
                "PUT",
                base + "/profile",
                {"diet": "OMNIVORE", "allergies": ["FISH"], "avoidedFoods": []},
                token=ana,
            )
            joined = True

    CREDENTIALS.write_text(
        json.dumps(
            {
                "base": args.base,
                "people": people,
                "household": household["id"],
                "fridge": fridge["id"],
                "milk": milk,
                "yogurt": yogurt,
                "doorKey": door["apiKey"],
                "scaleKey": scale["apiKey"],
                "scale": scale["device"]["id"],
            },
            indent=2,
        )
    )
    print(f"Household {household['id']} ready, credentials in {CREDENTIALS.name} (git-ignored)")
    if not joined:
        print("Ana is invited: open the link printed in the app log (MAIL_LOG_LINKS=true) as Ana.")


if __name__ == "__main__":
    sys.exit(main())
