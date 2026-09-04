#!/usr/bin/env python3
"""Check Kotlin sources against the repository KDoc conventions (see CLAUDE.md, "KDoc conventions").

Usage:
    check_kdoc.py PATH [PATH ...] [--max-line N] [--info]
    check_kdoc.py --strip FILE          # print FILE without KDoc blocks (to diff code only)

Exit code 1 when at least one violation is reported.
Reported codes:
    MISSING_DOC     public class/interface/object/fun/typealias without KDoc
    MISSING_SINCE   public declaration KDoc without "@since %CURRENT_VERSION%"
    PROP_DOC        KDoc block placed directly above a property (must be @property in the class KDoc)
    PROP_UNDOC      public property of a class not covered by @property in the class KDoc
    OVERRIDE_DOC    KDoc above an override (overrides inherit the base KDoc)
    OVERRIDE_PROP   @property for an "override val" constructor property (the base type documents it)
    NONPUBLIC_SINCE @since inside KDoc of a private/internal/protected member
    TYPE_PARAM      type parameter without "@param Name" in the KDoc
    SUBTYPE_LINK    KDoc of a type links [Sub] where Sub extends/implements that type
    LONG_LINE       KDoc line longer than --max-line (Detekt MaxLineLength)
    NONPUBLIC_DOC   (info only, shown with --info) handwritten KDoc on a non-public member
"""
import argparse
import pathlib
import re
import sys

MODIFIERS = r"(?:(?:public|abstract|sealed|data|value|open|inline|enum|annotation|inner|suspend|operator|infix|external|const|final|tailrec|override|private|internal|protected)\s+)*"
DECL_RE = re.compile(rf"^(?P<indent>\s*)(?P<mods>{MODIFIERS})(?P<kind>class|interface|object|fun|typealias)\s+(?P<rest>.*)$")
PROP_RE = re.compile(rf"^(?P<indent>\s*)(?P<mods>{MODIFIERS})(?P<kind>val|var)\s+(?P<name>[A-Za-z_][A-Za-z0-9_]*)")
CTOR_PROP_RE = re.compile(rf"^\s*(?P<mods>{MODIFIERS})(?P<kind>val|var)\s+(?P<name>[A-Za-z_][A-Za-z0-9_]*)\s*:")
NONPUBLIC = ("private", "internal", "protected")


def strip_kdoc(text: str) -> str:
    return re.sub(r"[ \t]*/\*\*.*?\*/[ \t]*\n?", "", text, flags=re.S)


def kdoc_before(lines, i):
    """Return (start, end) line indexes of the KDoc block right above line i (annotations allowed between), or None."""
    j = i - 1
    while j >= 0 and lines[j].strip().startswith("@"):
        j -= 1
    if j < 0 or lines[j].strip() != "*/":
        return None
    k = j
    while k >= 0 and not lines[k].lstrip().startswith("/**"):
        k -= 1
    return (k, j) if k >= 0 else None


def is_nonpublic(mods: str) -> bool:
    return any(m in mods.split() for m in NONPUBLIC)


def strip_parens(s: str) -> str:
    out, depth = [], 0
    for ch in s:
        if ch == "(":
            depth += 1
        elif ch == ")":
            depth -= 1
        elif depth == 0:
            out.append(ch)
    return "".join(out)


def split_top(s: str, sep: str):
    parts, depth, cur = [], 0, []
    for ch in s:
        if ch in "<(":
            depth += 1
        elif ch in ">)":
            depth -= 1
        if ch == sep and depth == 0:
            parts.append("".join(cur)); cur = []
        else:
            cur.append(ch)
    parts.append("".join(cur))
    return parts


def declaration_header(lines, i):
    """Join lines from i until the header ends (first '{' at depth 0, or blank line / '=')."""
    buf, depth = [], 0
    for j in range(i, min(i + 15, len(lines))):
        line = lines[j]
        buf.append(line.strip())
        for ch in line:
            if ch in "(<":
                depth += 1
            elif ch in ")>":
                depth -= 1
        if depth <= 0 and ("{" in line or line.rstrip().endswith("=") or line.strip() == "" or
                           (j > i and not line.strip().startswith((":", ")", ",")) and not lines[j - 1].rstrip().endswith((",", ":", "(")))):
            break
    return " ".join(buf)


