# 💬 Chatti 

![Java](https://img.shields.io/badge/Java-21-orange?style=flat-square&logo=java)
![JavaFX](https://img.shields.io/badge/JavaFX-GUI-blue?style=flat-square)
![MongoDB](https://img.shields.io/badge/MongoDB-NoSQL-green?style=flat-square&logo=mongodb)
![TCP Sockets](https://img.shields.io/badge/Network-TCP%20Sockets-lightgrey?style=flat-square)

Chatti es una aplicación de mensajería de escritorio de arquitectura cliente-servidor, diseñada para ofrecer un espacio de comunicación social, seguro y escalable. Inspirada en plataformas modernas de comunidades, permite la interacción tanto en grupos masivos como en chats uno a uno, soportando transferencia de archivos y encriptación.

## ✨ Características Principales

*   **Comunidades e Hilos (Threads):** Creación de comunidades privadas o públicas con canales temáticos (threads) y gestión de roles (Administrador, Moderador, Usuario).
*   **Chats Privados y Grupales:** Conversaciones directas (P2P) con encriptación de extremo a extremo y soporte para grupos de amigos.
*   **Chat Aleatorio (Random Chat):** Sistema de emparejamiento espontáneo para conocer a nuevos usuarios basados en intereses (Tags).
*   **Transferencia Multimedia:** Envío de imágenes y archivos de cualquier formato (hasta 10 MB) codificados en Base64.
*   **Seguridad y Auditoría:** Autenticación segura, registro de auditoría (Audit Logs) para canales comunitarios y sistema de baneos/bloqueos.
*   **Interfaz Personalizable:** Desarrollada con JavaFX, incluye modo claro/oscuro dinámico y personalización de temas mediante CSS.

## 🛠️ Tecnologías Utilizadas

### Cliente
*   **Lenguaje:** Java 21
*   **Framework UI:** JavaFX (con integración de ControlsFX)
*   **Gestión de Vistas:** Controladores FXML y componentes customizados (ListCells, TagPanes).

### Servidor
*   **Lenguaje:** Java 21
*   **Red:** Sockets TCP puros (`ServerSocket`, `DataInputStream`, `DataOutputStream`).
*   **Base de Datos:** MongoDB Atlas (Driver oficial para operaciones NoSQL fluidas y almacenamiento escalable).
*   **Procesamiento:** Arquitectura Multi-hilo (Thread-per-client) para servir conexiones concurrentes.

## 🏗️ Arquitectura de Datos

El sistema utiliza una base de datos NoSQL documental (MongoDB) optimizada para mensajería:
*   `Users`: Almacena credenciales, tags de intereses y foto de perfil (Base64).
*   `Communities` / `Threads`: Gestiona los ecosistemas grupales, listas de acceso y registros de auditoría inmutables.
*   `Messages`: Documentos atómicos que almacenan texto, fecha, ID de origen y payloads de archivos embebidos.
*   `Convos` / `Groups`: Relacionan a múltiples usuarios con arrays de IDs de mensajes para cargas eficientes (paginación o lazy-loading).

## 🚀 Instalación y Despliegue

### Requisitos previos
*   Java Development Kit (JDK) 21 o superior.
*   (Opcional) Un clúster de MongoDB local o en Atlas si deseas levantar tu propio servidor.

### Ejecución local
* 1º Ejecuta el archivo en chatti-server/src/main/java/ies/dam/servidor/Server.java para la inicialización del servidor en local.
* 2º Selecciona la base de datos default con la consola de la terminal.
* 3º Ejecuta el archivo en chatti-client/src/main/java/ies/dam/client/chatticlient/ClientApplication.java para ejecutar una instancia de usuario
* 4º Crea una cuenta o en su defecto usa la cuenta de usuario: victor1 y psw: 1234.
* 5º Si quieres probar el chat deberas iniciar otra instancia con un usuario nuevo o usar la cuenta de usuario: victor2 y psw: 1234.
