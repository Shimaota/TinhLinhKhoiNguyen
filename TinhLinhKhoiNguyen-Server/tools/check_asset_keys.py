"""Verify that the AES keys in server.properties decrypt every PNG in DataGame/ver_*/res_*.zip.

The client decrypts with AES/CBC/PKCS5Padding, IV = first 16 bytes of the file
(core/.../util/AesDecryptor.java). icon/, ui/ and objects/ use assets.textureDecryptKey;
skeleton atlas pages (skeletons/) use assets.atlasDecryptKey. Some files are stored unencrypted.

Usage:  pip install pycryptodome
        python tools/check_asset_keys.py [path/to/server.properties] [path/to/DataGame]
"""
import collections
import glob
import os
import sys
import zipfile

from Crypto.Cipher import AES
from Crypto.Util.Padding import unpad

PNG = b"\x89PNG\r\n\x1a\n"
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))


def read_props(path):
    props = {}
    with open(path, encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if line and not line.startswith("#") and "=" in line:
                k, v = line.split("=", 1)
                props[k.strip()] = v.strip()
    return props


def decrypt(data, key):
    try:
        out = unpad(AES.new(key, AES.MODE_CBC, data[:16]).decrypt(data[16:]), 16)
        return out.startswith(PNG)
    except ValueError:
        return False


def main():
    props_path = sys.argv[1] if len(sys.argv) > 1 else os.path.join(ROOT, "TLKN-Server", "server.properties")
    if not os.path.isfile(props_path):
        props_path = os.path.join(ROOT, "TLKN-Server", "server.properties.example")
    datagame = sys.argv[2] if len(sys.argv) > 2 else os.path.join(ROOT, "DataGame")

    props = read_props(props_path)
    tex = bytes.fromhex(props["assets.textureDecryptKey"])
    atlas = bytes.fromhex(props["assets.atlasDecryptKey"])
    print("keys from:", props_path)

    stats = collections.Counter()
    bad = []
    for z in sorted(glob.glob(os.path.join(datagame, "ver_*", "*.zip"))):
        with zipfile.ZipFile(z) as zf:
            for name in zf.namelist():
                if not name.endswith(".png"):
                    continue
                data = zf.read(name)
                top = name.split("/")[0]
                if data.startswith(PNG):
                    kind = "plain"
                elif decrypt(data, tex):
                    kind = "textureKey"
                elif decrypt(data, atlas):
                    kind = "atlasKey"
                else:
                    kind = "FAILED"
                    bad.append(f"{os.path.basename(z)}:{name}")
                stats[(top, kind)] += 1

    for (top, kind), n in sorted(stats.items()):
        print(f"  {top:<10} {kind:<11} {n}")
    if bad:
        print(f"FAILED: {len(bad)} file(s) could not be decrypted with either key:")
        for b in bad[:30]:
            print("   ", b)
        sys.exit(1)
    print("OK: every PNG is plain or decrypts with the configured keys.")


if __name__ == "__main__":
    main()
