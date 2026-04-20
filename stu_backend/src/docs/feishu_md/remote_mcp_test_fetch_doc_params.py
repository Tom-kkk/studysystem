import json
import requests
from pathlib import Path


def main() -> int:
    mcp = json.loads(
        Path(r"C:/Users/32145/Desktop/javarainy/.cursor/mcp.json").read_text(
            encoding="utf-8"
        )
    )
    args = mcp["mcpServers"]["lark-mcp"]["args"]
    app_id = args[args.index("-a") + 1]
    app_secret = args[args.index("-s") + 1]

    doc_id = "DE6JdJanMo0BHox9kgfcq5p1n2c"  # 21

    token_url = "https://open.feishu.cn/open-apis/auth/v3/tenant_access_token/internal"
    resp = requests.post(
        token_url, json={"app_id": app_id, "app_secret": app_secret}, timeout=30
    )
    resp.raise_for_status()
    tat = resp.json()["tenant_access_token"]

    mcp_url = "https://mcp.feishu.cn/mcp"
    headers = {
        "Content-Type": "application/json",
        "X-Lark-MCP-TAT": tat,
        "X-Lark-MCP-Allowed-Tools": "fetch-doc",
    }

    # init once
    requests.post(
        mcp_url,
        headers=headers,
        json={"jsonrpc": "2.0", "id": 1, "method": "initialize", "params": {}},
        timeout=60,
    ).raise_for_status()

    tries = [
        {"docID": doc_id},
        {"docID": doc_id, "includeMedia": True},
        {"docID": doc_id, "media": True},
        {"docID": doc_id, "includeResources": True},
        {"docID": doc_id, "format": "raw"},
        {"docID": doc_id, "withMedia": True},
    ]

    for i, arguments in enumerate(tries):
        payload = {
            "jsonrpc": "2.0",
            "id": 100 + i,
            "method": "tools/call",
            "params": {"name": "fetch-doc", "arguments": arguments},
        }
        r = requests.post(mcp_url, headers=headers, json=payload, timeout=180)
        r.raise_for_status()
        j = r.json()
        content = j.get("result", {}).get("content") or []
        txt = content[0].get("text", "") if content else ""
        inner = json.loads(txt) if txt else {}

        print(f"--- try {i} args={arguments}")
        print("inner_keys=", sorted(inner.keys()))
        md = inner.get("markdown")
        if isinstance(md, str):
            print("markdown_len=", len(md), "preview=", repr(md[:80]))
        # check for any media/resource-like values
        media_like = []
        for k, v in inner.items():
            lk = k.lower()
            if any(x in lk for x in ["media", "resource", "token", "file"]):
                media_like.append((k, type(v).__name__))
        if media_like:
            print("media_like=", media_like)

    return 0


if __name__ == "__main__":
    raise SystemExit(main())

