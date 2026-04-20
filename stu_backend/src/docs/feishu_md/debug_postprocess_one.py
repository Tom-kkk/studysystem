import re
from pathlib import Path


idx = 21
title = "21 使用EL表达式和JSTL重写学生界面"


def sanitize_filename(s: str) -> str:
    s = s.replace("/", " ").replace("\\", " ")
    s = s.replace(":", " ").replace("*", " ").replace("?", " ")
    s = s.replace("\"", " ").replace("<", " ").replace(">", " ")
    s = s.replace("|", " ").strip()
    s = re.sub(r"\s+", " ", s)
    return s


def main() -> None:
    repo_root = Path(r"C:/Users/32145/Desktop/javarainy")
    md_dir = repo_root / "feishu_md"
    assets_dir = md_dir / "assets"

    md_path = None
    for c in md_dir.glob(f"{idx}*.md"):
        if c.name.startswith(str(idx) + " "):
            md_path = c
            break
    if md_path is None:
        print("md not found")
        return

    md = md_path.read_text(encoding="utf-8")
    token_re = re.compile(r'<image[^>]*token="([^"]+)"')
    tokens = token_re.findall(md)
    unique_tokens = list(dict.fromkeys(tokens))
    print("md", md_path.name, "tokens", len(tokens), "unique", len(unique_tokens))

    # check assets prefix matches
    for t_i, token in enumerate(unique_tokens[:5]):
        asset_prefix = f"{idx:02d}-{sanitize_filename(title)}-img-{t_i}"
        matches = sorted(assets_dir.glob(asset_prefix + ".*"))
        print("  t_i", t_i, "token_head", token[:8], "asset_prefix", asset_prefix, "matches", [m.name for m in matches[:3]])


if __name__ == "__main__":
    main()

