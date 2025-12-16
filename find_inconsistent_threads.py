import json
from pathlib import Path


def main() -> None:
    root = Path("Sample_SO_data")
    if not root.exists():
        print("Sample_SO_data directory not found")
        return

    for path in sorted(root.glob("thread_*.json")):
        try:
            with path.open(encoding="utf-8") as f:
                data = json.load(f)
        except Exception as e:
            print(f"[ERROR] 读取失败: {path.name} - {e}")
            continue

        question = data.get("question", {})
        # 兼容字段名 is_answered / answered
        answered = question.get("is_answered")
        if answered is None:
            answered = question.get("answered")

        answers = data.get("answers") or []

        # 条件：问题标记已回答，但没有任何 answer
        if answered is True and len(answers) == 0:
            print(path.name)


if __name__ == "__main__":
    main()


