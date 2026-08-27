#!/usr/bin/env python3
"""Repackage the aapt2-linked APK with proper zip alignment.

Reads the base APK (binary AndroidManifest.xml, resources.arsc, res/, assets/)
plus classes.dex, and writes a new zip where every STORED entry's data starts
at a 4-byte boundary (resources.arsc and classes.dex are stored uncompressed,
as required for modern Android). Deflated entries need no alignment.

Usage: pack.py <base.apk> <classes.dex> <out.apk>
"""
import sys
import zipfile

STORE_SUFFIXES = (".arsc", ".dex", ".png", ".jpg", ".webp", ".gif", ".wav", ".ogg", ".mp3", ".mp4")
ALIGN = 4


def main(base_apk: str, dex: str, out_apk: str) -> None:
    entries = []  # (name, data)
    with zipfile.ZipFile(base_apk) as zin:
        for info in zin.infolist():
            entries.append((info.filename, zin.read(info.filename)))
    with open(dex, "rb") as fh:
        entries.append(("classes.dex", fh.read()))

    # deterministic, install-friendly ordering
    def order(item):
        name = item[0]
        pri = {"AndroidManifest.xml": 0, "resources.arsc": 1, "classes.dex": 2}
        return (pri.get(name, 3), name)

    entries.sort(key=order)

    zout = zipfile.ZipFile(out_apk, "w")
    for name, data in entries:
        store = name.endswith(STORE_SUFFIXES)
        zi = zipfile.ZipInfo(name, date_time=(2009, 1, 1, 0, 0, 0))
        zi.compress_type = zipfile.ZIP_STORED if store else zipfile.ZIP_DEFLATED
        zi.create_system = 0
        zi.external_attr = 0o644 << 16
        if store:
            # local file header = 30 bytes + name + extra; pad extra so the
            # entry *data* lands on a 4-byte boundary
            offset = zout.fp.tell()
            header = 30 + len(name.encode("utf-8"))
            pad = (ALIGN - (offset + header) % ALIGN) % ALIGN
            if pad:
                zi.extra = b"\x00" * pad
        zout.writestr(zi, data)
    zout.close()
    print(f"packed {out_apk}: {len(entries)} entries")


if __name__ == "__main__":
    if len(sys.argv) != 4:
        sys.exit(__doc__)
    main(sys.argv[1], sys.argv[2], sys.argv[3])
