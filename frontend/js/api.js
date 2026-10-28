// ============================================================
// Sahur Airlines - funciones que usan todas las pantallas
// ============================================================

// Direccion de cada microservicio
const URLS = {
    empleados: "http://localhost:8081",
    pasajeros: "http://localhost:8082",
    vuelos: "http://localhost:8083",
    reservas: "http://localhost:8084",
    pagos: "http://localhost:8085",
    impresora: "http://localhost:8086"   // impresora-service (Python) en la compu de la impresora
};

// Foto de la cara del capitan (la mascota)
const CARA_CAPITAN = "img/capitan-cara.jpg";

// Llama a un servicio. Ejemplo: await api("pasajeros", "/pasajeros")
// Si el servicio responde con error, lanza un Error con el mensaje que mando el servicio.
async function api(servicio, ruta, metodo = "GET", cuerpo = null) {
    const opciones = { method: metodo, headers: {} };
    if (cuerpo !== null) {
        opciones.headers["Content-Type"] = "application/json";
        opciones.body = JSON.stringify(cuerpo);
    }

    let respuesta;
    try {
        respuesta = await fetch(URLS[servicio] + ruta, opciones);
    } catch (e) {
        throw new Error("No hay conexion con " + servicio + "-service. Revisa que este corriendo.");
    }

    const texto = await respuesta.text();
    if (!respuesta.ok) {
        if (respuesta.status === 404 && !texto) {
            throw new Error("No se encontro el registro.");
        }
        throw new Error(texto || "Error " + respuesta.status);
    }

    // Algunas respuestas son texto ("Pasajero eliminado") y otras JSON
    try {
        return JSON.parse(texto);
    } catch (e) {
        return texto;
    }
}

// ---------- Sesion del empleado ----------

function guardarSesion(datos) {
    localStorage.setItem("sahur_sesion", JSON.stringify(datos));
}

function obtenerSesion() {
    const datos = localStorage.getItem("sahur_sesion");
    return datos ? JSON.parse(datos) : null;
}

function cerrarSesion() {
    localStorage.removeItem("sahur_sesion");
    window.location.href = "index.html";
}

// Si no hay sesion, regresa al login. Devuelve la sesion.
function requerirSesion() {
    const sesion = obtenerSesion();
    if (!sesion) {
        window.location.href = "index.html";
    }
    return sesion;
}

// ---------- Menu lateral ----------

// Pictogramas del menu, al estilo de la senalizacion de aeropuerto
const ICONOS = {
    inicio: `<path d="M3.5 11 12 4l8.5 7"/><path d="M5.5 9.5V20h13V9.5"/><path d="M10 20v-5.5h4V20"/>`,
    boleto: `<path d="M3.5 6.5h17v3.5a2 2 0 0 0 0 4v3.5h-17V14a2 2 0 0 0 0-4z"/><path d="M14.5 6.5v11" stroke-dasharray="2 2.4"/>`,
    recibo: `<path d="M6 3.5h12v17l-3-2-3 2-3-2-3 2z"/><path d="M9 8.5h6M9 12.5h6"/>`,
    grafica: `<path d="M4 20h16"/><path d="M7 16.5V11M12 16.5V6.5M17 16.5v-8"/>`,
    personas: `<circle cx="9" cy="8" r="3.2"/><path d="M3.5 19.5a5.5 5.5 0 0 1 11 0"/><path d="M16 5.2a3 3 0 0 1 0 5.6M17.6 14a5 5 0 0 1 3 5.5"/>`,
    avion: `<path d="M21 15.5v-2l-8-5V4.2a1.5 1.5 0 0 0-3 0v4.3l-8 5v2l8-2.5V18l-2.2 1.6V21l3.7-1 3.7 1v-1.4L13 18v-5z"/>`,
    torre: `<path d="M9.5 21 10.5 12h3l1 9"/><path d="M6.5 8h11L16 12H8z"/><path d="M12 8V4.5M10 4.5h4"/><path d="M5 21h14"/>`,
    gafete: `<rect x="5" y="3.5" width="14" height="17" rx="2.5"/><circle cx="12" cy="10.5" r="2.5"/><path d="M8.5 17.5a3.5 3.5 0 0 1 7 0"/><path d="M10 6.5h4"/>`
};

function icono(nombre) {
    return `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"
                 stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">${ICONOS[nombre]}</svg>`;
}

const PAGINAS = [
    { archivo: "inicio.html", nombre: "Inicio", icono: "inicio" },
    { archivo: "vender.html", nombre: "Vender boleto", icono: "boleto" },
    { archivo: "reservas.html", nombre: "Reservas y pagos", icono: "recibo" },
    { archivo: "reportes.html", nombre: "Consultas y reportes", icono: "grafica" },
    { archivo: "pasajeros.html", nombre: "Pasajeros", icono: "personas" },
    { archivo: "vuelos.html", nombre: "Vuelos", icono: "avion" },
    { archivo: "catalogos.html", nombre: "Aeropuertos y aviones", icono: "torre" },
    { archivo: "empleados.html", nombre: "Empleados", icono: "gafete" }
];

