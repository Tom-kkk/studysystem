import json
from pathlib import Path

import requests


def main() -> int:
    mcp_path = Path(r"C:/Users/32145/Desktop/javarainy/.cursor/mcp.json")
    data = json.loads(mcp_path.read_text(encoding="utf-8"))

    args = data["mcpServers"]["lark-mcp"]["args"]
    app_id = args[args.index("-a") + 1]
    app_secret = args[args.index("-s") + 1]

    # docID from url: /docx/Jg0gdjSHJoiOhexfQp3cPckxnAh
    doc_id = "Jg0gdjSHJoiOhexfQp3cPckxnAh"

    # 1) get tenant_access_token (TAT)
    token_url = "https://open.feishu.cn/open-apis/auth/v3/tenant_access_token/internal"
    resp = requests.post(
        token_url, json={"app_id": app_id, "app_secret": app_secret}, timeout=30
    )
    resp.raise_for_status()
    tat = resp.json()["tenant_access_token"]

    # 2) call remote MCP fetch-doc
    mcp_url = "https://mcp.feishu.cn/mcp"
    headers = {
        "Content-Type": "application/json",
        "X-Lark-MCP-TAT": tat,
        # Limit the discovered/allowed tool set for this session.
        "X-Lark-MCP-Allowed-Tools": "fetch-doc",
    }

    init_payload = {"jsonrpc": "2.0", "id": 1, "method": "initialize", "params": {}}
    r1 = requests.post(mcp_url, headers=headers, json=init_payload, timeout=60)
    r1.raise_for_status()

    call_payload = {
        "jsonrpc": "2.0",
        "id": 2,
        "method": "tools/call",
        "params": {"name": "fetch-doc", "arguments": {"docID": doc_id}},
    }
    r2 = requests.post(mcp_url, headers=headers, json=call_payload, timeout=120)
    r2.raise_for_status()

    j2 = r2.json()
    content = j2.get("result", {}).get("content")
    if isinstance(content, list) and content:
        first = content[0]
        txt = first.get("text", "")
        preview = txt[:300].replace("\n", " ")
        print("FETCH_DOC_OK")
        print("preview:", preview)
    else:
        print("FETCH_DOC_NO_CONTENT")
        print(json.dumps(j2, ensure_ascii=False)[:2000])

    return 0


if __name__ == "__main__":
    raise SystemExit(main())

