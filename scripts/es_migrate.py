import json
import requests
import time

HOST_TO = 'localhost'
INDEX_TO = 'mmf-airport'
TYPE = "airport"
SEED_FILE_PATH = '/Users/arko/Downloads/mg.sql'
RECREATE_INDEX = True


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


elastic_to_index = "http://" + HOST_TO + ":9200/" + INDEX_TO

settings = {
    "analysis": {
        "analyzer": {
            "termIndex": {
                "type": "custom",
                "tokenizer": "keyword",
                "filter": "lowercase"
            }
        }
    }
}

mappings = {
    "properties": {
        "country": {
            "type": "string",
            "fields": {
                "term": {
                    "type": "string",
                    "analyzer": "termIndex"
                }
            }
        },
        "name": {
            "type": "string",
            "fields": {
                "term": {
                    "type": "string",
                    "analyzer": "termIndex"
                }
            }
        },
        "type": {
            "type": "string",
            "analyzer": "termIndex"
        }
    }
}

res = requests.get(elastic_to_index)
if res:
    if RECREATE_INDEX:
        res = requests.delete(elastic_to_index)
        print("Index deleted")
        create_index_response = requests.put(elastic_to_index)
        print(create_index_response.text)
else:
    create_index_response = requests.put(elastic_to_index)
    print(create_index_response.text)

time.sleep(5)

print(requests.post(elastic_to_index + "/_close").text)
print("Settings")
print(requests.put(elastic_to_index + "/_settings", data=json.dumps(settings)).text)
print("Mappings")
print(requests.post(elastic_to_index + "/_mapping/" + TYPE, data=json.dumps(mappings)).text)
print(requests.post(elastic_to_index + "/_open").text)

with open(SEED_FILE_PATH) as f:
    content = f.readlines()
    count = 0
    for line in content:
        if line.startswith("INSERT INTO `mmt` VALUES ("):
            line = line.replace("INSERT INTO `mmt` VALUES (", "")
            dataset = line.split("),(")
            size = len(dataset)
            print(size)

            # These codes are not returned by the restcountries API
            country_codes = {
                "St. Helena": "sh",
                "Netherlands Antilles": "an",
                "Democratic Republic of Congo": "zr",
                "St. Lucia": "lc",
                "Libyan Arab Jamahiriya": "ly",
                "Antarctica": "aq"
            }

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
                    "name": data[4].replace('\\', ''),
                    "rating": float(data[5]),
                    "city": data[6].replace('\\', ''),
                    "state": data[7].replace('\\', ''),
                    "country": data[8].replace('\\', ''),
                    "tz": data[9],
                    "type": data[10],
                    "url": data[11],
                    "directFlights": int(data[13])
                }
                elev = data[12]
                if elev:
                    datadict['elev'] = int(elev)

                if datadict['country'] not in country_codes:
                    res = requests.get(
                            "https://restcountries.eu/rest/v1/name/{}?fullText=true".format(datadict['country']))
                    got_country = False
                    country_data = None
                    if res:
                        country_data = json.loads(res.text)
                        if (len(country_data) > 0 and 'alpha2Code' in country_data[0]):
                            got_country = True
                    if not got_country:
                        res = requests.get("https://restcountries.eu/rest/v1/name/{}".format(datadict['country']))
                        if res:
                            country_data = json.loads(res.text)
                            if (len(country_data) > 0 and 'alpha2Code' in country_data[0]):
                                got_country = True
                    if got_country:
                        country_codes[datadict['country']] = country_data[0]['alpha2Code'].lower()
                    else:
                        print(country_codes[datadict['country']] + " Failed")
                if datadict['country'] in country_codes:
                    datadict['ctryCd'] = country_codes[datadict['country']]

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
