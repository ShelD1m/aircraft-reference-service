#!/usr/bin/env python3
"""HTTP smoke test; creates one uniquely named demo airport (no deletion)."""
import json
import os
from urllib.request import Request, urlopen
from urllib.error import HTTPError
from uuid import uuid4

BASE = os.getenv('NSI_BASE_URL', 'http://localhost:8082').rstrip('/')
TOKEN = os.getenv('NSI_BEARER_TOKEN', '')

def request(method, path, body=None, key=None):
    headers = {'Accept': 'application/json', 'X-Correlation-Id': str(uuid4())}
    if TOKEN:
        headers['Authorization'] = 'Bearer ' + TOKEN
    else:
        headers['X-Demo-Role'] = 'SUPERVISOR'
    if key:
        headers['Idempotency-Key'] = key
    data = None
    if body is not None:
        headers['Content-Type'] = 'application/json'
        data = json.dumps(body).encode()
    try:
        with urlopen(Request(BASE + path, data=data, method=method, headers=headers), timeout=15) as response:
            assert response.headers.get('X-Correlation-Id') == headers['X-Correlation-Id']
            return response.status, json.load(response)
    except HTTPError as error:
        return error.code, json.load(error)

def main():
    path = '/reference/aircraft-types/A320/checklist-template'
    status, template = request('GET', path)
    assert status == 200, (status, template)
    assert template['aircraftTypeId'] == 'A320' and template['items']
    for field in ('templateId', 'templateVersionId'):
        from uuid import UUID
        UUID(template[field])
    status, _ = request('GET', '/reference/aircraft-types/UNKNOWN_TYPE/checklist-template')
    assert status == 404, status
    key = 'smoke-' + uuid4().hex
    import random, string
    body = {'iata_code': ''.join(random.choices(string.ascii_uppercase, k=3)),
            'name': 'Smoke test airport', 'city': 'Test city', 'country': 'Test country'}
    path = '/api/v1/references/airports'
    status, created = request('POST', path, body, key)
    assert status == 201, (status, created)
    status, repeated = request('POST', path, body, key)
    assert status == 201 and repeated == created, (status, repeated)
    print('PASS: CORE template, unknown type, correlation, airport create and idempotency replay')

if __name__ == '__main__':
    main()
