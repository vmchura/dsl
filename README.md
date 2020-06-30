# dsl
Objetivos principales:
-   Ayudar a DeathFate a organizar y ordenar los torneos DSL
-   Interpretar los resultados de los torneos DSL para conocer mejor a los jugadores.
-   Divertirse jugando StarCraft, divertirse programando... y por Aiur!.

#### requerimientos hito 1
-   Sólo permitir registrarse enlazando la cuenta de discord
-   Sólo permitir registrar a usuarios registrados en un servidor de discord (en este caso el servidor administrado por DeathFate)
-   El usuario debe poder visualizar las rondas/matchs donde participará.
-   El usuario debe poder subir/cargar replays de las partidas en las rondas/matchs donde participe.

##### requerimientos hito 2
-   Permitir la visualización de las rondas/resultados de los torneos 
-   Permitir la descarga de replays de los torneos


##### requerimiento hito 3
-   Panel de estadísticas Torneo/Jugador

##### requerimiento hito 4
-   Permitir el registro de "invitado" por discord
-   Habilitar ventajas anteriormente mencionadas (de visualización y descarga de replays) a los invitados.


# Start Server
- DB

start db on arch:
    sudo systemctl start mongodb.service
    
database to use: dsl


