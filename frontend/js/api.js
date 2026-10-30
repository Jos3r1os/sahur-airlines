const URLS = {
    empleados: "http://localhost:8081",
    pasajeros: "http://localhost:8082",
    vuelos: "http://localhost:8083",
    reservas: "http://localhost:8084",
    pagos: "http://localhost:8085",
    impresora: "http://localhost:8086"
};

const CARA_CAPITAN = "img/capitan-cara.jpg";

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

    try {
        return JSON.parse(texto);
    } catch (e) {
        return texto;
    }
}


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

function requerirSesion() {
    const sesion = obtenerSesion();
    if (!sesion) {
        window.location.href = "index.html";
    }
    return sesion;
}


const ICONOS = {
    inicio: `<path d="M3.5 11 12 4l8.5 7"/><path d="M5.5 9.5V20h13V9.5"/><path d="M10 20v-5.5h4V20"/>`,
    boleto: `<path d="M3.5 6.5h17v3.5a2 2 0 0 0 0 4v3.5h-17V14a2 2 0 0 0 0-4z"/><path d="M14.5 6.5v11" stroke-dasharray="2 2.4"/>`,
    recibo: `<path d="M6 3.5h12v17l-3-2-3 2-3-2-3 2z"/><path d="M9 8.5h6M9 12.5h6"/>`,
    grafica: `<path d="M4 20h16"/><path d="M7 16.5V11M12 16.5V6.5M17 16.5v-8"/>`,
    personas: `<circle cx="9" cy="8" r="3.2"/><path d="M3.5 19.5a5.5 5.5 0 0 1 11 0"/><path d="M16 5.2a3 3 0 0 1 0 5.6M17.6 14a5 5 0 0 1 3 5.5"/>`,
    avion: `<path d="M21 15.5v-2l-8-5V4.2a1.5 1.5 0 0 0-3 0v4.3l-8 5v2l8-2.5V18l-2.2 1.6V21l3.7-1 3.7 1v-1.4L13 18v-5z"/>`,
    torre: `<path d="M9.5 21 10.5 12h3l1 9"/><path d="M6.5 8h11L16 12H8z"/><path d="M12 8V4.5M10 4.5h4"/><path d="M5 21h14"/>`,
    puerta: `<path d="M6 20.5V4.8A1.3 1.3 0 0 1 7.3 3.5h9.4a1.3 1.3 0 0 1 1.3 1.3v15.7"/><path d="M3.5 20.5h17"/><path d="M14.5 12v.5"/>`,
    gafete: `<rect x="5" y="3.5" width="14" height="17" rx="2.5"/><circle cx="12" cy="10.5" r="2.5"/><path d="M8.5 17.5a3.5 3.5 0 0 1 7 0"/><path d="M10 6.5h4"/>`
};

function icono(nombre) {
    return `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"
                 stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">${ICONOS[nombre]}</svg>`;
}

const TODOS = ["Administrador", "Supervisor", "Vendedor"];
const JEFES = ["Administrador", "Supervisor"];

const PAGINAS = [
    { archivo: "inicio.html", nombre: "Inicio", icono: "inicio", para: TODOS },
    { archivo: "vender.html", nombre: "Vender boleto", icono: "boleto", para: TODOS },
    { archivo: "reservas.html", nombre: "Reservas y pagos", icono: "recibo", para: TODOS },
    { archivo: "abordaje.html", nombre: "Abordaje", icono: "puerta", para: TODOS },
    { archivo: "reportes.html", nombre: "Consultas y reportes", icono: "grafica", para: JEFES },
    { archivo: "pasajeros.html", nombre: "Pasajeros", icono: "personas", para: TODOS },
    { archivo: "vuelos.html", nombre: "Vuelos", icono: "avion", para: JEFES },
    { archivo: "catalogos.html", nombre: "Aeropuertos y aviones", icono: "torre", para: JEFES },
    { archivo: "empleados.html", nombre: "Empleados", icono: "gafete", para: ["Administrador"] }
];

function puestoDe(sesion) {
    const puesto = (sesion && sesion.puesto ? sesion.puesto : "").trim().toLowerCase();
    return TODOS.find(p => p.toLowerCase() === puesto) || "Vendedor";
}

