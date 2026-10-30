-- ============================================================
-- Sahur Airlines - usuarios de la base de datos
-- Oracle corre este archivo solo la primera vez que se crea la base
-- (cuando el volumen oracle-data esta vacio).
-- vuelos_user ya lo crea docker-compose con APP_USER; aqui van los otros 4.
-- ============================================================

ALTER SESSION SET CONTAINER = FREEPDB1;

CREATE USER empleados_user IDENTIFIED BY empleados123 DEFAULT TABLESPACE users QUOTA UNLIMITED ON users;
GRANT CONNECT, RESOURCE, CREATE VIEW TO empleados_user;

CREATE USER pasajeros_user IDENTIFIED BY pasajeros123 DEFAULT TABLESPACE users QUOTA UNLIMITED ON users;
GRANT CONNECT, RESOURCE, CREATE VIEW TO pasajeros_user;

CREATE USER reservas_user IDENTIFIED BY reservas123 DEFAULT TABLESPACE users QUOTA UNLIMITED ON users;
GRANT CONNECT, RESOURCE, CREATE VIEW TO reservas_user;

CREATE USER pagos_user IDENTIFIED BY pagos123 DEFAULT TABLESPACE users QUOTA UNLIMITED ON users;
GRANT CONNECT, RESOURCE, CREATE VIEW TO pagos_user;
