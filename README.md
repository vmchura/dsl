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
     
6.  Discord BOT, subir archivos a un canal de discord.
    1.  Conseguir discordBotToken: "qwdasd4....d2131lk" (59 caracteres varios)
    2.  Agregar el bot al guild de discord con el siguiente enlace: https://discord.com/api/oauth2/authorize?client_id=$discordClientID&scope=bot&permissions=524288
            1.  Con el número 524288 el bot tiene permiso para conocer a los integrantes del guild. 
    3.  [**OPCIONAL**]Agregar el bot al guild de discord con el siguiente enlace: https://discord.com/api/oauth2/authorize?client_id=$discordClientID&scope=bot&permissions=34816
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

## Pasos para crear un torneo

Para este paso se debe ser Admin en la aplicación y haber realizado el paso Opcional Discord BOT
1.  Ir a la dirección: /tournament/create
2.  En discordGuildID escribir el ID del canal de discord (18 números)
3.  En challongeID escribir el ID del torneo en challonge, por ejemplo del torneo https://challonge.com/bq8mhaqk su ID es bq8mhaqk
4.  [**OPCIONAL**] si el módulo de subir replays a un canal de discord está habilitado usar el ID del canal (18 números)
5.  Enviar formulario
6.  El torneo se creó, pero falta relacionar a los participantes de Challonge con los integrantes del canal de Discord.
7.  En el panel que se redirecciona realizar las mencionadas relaciones, se implementó un algoritmo para que el emparejamiento sea simple, pero en caso de ser necesario buscar en la lista de opciones al integrante de discord adecuado.

## Agregar bot al servidor
clientid = 728055666610274325
permisos = 592896
Permisos de View Server insights
View Channels
Send Messages
Read Messsage History

https://discord.com/api/oauth2/authorize?client_id=728055666610274325&scope=bot&permissions=592896

## Add roles based on tournaments

ROLES: UUID message
ROLES REDEMPTION: UUID message

## database management
# dump
mongodump --uri="" --out="/home/vmchura/Documents/006.ExtraCurriculares/DSL_DB_BU/FECHA"
# restore
mongorestore --uri="mongodb://localhost:27017/?readPreference=primary&appname=MongoDB%20Compass&ssl=false" --drop "/home/vmchura/Documents/006.ExtraCurriculares/DSL_DB_BU/FECHA"

## Licencia

La licencia está bajo los términos de  Apache License, Version 2.0, January 2004
Más detalles en el archivo LICENSE.

## Intención de colaboración
Cualquier aporte a este software es bienvenido, pero ese aporte también se hará en los términos de la LICENSE.


 
                             
# Crear torneos
/tournament/create

DSSL 9
http://www.deathfate.net/tournament/showparticipantscorrelation/10086769/722170993371775067/866364844999180318
