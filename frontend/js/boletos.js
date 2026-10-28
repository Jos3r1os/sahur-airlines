// ============================================================
// Pase de abordar (se usa en vender.html y reservas.html)
// ============================================================

// Dibuja un boleto como pase de abordar.
// datos = { boleto, vuelo, asiento, pasajero, ciudades }
//   boleto:   { codigoBoleto, precio, estado }
//   vuelo:    { idVuelo, ruta: { aeropuertoOrigen, aeropuertoDestino }, fechaSalida }
//   asiento:  { numeroAsiento, ubicacion, tipoClase: { nombre } }   (puede venir vacio)
//   pasajero: { nombres, apellidos }                                (puede venir vacio)
//   ciudades: { "GUA": "Ciudad de Guatemala", ... }
function paseDeAbordar({ boleto, vuelo, asiento, pasajero, ciudades }) {
    const origen = vuelo ? vuelo.ruta.aeropuertoOrigen : "---";
    const destino = vuelo ? vuelo.ruta.aeropuertoDestino : "---";
    const nombre = pasajero ? `${pasajero.nombres || ""} ${pasajero.apellidos || ""}`.trim() : "";
    const clase = asiento && asiento.tipoClase ? asiento.tipoClase.nombre : "";

    // El sello del capitan solo aparece cuando el boleto ya esta pagado (o usado)
    let marca;
    if (boleto.estado === "PAGADO" || boleto.estado === "USADO") {
        const texto = boleto.estado === "USADO" ? "Abordado" : "Pagado";
        marca = `<div class="sello"><img src="${CARA_CAPITAN}" alt="">${texto}</div>`;
    } else {
        marca = etiquetaEstado(boleto.estado);
    }

    return `
    <article class="pase ${boleto.estado === "CANCELADO" ? "cancelado" : ""}">
        <div class="pase-cuerpo">
            <div class="pase-marca">Sahur Airlines <span>Pase de abordar</span></div>
            <div class="pase-ruta">
                <div><b>${escapar(origen)}</b><small>${escapar(ciudades[origen] || "")}</small></div>
                <span class="avion" aria-hidden="true">&#9992;</span>
                <div><b>${escapar(destino)}</b><small>${escapar(ciudades[destino] || "")}</small></div>
            </div>
            <dl class="pase-datos">
                <div><dt>Pasajero</dt><dd>${escapar(nombre)}</dd></div>
                <div><dt>Vuelo</dt><dd>${vuelo ? codigoVuelo(vuelo.idVuelo) : ""}</dd></div>
                <div><dt>Fecha</dt><dd>${vuelo ? soloFecha(vuelo.fechaSalida) : ""}</dd></div>
                <div><dt>Salida</dt><dd>${vuelo ? soloHora(vuelo.fechaSalida) : ""}</dd></div>
                <div><dt>Asiento</dt><dd>${asiento ? escapar(asiento.numeroAsiento) : ""}</dd></div>
                <div><dt>Clase</dt><dd>${escapar(clase)}</dd></div>
            </dl>
        </div>
        <dl class="pase-talon">
            <div><dt>Codigo</dt><dd>${escapar(boleto.codigoBoleto)}</dd></div>
            <div><dt>Precio</dt><dd>${dinero(boleto.precio)}</dd></div>
            ${marca}
        </dl>
    </article>`;
}

// Precio que tendra el boleto: precio base del vuelo + recargo de la clase
// (el precio real lo calcula reservas-service; esto es solo para mostrarlo antes)
function precioEstimado(vuelo, asiento) {
    const recargo = asiento && asiento.tipoClase ? Number(asiento.tipoClase.porcentajeRecargo || 0) : 0;
    return Math.round(Number(vuelo.precioBase) * (1 + recargo / 100) * 100) / 100;
}
// ============================================================
// Impresora termica (impresora-service, puerto 8086)
// ============================================================

// Hora en 24 horas para el ticket: "09:40"
function hora24(iso) {
    if (!iso) return "";
    return new Date(iso).toLocaleTimeString("es-GT", { hour: "2-digit", minute: "2-digit", hour12: false });
}

