import json
import requests

HOST_TO = 'localhost'
INDEX_TO = 'mmf-airport'
TYPE = "airport"
SEED_FILE_PATH = '/Users/arko/Downloads/mg.sql'

elastic_to_index = "http://" + HOST_TO + ":9200/" + INDEX_TO


def getElements(data):
    elements = []
    str = ""
    break_at_comma = True
    prev_char = ''
    for c in data:
        if prev_char != '\\' and c == "'":
            break_at_comma = not break_at_comma
        else:
            if break_at_comma and c == ',':
                elements.append(str)
                str = ""
            else:
                str += c
        prev_char = c
    elements.append(str)
    return elements


# Delete target index if it exists
res = requests.get(elastic_to_index)
if res:
    res = requests.delete(elastic_to_index)
    print("Index deleted")

create_index_response = requests.put(elastic_to_index)
print(create_index_response.text)

with open(SEED_FILE_PATH) as f:
    content = f.readlines()
    count = 0
    for line in content:
        if line.startswith("INSERT INTO `mmt` VALUES ("):
            line = line.replace("INSERT INTO `mmt` VALUES (", "")
            dataset = line.split("),(")
            size = len(dataset)
            print(size)
            for i in range(size):
                data = dataset[i]
                if (i == size - 1):
                    data = data.replace(");", "")
                data = getElements(data)
                datadict = {
                    "id": int(data[0]),
                    "code": data[1],
                    "lat": float(data[2]),
                    "lon": float(data[3]),
                    "name": data[4],
                    "rating": float(data[5]),
                    "city": data[6],
                    "state": data[7],
                    "country": data[8],
                    "tz": data[9],
                    "type": data[10],
                    "url": data[11]
                }
                elev = data[12]
                if elev:
                    datadict['elev'] = int(elev)
                directflights = data[12]
                if directflights:
                    datadict['directFlights'] = int(directflights)
                if not json.loads(requests.post('{}/{}/{}'.format(elastic_to_index, TYPE, datadict['id']),
                                                data=json.dumps(datadict)).text)['created']:
                    break
                count += 1
                if (count % 100 == 0):
                    print (count)
    print (count)

    # data = {
    #     "id": 1,
    #     "code": "AAA",
    #     "lat": -17.3595,
    #     "lon": -145.494,
    #     "name": "Anaa Airport",
    #     "rating": 3.87,
    #     "city": "Anaa",
    #     "state": "Tuamotu-Gambier",
    #     "country": "French Polynesia",
    #     "tz": "Pacific/Midway",
    #     "type": "Airports",
    #     "url": "",
    #     "elev": 7,
    #     "direct_flights": 2
    # }
