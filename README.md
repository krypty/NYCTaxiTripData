# Welcome to the NYCTaxiTripData wiki!

## Goals

Le projet vise à identifier les régions de NewYork les plus favorables pour un chauffeur de taxi. L'idée est de définir dans quel district de NY il est le plus susceptible de retrouver au plus vite un nouveau client.

On cherche à minimiser le temps entre deux courses, temps durant lequel il ne gagne pas d'argent, et donc maximiser le temps de qu'il passe à transporter un client.

On visualise ces données à l'aide d'une graphique colorimétrique superposé aux 51 districts de NY. On utilise une échelle de couleur de bleu à rouge, afin de montrer les densités.

![NY districts](http://img15.hostingpics.net/pics/454243outputjanvier.png)

## Setup
### Get Data
    mkdir taxidata
    cd taxidata
    wget https://nyctaxitrips.blob.core.windows.net/data/trip_data_1.csv.zip
    unzip trip_data_1.csv.zip
    head -n 10 trip_data_1.csv

## Ce que l'on a

Exemple d'entrée du fichier csv

| medallion | hack_license | vendor_id | rate_code | store_and_fwd_flag | pickup_datetime | dropoff_datetime | passenger_count | trip_time_in_secs | trip_distance | pickup_longitude | pickup_latitude | dropoff_longitude | dropoff_latitude |
| -------- | -------- | -------- | -------- | -------- | -------- | -------- | -------- | -------- | -------- | -------- | -------- | -------- | -------- |
| 89D227B655E5C82AECF13C3F540D4CF4 | BA96DE419E711691B9445D6A6307C170 | CMT | 1 | N | 2013-01-01 15:11:48 | 2013-01-01 15:18:10 | 4 | 382 | 1.00 | -73.978165 | 40.757977 | -73.989838 | 40.751171 |


## De quoi on a besoin

On doit séparer les quartiers et les régions de NewYork, pour ça, on utilise : 
- https://github.com/dwillis/nyc-maps/blob/master/boroughs.geojson
- https://github.com/dwillis/nyc-maps/blob/master/city_council.geojson

## Librairies

- [Scala-CSV](https://github.com/tototoshi/scala-csv)
- [Spark-CSV](https://github.com/databricks/spark-csv)
- [JSON](https://www.playframework.com/documentation/2.3.x/ScalaJson)
- [JSON extension](https://github.com/mandubian/play-json-zipper)

## Minestrone 1
* Calculer le temps d'attente pour 1 chauffeur de taxi
  * Il pose un client dans un quartier --> on obtient t0 et on note le quartier de dépot q
  * Il retrouve un client à l'instant t1 --> on calcule deltaT = t1 -t0 et on associe ce temps à la paire (q, deltaT)
  * On somme ensuite les temps d'attente avec un groupBy quartier de dépot q
  * Finalement, on attribue linéairement une couleur HSB [0, 1] au temps moyen du quartier de dépot
