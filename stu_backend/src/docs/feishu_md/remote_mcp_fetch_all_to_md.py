import base64
import json
import os
import re
import time
from pathlib import Path

import requests


DOCS: list[tuple[int, str, str]] = [
    (0, "0 开始之前", "Jg0gdjSHJoiOhexfQp3cPckxnAh"),
    (1, "1 NetBeans Java Web开发环境", "K0WHdlMM5oE9O2xvbnscDZh8nse"),
    (2, "2 注释的格式", "Qk1TdC1TuodRkoxpbmrcoujZnCe"),
    (3, "3 验证码工具类CheckcodeUtil", "ZY0tdyJ17oTKMWxG7PYc8aVUnYd"),
    (4, "4 属性工具类proprertyUtil", "Sy5XdSsBGojO00xiV5RcFJSenNj"),
    (5, "5 日志工具类LogerUtil", "NYybdI6O3oR4SXx4TCbcxsbcnrf"),
    (6, "6 数据库设计", "WKK6drrZLoiRKUxEVnzcz1GVnqg"),
    (7, "7 数据访问对象DAO", "UOTkdZlgMo8dhXxRdIJclJKznFf"),
    (8, "8 发送验证码邮件工具类EmailUtil", "FHCPd34EhowqofxTbsecpw7knNf"),
    (9, "9 口令重置和邮箱设置", "O8Pidq2IeoJsTDxWAmYcz3xynwf"),
    (10, "10 教师界面", "X0sRdAK4joc06YxCNyAc1QS6nnh"),
    (11, "11 “新建课程”的界面", "UiWqdfkQ8occYpxV9RBcQq4Vnmd"),
    (12, "12 教学班ClassDAO", "QgwydWTvsoFeINx6uYYczPsfnTe"),
    (13, "13 课程上传和数据导入", "JCh9djWFEoL2k3xTXcxcDmt0nRU"),
    (14, "14 课程CourseDAO", "HC31dzDVSoIOqxxYEVCcyx9NnFg"),
    (15, "15 “教师界面”信息显示", "XFv1dOgYRo4eZhxwVrScyRy9nLg"),
    (16, "16 实验项目状态后端更新", "ZoD9d7zLZoFcLvx2Yw1cYVponCe"),
    (17, "17 学生界面", "RkWmdR3HmoPd6Dxci8mc8ocUnac"),
    (18, "18 实验报告上传", "HLqLdXyFJoBkPNxwl3hcQtLynvf"),
    (19, "19 学生查看实验报告", "DNjydtT1bojurlxQ611cETlgnRc"),
    (20, "20 过滤器", "SzB7dONAeojKfCxREHYcH7TinDB"),
    (21, "21 使用EL表达式和JSTL重写学生界面", "DE6JdJanMo0BHox9kgfcq5p1n2c"),
    (22, "22 使用EL和JSTL重写教师界面", "VNjfdhwgBoM1gXx2RVIc4n8Gn4b"),
]


def sanitize_filename(s: str) -> str:
    # Windows filename safe: remove forbidden characters.
    s = s.replace("/", " ")
    s = s.replace("\\", " ")
    s = s.replace(":", " ")
    s = s.replace("*", " ")
    s = s.replace("?", " ")
    s = s.replace("\"", " ")
    s = s.replace("<", " ")
    s = s.replace(">", " ")
    s = s.replace("|", " ")
    s = s.strip()
    # Avoid double spaces
    s = re.sub(r"\s+", " ", s)
    return s


def get_tenant_access_token(app_id: str, app_secret: str) -> str:
    token_url = "https://open.feishu.cn/open-apis/auth/v3/tenant_access_token/internal"
    r = requests.post(
        token_url, json={"app_id": app_id, "app_secret": app_secret}, timeout=30
    )
    r.raise_for_status()
    return r.json()["tenant_access_token"]


def looks_like_base64(s: str) -> bool:
    s = s.strip()
    if len(s) < 40:
        return False
    # Base64: A-Z a-z 0-9 + / and optional = padding; allow newlines
    s2 = re.sub(r"\s+", "", s)
    if len(s2) % 4 != 0:
        return False
    return re.fullmatch(r"[A-Za-z0-9+/=]+", s2) is not None


def detect_image_ext(data: bytes) -> str:
    # Minimal magic-number detection.
    if data.startswith(b"\x89PNG\r\n\x1a\n"):
        return ".png"
    if data.startswith(b"\xff\xd8\xff"):
        return ".jpg"
    if data.startswith(b"GIF87a") or data.startswith(b"GIF89a"):
        return ".gif"
    if data[0:4] == b"RIFF" and b"WEBP" in data[8:16]:
        return ".webp"
    return ".bin"


