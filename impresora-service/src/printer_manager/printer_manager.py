# ============================================================
# Sahur Airlines - impresora termica POS-76 (comandos ESC/POS)
# Imprime el pase de abordar con la cara del Capitan Sahur
# (Tung Tung Tung) arriba, los datos del vuelo y un codigo de barras.
# ============================================================

import os
import unicodedata

try:
    import win32print          # pip install pywin32  (solo Windows)
except ImportError:
    win32print = None

try:
    from PIL import Image, ImageDraw, ImageOps, ImageEnhance   # pip install pillow
except ImportError:
    Image = None

CARPETA = os.path.dirname(os.path.abspath(__file__))


def buscar_cara_capitan():
    """Sube carpeta por carpeta hasta encontrar frontend/img/capitan-cara.jpg del proyecto."""
    carpeta = CARPETA
    for _ in range(6):
        ruta = os.path.join(carpeta, "frontend", "img", "capitan-cara.jpg")
        if os.path.exists(ruta):
            return ruta
        carpeta = os.path.dirname(carpeta)
    return None


CARA_CAPITAN = buscar_cara_capitan()

# ---------- Comandos ESC/POS ----------
INICIAR = b"\x1b@"
CENTRO = b"\x1ba\x01"
IZQUIERDA = b"\x1ba\x00"
NEGRITA = b"\x1bE\x01"
SIN_NEGRITA = b"\x1bE\x00"
NORMAL = b"\x1d!\x00"
DOBLE_ALTO = b"\x1d!\x01"
DOBLE = b"\x1d!\x11"          # doble de ancho y de alto
CORTAR = b"\x1dVA\x03"        # avanza un poco y corta (corte parcial)


def sin_tildes(texto):
    """La impresora no entiende tildes ni enes en UTF-8: se quitan antes de imprimir."""
    limpio = str(texto if texto is not None else "").replace("\u00a0", " ")   # espacio de "Q 650.00"
    limpio = unicodedata.normalize("NFD", limpio)
    limpio = "".join(c for c in limpio if unicodedata.category(c) != "Mn")
    return limpio.encode("ascii", "replace")


