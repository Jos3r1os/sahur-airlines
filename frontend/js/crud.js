const SECCIONES = {};

function crearSeccion(cfg) {
    cfg.lista = [];
    cfg.editando = null;
    cfg.nuevo = cfg.femenino ? "Nueva" : "Nuevo";
    cfg.hecho = cfg.femenino ? "a" : "o";
    cfg.este = cfg.femenino ? "esta" : "este";
    SECCIONES[cfg.clave] = cfg;

    const campos = cfg.campos.map(c => htmlCampo(cfg, c)).join("");
    const columnas = cfg.columnas.map(c => `<th>${c.titulo}</th>`).join("");
    const buscador = cfg.buscar ? `
        <div class="campo">
            <label for="${cfg.clave}-buscar">Buscar</label>
            <input id="${cfg.clave}-buscar" type="search">
        </div>` : "";

    document.getElementById(cfg.contenedor).innerHTML = `
        <div class="dos-columnas">
            <section class="panel">
                ${buscador}
                <div class="tabla-contenedor">
                    <table>
                        <thead><tr>${columnas}<th></th></tr></thead>
                        <tbody id="${cfg.clave}-filas"><tr><td class="vacio" colspan="${cfg.columnas.length + 1}">Cargando...</td></tr></tbody>
                    </table>
                </div>
            </section>
            <section class="panel">
                <h2 id="${cfg.clave}-titulo">${cfg.nuevo} ${cfg.nombre}</h2>
                <form id="${cfg.clave}-form">
                    ${campos}
                    <div class="acciones">
                        <button class="boton boton-principal" type="submit" id="${cfg.clave}-guardar">Registrar ${cfg.nombre}</button>
                        <button class="boton boton-secundario" type="button" id="${cfg.clave}-cancelar" hidden>Cancelar edicion</button>
                    </div>
                </form>
            </section>
        </div>`;

    const form = document.getElementById(`${cfg.clave}-form`);
    form.addEventListener("submit", e => { e.preventDefault(); guardarRegistro(cfg.clave); });
    document.getElementById(`${cfg.clave}-cancelar`).addEventListener("click", () => limpiarFormulario(cfg.clave));
    if (cfg.buscar) {
        document.getElementById(`${cfg.clave}-buscar`).addEventListener("input", () => pintarTabla(cfg.clave));
    }
    if (cfg.alIniciar) cfg.alIniciar(form);

    return cargarSeccion(cfg.clave);
}

function htmlCampo(cfg, c) {
    const idHtml = `${cfg.clave}-${c.id}`;
    const requerido = c.requerido ? "required" : "";
    const ayuda = c.ayuda ? `<p class="ayuda">${c.ayuda}</p>` : "";
    let control;
    if (c.tipo === "select") {
        control = `<select id="${idHtml}" ${requerido}></select>`;
    } else {
        const paso = c.paso ? `step="${c.paso}"` : "";
        control = `<input id="${idHtml}" type="${c.tipo || "text"}" ${paso} ${requerido}>`;
    }
    return `<div class="campo"><label for="${idHtml}">${c.etiqueta}</label>${control}${ayuda}</div>`;
}

function refrescarOpciones(clave) {
    const cfg = SECCIONES[clave];
    if (!cfg) return;
    cfg.campos.filter(c => c.tipo === "select").forEach(c => {
        const select = document.getElementById(`${cfg.clave}-${c.id}`);
        const actual = select.value;
        select.innerHTML = c.opciones().map(o =>
            `<option value="${escapar(o.valor)}">${escapar(o.texto)}</option>`).join("");
        if ([...select.options].some(o => o.value === actual)) select.value = actual;
    });
}

async function cargarSeccion(clave) {
    const cfg = SECCIONES[clave];
    try {
        cfg.lista = await api(cfg.servicio, cfg.ruta);
        refrescarOpciones(clave);
        pintarTabla(clave);
        if (cfg.alCargar) cfg.alCargar(cfg.lista);
    } catch (e) {
        document.getElementById(`${clave}-filas`).innerHTML = filaVacia(e.message, cfg.columnas.length + 1);
    }
}