function puedeVer(sesion, archivo) {
    const pagina = PAGINAS.find(p => p.archivo === archivo);
    return !pagina || pagina.para.includes(puestoDe(sesion));
}

function pintarMenu(paginaActual) {
    const sesion = requerirSesion();
    if (!sesion) return;

    if (!sesion.puesto) {
        cerrarSesion();
        throw new Error("Sesion sin puesto: se pide iniciar sesion otra vez.");
    }

    if (!puedeVer(sesion, paginaActual)) {
        document.documentElement.style.visibility = "hidden";
        window.location.replace("inicio.html?sin-permiso=" + encodeURIComponent(paginaActual));
        throw new Error("Pantalla no permitida para " + puestoDe(sesion) + ": se detiene el resto de la pagina.");
    }

    const enlaces = PAGINAS.filter(p => puedeVer(sesion, p.archivo)).map(p => {
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
            <span class="puesto">${escapar(puestoDe(sesion))}</span>
            <button type="button" onclick="cerrarSesion()">Cerrar sesion</button>
        </div>`;

    const cara = document.getElementById("cara-marca");
    cara.addEventListener("click", () => tocarTambor(cara));

    const negada = new URLSearchParams(location.search).get("sin-permiso");
    const pagina = PAGINAS.find(p => p.archivo === negada);
    if (pagina) {
        mostrarMensaje(`${pagina.nombre} es solo para ${pagina.para.join(" y ")}. Pidele acceso a un Administrador.`, "error");
        history.replaceState(null, "", location.pathname);
    }

    revisarEmpleado(sesion);
}

async function revisarEmpleado(sesion) {
    try {
        const empleado = await api("empleados", `/empleados/${sesion.idEmpleado}`);
        if (empleado.estado === "INACTIVO") {
            cerrarSesion();
        } else if (empleado.puesto !== sesion.puesto) {
            guardarSesion({ ...sesion, puesto: empleado.puesto });
            location.reload();
        }
    } catch (e) {
    }
}

function tocarTambor(imagen) {
    imagen.classList.remove("tocando");
    void imagen.offsetWidth;
    imagen.classList.add("tocando");
}


function capitanDice(textoHtml) {
    return `<div class="capitan">
                <img src="${CARA_CAPITAN}" alt="Capitan Sahur">
                <p class="globo">${textoHtml}</p>
            </div>`;
}

function filaVacia(texto, columnas) {
    return `<tr><td colspan="${columnas}" class="vacio">${capitanDice(escapar(texto))}</td></tr>`;
}


let temporizadorMensaje;

function mostrarMensaje(texto, tipo = "ok") {
    let caja = document.getElementById("mensaje");
    if (!caja) {
        caja = document.createElement("div");
        caja.id = "mensaje";
        caja.setAttribute("role", "status");
        document.body.appendChild(caja);
    }
    caja.className = "mensaje " + tipo;
    caja.innerHTML = `<img src="${CARA_CAPITAN}" alt=""><span>${escapar(texto)}</span>`;
    void caja.offsetWidth;
    caja.classList.add("visible");
    clearTimeout(temporizadorMensaje);
    temporizadorMensaje = setTimeout(() => caja.classList.remove("visible"), 5000);
}


function dinero(valor) {
    const numero = Number(valor || 0);
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

function codigoVuelo(idVuelo) {
    return "SAH " + String(idVuelo).padStart(3, "0");
}

function etiquetaEstado(estado) {
    const ok = ["ACTIVO", "PAGADA", "PAGADO", "COMPLETADO", "PROGRAMADO", "USADO", "FINALIZADO"];
    const alerta = ["PENDIENTE", "EMITIDO", "RETRASADO", "MANTENIMIENTO", "EN_VUELO", "REEMBOLSADO"];
    const error = ["CANCELADA", "CANCELADO", "INACTIVO"];
    let clase = "";
    if (ok.includes(estado)) clase = "estado-ok";
    else if (alerta.includes(estado)) clase = "estado-alerta";
    else if (error.includes(estado)) clase = "estado-error";
    const texto = estado ? estado.charAt(0) + estado.slice(1).toLowerCase().replaceAll("_", " ") : "Sin estado";
    return `<span class="estado ${clase}">${escapar(texto)}</span>`;
}

function escapar(texto) {
    if (texto === null || texto === undefined) return "";
    return String(texto)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;");
}