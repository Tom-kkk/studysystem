import json
from pathlib import Path

import requests


def main() -> int:
    mcp = json.loads(
        Path(r"C:/Users/32145/Desktop/javarainy/.cursor/mcp.json").read_text(
            encoding="utf-8"
        )
    )
    args = mcp["mcpServers"]["lark-mcp"]["args"]
    app_id = args[args.index("-a") + 1]
    app_secret = args[args.index("-s") + 1]

    token = "boxcnW1ZKpXSBjHPpFlb4T0XoXb"

    tat = requests.post(
        "https://open.feishu.cn/open-apis/auth/v3/tenant_access_token/internal",
        json={"app_id": app_id, "app_secret": app_secret},
        timeout=30,
    ).json()["tenant_access_token"]

    mcp_url = "https://mcp.feishu.cn/mcp"
    headers = {
        "Content-Type": "application/json",
        "X-Lark-MCP-TAT": tat,
        "X-Lark-MCP-Allowed-Tools": "fetch-file",
    }

    # init
    requests.post(
        mcp_url,
        headers=headers,
        json={"jsonrpc": "2.0", "id": 1, "method": "initialize", "params": {}},
        timeout=60,
    ).raise_for_status()

    # fetch-file: try possible argument keys
    for arg_key in ["file_token", "resource_token"]:
        print("---- try arg_key:", arg_key)
        payload = {
            "jsonrpc": "2.0",
            "id": 2,
            "method": "tools/call",
            "params": {"name": "fetch-file", "arguments": {arg_key: token}},
        }
        r = requests.post(mcp_url, headers=headers, json=payload, timeout=180)
        r.raise_for_status()
        j = r.json()
        content = j.get("result", {}).get("content")
        print("top_keys", j.keys())
        print("content_type", type(content))
        if isinstance(content, list) and content:
            first = content[0]
            print("first_keys", first.keys())
            txt = first.get("text", "")
            if isinstance(txt, str):
                print("text_preview_len", len(txt))
                print("text_preview", repr(txt[:400]))
        else:
            print(json.dumps(j, ensure_ascii=False)[:2000])

    return 0


if __name__ == "__main__":
    raise SystemExit(main())