// Dibuja el menu en el elemento <aside id="menu">
function pintarMenu(paginaActual) {
    const sesion = requerirSesion();
    if (!sesion) return;

    const enlaces = PAGINAS.map(p => {
        const activa = p.archivo === paginaActual;
        return `<a href="${p.archivo}" class="${activa ? "activa" : ""}" ${activa ? 'aria-current="page"' : ""}>
                    <span class="icono">${icono(p.icono)}</span>${p.nombre}
                </a>`;
    }).join("");

    document.getElementById("menu").innerHTML = `
        <div class="marca">
            <img src="${CARA_CAPITAN}" alt="Capitan Sahur" title="Saluda al capitan" id="cara-marca">
            <div>Sahur <span>Airlines</span></div>
        </div>
        <nav aria-label="Menu principal">${enlaces}</nav>
        <div class="usuario">
            De turno hoy
            <strong>${escapar(sesion.usuario)}</strong>
            <button type="button" onclick="cerrarSesion()">Cerrar sesion</button>
        </div>`;

    // Si tocas la cara del capitan, el capitan toca: tung, tung, tung
    const cara = document.getElementById("cara-marca");
    cara.addEventListener("click", () => tocarTambor(cara));
}

// Reinicia la animacion "tung tung tung" en una imagen del capitan
function tocarTambor(imagen) {
    imagen.classList.remove("tocando");
    void imagen.offsetWidth;   // obliga al navegador a reiniciar la animacion
    imagen.classList.add("tocando");
}

// ---------- El capitan habla ----------

// Devuelve el HTML del capitan con un globo de texto
function capitanDice(textoHtml) {
    return `<div class="capitan">
                <img src="${CARA_CAPITAN}" alt="Capitan Sahur">
                <p class="globo">${textoHtml}</p>
            </div>`;
}

// Fila de tabla vacia, con el capitan explicando que hacer
function filaVacia(texto, columnas) {
    return `<tr><td colspan="${columnas}" class="vacio">${capitanDice(escapar(texto))}</td></tr>`;
}

// ---------- Mensajes ----------

let temporizadorMensaje;

// tipo: "ok" o "error"
function mostrarMensaje(texto, tipo = "ok") {
    let caja = document.getElementById("mensaje");
    if (!caja) {
        caja = document.createElement("div");
        caja.id = "mensaje";
        caja.setAttribute("role", "status");
        document.body.appendChild(caja);
    }
    // Se quita y se vuelve a poner "visible" para que el capitan anime cada mensaje nuevo
    caja.className = "mensaje " + tipo;
    caja.innerHTML = `<img src="${CARA_CAPITAN}" alt=""><span>${escapar(texto)}</span>`;
    void caja.offsetWidth;
    caja.classList.add("visible");
    clearTimeout(temporizadorMensaje);
    temporizadorMensaje = setTimeout(() => caja.classList.remove("visible"), 5000);
}

// ---------- Formatos ----------

function dinero(valor) {
    const numero = Number(valor || 0);
    // \u00a0 = espacio que no se parte, para que la Q no quede sola en otra linea
    return "Q\u00a0" + numero.toLocaleString("es-GT", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

function fechaHora(iso) {
    if (!iso) return "";
    const f = new Date(iso);
    return f.toLocaleDateString("es-GT", { day: "2-digit", month: "short", year: "numeric" }) + " " +
        f.toLocaleTimeString("es-GT", { hour: "2-digit", minute: "2-digit" });
}

function soloHora(iso) {
    if (!iso) return "";
    return new Date(iso).toLocaleTimeString("es-GT", { hour: "2-digit", minute: "2-digit" });
}

function soloFecha(iso) {
    if (!iso) return "";
    return new Date(iso).toLocaleDateString("es-GT", { day: "2-digit", month: "short" });
}

// Codigo de vuelo para mostrar: 1 -> "SAH 001"
function codigoVuelo(idVuelo) {
    return "SAH " + String(idVuelo).padStart(3, "0");
}

// Etiqueta de color segun el estado
function etiquetaEstado(estado) {
    const ok = ["ACTIVO", "PAGADA", "PAGADO", "COMPLETADO", "PROGRAMADO", "USADO", "FINALIZADO"];
    const alerta = ["PENDIENTE", "EMITIDO", "RETRASADO", "MANTENIMIENTO", "EN_VUELO", "REEMBOLSADO"];
    const error = ["CANCELADA", "CANCELADO", "INACTIVO"];
    let clase = "";
    if (ok.includes(estado)) clase = "estado-ok";
    else if (alerta.includes(estado)) clase = "estado-alerta";
    else if (error.includes(estado)) clase = "estado-error";
    // Se muestra "Programado" en vez de "PROGRAMADO" y "En vuelo" en vez de "EN_VUELO"
    const texto = estado ? estado.charAt(0) + estado.slice(1).toLowerCase().replaceAll("_", " ") : "Sin estado";
    return `<span class="estado ${clase}">${escapar(texto)}</span>`;
}

// Evita que un texto con < > rompa el HTML
function escapar(texto) {
    if (texto === null || texto === undefined) return "";
    return String(texto)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;");
}