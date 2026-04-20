import re
import sys
from pathlib import Path

import cv2
from rapidocr_onnxruntime import RapidOCR


def _clean_line(s: str) -> str:
    s = s.replace("\u200b", "").replace("\ufeff", "")
    s = re.sub(r"\s+", " ", s).strip()
    return s


def ocr_lines(image_path: Path) -> list[str]:
    img = cv2.imread(str(image_path))
    if img is None:
        raise RuntimeError(f"无法读取图片: {image_path}")

    engine = RapidOCR()
    result, _ = engine(img)
    # result: List[[box, text, score], ...]
    items: list[tuple[float, float, str]] = []
    for box, text, score in (result or []):
        if not text:
            continue
        # box: [[x1,y1],[x2,y2],[x3,y3],[x4,y4]]
        ys = [p[1] for p in box]
        xs = [p[0] for p in box]
        y = float(sum(ys) / len(ys))
        x = float(sum(xs) / len(xs))
        t = _clean_line(str(text))
        if t:
            items.append((y, x, t))

    items.sort(key=lambda t: (t[0], t[1]))

    # 简单按 y 聚类成“行”
    lines: list[str] = []
    cur_y: float | None = None
    cur_parts: list[str] = []

    def flush():
        nonlocal cur_parts
        if cur_parts:
            line = _clean_line(" ".join(cur_parts))
            if line:
                lines.append(line)
        cur_parts = []

    for y, x, t in items:
        if cur_y is None:
            cur_y = y
            cur_parts = [t]
            continue
        if abs(y - cur_y) <= 12:  # 经验阈值，适配常见字号
            cur_parts.append(t)
        else:
            flush()
            cur_y = y
            cur_parts = [t]
    flush()
    return lines


def lines_to_md(title: str, lines: list[str]) -> str:
    out: list[str] = [f"# {title}", ""]
    for line in lines:
        # 粗略识别编号列表
        m = re.match(r"^(\d+)[\.\、]\s*(.+)$", line)
        if m:
            out.append(f"{m.group(1)}. {m.group(2)}")
            continue
        # 章节标题（数字开头 + 空格 + 中文/英文）
        if re.match(r"^\d+\s+\S+", line) and len(line) <= 40:
            out.append("")
            out.append(f"## {line}")
            out.append("")
            continue
        out.append(line)
        out.append("")
    return "\n".join(out).rstrip() + "\n"


def main() -> int:
    if len(sys.argv) < 4:
        print("用法: python ocr_image_to_md.py <title> <image_path> <out_md_path>", file=sys.stderr)
        return 2

    title = sys.argv[1]
    image_path = Path(sys.argv[2]).expanduser().resolve()
    out_md_path = Path(sys.argv[3]).expanduser().resolve()
    out_md_path.parent.mkdir(parents=True, exist_ok=True)

    lines = ocr_lines(image_path)
    md = lines_to_md(title, lines)
    out_md_path.write_text(md, encoding="utf-8")
    print(f"OK: {out_md_path} (lines={len(lines)})")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