// Lo que la impresora necesita de un boleto (todo ya como texto listo para imprimir)
function datosImpresion({ boleto, vuelo, asiento, pasajero, ciudades, idReserva }) {
    const origen = vuelo ? vuelo.ruta.aeropuertoOrigen : "";
    const destino = vuelo ? vuelo.ruta.aeropuertoDestino : "";
    return {
        codigo: boleto.codigoBoleto,
        pasajero: pasajero ? `${pasajero.nombres || ""} ${pasajero.apellidos || ""}`.trim() : "",
        origen,
        destino,
        ciudadOrigen: ciudades[origen] || origen,
        ciudadDestino: ciudades[destino] || destino,
        vuelo: vuelo ? codigoVuelo(vuelo.idVuelo) : "",
        fecha: vuelo ? new Date(vuelo.fechaSalida).toLocaleDateString("es-GT", { day: "2-digit", month: "short", year: "numeric" }) : "",
        hora: vuelo ? hora24(vuelo.fechaSalida) : "",
        asiento: asiento ? asiento.numeroAsiento : "",
        clase: asiento && asiento.tipoClase ? asiento.tipoClase.nombre : "",
        precio: dinero(boleto.precio),
        estado: boleto.estado ? boleto.estado.charAt(0) + boleto.estado.slice(1).toLowerCase() : "",
        reserva: idReserva
    };
}

// Manda los pases a la impresora. Lanza un Error con un mensaje claro si algo falla.
async function imprimirPases(pases) {
    const respuesta = await api("impresora", "/imprimir/pases", "POST", { pases });
    return respuesta.impresos;
}

// Pinta en el elemento la etiqueta con el estado de la impresora
async function revisarImpresora(idElemento) {
    const caja = document.getElementById(idElemento);
    if (!caja) return;
    caja.innerHTML = `<span class="estado cargando">Buscando la impresora</span>`;
    try {
        const e = await api("impresora", "/estado");
        if (e.modoPrueba) {
            caja.innerHTML = `<span class="estado estado-alerta">Modo prueba: no imprime, guarda el ticket</span>`;
        } else if (e.conectada) {
            caja.innerHTML = `<span class="estado estado-ok">Impresora ${escapar(e.impresora)} lista</span>`;
        } else {
            caja.innerHTML = `<span class="estado estado-alerta">Windows no encuentra la impresora ${escapar(e.impresora)}</span>`;
        }
    } catch (error) {
        caja.innerHTML = `<span class="estado estado-error">La impresora esta apagada</span>
            <span class="ayuda">Abre impresora-service y ejecuta <code>python servidor_impresora.py</code>.</span>`;
    }
}

// Vista previa del ticket tal como sale de la impresora termica
function ticketTermico(p) {
    const fila = (etiqueta, valor) => `<div><dt>${etiqueta}</dt><dd>${escapar(valor)}</dd></div>`;
    return `
    <article class="ticket-termico" aria-label="Ticket del boleto ${escapar(p.codigo)}">
        <img class="ticket-cara" src="${CARA_CAPITAN}" alt="">
        <b class="ticket-marca">SAHUR AIRLINES</b>
        <span>Pase de abordar</span>
        <hr>
        <b class="ticket-ruta">${escapar(p.origen)} &gt; ${escapar(p.destino)}</b>
        <span>${escapar(p.ciudadOrigen)} a ${escapar(p.ciudadDestino)}</span>
        <hr>
        <p class="ticket-pasajero"><small>Pasajero</small>${escapar(p.pasajero.toUpperCase())}</p>
        <dl>
            ${fila("Vuelo", p.vuelo)}
            ${fila("Fecha", p.fecha)}
            ${fila("Salida", p.hora)}
            ${fila("Asiento", p.asiento)}
            ${fila("Clase", p.clase)}
            ${fila("Reserva", "#" + p.reserva)}
            ${fila("Precio", p.precio)}
        </dl>
        <hr>
        <div class="ticket-barras" aria-hidden="true"></div>
        <span>${escapar(p.codigo)}</span>
        <b class="ticket-estado">* ${escapar(p.estado.toUpperCase())} *</b>
        <span>Tung, tung, tung... buen viaje!</span>
    </article>`;
}