def parse_type_decl(header: str):
    """Return (name, type_params, supertypes) for a class/interface/object header."""
    m = re.search(r"\b(class|interface|object)\s+([A-Za-z_][A-Za-z0-9_]*|`[^`]+`)\s*(<[^>]*(?:<[^>]*>[^>]*)*>)?", header)
    if not m:
        return None
    name = m.group(2).strip("`")
    tparams = m.group(3)
    names = []
    if tparams:
        for tp in split_top(tparams[1:-1], ","):
            tp = tp.strip()
            tp = re.sub(r"^(out|in)\s+", "", tp)
            names.append(tp.split(":")[0].strip())
    after = header[m.end():]
    after = strip_parens(after)
    after = after.split("{")[0]
    supers = []
    if ":" in after:
        sup = after.split(":", 1)[1]
        for part in split_top(sup, ","):
            part = part.strip()
            sm = re.match(r"([A-Za-z_][A-Za-z0-9_.]*)", part)
            if sm:
                supers.append(sm.group(1).split(".")[-1])
    return name, names, supers


def fun_type_params(header: str):
    m = re.search(r"\bfun\s*<([^>]*(?:<[^>]*>[^>]*)*)>", header)
    if not m:
        return []
    out = []
    for tp in split_top(m.group(1), ","):
        tp = re.sub(r"^\s*(out|in|reified)\s+", "", tp.strip())
        out.append(tp.split(":")[0].strip())
    return out


def class_body_range(lines, i):
    """Return index range (start, end) of the body of the class declared at line i, or None if bodyless."""
    depth, started = 0, False
    for j in range(i, len(lines)):
        for ch in lines[j]:
            if ch == "{":
                depth += 1; started = True
            elif ch == "}":
                depth -= 1
                if started and depth == 0:
                    return (i, j)
        if not started and j > i and lines[j].strip() == "":
            return None
    return None


