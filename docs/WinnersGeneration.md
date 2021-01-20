Process to add winners to a tournament

A user identified as admin
* it can select a tournament
* also select a tournament series
* also select 3 players with an order
* give a season to the series

This information is enough to process

The information is formed based on actors

WinnersGatheredActor [Actor]
Is responsable of gathering all information necessary to send to the UI

Externally

Receives: Gather
Response: WinnersGatheredInfo


WinnersSaverActor [Actor]
Is responsable of save all at one all information
Externally

Receives: Save(WinnersGatheredInfoFilled)
Response: Success