class Ticket:
    """Arma los bytes de un ticket poco a poco."""

    def __init__(self, columnas):
        self.columnas = columnas
        self.datos = bytearray(INICIAR)

    def cmd(self, *comandos):
        for c in comandos:
            self.datos.extend(c)
        return self

    def linea(self, texto=""):
        self.datos.extend(sin_tildes(texto) + b"\n")
        return self

    def raya(self, caracter="-"):
        return self.linea(caracter * self.columnas)

    def dos_columnas(self, izquierda, derecha, ancho=None):
        """Escribe 'izquierda ....... derecha' ocupando 'ancho' columnas."""
        ancho = ancho or self.columnas
        izquierda, derecha = str(izquierda), str(derecha)
        espacio = max(1, ancho - len(izquierda) - len(derecha))
        return self.linea(izquierda + " " * espacio + derecha)

    def imagen(self, raster):
        """raster = (ancho_en_bytes, alto, bytes). Se manda en tiras para no llenar la memoria de la impresora."""
        ancho_bytes, alto, puntos = raster
        tira = 128
        for inicio in range(0, alto, tira):
            filas = min(tira, alto - inicio)
            parte = puntos[inicio * ancho_bytes:(inicio + filas) * ancho_bytes]
            self.datos.extend(b"\x1dv0\x00")
            self.datos.extend(bytes([ancho_bytes % 256, ancho_bytes // 256, filas % 256, filas // 256]))
            self.datos.extend(parte)
        return self

    def codigo_barras(self, texto):
        """Codigo de barras CODE128 con el texto impreso debajo."""
        dato = b"{B" + sin_tildes(texto)
        self.datos.extend(b"\x1dh\x50")      # alto: 80 puntos
        self.datos.extend(b"\x1dw\x02")      # ancho de cada barra
        self.datos.extend(b"\x1dH\x02")      # texto debajo de las barras
        self.datos.extend(b"\x1dk\x49" + bytes([len(dato)]) + dato)
        self.datos.extend(b"\n")
        return self


class PrinterManager:
    def __init__(self, printer_name="POS-76", columnas=32, ancho_logo=256, modo_prueba=False):
        # columnas: letras por linea. Papel de 76 mm suele aceptar 32 a 42; si las lineas
        # se parten, baja este numero; si sobra mucho espacio, subelo.
        self.printer_name = printer_name
        self.columnas = columnas
        self.ancho_logo = ancho_logo
        self.modo_prueba = modo_prueba or win32print is None
        self._logo = None

    # ---------- Estado de la impresora ----------

    def impresoras_disponibles(self):
        if win32print is None:
            return []
        banderas = win32print.PRINTER_ENUM_LOCAL | win32print.PRINTER_ENUM_CONNECTIONS
        return [p[2] for p in win32print.EnumPrinters(banderas)]

    def esta_conectada(self):
        if self.modo_prueba:
            return True
        return self.printer_name in self.impresoras_disponibles()

    # ---------- La cara del capitan en puntos blanco y negro ----------

    def logo_capitan(self):
        """Convierte la foto del capitan en una medalla redonda de puntos (se calcula una sola vez)."""
        if self._logo is not None:
            return self._logo
        if Image is None:
            print("OJO: falta pillow (pip install pillow). Los tickets saldran sin la cara del capitan.")
            return None
        if CARA_CAPITAN is None:
            print("OJO: no encontre frontend/img/capitan-cara.jpg. Los tickets saldran sin la cara del capitan.")
            return None
        lado = self.ancho_logo
        foto = Image.open(CARA_CAPITAN).convert("L")
        foto = ImageOps.fit(foto, (lado, lado))
        foto = ImageOps.autocontrast(foto, cutoff=2)
        foto = ImageEnhance.Contrast(foto).enhance(1.35)
        foto = ImageEnhance.Brightness(foto).enhance(1.12)   # el papel termico imprime oscuro

        # Recorte redondo con un aro negro, como la foto del capitan en la pagina
        lienzo = Image.new("L", (lado, lado), 255)
        mascara = Image.new("L", (lado, lado), 0)
        ImageDraw.Draw(mascara).ellipse((6, 6, lado - 7, lado - 7), fill=255)
        lienzo.paste(foto, (0, 0), mascara)
        ImageDraw.Draw(lienzo).ellipse((2, 2, lado - 3, lado - 3), outline=0, width=5)

        puntos = lienzo.convert("1")        # tramado Floyd-Steinberg
        ancho_bytes = (lado + 7) // 8
        datos = bytearray()
        pixeles = puntos.load()
        for y in range(lado):
            for bx in range(ancho_bytes):
                byte = 0
                for bit in range(8):
                    x = bx * 8 + bit
                    if x < lado and pixeles[x, y] == 0:      # 0 = negro = punto impreso
                        byte |= 0x80 >> bit
                datos.append(byte)
        self._logo = (ancho_bytes, lado, bytes(datos))
        return self._logo

    # ---------- Pase de abordar ----------

    def armar_pase(self, pase):
        """pase = dict con codigo, pasajero, origen, destino, ciudadOrigen, ciudadDestino,
        vuelo, fecha, hora, asiento, clase, precio, estado, reserva."""
        t = Ticket(self.columnas)

        t.cmd(CENTRO)
        logo = self.logo_capitan()
        if logo:
            t.imagen(logo)
        t.cmd(DOBLE, NEGRITA).linea("SAHUR AIRLINES").cmd(NORMAL, SIN_NEGRITA)
        t.linea("Pase de abordar")
        t.raya("=")

        # Ruta en grande: GUA > MIA
        t.cmd(DOBLE, NEGRITA).linea(f"{pase.get('origen', '')} > {pase.get('destino', '')}").cmd(NORMAL, SIN_NEGRITA)
        t.linea(f"{pase.get('ciudadOrigen', '')} a {pase.get('ciudadDestino', '')}")
        t.raya()

        t.cmd(IZQUIERDA)
        t.linea("Pasajero")
        t.cmd(NEGRITA).linea(str(pase.get("pasajero", "")).upper()[:self.columnas]).cmd(SIN_NEGRITA)
        t.linea()
        t.dos_columnas("Vuelo", pase.get("vuelo", ""))
        t.dos_columnas("Fecha", pase.get("fecha", ""))
        t.dos_columnas("Salida", pase.get("hora", ""))
        t.cmd(NEGRITA).dos_columnas("Asiento", pase.get("asiento", "")).cmd(SIN_NEGRITA)
        t.dos_columnas("Clase", pase.get("clase", ""))
        t.dos_columnas("Reserva", f"#{pase.get('reserva', '')}")
        t.dos_columnas("Precio", pase.get("precio", ""))
        t.raya()

        t.cmd(CENTRO)
        t.codigo_barras(pase.get("codigo", ""))
        estado = str(pase.get("estado", "")).upper()
        if estado:
            t.cmd(DOBLE_ALTO, NEGRITA).linea(f"* {estado} *").cmd(NORMAL, SIN_NEGRITA)
        t.linea()
        t.linea("Tung, tung, tung... buen viaje!")
        t.linea("Llega 2 horas antes de tu vuelo")
        t.linea("\n\n")
        t.cmd(CORTAR)
        return bytes(t.datos)

    def imprimir_pases(self, pases):
        """Imprime cada pase en su propio ticket (con corte entre uno y otro). Devuelve cuantos imprimio."""
        trabajo = b"".join(self.armar_pase(p) for p in pases)
        self._mandar(trabajo, "Pases de abordar Sahur Airlines")
        return len(pases)

    # ---------- Envio a la impresora ----------

    def _mandar(self, datos, nombre_trabajo):
        if self.modo_prueba:
            # Sin impresora: se guarda lo que se hubiera impreso, para revisarlo
            salida = os.path.join(CARPETA, "salida")
            os.makedirs(salida, exist_ok=True)
            with open(os.path.join(salida, "ultimo-trabajo.bin"), "wb") as archivo:
                archivo.write(datos)
            print(f"[modo prueba] {len(datos)} bytes guardados en salida/ultimo-trabajo.bin")
            return
        hPrinter = win32print.OpenPrinter(self.printer_name)
        try:
            win32print.StartDocPrinter(hPrinter, 1, (nombre_trabajo, None, "RAW"))
            try:
                win32print.StartPagePrinter(hPrinter)
                win32print.WritePrinter(hPrinter, datos)
                win32print.EndPagePrinter(hPrinter)
            finally:
                win32print.EndDocPrinter(hPrinter)
        finally:
            win32print.ClosePrinter(hPrinter)
