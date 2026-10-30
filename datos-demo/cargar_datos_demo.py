# ============================================================
# Sahur Airlines - datos de demostracion
# Carga clases, aeropuertos, rutas, aviones con asientos, vuelos de la
# proxima semana, empleados, pasajeros y unas ventas de ejemplo.
#
# Usa las mismas APIs que la pagina, asi que las contrasenas se guardan
# encriptadas y se aplican las mismas reglas. Solo usa Python (nada que instalar).
#
# 1. Enciende Oracle y los 5 servicios.
# 2. En la terminal, dentro del proyecto:   python datos-demo/cargar_datos_demo.py
#
# Se puede correr varias veces: lo que ya existe se salta.
# ============================================================

import json
import sys
import urllib.error
import urllib.request
from datetime import date, datetime, timedelta

URLS = {
    "empleados": "http://localhost:8081",
    "pasajeros": "http://localhost:8082",
    "vuelos": "http://localhost:8083",
    "reservas": "http://localhost:8084",
    "pagos": "http://localhost:8085",
}

# ---------- Datos ----------

CLASES = [
    {"nombre": "Economica", "porcentajeRecargo": 0},
    {"nombre": "Ejecutiva", "porcentajeRecargo": 60},
]

AEROPUERTOS = [
    {"codigoIata": "GUA", "nombre": "Aeropuerto Internacional La Aurora", "ciudad": "Ciudad de Guatemala", "pais": "Guatemala"},
    {"codigoIata": "FRS", "nombre": "Aeropuerto Internacional Mundo Maya", "ciudad": "Flores", "pais": "Guatemala"},
    {"codigoIata": "SAL", "nombre": "Aeropuerto Internacional Monsenor Oscar Arnulfo Romero", "ciudad": "San Salvador", "pais": "El Salvador"},
    {"codigoIata": "MEX", "nombre": "Aeropuerto Internacional Benito Juarez", "ciudad": "Ciudad de Mexico", "pais": "Mexico"},
    {"codigoIata": "CUN", "nombre": "Aeropuerto Internacional de Cancun", "ciudad": "Cancun", "pais": "Mexico"},
    {"codigoIata": "SJO", "nombre": "Aeropuerto Internacional Juan Santamaria", "ciudad": "San Jose", "pais": "Costa Rica"},
    {"codigoIata": "MIA", "nombre": "Aeropuerto Internacional de Miami", "ciudad": "Miami", "pais": "Estados Unidos"},
]

# Distancias y duraciones aproximadas, para la demo
RUTAS = [
    {"aeropuertoOrigen": "GUA", "aeropuertoDestino": "FRS", "distancia": 290, "duracionAproximada": 55},
    {"aeropuertoOrigen": "FRS", "aeropuertoDestino": "GUA", "distancia": 290, "duracionAproximada": 55},
    {"aeropuertoOrigen": "GUA", "aeropuertoDestino": "SAL", "distancia": 180, "duracionAproximada": 40},
    {"aeropuertoOrigen": "GUA", "aeropuertoDestino": "MEX", "distancia": 1050, "duracionAproximada": 125},
    {"aeropuertoOrigen": "GUA", "aeropuertoDestino": "CUN", "distancia": 850, "duracionAproximada": 100},
    {"aeropuertoOrigen": "GUA", "aeropuertoDestino": "SJO", "distancia": 870, "duracionAproximada": 105},
    {"aeropuertoOrigen": "GUA", "aeropuertoDestino": "MIA", "distancia": 1650, "duracionAproximada": 150},
    {"aeropuertoOrigen": "MIA", "aeropuertoDestino": "GUA", "distancia": 1650, "duracionAproximada": 160},
]

# Aviones chicos a proposito, para que el mapa de asientos se lea bien en la demo
AVIONES = [
    {"matricula": "TG-SAH", "modelo": "Airbus A320", "filas": 6, "letras": "ABCDEF", "filasEjecutiva": 2},
    {"matricula": "TG-TUN", "modelo": "ATR 72", "filas": 6, "letras": "ABCD", "filasEjecutiva": 0},
    {"matricula": "TG-PEN", "modelo": "Boeing 737", "filas": 6, "letras": "ABCDEF", "filasEjecutiva": 1},
]

