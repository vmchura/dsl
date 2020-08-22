# DSL


## Contenidos

1. [Información general](#Información-general)
2. [Tecnologías](#Tecnologías)
3. [Instalación](#Instalación)
4. [Licencia](#Licencia)

## Información general
Aplicación web para brindar una mejor experiencia a los participantes de los torneos DSL de StarCraft, actualmente alojado en
www.deathfate.gg
## Tecnologías
DSL web está implementado con las siguientes principales tecnologías.

**Backend - Lado del servidor**

*   Scala (https://scala-lang.org/)
*   Play Framework (https://www.playframework.com/)
*   MongoDB (https://www.mongodb.com/)

**Frontend - Lado del usuario**

*   Binding.scala (https://github.com/ThoughtWorksInc/Binding.scala)
*   Bootstrap V5 (https://v5.getbootstrap.com/)

**Entorno de desarrollo**
*   ItelliJ IDEA

## Instalación

Se hace énfasis en el entorno de desarrollo porque facilitará la descripción de la siguiente guía.


1.  Instalar javasdk (al menos la versión 8)
2.  Instalar la última versión de IntelliJ IDEA community o ultimate. (https://www.jetbrains.com/idea/)
3.  Instalar mongodb (https://docs.mongodb.com/manual/installation/)
    1. La instalación dependerá del sistema operativo. (https://www.tutorialspoint.com/mongodb/mongodb_environment.htm)
    2. Conseguir una línea de conexión válida, con los permisos para leer y escribir, la línea de conexión es similar a : **"mongodb://dsluser:dspass@localhost:27017/dsl"** 
        Pasos generales para conseguir esta línea de conexión en tu computadora, o puedes usar una de administrador
        1. Crear una base de datos, por ejemplo **dsl**   (https://www.tutorialspoint.com/mongodb/mongodb_create_database.htm)
        2. Crear un usuario, por ejemplo **dsluser** y agregarlo a la base de datos **dsl** con los permisos de escribir y leer(https://docs.mongodb.com/manual/reference/method/db.createUser/)                       
4.  Challonge: Generar una API KEY en https://challonge.com/settings/developer
    1. challongeApiKey: "adj...ad1" (40 caracteres)
5.  Discord: Login, crear un aplicación de Discord en https://discord.com/developers/applications
    1.  Del panel "General Information" conseguir discordClientID: "123456789012345678" (18 dígitos)
    2.  Del panel "General Information" conseguir discordClientSecret: "abscs.....314a" (32 caracteres alfanuméricos)
    3.  En el panel "OAuth2" agregar la url de redirección: http://localhost:9000/authenticate/discord
    4.  En producción esta url debe ser actualizada acorde y modificada en silhouette.discord.redirectURL=${?REDIRECT_URL}
     
6.  [**OPCIONAL**]Discord BOT, subir archivos a un canal de discord.
    1.  Conseguir discordBotToken: "qwdasd4....d2131lk" (59 caracteres varios) 
    2.  Agregar el bot al guild de discord con el siguiente enlace: https://discord.com/api/oauth2/authorize?client_id=$discordClientID&scope=bot&permissions=34816
        1.  Con el número 34816 el bot tiene permiso para escribir mensajes.     
7.  [**Opcional**] Dropbox: Generar una app en https://www.dropbox.com/developers/documentation (App Console) y conseguir access token
    1.  dropboxAccessToken: "qwdq...qj3" (64 caracteres)
8.  [**Opcional**] AWS Lambda: Para procesar las replays.
    1. Crear un terminal Lambda - REST usando la función: https://github.com/vmchura/screplambda
    2. Realizar los pasos mencionados para en la documentación oficial de AWS y obtener lambdaApiKey
        1. lambdaApiKey: "adqow41r...qwe" (40 caracteres) 
    
9.  **IMPORTANTE** Crear una copia del archivo secretkeystemplate.conf y llamarlo localhost.conf, ubicar todas las claves/apis/secrey keys/etc en el nuevo localhost.conf, este archivo no se le hace seguimiento por git por lo que sólo se mantiene en la computadora local.

## Ejecución

1.  Desde IntelliJ abrir el proyecto y esperar a que termine de descargar/indexar todos los componentes (toma un largo tiempo la primera vez)
2.  Abrir la consola sbt o **sbt shell**
3.  Escribir los siguientes comandos:
    ```
    sbt> project server
    sbt> run
    ```
4.  En el navegador http://localhost:9000 debería visualizarse la página inicial.


## Licencia

La licencia está bajo los términos de  Apache License, Version 2.0, January 2004
Más detalles en el archivo LICENSE.

## Intención de colaboración
Cualquier aporte a este software es bienvenido, pero ese aporte también se hará en los términos de la LICENSE.


 
                             
