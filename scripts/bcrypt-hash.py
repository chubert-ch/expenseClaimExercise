#!/usr/bin/env python3
import sys
import bcrypt

if len(sys.argv) != 2:
    print("Usage: python3 bcrypt-hash.py <password>")
    sys.exit(1)

print(bcrypt.hashpw(sys.argv[1].encode(), bcrypt.gensalt()).decode())