# Vuelos de la proxima semana: (dia 0 = lunes, hora, origen, destino, avion, precio base en Q)
VUELOS = [
    (0, "06:15", "GUA", "FRS", "TG-TUN", 650),
    (0, "09:40", "GUA", "MIA", "TG-SAH", 2450),
    (1, "08:00", "FRS", "GUA", "TG-TUN", 650),
    (1, "13:30", "MIA", "GUA", "TG-SAH", 2450),
    (2, "07:30", "GUA", "SAL", "TG-TUN", 890),
    (2, "10:15", "GUA", "MEX", "TG-PEN", 1890),
    (3, "11:00", "GUA", "CUN", "TG-SAH", 1750),
    (3, "16:45", "GUA", "FRS", "TG-TUN", 650),
    (4, "09:00", "GUA", "SJO", "TG-PEN", 1650),
    (4, "14:20", "GUA", "MIA", "TG-SAH", 2450),
    (5, "07:00", "GUA", "FRS", "TG-TUN", 650),
    (5, "12:30", "GUA", "MEX", "TG-PEN", 1890),
    (6, "18:00", "FRS", "GUA", "TG-TUN", 650),
    (6, "19:15", "MIA", "GUA", "TG-SAH", 2450),
]

# Contrasenas de demostracion: cambialas si el sistema se usa de verdad
EMPLEADOS = [
    {"usuario": "admin", "password": "Sahur2026", "nombres": "Jose Adrian", "apellidos": "Figueroa", "puesto": "Administrador"},
    {"usuario": "mgarcia", "password": "Super2026", "nombres": "Maria", "apellidos": "Garcia", "puesto": "Supervisor"},
    {"usuario": "cperez", "password": "Venta2026", "nombres": "Carlos", "apellidos": "Perez", "puesto": "Vendedor"},
    {"usuario": "lramirez", "password": "Venta2026", "nombres": "Lucia", "apellidos": "Ramirez", "puesto": "Vendedor"},
]

PASAJEROS = [
    {"numeroDocumento": "2589741230101", "nombres": "Ana Lucia", "apellidos": "Lopez Mendez", "fechaNacimiento": "1995-04-12", "telefono": "5512 3456", "correo": "ana.lopez@example.com"},
    {"numeroDocumento": "3012456780101", "nombres": "Pedro", "apellidos": "Xicara Ajanel", "fechaNacimiento": "1988-09-03", "telefono": "4423 9087", "correo": "pedro.xicara@example.com"},
    {"numeroDocumento": "1745896320101", "nombres": "Sofia", "apellidos": "Monterroso", "fechaNacimiento": "2001-01-27", "telefono": "3378 1122", "correo": "sofia.m@example.com"},
    {"numeroDocumento": "2456789010101", "nombres": "Diego", "apellidos": "Castillo Paz", "fechaNacimiento": "1979-06-18", "telefono": "5598 4410", "correo": "diego.castillo@example.com"},
    {"numeroDocumento": "1987654320101", "nombres": "Valeria", "apellidos": "Chen Morales", "fechaNacimiento": "1999-11-05", "telefono": "4120 7765", "correo": "valeria.chen@example.com"},
    {"numeroDocumento": "N12345678", "nombres": "John", "apellidos": "Miller", "fechaNacimiento": "1984-03-22", "telefono": "+1 305 555 0142", "correo": "john.miller@example.com"},
]

# Ventas de ejemplo para que Reservas y Reportes tengan datos:
# (documento del pasajero, usuario del vendedor, vuelo como (dia, origen, destino), asientos, metodo de pago o None = pendiente)
VENTAS = [
    ("2589741230101", "cperez", (0, "GUA", "MIA"), ["1A", "1B"], "TARJETA"),
    ("3012456780101", "lramirez", (0, "GUA", "FRS"), ["3C"], "EFECTIVO"),
    ("1745896320101", "cperez", (2, "GUA", "MEX"), ["4D"], None),
]


