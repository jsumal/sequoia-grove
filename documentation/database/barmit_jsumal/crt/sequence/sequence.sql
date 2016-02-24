
/* Employee ID sequence */
create sequence bajs_employee_id_sequence
minvalue 1
maxvalue 99999
start with 1
increment by 1
cache 20;

/* Request ID sequence */
create sequence bajs_request_id_sequence
minvalue 1
maxvalue 99999
start with 1
increment by 1
cache 20;

/* Shift ID sequence */
create sequence bajs_shift_id_sequence
minvalue 1
maxvalue 99999
start with 1
increment by 1
cache 20;

/* Hours ID sequence */
create sequence bajs_hours_id_sequence
minvalue 1
maxvalue 99999
start with 1
increment by 1
cache 20;
