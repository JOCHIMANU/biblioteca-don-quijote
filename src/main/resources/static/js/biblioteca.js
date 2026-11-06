/* Biblioteca de Don Quijote: microinteracciones (sello de lacre, paloma en vuelo, cuenta de la multa). */
(function () {
    'use strict';

    var sinMovimiento = window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches;

    // Sello de lacre: se estampa un instante antes de enviar el formulario.
    document.querySelectorAll('form.sello-form').forEach(function (form) {
        var yaEstampado = false;
        form.addEventListener('submit', function (e) {
            if (yaEstampado || sinMovimiento || !form.checkValidity()) { return; }
            var sello = form.querySelector('.sello-btn');
            if (!sello) { return; }
            e.preventDefault();
            yaEstampado = true;
            sello.classList.add('estampando');
            setTimeout(function () { form.submit(); }, 700);
        });
    });

    // Paloma mensajera: vuela desde el botón antes de enviar el formulario.
    document.querySelectorAll('form.despacho').forEach(function (form) {
        var enVuelo = false;
        form.addEventListener('submit', function (e) {
            if (enVuelo || sinMovimiento) { return; }
            var boton = form.querySelector('button');
            e.preventDefault();
            enVuelo = true;
            var caja = boton.getBoundingClientRect();
            var paloma = document.createElement('div');
            paloma.className = 'paloma-vuelo';
            paloma.innerHTML = '<svg viewBox="0 0 80 56" aria-hidden="true"><use href="#paloma"/></svg>';
            paloma.style.left = (caja.left + caja.width / 2 - 48) + 'px';
            paloma.style.top = (caja.top + caja.height / 2 - 34) + 'px';
            document.body.appendChild(paloma);
            boton.disabled = true;
            setTimeout(function () { form.submit(); }, 1100);
        });
    });

    // Filtros que se envían solos al cambiar la opción (sin manejadores en línea: lo prohíbe la CSP).
    document.querySelectorAll('select[data-autoenviar]').forEach(function (campo) {
        campo.addEventListener('change', function () { campo.form.submit(); });
    });

    // Acciones sin vuelta atrás (borrar, anular…): piden confirmación en un diálogo de la casa.
    var dialogo = document.getElementById('dialogo-confirmar');
    document.querySelectorAll('form[data-confirmar]').forEach(function (form) {
        var confirmado = false;
        form.addEventListener('submit', function (e) {
            if (confirmado) { return; }
            e.preventDefault();
            if (!dialogo || typeof dialogo.showModal !== 'function') {
                if (window.confirm(form.dataset.confirmar)) { confirmado = true; form.submit(); }
                return;
            }
            document.getElementById('dialogo-texto').textContent = form.dataset.confirmar;
            dialogo.returnValue = '';
            dialogo.addEventListener('close', function alCerrar() {
                dialogo.removeEventListener('close', alCerrar);
                if (dialogo.returnValue === 'si') { confirmado = true; form.submit(); }
            });
            dialogo.showModal();
        });
    });

    // Campos con búsqueda: escribir filtra en el servidor (máx. 10 sugerencias), así aguantan miles de registros.
    document.querySelectorAll('.combo').forEach(function (combo) {
        var entrada = combo.querySelector('.combo-entrada');
        var multiple = combo.classList.contains('combo-multi');
        // En modo simple hay un campo oculto con el id; en modo múltiple cada etiqueta lleva el suyo.
        var oculto = multiple ? null : combo.querySelector(':scope > input[type="hidden"]');
        var lista = combo.querySelector('.combo-lista');
        var etiquetas = multiple ? combo.querySelector('.elegidos') : null;
        var cuenta = multiple ? combo.querySelector('.cuenta-libros') : null;
        var activo = -1;
        var demora = null;
        var peticion = null;
        if (oculto && oculto.value) { combo.classList.add('elegido'); }

        var idsElegidos = function () {
            return etiquetas ? Array.prototype.map.call(etiquetas.querySelectorAll('li'), function (li) { return li.dataset.id; }) : [];
        };
        var contar = function () {
            if (!cuenta) { return; }
            var n = idsElegidos().length;
            cuenta.textContent = n === 0 ? '' : (n === 1 ? '1 libro en este préstamo' : n + ' libros en este préstamo');
        };
        var agregar = function (id, etiqueta) {
            if (idsElegidos().indexOf(String(id)) !== -1) { return; }
            var li = document.createElement('li');
            li.dataset.id = id;
            var texto = document.createElement('span');
            texto.className = 'etiqueta';
            texto.textContent = etiqueta;
            var campo = document.createElement('input');
            campo.type = 'hidden';
            campo.name = 'libroIds';
            campo.value = id;
            var quitar = document.createElement('button');
            quitar.type = 'button';
            quitar.className = 'quitar';
            quitar.title = 'Quitar de la lista';
            quitar.setAttribute('aria-label', 'Quitar ' + etiqueta);
            quitar.innerHTML = '&times;';
            li.appendChild(texto);
            li.appendChild(campo);
            li.appendChild(quitar);
            etiquetas.appendChild(li);
            contar();
        };
        if (etiquetas) {
            contar();
            etiquetas.addEventListener('click', function (e) {
                var boton = e.target.closest('.quitar');
                if (!boton) { return; }
                boton.closest('li').remove();
                contar();
                entrada.focus();
            });
        }

        var cerrar = function () {
            lista.hidden = true;
            entrada.setAttribute('aria-expanded', 'false');
            activo = -1;
        };
        var abrir = function () {
            lista.hidden = false;
            entrada.setAttribute('aria-expanded', 'true');
        };
        var elegir = function (li) {
            if (multiple) {
                // la etiqueta se agrega a la lista y el buscador queda libre para el siguiente libro
                agregar(li.dataset.id, li.dataset.etiqueta);
                entrada.value = '';
                buscar();
                return;
            }
            oculto.value = li.dataset.id;
            entrada.value = li.dataset.etiqueta;
            combo.classList.add('elegido');
            cerrar();
        };
        var marcar = function (indice) {
            var items = lista.querySelectorAll('li[data-id]');
            if (!items.length) { return; }
            activo = (indice + items.length) % items.length;
            items.forEach(function (li, i) { li.classList.toggle('activo', i === activo); });
            items[activo].scrollIntoView({ block: 'nearest' });
        };
        var pintar = function (sugerencias) {
            lista.innerHTML = '';
            activo = -1;
            if (!sugerencias.length) {
                var vacio = document.createElement('li');
                vacio.className = 'sin-resultados';
                vacio.textContent = combo.dataset.vacio;
                lista.appendChild(vacio);
                return;
            }
            sugerencias.forEach(function (s) {
                var li = document.createElement('li');
                li.setAttribute('role', 'option');
                li.dataset.id = s.id;
                li.dataset.etiqueta = s.etiqueta;
                var titulo = document.createElement('span');
                titulo.textContent = s.etiqueta;
                var detalle = document.createElement('small');
                detalle.textContent = s.detalle;
                li.appendChild(titulo);
                li.appendChild(detalle);
                // mousedown (no click) para elegir antes de que el campo pierda el foco
                li.addEventListener('mousedown', function (e) { e.preventDefault(); elegir(li); });
                lista.appendChild(li);
            });
        };
        var buscar = function () {
            if (peticion) { peticion.abort(); }
            peticion = new AbortController();
            lista.innerHTML = '<li class="buscando">Buscando en los anaqueles…</li>';
            abrir();
            var url = combo.dataset.url + '?q=' + encodeURIComponent(entrada.value.trim());
            if (multiple && idsElegidos().length) { url += '&excluir=' + idsElegidos().join(','); }
            fetch(url, { signal: peticion.signal, headers: { 'Accept': 'application/json' } })
                .then(function (r) { return r.json(); })
                .then(pintar)
                .catch(function (e) { if (e.name !== 'AbortError') { cerrar(); } });
        };

        entrada.addEventListener('input', function () {
            // si cambia el texto, la elección anterior ya no vale (en modo múltiple las etiquetas se quitan con la ×)
            if (oculto) {
                oculto.value = '';
                combo.classList.remove('elegido');
            }
            clearTimeout(demora);
            demora = setTimeout(buscar, 200);
        });
        entrada.addEventListener('focus', function () { if (!oculto || !oculto.value) { buscar(); } else { entrada.select(); } });
        entrada.addEventListener('blur', function () { clearTimeout(demora); cerrar(); });
        entrada.addEventListener('keydown', function (e) {
            if (e.key === 'ArrowDown') { e.preventDefault(); if (lista.hidden) { buscar(); } else { marcar(activo + 1); } }
            else if (e.key === 'ArrowUp') { e.preventDefault(); marcar(activo - 1); }
            else if (e.key === 'Enter' && !lista.hidden && activo >= 0) {
                e.preventDefault();
                elegir(lista.querySelectorAll('li[data-id]')[activo]);
            }
            else if (e.key === 'Escape') { cerrar(); }
        });
    });

    // Lectores: al teclear los 8 dígitos del DNI se pide el nombre al servidor (que consulta a Decolecta).
    var campoDni = document.getElementById('dni');
    var campoNombre = document.getElementById('nombre');
    var estadoDni = document.getElementById('dni-estado');
    if (campoDni && campoNombre && estadoDni && campoDni.dataset.consulta) {
        var pendiente = null;
        var ultimoAutocompletado = '';
        var textoInicial = estadoDni.textContent;

        var avisar = function (texto, clase) {
            estadoDni.textContent = texto;
            estadoDni.className = 'ayuda-dni' + (clase ? ' ' + clase : '');
        };

        campoDni.addEventListener('input', function () {
            if (pendiente) { pendiente.abort(); pendiente = null; }
            var dni = campoDni.value.trim();
            if (!/^\d{8}$/.test(dni)) { avisar(textoInicial); return; }

            pendiente = new AbortController();
            avisar('Consultando el registro', 'buscando');
            fetch(campoDni.dataset.consulta + dni, { signal: pendiente.signal, headers: { 'Accept': 'application/json' } })
                .then(function (r) { return r.json().then(function (cuerpo) { return { ok: r.ok, cuerpo: cuerpo }; }); })
                .then(function (res) {
                    if (!res.ok) { avisar(res.cuerpo.error || 'No se pudo consultar el DNI.', 'mal'); return; }
                    // No se pisa lo que el bibliotecario haya escrito a mano.
                    if (campoNombre.value.trim() === '' || campoNombre.value === ultimoAutocompletado) {
                        campoNombre.value = res.cuerpo.nombre;
                        ultimoAutocompletado = res.cuerpo.nombre;
                    }
                    avisar('Nombre hallado en el registro de RENIEC. Podéis corregirlo si hiciera falta.', 'ok');
                })
                .catch(function (e) {
                    if (e.name !== 'AbortError') { avisar('No se pudo consultar el DNI; escribid el nombre a mano.', 'mal'); }
                });
        });
    }

    // Devolución: recalcula la multa al cambiar la fecha, con la misma regla del servidor.
    var recalculo = document.querySelector('form.recalculo');
    if (recalculo) {
        var entrada = recalculo.querySelector('input[name="fecha"]');
        var oculta = document.getElementById('fecha-confirmada');
        var cuenta = document.getElementById('cuenta');
        var prestamo = recalculo.dataset.prestamo;
        var plazo = parseInt(recalculo.dataset.plazo, 10);
        var porDia = parseInt(recalculo.dataset.multaDia, 10);

        var dias = function (desde, hasta) {
            return Math.round((Date.parse(hasta + 'T00:00:00Z') - Date.parse(desde + 'T00:00:00Z')) / 86400000);
        };

        var actualizar = function () {
            if (!entrada.value) { return; }
            var retraso = Math.max(0, dias(prestamo, entrada.value) - plazo);
            var multa = retraso * porDia;
            oculta.value = entrada.value;
            document.getElementById('multa-monto').textContent = multa;
            document.getElementById('multa-unidad').textContent = multa === 1 ? 'sol' : 'soles';
            document.getElementById('multa-detalle').textContent = multa > 0
                ? retraso + ' día(s) de atraso × ' + porDia + ' sol por día'
                : 'Llega dentro del plazo: sin multa.';
            cuenta.classList.toggle('con-multa', multa > 0);
            cuenta.classList.toggle('sin-multa', multa === 0);
        };

        entrada.addEventListener('input', actualizar);
        entrada.addEventListener('change', actualizar);
        recalculo.addEventListener('submit', function (e) { e.preventDefault(); actualizar(); });
    }
})();