# ---------- Ayudantes ----------

def api(servicio, ruta, metodo="GET", cuerpo=None):
    datos = json.dumps(cuerpo).encode("utf-8") if cuerpo is not None else None
    peticion = urllib.request.Request(URLS[servicio] + ruta, data=datos, method=metodo,
                                      headers={"Content-Type": "application/json"})
    try:
        with urllib.request.urlopen(peticion, timeout=20) as respuesta:
            texto = respuesta.read().decode("utf-8")
    except urllib.error.HTTPError as e:
        raise RuntimeError(e.read().decode("utf-8") or f"Error {e.code}")
    except urllib.error.URLError:
        print(f"\nNo hay conexion con {servicio}-service ({URLS[servicio]}). Enciendelo y vuelve a correr el script.")
        sys.exit(1)
    try:
        return json.loads(texto)
    except ValueError:
        return texto


def paso(titulo):
    print(f"\n{titulo}")


def ubicacion(indice, total):
    # La misma regla que el generador de asientos de la pagina
    mitad = (total + 1) // 2
    if indice == 0 or indice == total - 1:
        return "VENTANA"
    if indice in (mitad - 1, mitad):
        return "PASILLO"
    return "CENTRO"


DIAS = ["lun", "mar", "mie", "jue", "vie", "sab", "dom"]


def proximo_lunes():
    hoy = date.today()
    return hoy + timedelta(days=7 - hoy.weekday())


# ---------- Carga ----------

