"""Verify explicit interface coverage for the 26 agreed locales (standard library only)."""
from pathlib import Path
import re
import xml.etree.ElementTree as ET

resources = Path(__file__).resolve().parents[1] / "app/src/main/res"
folders = ["values", "values-it", "values-en-rUS", "values-es", "values-pt-rBR",
           "values-fr", "values-de", "values-zh-rCN", "values-ja", "values-ko", "values-ru",
           "values-hi", "values-ar", "values-in", "values-bn", "values-ur", "values-tr",
           "values-vi", "values-th", "values-pl", "values-nl", "values-ro", "values-uk",
           "values-pt-rPT", "values-b+es+419", "values-zh-rTW"]

def all_resources(folder):
    result = {}
    for file in (resources / folder).glob("*.xml"):
        for element in ET.parse(file).getroot():
            name = element.get("name")
            if name and element.tag in ("string", "string-array", "plurals", "array"):
                assert name not in result, f"Duplicate resource: {folder}/{name}"
                result[name] = element
    return result

base = all_resources("values")
visible = {k: v for k, v in base.items() if v.tag == "string"
           and v.get("translatable") != "false" and k != "html_rose"}
tokens = lambda e: sorted(re.findall(r"%(?:\d+\$)?[dsf]|%%", "".join(e.itertext())))
errors = []
for folder in folders:
    localized = all_resources(folder)
    for key, original in visible.items():
        if key not in localized:
            errors.append(f"Missing: {folder}/{key}")
            continue
        value = "".join(localized[key].itertext())
        if not value.strip(): errors.append(f"Empty: {folder}/{key}")
        if tokens(localized[key]) != tokens(original): errors.append(f"Format: {folder}/{key}")
        if "\ufffd" in value: errors.append(f"Replacement character: {folder}/{key}")
    for key in ("timeout_options", "large_timeout_options", "list_size_options"):
        if key not in localized or len(localized[key]) != len(base[key]):
            errors.append(f"Options: {folder}/{key}")
assert not errors, "\n".join(errors)
assert len(base["modern_language_names"]) == len(base["modern_language_values"]) == 26
print(f"PASS: {len(visible)} interface strings and three option arrays in all 26 locales; "
      "no missing keys, empty values, duplicate resources or formatting-token changes.")
