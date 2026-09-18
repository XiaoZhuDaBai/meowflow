# -*- coding: utf-8 -*-
import sys
sys.stdout.reconfigure(encoding='utf-8')

fpath = r'd:\Code\喵流\backend\meowflow\meowflow-template\src\main\java\com\meowflow\template\catalog\BuiltinTemplateCatalog.java'

with open(fpath, 'r', encoding='utf-8') as f:
    content = f.read()

lines = content.split('\n')

# Check lines 215-230 to find line with issue
for i in range(214, 235):
    if i < len(lines):
        line = lines[i]
        # Show first 80 chars and the column 14-30 char-by-char
        snippet = line[:80]
        print(f'L{i+1:4d} (len={len(line)}): {snippet}')

# Also check for triple-quote string blocks " or '
print('--- triple quote count:')
text = content
print('  """ count:', text.count('"""'))
print('  \'\'\' count:', text.count("'''"))