function pintarTabla(clave) {
    const cfg = SECCIONES[clave];
    const cuerpo = document.getElementById(`${clave}-filas`);
    const texto = cfg.buscar ? document.getElementById(`${clave}-buscar`).value.trim().toLowerCase() : "";
    const lista = cfg.lista.filter(x => !texto || cfg.buscar(x).toLowerCase().includes(texto));

    if (lista.length === 0) {
        cuerpo.innerHTML = filaVacia(texto ? "Ningun registro coincide con la busqueda." : cfg.vacio, cfg.columnas.length + 1);
        return;
    }
    cuerpo.innerHTML = lista.map(x => `
        <tr class="${cfg.editando === x[cfg.id] ? "seleccionada" : ""}">
            ${cfg.columnas.map(c => `<td>${c.valor(x)}</td>`).join("")}
            <td class="acciones-tabla">
                ${cfg.acciones ? cfg.acciones(x) : ""}
                <button class="boton boton-secundario boton-chico" onclick="editarRegistro('${clave}', ${x[cfg.id]})">Editar</button>
                <button class="boton boton-peligro boton-chico" onclick="eliminarRegistro('${clave}', ${x[cfg.id]})">Eliminar</button>
            </td>
        </tr>`).join("");
}

function editarRegistro(clave, id) {
    const cfg = SECCIONES[clave];
    const x = cfg.lista.find(r => r[cfg.id] === id);
    cfg.editando = id;
    cfg.campos.forEach(c => {
        const control = document.getElementById(`${clave}-${c.id}`);
        const valor = c.leer ? c.leer(x) : x[c.id];
        control.value = valor === null || valor === undefined ? "" : valor;
        if (c.soloAlCrear) control.required = false;
    });
    document.getElementById(`${clave}-titulo`).textContent = `Editar ${cfg.nombre}`;
    document.getElementById(`${clave}-guardar`).textContent = "Guardar cambios";
    document.getElementById(`${clave}-cancelar`).hidden = false;
    pintarTabla(clave);
    document.getElementById(`${clave}-${cfg.campos[0].id}`).focus();
}

function limpiarFormulario(clave) {
    const cfg = SECCIONES[clave];
    cfg.editando = null;
    document.getElementById(`${clave}-form`).reset();
    cfg.campos.forEach(c => {
        if (c.soloAlCrear) document.getElementById(`${clave}-${c.id}`).required = !!c.requerido;
    });
    refrescarOpciones(clave);
    document.getElementById(`${clave}-titulo`).textContent = `${cfg.nuevo} ${cfg.nombre}`;
    document.getElementById(`${clave}-guardar`).textContent = `Registrar ${cfg.nombre}`;
    document.getElementById(`${clave}-cancelar`).hidden = true;
    pintarTabla(clave);
}

function leerFormulario(clave) {
    const cfg = SECCIONES[clave];
    const valores = {};
    cfg.campos.forEach(c => {
        const v = document.getElementById(`${clave}-${c.id}`).value;
        valores[c.id] = typeof v === "string" ? v.trim() : v;
    });
    return valores;
}

async function guardarRegistro(clave) {
    const cfg = SECCIONES[clave];
    const boton = document.getElementById(`${clave}-guardar`);
    boton.disabled = true;
    try {
        const datos = cfg.aDatos(leerFormulario(clave), cfg.editando !== null);
        if (cfg.editando !== null) {
            await api(cfg.servicio, `${cfg.ruta}/${cfg.editando}`, "PUT", datos);
            mostrarMensaje("Cambios guardados");
        } else {
            await api(cfg.servicio, cfg.ruta, "POST", datos);
            mostrarMensaje(`${mayuscula(cfg.nombre)} registrad${cfg.hecho}`);
        }
        limpiarFormulario(clave);
        await cargarSeccion(clave);
    } catch (e) {
        mostrarMensaje(e.message, "error");
    } finally {
        boton.disabled = false;
    }
}

async function eliminarRegistro(clave, id) {
    const cfg = SECCIONES[clave];
    if (cfg.antesDeEliminar) {
        const error = cfg.antesDeEliminar(cfg.lista.find(r => r[cfg.id] === id));
        if (error) { mostrarMensaje(error, "error"); return; }
    }
    if (!confirm(`Eliminar ${cfg.este} ${cfg.nombre}?`)) return;
    try {
        await api(cfg.servicio, `${cfg.ruta}/${id}`, "DELETE");
        mostrarMensaje(`${mayuscula(cfg.nombre)} eliminad${cfg.hecho}`);
        if (cfg.editando === id) limpiarFormulario(clave);
        await cargarSeccion(clave);
    } catch (e) {
        mostrarMensaje(e.message, "error");
    }
}

function mayuscula(texto) {
    return texto.charAt(0).toUpperCase() + texto.slice(1);
}

function textoEstado(estado) {
    return estado ? estado.charAt(0) + estado.slice(1).toLowerCase().replaceAll("_", " ") : "";
}