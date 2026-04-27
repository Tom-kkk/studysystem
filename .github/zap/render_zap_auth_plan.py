#!/usr/bin/env python3
"""将 baseline-authenticated.in.yaml 与 CI 环境变量合成为可执行的 ZAP 计划（密码安全转义）。"""
import json
import os
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
TEMPLATE = ROOT / ".github" / "zap" / "baseline-authenticated.in.yaml"
OUT = ROOT / "zap-plan-generated.yaml"


def normalize_zap_url(u: str) -> str:
    u = (u or "").strip().rstrip("/")
    for suffix in (":80", ":443"):
        if u.endswith(suffix):
            u = u[: -len(suffix)]
    return u


def main() -> int:
    try:
        target = normalize_zap_url(os.environ["ZAP_TARGET_URL"])
        user = os.environ["ZAP_SCAN_USER"]
        pw = os.environ["ZAP_SCAN_PASS"]
    except KeyError as e:
        print("Missing required environment variable:", e, file=sys.stderr)
        return 1
    if not target:
        print("ZAP_TARGET_URL is empty", file=sys.stderr)
        return 1

    text = TEMPLATE.read_text(encoding="utf-8")
    re_incl = re.escape(target) + ".*"
    re_logout = re.escape(target + "/api/auth/logout") + ".*"
    text = text.replace("__TARGET__", target)
    text = text.replace("__TARGET_RE_INCL__", re_incl)
    text = text.replace("__TARGET_RE_EXC_LOGOUT__", re_logout)
    text = text.replace("__USER_JSON__", json.dumps(user))
    text = text.replace("__PASS_JSON__", json.dumps(pw))
    OUT.write_text(text, encoding="utf-8")
    print("Wrote", OUT)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
