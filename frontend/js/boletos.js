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