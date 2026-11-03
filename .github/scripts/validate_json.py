"""Checks that every JSON file under the given folders is valid and has no duplicate keys.

Errors are printed as GitHub Actions annotations, so they show up on the changed files in a pull request.
"""

import json
import sys
from pathlib import Path


class DuplicateKeyError(ValueError):
	pass


def reject_duplicates(pairs):
	result = {}
	for key, value in pairs:
		if key in result:
			raise DuplicateKeyError(f"duplicate key '{key}'")
		result[key] = value
	return result


def main(folders):
	checked = 0
	errors = 0
	for folder in folders:
		for path in sorted(Path(folder).rglob("*.json")):
			checked += 1
			try:
				json.loads(path.read_text(encoding="utf-8"), object_pairs_hook=reject_duplicates)
			except json.JSONDecodeError as e:
				errors += 1
				print(f"::error file={path},line={e.lineno},col={e.colno}::Invalid JSON: {e.msg}")
			except (DuplicateKeyError, UnicodeDecodeError) as e:
				errors += 1
				print(f"::error file={path}::{e}")

	print(f"Checked {checked} JSON files, found {errors} problem(s).")
	return 1 if errors else 0


if __name__ == "__main__":
	sys.exit(main(sys.argv[1:]))
