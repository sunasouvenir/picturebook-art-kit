import sys, zipfile
z = zipfile.ZipFile(sys.argv[1]); bad = []
for i in z.infolist():
    if i.compress_type == zipfile.ZIP_STORED:
        z.fp.seek(i.header_offset); h = z.fp.read(30)
        n = int.from_bytes(h[26:28], 'little'); e = int.from_bytes(h[28:30], 'little')
        if (i.header_offset + 30 + n + e) % 4: bad.append(i.filename)
print('aligned' if not bad else 'MISALIGNED ' + ' '.join(bad))
