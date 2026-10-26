// ============================================================
// Sahur Airlines - funciones que usan todas las pantallas
// ============================================================

// Direccion de cada microservicio
const URLS = {
    empleados: "http://localhost:8081",
    pasajeros: "http://localhost:8082",
    vuelos: "http://localhost:8083",
    reservas: "http://localhost:8084",
    pagos: "http://localhost:8085"
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

const PAGINAS = [
    { archivo: "inicio.html", nombre: "Inicio" },
    { archivo: "vender.html", nombre: "Vender boleto" },
    { archivo: "reservas.html", nombre: "Reservas y pagos" },
    { archivo: "reportes.html", nombre: "Consultas y reportes" },
    { archivo: "pasajeros.html", nombre: "Pasajeros" },
    { archivo: "vuelos.html", nombre: "Vuelos" },
    { archivo: "catalogos.html", nombre: "Aeropuertos y aviones" },
    { archivo: "empleados.html", nombre: "Empleados" }
];

// Dibuja el menu en el elemento <aside id="menu">
function pintarMenu(paginaActual) {
    const sesion = requerirSesion();
    if (!sesion) return;

    const enlaces = PAGINAS.map(p =>
        `<a href="${p.archivo}" class="${p.archivo === paginaActual ? "activa" : ""}">${p.nombre}</a>`
    ).join("");

    document.getElementById("menu").innerHTML = `
        <div class="marca">
            <img src="${CARA_CAPITAN}" alt="">
            <div>Sahur <span>Airlines</span></div>
        </div>
        <nav>${enlaces}</nav>
        <div class="usuario">
            Sesion iniciada como
            <strong>${escapar(sesion.usuario)}</strong>
            <button type="button" onclick="cerrarSesion()">Cerrar sesion</button>
        </div>`;
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
    caja.className = "mensaje visible " + tipo;
    caja.innerHTML = `<img src="${CARA_CAPITAN}" alt=""><span>${escapar(texto)}</span>`;
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