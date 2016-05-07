import json

import requests

HOST_TO = 'localhost'
INDEX_TO = 'mmf-airport'
TYPE = "airport"

elastic_to_index = "http://" + HOST_TO + ":9200/" + INDEX_TO

# Delete target index if it exists
res = requests.get(elastic_to_index)
if res:
    res = requests.delete(elastic_to_index)
    print("Index deleted")

create_index_response = requests.put(elastic_to_index)
print(create_index_response.text)

data = {
    "id": 1,
    "code": "AAA",
    "lat": -17.3595,
    "lon": -145.494,
    "name": "Anaa Airport",
    "rating": 3.87,
    "city": "Anaa",
    "state": "Tuamotu-Gambier",
    "country": "French Polynesia",
    "tz": "Pacific/Midway",
    "type": "Airports",
    "url": "",
    "elev": 7,
    "direct_flights": 2
}
print(requests.post(elastic_to_index + "/" + TYPE, data=json.dumps(data)).text)
