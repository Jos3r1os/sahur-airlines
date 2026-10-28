# ============================================================
# Sahur Airlines - impresora-service (puerto 8086)
# Servidor pequeno que corre en la computadora donde esta conectada
# la impresora termica. La pagina web le manda los pases y el los imprime.
#
# Instalar una vez:   pip install pywin32 pillow
# Ejecutar:           python servidor_impresora.py
# Otra impresora:     python servidor_impresora.py --impresora "Nombre en Windows"
# Sin impresora:      python servidor_impresora.py --prueba
# ============================================================

import argparse
import json
import traceback
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

from printer_manager import PrinterManager

impresora = None


class Manejador(BaseHTTPRequestHandler):

    # ---------- Respuestas ----------

    def _cabeceras(self, codigo, tipo):
        self.send_response(codigo)
        self.send_header("Content-Type", tipo)
        # La pagina se abre desde otro puerto, asi que hay que permitirle llamar (CORS)
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
        self.send_header("Access-Control-Allow-Headers", "Content-Type")
        self.end_headers()

    def _json(self, codigo, datos):
        self._cabeceras(codigo, "application/json; charset=utf-8")
        self.wfile.write(json.dumps(datos).encode("utf-8"))

    def _texto(self, codigo, texto):
        # Los errores van como texto: api.js los muestra tal cual en el mensaje del capitan
        self._cabeceras(codigo, "text/plain; charset=utf-8")
        self.wfile.write(texto.encode("utf-8"))

    # ---------- Rutas ----------

    def do_OPTIONS(self):
        self._cabeceras(204, "text/plain")

    def do_GET(self):
        if self.path == "/estado":
            self._json(200, {
                "impresora": impresora.printer_name,
                "conectada": impresora.esta_conectada(),
                "modoPrueba": impresora.modo_prueba,
                "disponibles": impresora.impresoras_disponibles()
            })
        else:
            self._texto(404, "Ruta no encontrada")

    def do_POST(self):
        if self.path != "/imprimir/pases":
            self._texto(404, "Ruta no encontrada")
            return
        try:
            largo = int(self.headers.get("Content-Length", 0))
            cuerpo = json.loads(self.rfile.read(largo) or b"{}")
            pases = cuerpo.get("pases", [])
            if not pases:
                self._texto(400, "No llego ningun pase para imprimir.")
                return
            if not impresora.esta_conectada():
                self._texto(503, f"No encuentro la impresora {impresora.printer_name}. "
                                 "Revisa que este encendida y conectada a la computadora.")
                return
            impresos = impresora.imprimir_pases(pases)
            self._json(200, {"impresos": impresos})
        except Exception as e:
            traceback.print_exc()
            self._texto(500, f"La impresora fallo: {e}")

    def log_message(self, formato, *args):
        print("[impresora]", formato % args)


def main():
    global impresora
    opciones = argparse.ArgumentParser(description="Servidor de impresion de Sahur Airlines")
    opciones.add_argument("--impresora", default="POS-76", help="Nombre de la impresora en Windows")
    opciones.add_argument("--puerto", type=int, default=8086)
    opciones.add_argument("--columnas", type=int, default=32, help="Letras por linea del papel")
    opciones.add_argument("--prueba", action="store_true", help="No imprime; guarda el trabajo en salida/")
    args = opciones.parse_args()

    impresora = PrinterManager(args.impresora, columnas=args.columnas, modo_prueba=args.prueba)
    impresora.logo_capitan()   # prepara la cara del capitan una sola vez

    print(f"Impresora: {impresora.printer_name}  (modo prueba: {impresora.modo_prueba})")
    if not impresora.modo_prueba and not impresora.esta_conectada():
        print("OJO: Windows no tiene una impresora con ese nombre. Disponibles:")
        for nombre in impresora.impresoras_disponibles():
            print("   -", nombre)
    print(f"impresora-service escuchando en http://localhost:{args.puerto}")
    ThreadingHTTPServer(("localhost", args.puerto), Manejador).serve_forever()


if __name__ == "__main__":
    main()
