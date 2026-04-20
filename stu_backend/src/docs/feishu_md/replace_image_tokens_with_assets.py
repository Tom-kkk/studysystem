import re
from pathlib import Path


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
    s = s.replace("/", " ").replace("\\", " ")
    s = s.replace(":", " ").replace("*", " ").replace("?", " ")
    s = s.replace("\"", " ").replace("<", " ").replace(">", " ")
    s = s.replace("|", " ").strip()
    s = re.sub(r"\s+", " ", s)
    return s


def find_asset_for(prefix: str, assets_dir: Path) -> str | None:
    # Choose the first match by lexicographic order.
    matches = sorted(assets_dir.glob(prefix + ".*"))
    if not matches:
        return None
    return matches[0].name


def main() -> int:
    repo_root = Path(r"C:/Users/32145/Desktop/javarainy")
    md_dir = repo_root / "feishu_md"
    assets_dir = md_dir / "assets"

    img_tag_re = re.compile(r"<image\b[^>]*\btoken=\"([^\"]+)\"[^>]*?/?>")
    token_re = re.compile(r'<image[^>]*token="([^"]+)"')

    replaced_files = 0
    total_replaced = 0

    for idx, title, _doc_id in DOCS:
        # Robustly find md by numeric prefix (filenames are created by the export script).
        # Example: "12 教学班ClassDAO.md" / "21 使用EL表达式和JSTL重写学生界面.md"
        md_path = None
        candidates = sorted(md_dir.glob(f"{idx}*.md"))
        for c in candidates:
            if c.name.startswith(str(idx) + " "):
                md_path = c
                break
        if md_path is None and candidates:
            # Fallback: first candidate
            md_path = candidates[0]
        if md_path is None or not md_path.exists():
            continue

        md = md_path.read_text(encoding="utf-8")

        tokens = token_re.findall(md)
        if not tokens:
            continue
        unique_tokens = list(dict.fromkeys(tokens))

        token_to_asset: dict[str, str] = {}
        for t_i, token in enumerate(unique_tokens):
            asset_prefix = f"{idx:02d}-{sanitize_filename(title)}-img-{t_i}"
            asset_file = find_asset_for(asset_prefix, assets_dir)
            if asset_file:
                token_to_asset[token] = f"assets/{asset_file}"

        if not token_to_asset:
            continue

        def repl(m: re.Match) -> str:
            token = m.group(1)
            asset_ref = token_to_asset.get(token)
            if not asset_ref:
                return m.group(0)
            return f"![]({asset_ref})"

        new_md = img_tag_re.sub(repl, md)
        if new_md != md:
            replaced_files += 1
            total_replaced += new_md.count("![](") - md.count("![](")
            md_path.write_text(new_md, encoding="utf-8")
            print(f"[{idx}] {title}: token_images={len(unique_tokens)}, mapped={len(token_to_asset)}")

    print(f"done: replaced_files={replaced_files}, total_new_images_est={total_replaced}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

