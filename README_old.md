# dsl
Objetivos principales:
-   Ayudar a DeathFate a organizar y ordenar los torneos DSL
-   Interpretar los resultados de los torneos DSL para conocer mejor a los jugadores.
-   Divertirse jugando StarCraft, divertirse programando... y por Aiur!.

#### requerimientos hito 1
-   Sólo permitir registrarse enlazando la cuenta de discord
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


#Descripción del proceso del seguimiento del torneo

Antes de hacer el seguimiento a un torneo se necesitarán algunos requisitos.
-   El ID del servidor de discord donde ya estén los participantes reunidos.
    -   Ejemplo: el ID del servidor de Deathfate es 699897834685857792

-   Agregar al servidor de discord al bot "dsl-analytics" que sólo requerirá el permiso de ingresar para poder acceder a qué usuarios pertecen al canal.

-   El ID del torneo creado en challonge donde ya estén los participantes registrados.
    -   Ejemplo: el ID del torneo de DSL2 es: DeathfateStarLeague2
    
A los usuarios del servidor de Discord se les llamara               : DisUser
A los nombres registrados en el torneo de Challonge se les llamará  : ChaName

La idea es relacionar a un ChaName con sólo un DiUser.

Para un primer intento se optará por un método semi-automático.

Si sólo exisite un único ChaName: VmChQ y sólo hay un DisUser con VmChQ, entonces la relación es automática.
en cualquier otro caso la relación debe realizarme manualmente, por ejemplo:

ZtiiK y ztiik no deberían considerarse automáticamente lo mismo. 

**Ya que cada torneo puede desarrollarse de diferente manera no será posible volver a implementar TODAS las características de Challonge, sino sólo se listarán las partidas tratando de mostrar a qué ronda o bracket corresponden.**

Por lo pronto, cada DisUser podrá visualizar todas los matches donde participa (seleccionando previamente el torneo)

En cada match que un DisUser pertenezca él mismo podrá subir un archivo/replay del juego.
Se podrá subir más de un archivo.
No se podrá eliminar un archivo ya subido, sólo se podrá marcar como "error". La idea de esta comportamiento es para que los archivos queden para posterioridad del conocimiento humano.

Una vez subido/cargado este archivo faltaría relacionar a los jugadores de la replay, por lo pronto se asumirán 1vs1, así que ambos DisUser asignados al match deberían estar presentes en el replay como jugadores.

Para hacer esta relación se ejecutará un pequeño algoritmo que tratará de adivinar la relación, en caso de no acertar, los jugadores o un administrador  externo podrá realizar esta relación.
Este comportamiento se realizará porque puede ocurrir que los jugadores estén registrados en en Discord como Jugador1 y Jugador2, pero en el replay sólo aparezcan con sus nick PlayerX y GamerZ.

Si todo lo anterior llega a realizar con éxito, entonces se tiene toda la información necesaria para obtener información del desenvolvimiento de los jugadores en el torneo.




#### Algunos criterios más técnicos
Los torneos a seguir se guardarán en la colección: dsl.tournaments
registro de dsl.tournaments:
tournamentid, challongid, tournamentid, discordserverid, active

los nombres ChaName y la relación con los DisUser e guardarán en la colección:  dsl.participants 
registro de dsl.participants:
tournamentid, chanameid, discorduserid?, userid?

la combinación (tournamentid,chanameid) es única o primary key. 


los matches/partidos se guardarán en la colección dsl.matches
registro de dsl.matches:
matchid, tournamentid, challongmatchid, chanameid1, chanameid2, disuserid1, disuserid2


####### Fin de criterios más técnicos




# Start Server
- DB

start db on arch:
    sudo systemctl start mongodb.service
    
database to use: dsl


