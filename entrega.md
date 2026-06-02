# Tercera Entrega Proyecto BD 2026-10

# Sistema de Gestión de Experiencias Mundialistas (SGEM)

## Copa Mundial de la FIFA 2026

# Historias de usuario a implementar

## Historia de Usuario 1

Mariana López llega por primera vez a FanExperience para adquirir una experiencia del mundial.

La recepcionista solicita sus datos personales y los registra en el sistema:

* Tipo y número de documento
* Nombre completo
* Correo electrónico
* Teléfono

Luego, Mariana selecciona la experiencia “México vs Japón – Experiencia VIP”.
El sistema muestra:

* Precio base
* Servicios opcionales disponibles
* Cupos restantes

Mariana selecciona:

* Transporte aeropuerto-estadio
* Kit de souvenirs
* Traducción simultánea

El sistema calcula automáticamente:

* Precio base de la experiencia
* Costos adicionales de servicios
* Impuestos asociados a cada servicio
* Valor total de la inscripción

Finalmente:

* El sistema registra el pago realizado con tarjeta de crédito.
* Se genera el comprobante de inscripción.
* Se genera el número de reserva de manera automática.

---

## Historia de Usuario 2

El coordinador Carlos Ramírez atiende a Mariana López días antes del partido.

Carlos ingresa el número de reserva en el sistema y se muestran:

* Datos del cliente
* Experiencia adquirida
* Servicios adicionales seleccionados
* Historial de experiencias anteriores del cliente

Durante la atención, Mariana solicita:

* Agregar un acceso VIP adicional
* Cambiar el servicio de alimentación estándar por alimentación premium

Carlos registra las modificaciones y el sistema recalcula automáticamente:

* Subtotal de servicios
* Impuestos aplicables
* Nuevo valor total de la experiencia

Posteriormente, Carlos registra una novedad indicando que la cliente requiere asistencia especial de movilidad durante el evento.

Finalmente:

* El sistema genera una ventana con el resumen actualizado de la experiencia.
* El sistema genera otra ventana con el detalle de pagos e impuestos para impresión.

---

# Otras funcionalidades a implementar

* Una vez se registre una modificación importante en una experiencia, dicha novedad debe agregarse automáticamente al historial del cliente mediante un trigger de base de datos.
* Cuando un cliente se inscriba a una experiencia:
    * El sistema debe validar automáticamente que existan cupos disponibles.
    * Si no hay cupos, no debe permitir el registro.
* El cálculo del valor total debe incluir automáticamente:
    * Precio base
    * Servicios adicionales
    * Impuestos asociados a cada servicio
* Al registrar pagos:
    * El sistema debe actualizar automáticamente el saldo pendiente de la inscripción.

---

# Entregables

* Código fuente del proyecto Java
* Script de creación de base de datos Oracle
* Scripts SQL de inserción de datos
* Modelo relacional actualizado

---

# Notas

i. El proyecto debe implementarse utilizando JDBC o JPA, pero no se pueden combinar ambas alternativas.

ii. La interfaz gráfica no debe realizar llamadas directas JDBC o JPA; se deben utilizar patrones adecuados (DAO, Controller, Service, etc.).

iii. Sólo se calificarán funcionalidades cuya persistencia esté completa; todas las capas de la aplicación deben estar implementadas.

iv. Se descontarán puntos por malas prácticas de programación, especialmente código “quemado”.

v. Toda la información debe obtenerse desde la base de datos.

vi. No se debe implementar conexión real con impresora; únicamente se debe mostrar en pantalla la información que se imprimiría.

---

# Consultas mínimas requeridas

1. Experiencias disponibles por ciudad, estadio o partido.
2. Clientes con mayor número de inscripciones.
3. Total de ingresos generados por cada guía.
4. Experiencias más demandadas.
5. Total de impuestos cobrados por país sede.
6. Clientes con pagos pendientes.
7. Cupos disponibles por experiencia.

---

# Distribución de la nota

| Componente | Porcentaje |
| :--- | :--- |
| Modelo Relacional / ER | 15% |
| Modelo Físico Oracle | 15% |
| Implementación completa Historia de Usuario 1 | 25% |
| Implementación completa Historia de Usuario 2 | 25% |
| Triggers y reglas de negocio | 10% |
| Consultas SQL requeridas | 10% |

**Nota. La sustentación será el 80% de la nota, el otro 20% será la nota de la entrega antes de la sustentación. La sustentación puede ser una modificación de la aplicación , una nueva consulta o cualquier otra modificación, la cual será la misma para todo el grupo, pero deberá llevarse a cabo de forma individual.

---

# Consideraciones adicionales de evaluación

* No se asignará puntaje parcial a funcionalidades incompletas entre capas. Para obtener puntaje, la funcionalidad debe estar implementada completamente:
    * Interfaz gráfica
    * Lógica de negocio
    * Persistencia en base de datos
* Las funcionalidades implementadas únicamente en interfaz o únicamente en base de datos serán consideradas incompletas
* Todas las consultas deben retornar resultados validos utilizando los datos entregadosen los scripts de inserción