def main():
    paso("Clases")
    clases = {c["nombre"]: c for c in api("vuelos", "/clases")}
    for c in CLASES:
        if c["nombre"] in clases:
            print(f"  ya existe {c['nombre']}")
        else:
            clases[c["nombre"]] = api("vuelos", "/clases", "POST", c)
            print(f"  + {c['nombre']}")

    paso("Aeropuertos")
    aeropuertos = {a["codigoIata"] for a in api("vuelos", "/aeropuertos")}
    for a in AEROPUERTOS:
        if a["codigoIata"] in aeropuertos:
            print(f"  ya existe {a['codigoIata']}")
        else:
            api("vuelos", "/aeropuertos", "POST", a)
            print(f"  + {a['codigoIata']} {a['ciudad']}")

    paso("Rutas")
    rutas = {(r["aeropuertoOrigen"], r["aeropuertoDestino"]): r for r in api("vuelos", "/rutas")}
    for r in RUTAS:
        clave = (r["aeropuertoOrigen"], r["aeropuertoDestino"])
        if clave in rutas:
            print(f"  ya existe {clave[0]} - {clave[1]}")
        else:
            rutas[clave] = api("vuelos", "/rutas", "POST", r)
            print(f"  + {clave[0]} - {clave[1]}")

    paso("Aviones y asientos")
    aviones = {a["matricula"]: a for a in api("vuelos", "/aviones")}
    for a in AVIONES:
        if a["matricula"] in aviones:
            print(f"  ya existe {a['matricula']}")
            continue
        capacidad = a["filas"] * len(a["letras"])
        avion = api("vuelos", "/aviones", "POST", {"matricula": a["matricula"], "modelo": a["modelo"],
                                                   "capacidad": capacidad, "estado": "ACTIVO"})
        aviones[a["matricula"]] = avion
        for fila in range(1, a["filas"] + 1):
            clase = clases["Ejecutiva" if fila <= a["filasEjecutiva"] else "Economica"]
            for i, letra in enumerate(a["letras"]):
                api("vuelos", "/asientos", "POST", {
                    "avion": {"idAvion": avion["idAvion"]},
                    "tipoClase": {"idClase": clase["idClase"]},
                    "numeroAsiento": f"{fila}{letra}",
                    "ubicacion": ubicacion(i, len(a["letras"])),
                })
        print(f"  + {a['matricula']} {a['modelo']} con {capacidad} asientos")

    paso("Vuelos de la proxima semana")
    lunes = proximo_lunes()
    existentes = {(v["ruta"]["idRuta"], v["fechaSalida"][:16]) for v in api("vuelos", "/vuelos")}
    vuelos = {}
    for dia, hora, origen, destino, matricula, precio in VUELOS:
        ruta = rutas[(origen, destino)]
        salida = datetime.combine(lunes + timedelta(days=dia), datetime.strptime(hora, "%H:%M").time())
        llegada = salida + timedelta(minutes=ruta["duracionAproximada"])
        clave = (ruta["idRuta"], salida.strftime("%Y-%m-%dT%H:%M"))
        if clave in existentes:
            print(f"  ya existe {origen} - {destino} {salida:%d/%m %H:%M}")
            continue
        vuelo = api("vuelos", "/vuelos", "POST", {
            "ruta": {"idRuta": ruta["idRuta"]},
            "avion": {"idAvion": aviones[matricula]["idAvion"]},
            "fechaSalida": salida.strftime("%Y-%m-%dT%H:%M:00"),
            "fechaLlegada": llegada.strftime("%Y-%m-%dT%H:%M:00"),
            "precioBase": precio,
            "estado": "PROGRAMADO",
        })
        vuelos[(dia, origen, destino)] = vuelo
        print(f"  + SAH {vuelo['idVuelo']:03d}  {origen} - {destino}  {DIAS[dia]} {salida:%d/%m %H:%M}  Q{precio}")

    paso("Empleados")
    empleados = {e["usuario"]: e for e in api("empleados", "/empleados")}
    for e in EMPLEADOS:
        if e["usuario"] in empleados:
            print(f"  ya existe {e['usuario']} (no se cambia su contrasena)")
        else:
            empleados[e["usuario"]] = api("empleados", "/empleados", "POST", {**e, "estado": "ACTIVO"})
            print(f"  + {e['usuario']} ({e['puesto']})")

    paso("Pasajeros")
    pasajeros = {p["numeroDocumento"]: p for p in api("pasajeros", "/pasajeros")}
    for p in PASAJEROS:
        if p["numeroDocumento"] in pasajeros:
            print(f"  ya existe {p['nombres']} {p['apellidos']}")
        else:
            pasajeros[p["numeroDocumento"]] = api("pasajeros", "/pasajeros", "POST", p)
            print(f"  + {p['nombres']} {p['apellidos']}")

    paso("Ventas de ejemplo")
    if api("reservas", "/reservas"):
        print("  ya hay reservas: no se agregan ventas de ejemplo")
    elif not vuelos:
        print("  los vuelos ya existian: no se agregan ventas de ejemplo")
    else:
        for documento, vendedor, clave_vuelo, numeros, metodo in VENTAS:
            vuelo = vuelos[clave_vuelo]
            libres = {a["numeroAsiento"]: a for a in api("reservas", f"/boletos/vuelo/{vuelo['idVuelo']}/asientos-disponibles")}
            reserva = api("reservas", "/reservas", "POST", {"idPasajero": pasajeros[documento]["idPasajero"],
                                                            "idEmpleado": empleados[vendedor]["idEmpleado"]})
            for numero in numeros:
                api("reservas", "/boletos", "POST", {"idReserva": reserva["idReserva"], "idVuelo": vuelo["idVuelo"],
                                                     "idAsiento": libres[numero]["idAsiento"], "usuario": vendedor})
            reserva = api("reservas", f"/reservas/{reserva['idReserva']}")
            if metodo:
                api("pagos", "/pagos", "POST", {"idReserva": reserva["idReserva"], "monto": reserva["total"], "metodoPago": metodo})
                estado = f"pagada con {metodo.lower()}"
            else:
                estado = "pendiente de pago"
            print(f"  + reserva #{reserva['idReserva']}: {len(numeros)} boleto(s) en SAH {vuelo['idVuelo']:03d}, {estado}")

    print("\nListo. Usuarios para entrar:")
    print("  usuario      contrasena   puesto")
    for e in EMPLEADOS:
        print(f"  {e['usuario']:<12} {e['password']:<12} {e['puesto']}")


if __name__ == "__main__":
    try:
        main()
    except RuntimeError as e:
        print(f"\nUn servicio rechazo el dato: {e}")
        sys.exit(1)