def remote_mcp_initialize(tat: str, allowed_tools: str) -> None:
    mcp_url = "https://mcp.feishu.cn/mcp"
    headers = {
        "Content-Type": "application/json",
        "X-Lark-MCP-TAT": tat,
        "X-Lark-MCP-Allowed-Tools": allowed_tools,
    }
    init_payload = {"jsonrpc": "2.0", "id": 1, "method": "initialize", "params": {}}
    r = requests.post(
        mcp_url, headers=headers, json=init_payload, timeout=60
    )
    r.raise_for_status()


def fetch_doc_markdown(tat: str, doc_id: str) -> str:
    mcp_url = "https://mcp.feishu.cn/mcp"
    headers = {
        "Content-Type": "application/json",
        "X-Lark-MCP-TAT": tat,
        "X-Lark-MCP-Allowed-Tools": "fetch-doc",
    }

    # initialize per-session (works even if repeated)
    init_payload = {"jsonrpc": "2.0", "id": 1, "method": "initialize", "params": {}}
    r1 = requests.post(mcp_url, headers=headers, json=init_payload, timeout=60)
    r1.raise_for_status()

    call_payload = {
        "jsonrpc": "2.0",
        "id": 2,
        "method": "tools/call",
        "params": {
            "name": "fetch-doc",
            "arguments": {"docID": doc_id},
        },
    }
    r2 = requests.post(mcp_url, headers=headers, json=call_payload, timeout=180)
    r2.raise_for_status()
    j2 = r2.json()

    content = j2.get("result", {}).get("content")
    if not isinstance(content, list) or not content:
        raise RuntimeError(f"fetch-doc missing content: {json.dumps(j2)[:500]}")

    first = content[0]
    txt = first.get("text", "")
    # `text` is a JSON string that contains markdown.
    inner = json.loads(txt)
    md = inner.get("markdown")
    if not isinstance(md, str):
        raise RuntimeError(f"fetch-doc missing markdown field: {inner.keys()}")
    return md


def fetch_file_by_resource_token(tat: str, resource_token: str, timeout_sec: int = 180) -> bytes:
    """
    Download binary file content via remote MCP `fetch-file`.

    Note: parameter key is `resource_token` (not `file_token`) for this MCP.
    """
    mcp_url = "https://mcp.feishu.cn/mcp"
    headers = {
        "Content-Type": "application/json",
        "X-Lark-MCP-TAT": tat,
        "X-Lark-MCP-Allowed-Tools": "fetch-file",
    }

    # initialize for this session/tool set
    init_payload = {"jsonrpc": "2.0", "id": 1, "method": "initialize", "params": {}}
    r1 = requests.post(mcp_url, headers=headers, json=init_payload, timeout=60)
    r1.raise_for_status()

    call_payload = {
        "jsonrpc": "2.0",
        "id": 2,
        "method": "tools/call",
        "params": {
            "name": "fetch-file",
            "arguments": {"resource_token": resource_token},
        },
    }

    r2 = requests.post(mcp_url, headers=headers, json=call_payload, timeout=timeout_sec)
    r2.raise_for_status()
    j2 = r2.json()

    content = j2.get("result", {}).get("content")
    if not isinstance(content, list) or not content:
        raise RuntimeError(f"fetch-file missing content: {json.dumps(j2)[:800]}")
    first = content[0]
    # 图片/附件二进制通常在 `data` 字段（base64）；错误信息可能在 `text` 字段。
    data_b64 = first.get("data")
    txt = first.get("text") or ""
    if isinstance(data_b64, str) and data_b64.strip():
        payload_str = data_b64
    else:
        payload_str = txt if isinstance(txt, str) else ""

    # Access denied returns plain text error; don't attempt decode.
    if payload_str and isinstance(payload_str, str) and ("Access denied" in payload_str or "scope" in payload_str.lower()):
        raise PermissionError(payload_str)
    if not payload_str:
        raise RuntimeError("fetch-file returned empty data/text")

    if looks_like_base64(payload_str):
        raw = base64.b64decode(re.sub(r"\s+", "", payload_str))
        if not raw:
            raise RuntimeError("fetch-file base64 decoded empty")
        return raw

    # Sometimes it might be a JSON string with base64; try to parse.
    try:
        inner = json.loads(payload_str)
        for key in ["data", "base64", "content", "file", "binary"]:
            if key in inner and isinstance(inner[key], str) and looks_like_base64(inner[key]):
                raw = base64.b64decode(re.sub(r"\s+", "", inner[key]))
                if raw:
                    return raw
    except Exception:
        pass

    # Fallback: we can't decode it to bytes.
    raise RuntimeError(f"fetch-file returned non-decodable text (len={len(txt)})")


