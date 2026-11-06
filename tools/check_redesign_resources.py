"""Check explicit coverage of the redesign in the ten supported languages.

Uses only Python's standard library. Other upstream locales keep the established
English fallback, so the narrowly scoped MissingTranslation suppression does not
replace this coverage check. SPDX-License-Identifier: GPL-3.0-or-later
"""
from pathlib import Path
import re
import xml.etree.ElementTree as ET

resources = Path(__file__).resolve().parents[1] / "app/src/main/res"
folders = ["values", "values-it", "values-es", "values-pt-rBR", "values-fr",
           "values-de", "values-zh-rCN", "values-ja", "values-ko", "values-ru"]

def strings(folder):
    return {element.attrib["name"]: "".join(element.itertext())
            for element in ET.parse(resources / folder / "redesign_strings.xml").getroot()
            if element.tag == "string"}

english = strings("values")
for folder in folders:
    translated = strings(folder)
    assert translated.keys() == english.keys(), f"Incomplete translation: {folder}"
    for key, value in translated.items():
        assert value.strip(), f"Empty translation: {folder}/{key}"
        assert re.findall(r"%\d+\$[dsf]", value) == re.findall(r"%\d+\$[dsf]", english[key]), key
print(f"PASS: {len(english)} strings explicitly present in all {len(folders)} languages.")

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
visible_strings = {key: value for key, value in base.items()
                   if value.tag == "string" and value.get("translatable") != "false"
                   and key != "html_rose"}  # Nonlinguistic ASCII decoration.
visible_arrays = ("timeout_options", "large_timeout_options", "list_size_options")
for folder in folders[1:]:
    localized = all_resources(folder)
    missing = visible_strings.keys() - localized.keys()
    assert not missing, f"Missing interface strings: {folder}: {sorted(missing)}"
    for key in visible_strings:
        assert "".join(localized[key].itertext()).strip(), f"Empty: {folder}/{key}"
    for key in visible_arrays:
        assert key in localized, f"Missing option array: {folder}/{key}"
        assert len(localized[key]) == len(base[key]), f"Option count: {folder}/{key}"
    # Every newly completed message must preserve all of its formatting tokens.
    completion = ET.parse(resources / folder / "translation_completion.xml").getroot()
    for element in completion:
        if element.tag == "string":
            key = element.attrib["name"]
            tokens = lambda e: sorted(re.findall(r"%\d+\$[dsf]", "".join(e.itertext())))
            assert tokens(element) == tokens(base[key]), f"Format arguments: {folder}/{key}"
print(f"PASS: {len(visible_strings)} interface string keys and all three text option arrays "
      "covered in the nine non-English languages.")
