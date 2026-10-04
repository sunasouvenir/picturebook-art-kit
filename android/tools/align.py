"""압축하지 않은 항목(resources.arsc 등)을 4바이트 경계에 맞춰 다시 써요 (zipalign 대신)."""
import sys, zipfile
src, dst = sys.argv[1], sys.argv[2]
zin = zipfile.ZipFile(src)
with zipfile.ZipFile(dst, 'w') as zout:
    for info in zin.infolist():
        data = zin.read(info.filename)
        ni = zipfile.ZipInfo(info.filename, date_time=(2026, 10, 4, 0, 0, 0))
        ni.external_attr = info.external_attr
        stored = info.filename == 'resources.arsc' or info.filename.endswith('.png')
        ni.compress_type = zipfile.ZIP_STORED if stored else zipfile.ZIP_DEFLATED
        if stored:
            off = zout.fp.tell() + 30 + len(ni.filename.encode('utf-8'))
            ni.extra = b'\0' * ((4 - off % 4) % 4)
        zout.writestr(ni, data)