def main() -> int:
    repo_root = Path(r"C:/Users/32145/Desktop/javarainy")
    out_dir = repo_root / "feishu_md"
    out_dir.mkdir(parents=True, exist_ok=True)
    assets_dir = out_dir / "assets"
    assets_dir.mkdir(parents=True, exist_ok=True)

    mcp_path = repo_root / ".cursor" / "mcp.json"
    data = json.loads(mcp_path.read_text(encoding="utf-8"))

    args = data["mcpServers"]["lark-mcp"]["args"]
    app_id = args[args.index("-a") + 1]
    app_secret = args[args.index("-s") + 1]

    print("Fetching tenant_access_token ...")
    tat = get_tenant_access_token(app_id, app_secret)
    print("TAT acquired.")

    for idx, title, doc_id in DOCS:
        out_name = sanitize_filename(title) + ".md"
        out_path = out_dir / out_name
        print(f"[{idx}] {title} -> {out_name}")

        md = ""
        last_err = None
        for attempt in range(1, 4):
            try:
                md = fetch_doc_markdown(tat, doc_id)
                last_err = None
                break
            except Exception as e:
                last_err = e
                time.sleep(2 * attempt)
        if last_err is not None and not md:
            err_path = out_dir / (out_name + ".error.json")
            err_path.write_text(
                json.dumps({"error": str(last_err)}, ensure_ascii=False, indent=2),
                encoding="utf-8",
            )
            print(f"  ERROR: {last_err} (saved .error.json)")
            continue

        # Download embedded images via <image token="..."/> tags.
        # If the app doesn't have `docs:document.media:download`, this step will fail.
        image_tokens = re.findall(r'<image[^>]*token="([^"]+)"', md)
        if image_tokens:
            unique_tokens = list(dict.fromkeys(image_tokens))  # stable unique
            print(f"  images found: {len(unique_tokens)}")
            token_to_asset: dict[str, str] = {}

            for t_i, token in enumerate(unique_tokens):
                try:
                    asset_prefix = f"{idx:02d}-{sanitize_filename(title)}-img-{t_i}"
                    # 如果之前已成功下载过同名文件，直接复用，避免重复请求。
                    existing = sorted(assets_dir.glob(asset_prefix + ".*"))
                    if existing and existing[0].is_file() and existing[0].stat().st_size > 0:
                        token_to_asset[token] = f"assets/{existing[0].name}"
                        continue

                    raw = fetch_file_by_resource_token(tat, token)
                    ext = detect_image_ext(raw)
                    asset_name = asset_prefix + ext
                    asset_path = assets_dir / asset_name
                    asset_path.write_bytes(raw)
                    token_to_asset[token] = f"assets/{asset_name}"
                    time.sleep(0.5)
                except PermissionError as pe:
                    # Permission issue: stop downloading images for all remaining docs
                    msg = str(pe)
                    print(f"  PERMISSION ERROR while downloading images: {msg[:250]}")
                    token_to_asset = {}
                    break
                except Exception as e:
                    print(f"  image download failed token={token[:8]}... err={e}")
                    continue

            # Replace <image token="..."/> with Markdown image if we have assets.
            if token_to_asset:
                def repl(m: re.Match) -> str:
                    token = m.group(1)
                    asset_ref = token_to_asset.get(token)
                    if not asset_ref:
                        return m.group(0)
                    # keep it simple: embed as markdown image
                    # Avoid spaces in alt text to prevent parser issues.
                    return f"![]({asset_ref})"

                # 注意：前面的 `[^>]*` 必须使用非贪婪，否则可能把结尾的 `/` 吃掉导致不匹配。
                md = re.sub(
                    r'<image\b[^>]*?\btoken="([^"]+)"[^>]*?/>',
                    repl,
                    md,
                )

        out_path.write_text(md, encoding="utf-8")
        print(f"  OK: wrote {len(md)} chars")
        time.sleep(0.8)

    return 0


if __name__ == "__main__":
    raise SystemExit(main())

