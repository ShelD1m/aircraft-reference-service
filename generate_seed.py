"""Переcоздаёт V2__demo_data.sql с детерминированными учебными UUID.
Требуется только стандартная библиотека Python 3."""
import uuid, json
from pathlib import Path
NAMESPACE=uuid.UUID('735f968a-7ac9-4fd0-aef0-a7b76fdb2c6d')
def uid(x):return str(uuid.uuid5(NAMESPACE,x))
def q(x):return "'"+str(x).replace("'","''")+"'"
def js(x):return q(json.dumps(x,ensure_ascii=False,sort_keys=True))+'::jsonb'
base=Path(__file__).parent
sql=["-- ТОЛЬКО УЧЕБНЫЕ ДАННЫЕ. Не использовать эти длительности как авиационные нормативы.","-- Все UUID фиксированы для воспроизводимых демонстрационных запросов."]
dirs=[('AIRCRAFT_TYPE','AIRCRAFT_TYPE','Типы воздушных судов','Условный справочник типов самолётов'),
 ('AIRCRAFT','AIRCRAFT','Воздушные суда','Вымышленные борта для демонстрации'),
 ('OPERATION_TYPE','OPERATION_TYPE','Типовые операции','Тестовые операции и вымышленные длительности'),
 ('PROCEDURE_TEMPLATE','PROCEDURE_TEMPLATE','Шаблоны наземной подготовки','Демонстрационный шаблон с этапами'),
 ('GROUND_STATUS','GENERIC','Статусы наземных работ','Универсальный справочник значений')]
for code,kind,name,desc in dirs:
 sql.append(f"INSERT INTO directories(id,code,kind,name,description) VALUES ({q(uid('dir:'+code))}::uuid,{q(code)},{q(kind)},{q(name)},{q(desc)});")
for code, *_ in dirs:
 sql.append(f"INSERT INTO reference_versions(id,directory_id,version_number,status,created_by,approved_by,published_at) VALUES ({q(uid('version:'+code+':1'))}::uuid,{q(uid('dir:'+code))}::uuid,1,'APPROVED','demo-editor','demo-supervisor',now());")
def record(kind,version,code,name,desc,data,logical_key=None):
 logical_key=logical_key or kind+':'+code
 sql.append(f"INSERT INTO directory_records(id,logical_id,version_id,code,name,description,data) VALUES ({q(uid('record:'+kind+':'+str(version)+':'+code))}::uuid,{q(uid('logical:'+logical_key))}::uuid,{q(uid('version:'+kind+':'+str(version)))}::uuid,{q(code)},{q(name)},{q(desc)},{js(data)});")
 record_cache[(kind,version,code)]=uid('record:'+kind+':'+str(version)+':'+code)
record_cache={}
record('AIRCRAFT_TYPE',1,'A320','Airbus A320','Учебный пример типа узкофюзеляжного ВС',{'category':'NARROW_BODY','icao_type_designator':'A320','status':'ACTIVE'})
record('AIRCRAFT_TYPE',1,'B738','Boeing 737-800','Учебный пример типа ВС',{'category':'NARROW_BODY','icao_type_designator':'B738','status':'ACTIVE'})
record('AIRCRAFT',1,'DEMO-001','Учебный борт №1','Регистрационный номер вымышленный',{'tail_number':'DEMO-001','aircraft_type_code':'A320','status':'ACTIVE'})
record('AIRCRAFT',1,'DEMO-002','Учебный борт №2','Регистрационный номер вымышленный',{'tail_number':'DEMO-002','aircraft_type_code':'B738','status':'ACTIVE'})
ops=[('INSPECT','Внешний осмотр',900),('REFUEL','Заправка ВС',1200),('BOARDING','Посадка пассажиров',1800),('PUSHBACK','Буксировка на вылет',600)]
for code,name,duration in ops:record('OPERATION_TYPE',1,code,name,'Условная продолжительность, не норматив',{'default_duration_seconds':duration,'status':'ACTIVE'})
record('PROCEDURE_TEMPLATE',1,'PRE_DEPARTURE_A320','Учебная подготовка A320 к вылету','Только для демонстрации API',{
  'aircraft_type_code':'A320','procedure_code':'PRE_DEPARTURE','source_document':'DEMO-ONLY','source_revision':'1.0',
  'note':'Длительности демонстрационные, не для реальной эксплуатации'})
record('GROUND_STATUS',1,'PLANNED','Запланировано','Работы ещё не начаты',{'sort_order':1})
record('GROUND_STATUS',1,'IN_PROGRESS','В процессе','Работы в процессе',{'sort_order':2})
record('GROUND_STATUS',1,'DONE','Завершено','Работы завершены',{'sort_order':3})
proc_record=record_cache[('PROCEDURE_TEMPLATE',1,'PRE_DEPARTURE_A320')]
for n,(operation,duration) in enumerate([('INSPECT',900),('REFUEL',1200),('BOARDING',1800)],start=1):
 sql.append(f"INSERT INTO procedure_stages(id,logical_id,record_id,sequence_number,operation_code,description,expected_duration_seconds,required_role,mandatory) VALUES ("+
 f"{q(uid('stage:1:'+operation))}::uuid,{q(uid('logical:stage:'+operation))}::uuid,{q(proc_record)}::uuid,{n},{q(operation)},"+
 f"{q('Учебный этап (не операционный регламент)')},{duration},'TECHNICIAN',TRUE);")
# Преднастроенный DRAFT v2 для проверки процесса согласования; активная v1 неизменна.
code='OPERATION_TYPE';sql.append(f"INSERT INTO reference_versions(id,directory_id,version_number,status,base_version_id,change_reason,created_by) VALUES ("+
 f"{q(uid('version:'+code+':2'))}::uuid,{q(uid('dir:'+code))}::uuid,2,'DRAFT',"+
 f"{q(uid('version:'+code+':1'))}::uuid,'Учебная проверка обновления нормативов','demo-editor');")
for op,name,duration in ops:
 v=1500 if op=='REFUEL' else duration
 record('OPERATION_TYPE',2,op,name,'Учебное изменение (DRAFT)',{'default_duration_seconds':v,'status':'ACTIVE'})
(base/'src/main/resources/db/migration/V2__demo_data.sql').write_text('\n'.join(sql)+'\n',encoding='utf-8')
(base/'docs/DEMO_IDS.md').write_text('# Фиксированные идентификаторы моковых данных\n\n'+
  '\n'.join([f'- `{code}`: directory ID `{uid("dir:"+code)}`, утверждённая v1 `{uid("version:"+code+":1")}`' for code,*_ in dirs])+
  '\n\nДля проверки согласования используйте заранее созданный DRAFT типа операций v2: `'+uid('version:OPERATION_TYPE:2')+'`.\n',encoding='utf-8')
print('generated',len(sql),'SQL statements')
