#!/usr/bin/env python3
"""Offline comparison of the leaked Bob hash with the supplied short wordlist."""
import argparse
from pathlib import Path
import bcrypt

parser = argparse.ArgumentParser()
parser.add_argument('hash', help='Bob passwordHash from the admin /users response')
args = parser.parse_args()
for candidate in Path(__file__).with_name('wordlist.txt').read_text().splitlines():
    if bcrypt.checkpw(candidate.encode(), args.hash.encode()):
        print(candidate)
        break
else:
    raise SystemExit('No matching word in solve/wordlist.txt')
