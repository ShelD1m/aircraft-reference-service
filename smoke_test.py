#!/usr/bin/env python3
"""Мини-интеграционный тест (без библиотек): требует запущенного сервера.
Создаёт уникальный GENERIC-справочник, не трогая учебные справочники/версии."""
from urllib.request import Request,urlopen
from urllib.error import HTTPError,URLError
from uuid import uuid4
import json,sys

BASE='http://localhost:8080/api/v1/references'
step=0

def req(method,path,role='TECHNICIAN',body=None,key=None):
    global step
    step+=1
    headers={'X-Demo-Role':role,'Accept':'application/json'}
    data=None
    if body is not None:
        headers['Content-Type']='application/json'
        data=json.dumps(body).encode()
    if key:
        headers['Idempotency-Key']=key
    r=Request(BASE+path,method=method,headers=headers,data=data)
    try:
        with urlopen(r,timeout=15) as f:
            raw=f.read();return f.status, json.loads(raw) if raw else None
    except HTTPError as e:
        raw=e.read();return e.code,json.loads(raw) if raw else None


def check(status,expected,message):
    if status!=expected:raise AssertionError(f'{message}: expected {expected}, got {status}')
    print(f'  ✓ {message} ({status})')

try:
    print('NSI demo smoke test')
    st,items=req('GET','/aircraft-types')
    check(st,200,'Справочник типов ВС')
    assert any(i['code']=='A320' for i in items['items']), 'Отсутствует A320'
    st,template=req('GET','/procedure-templates/resolve?aircraft_type_code=A320&procedure_code=PRE_DEPARTURE')
    check(st,200,'Поиск опубликованного шаблона')
    assert len(template['stages'])==3
    st,_=req('GET','/sync/manifest');check(st,200,'Манифест')

    tag=uuid4().hex[:10].upper()
    code='SMOKE_'+tag
    create={'code':code,'kind':'GENERIC','name':'Автотест НСИ','description':'Создано smoke_test.py'}
    key='smoke-create-'+tag
    st,created=req('POST','/directories',role='SUPERVISOR',body=create,key=key)
    check(st,201,'Создание справочника')
    directory=created['directory_id'];version=created['draft_version_id']
    path=f'/directories/{directory}/versions/{version}'

    st,repeat=req('POST','/directories',role='SUPERVISOR',body=create,key=key)
    check(st,201,'Идемпотентный повтор')
    assert repeat==created,'Повтор должен вернуть исходный ответ'

    st,_=req('POST',path+'/records',role='TECHNICIAN',body={
        'code':'ITEM','name':'Пример','data':{'value':1}},key='smoke-denied-'+tag)
    check(st,403,'Запрет изменения техником')

    st,record=req('POST',path+'/records',role='REFERENCE_EDITOR',
                  body={'code':'ITEM','name':'Пример','data':{'value':1}},key='smoke-record-'+tag)
    check(st,201,'Добавление записи в DRAFT')

    st,review=req('POST',path+'/submit',role='REFERENCE_EDITOR',key='smoke-submit-'+tag)
    check(st,200,'Отправка на REVIEW')
    assert review['status']=='REVIEW'

    st,_=req('POST',path+'/approve',role='REFERENCE_EDITOR',key='smoke-forbidden-'+tag)
    check(st,403,'Запрет approve редактором')

    st,approved=req('POST',path+'/approve',role='SUPERVISOR',key='smoke-approve-'+tag)
    check(st,200,'Утверждение супервайзером')
    assert approved['status']=='APPROVED'

    st,vs=req('GET','/versions/'+version+'/status')
    check(st,200,'Проверка активной версии')
    assert vs['usable_for_new_checklist'] is True

    st,manifest=req('GET','/sync/manifest')
    check(st,200,'Получение обновлённого манифеста')
    assert any(i['directory_code']==code for i in manifest['items'])

    st,_=req('GET','/audit',role='SUPERVISOR')
    check(st,200,'Журнал аудита')
    print('\nPASS: ключевые сценарии пройдены. Создан справочник',code)
except (AssertionError,URLError,TimeoutError,ValueError) as ex:
    print('FAIL:',ex,file=sys.stderr)
    print('Проверьте docker compose ps и docker compose logs api',file=sys.stderr)
    sys.exit(1)
