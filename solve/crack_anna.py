#!/usr/bin/env python3
"""Optional offline comparison of Anna's leaked hash with the short wordlist."""
import argparse
from pathlib import Path
import bcrypt

parser = argparse.ArgumentParser()
parser.add_argument('hash', help='Anna passwordHash from the debug /users response')
args = parser.parse_args()
for candidate in Path(__file__).with_name('wordlist.txt').read_text().splitlines():
    if bcrypt.checkpw(candidate.encode(), args.hash.encode()):
        print(candidate)
        break
else:
    raise SystemExit('No matching word in solve/wordlist.txt')