def check_file(path: pathlib.Path, max_line: int, info: bool):
    text = path.read_text()
    lines = text.split("\n")
    problems = []
    subtypes = {}   # parent -> set(child)
    type_docs = {}  # type name -> (kdoc_start, kdoc_end)

    def report(code, lineno, msg):
        problems.append((code, lineno + 1, msg))

    # ---- pass 1: declarations ----
    for i, line in enumerate(lines):
        stripped = line.strip()
        if stripped.startswith(("//", "*", "/*")):
            continue
        d = DECL_RE.match(line)
        p = PROP_RE.match(line) if not d else None
        if d:
            mods, kind, rest = d.group("mods"), d.group("kind"), d.group("rest")
            if kind == "object" and (rest.startswith(":") or rest.startswith("{")):
                continue  # anonymous object
            if stripped.startswith("companion object"):
                continue
            header = declaration_header(lines, i)
            doc = kdoc_before(lines, i)
            nonpublic = is_nonpublic(mods)
            override = "override" in mods.split()
            if kind in ("class", "interface", "object"):
                parsed = parse_type_decl(header)
                if parsed:
                    name, tps, supers = parsed
                    for s in supers:
                        subtypes.setdefault(s, set()).add(name)
                    if doc:
                        type_docs[name] = doc
                else:
                    tps = []
            else:
                tps = fun_type_params(header) if kind == "fun" else []
            if override:
                if doc:
                    report("OVERRIDE_DOC", i, "override has its own KDoc; overrides inherit the base KDoc")
                continue
            if nonpublic:
                if doc:
                    block = "\n".join(lines[doc[0]:doc[1] + 1])
                    if "@since" in block:
                        report("NONPUBLIC_SINCE", i, "non-public member KDoc must not carry @since")
                    elif info:
                        report("NONPUBLIC_DOC", i, "handwritten KDoc on non-public member (allowed)")
                continue
            if not doc:
                report("MISSING_DOC", i, f"public {kind} without KDoc")
                continue
            block = "\n".join(lines[doc[0]:doc[1] + 1])
            if "@since %CURRENT_VERSION%" not in block:
                report("MISSING_SINCE", i, "public declaration KDoc without '@since %CURRENT_VERSION%'")
            for tp in tps:
                if not re.search(rf"@param\s+{re.escape(tp)}\s", block):
                    report("TYPE_PARAM", i, f"type parameter '{tp}' is not documented with @param")
            # properties covered by @property
            if kind in ("class", "interface"):
                documented = set(re.findall(r"@property\s+([A-Za-z_][A-Za-z0-9_]*)", block))
                ctor_props = []
                for cm in CTOR_PROP_RE.finditer(strip_parens_keep_inner(header)):
                    if "override" in cm.group("mods").split():
                        if cm.group("name") in documented:
                            report("OVERRIDE_PROP", i, f"@property '{cm.group('name')}' documents an override; the base type documents it")
                    elif not is_nonpublic(cm.group("mods")):
                        ctor_props.append(cm.group("name"))
                body = class_body_range(lines, i)
                body_props = []
                if body:
                    # Only members declared directly in the class body (one indent level deeper than the class);
                    # deeper lines are locals inside functions, nested classes or companion objects.
                    member_indent = len(d.group("indent")) + 4
                    for j in range(body[0] + 1, body[1]):
                        pm = PROP_RE.match(lines[j])
                        if not pm or len(pm.group("indent")) != member_indent:
                            continue
                        if is_nonpublic(pm.group("mods")) or "override" in pm.group("mods").split():
                            continue
                        body_props.append(pm.group("name"))
                for name in ctor_props + body_props:
                    if name not in documented:
                        report("PROP_UNDOC", i, f"public property '{name}' is not documented with @property in the class KDoc")
        elif p:
            doc = kdoc_before(lines, i)
            if doc:
                report("PROP_DOC", i, f"KDoc above property '{p.group('name')}'; document it with @property in the class KDoc")
        # long KDoc lines
        if stripped.startswith(("/**", "*")) and len(line) > max_line:
            report("LONG_LINE", i, f"KDoc line longer than {max_line} characters")

    # ---- pass 2: parent -> subtype links ----
    for parent, (s, e) in type_docs.items():
        block = "\n".join(lines[s:e + 1])
        for child in sorted(subtypes.get(parent, ())):
            if re.search(rf"\[{re.escape(child)}\]", block):
                report("SUBTYPE_LINK", s, f"KDoc of '{parent}' links its subtype [{child}]")

    return sorted(problems, key=lambda t: t[1])


def strip_parens_keep_inner(header: str) -> str:
    """Return the primary-constructor parameter list of a class header (text inside the first top-level parens)."""
    depth, start = 0, None
    for idx, ch in enumerate(header):
        if ch == "(":
            if depth == 0:
                start = idx
            depth += 1
        elif ch == ")":
            depth -= 1
            if depth == 0 and start is not None:
                inner = header[start + 1:idx]
                return "\n".join(x.strip() for x in split_top(inner, ","))
    return ""


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("paths", nargs="*")
    ap.add_argument("--max-line", type=int, default=140)
    ap.add_argument("--info", action="store_true", help="also report informational findings")
    ap.add_argument("--strip", metavar="FILE", help="print FILE with KDoc blocks removed and exit")
    args = ap.parse_args()

    if args.strip:
        sys.stdout.write(strip_kdoc(pathlib.Path(args.strip).read_text()))
        return 0
    if not args.paths:
        ap.error("at least one PATH is required")

    files = []
    for p in args.paths:
        p = pathlib.Path(p)
        files += sorted(p.rglob("*.kt")) if p.is_dir() else [p]

    total = 0
    for f in files:
        for code, lineno, msg in check_file(f, args.max_line, args.info):
            print(f"{f}:{lineno}: {code} {msg}")
            if code != "NONPUBLIC_DOC":
                total += 1
    print(f"{len(files)} files, {total} violations")
    return 1 if total else 0


if __name__ == "__main__":
    sys.exit(main())
