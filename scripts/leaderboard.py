"""Helper functions.
"""
import sys
import os
import requests
import json
import urllib
import xkcdpass.xkcd_password as xp
import random
import jsonpickle
from collections import OrderedDict
from optparse import OptionParser
__author__ = """\n""".join(['Vivek Shrivastava (vivek@obiai.tech)'])

ENABLE_RANDOM_USERNAMES = True
GLOBAL_LEADERBOARD = {}
USER_ID_TO_HANDLE = {}

class BandwidthGrade(object):
    def __init__(self, **kwargs):
        self.__dict__.update(kwargs)
'''
        private @MetricType int uploadBandwidthMetric = MetricType.UNKNOWN;
        private @MetricType int downloadBandwidthMetric = MetricType.UNKNOWN;
        private long downloadUpdatedAtMs;
        private long uploadUpdatedAtMs;
        private double uploadMbps;
        private double downloadMbps;
        String isp = Utils.EMPTY_STRING;;
        String externalIP = Utils.EMPTY_STRING;;
        String bestServerName = Utils.EMPTY_STRING;
        String bestServerCountry = Utils.EMPTY_STRING;
        double bestServerLatencyMs;
        double lat;
        double lon;
'''

class InferenceRecord(object):
    def __init__(self, **kwargs):
        self.__dict__.update(kwargs)
'''
    public String uid;
    public String phoneInfo;
    public String appVersion;
    public String titleMessage;
    public long timestamp;
    public String bandwidthGradeJson;
    public String wifiGradeJson;
    public String pingGradeJson;
    public String httpGradeJson;
    public List<String> detailedMessageList;
    public String conditionsUsedForInference;
'''

class LeaderBoardInfo(object):
    def __init__(self, speed, handle, uid, inference_id, app_version):
        self.speed = speed
        self.handle = handle
        self.uid = uid
        self.inference_id = inference_id
        self.app_version = app_version
        self.key = format_speed(speed) + "_" + handle

    def __str__(self):
        #return self.key
        return json.dumps(self.__dict__)

    def __repr__(self):
        return self.__str__()

class Handle(object):
    def __init__(self, key=None, uid=None, **kwargs):
        self.key = key
        self.uid = uid
        self.__dict__.update(kwargs)

    def __str__(self):
        return json.dumps(self.__dict__)

    def __repr__(self):
        return self.__str__()

def random_capitalisation(s, chance):
    new_str = []
    for i, c in enumerate(s):
        new_str.append(c.upper() if random.random() < chance else c)
    return "".join(new_str)

def capitalize_first_letter(s):
    new_str = []
    s = s.split(" ")
    for i, c in enumerate(s):
        new_str.append(c.capitalize())
    return "".join(new_str)

def generate_random_number(chance):
    if random.random() < chance:
        return random.randint(1, 1000)
    else:
        return 0

def generate_random_username():
    words = xp.locate_wordfile()
    mywords = xp.generate_wordlist(wordfile=words, min_length=5, max_length=8, valid_chars='[a-z]')
    raw_password = xp.generate_xkcdpassword(mywords, numwords=1)
    capitalized_password = random_capitalisation(raw_password, 1/10.0)
    number_prefix = generate_random_number(5/10.0)
    if number_prefix > 0:
        capitalized_password = capitalized_password + str(number_prefix)
    return capitalized_password

def generate_handle(user_id, random_handle=False):
    if not random_handle:
        handle = Handle(key=user_id, uid=user_id)
    else:
        handle = Handle(key=generate_random_username(), uid=user_id)
    return handle

def format_speed(speed):
    speed_to_return = ("%0.2f" % speed).rjust(6, '0')
    return speed_to_return.replace('.',':')
    
def get_float_value(json_dict, key):
    if not json_dict or not key:
        return None
    try:
        value = float(json_dict[key]) if json_dict.get(key, None) else None
    except ValueError:
        print ("Cannot convert to float", json_dict.get(key, None))
        return None
    else:
        return value

def read_json(file_stream):
    json_to_return = None
    try:
        json_to_return = json.load(file_stream)
    except json.decoder.JSONDecodeError as e:
        print ("Invalid JSON input ({0}). Input:{1}".format(e.msg, e.doc))
        return None
    else:
        return json_to_return

def read_json_from_file(file_name):
    json_to_return = None
    try:
        with open(file_name) as file_stream:
            json_to_return = read_json(file_stream)
    except (OSError, IOError) as e:
        print ("Cannot read file {file_to_read}.".format(file_to_read=file_name))
        return None
    else:
        return json_to_return

def parse_handle(handle_json_dict, handle_key):
    handle = Handle(**handle_json_dict)
    handle.key = handle_key
    return handle

def parse_inference(inference_json_dict, inference_key):
    inference_record = InferenceRecord(**inference_json_dict)
    bandwidth_grade_dict = json.loads(inference_record.bandwidthGradeJson)
    bandwidth_grade = BandwidthGrade(**bandwidth_grade_dict)
    handle_to_use = USER_ID_TO_HANDLE.get(inference_record.uid)

    if handle_to_use is None:
        handle_to_use = generate_handle(inference_record.uid, ENABLE_RANDOM_USERNAMES) 
        USER_ID_TO_HANDLE[inference_record.uid] = handle_to_use 

    if bandwidth_grade.downloadMbps > 0:
        leaderboard_info = LeaderBoardInfo(speed=bandwidth_grade.downloadMbps, handle=handle_to_use.key, uid=inference_record.uid, inference_id=inference_key, app_version=inference_record.appVersion)
    else:
        leaderboard_info = None

    return leaderboard_info
		
def merge_two_dicts(x, y):
    """Given two dicts, merge them into a new dict as a shallow copy."""
    z = x.copy()
    z.update(y)
    return z

def parse_json(json_data_dict):
    leaderboard_dict = {}
    if not isinstance(json_data_dict, dict):
        return {}
    for key in json_data_dict:
        value = json_data_dict[key]
        #print("key is {0}".format(key))
        #print("The key and value are ({}) = ({})".format(key, value))
        if key != "inferences":
            leaderboard_dict = merge_two_dicts(leaderboard_dict, parse_json(value))
        else:
            for inference_key, inference_value in value.iteritems():
                linfo = parse_inference(inference_json_dict=inference_value, inference_key=inference_key)
                if linfo is not None:
                    leaderboard_dict[linfo.key] = linfo
    return leaderboard_dict	

    

def fetch_data(url_to_fetch):
    response = urllib.urlopen(url_to_fetch)
    data = json.loads(response.read())
    if data is None:
        data = {}
    return data

def write_leaderboard(leaderboard_dict, leaderboard_url):
    leaderboard_data = jsonpickle.encode(leaderboard_dict, unpicklable=False)
    r = requests.put(leaderboard_url, data=leaderboard_data)
    print r.status_code
    
def iterate_over_dobby_inferences():
    leaderboard_dict = {}
    prefixes_to_iterate = ["wifidoc/release/users", "dobby/release/users"]
    base_url = "https://dobbybackend.firebaseio.com"
    for prefix in prefixes_to_iterate:
        url_to_fetch = base_url + "/" + prefix + ".json"
        print ("processing url {0}".format(url_to_fetch))
        json_dict = fetch_data(url_to_fetch)
        leaderboard_dict =  merge_two_dicts(leaderboard_dict, parse_json(json_dict))
    return leaderboard_dict

def fetch_handle_info(handles_url):
    handles_dict = {}
    handles_json_dict = fetch_data(handles_url)
    for key, value in handles_json_dict.iteritems():
        handle = parse_handle(handle_json_dict=value, handle_key=key)
        handles_dict[handle.key] = handle
        #Update global dict
        USER_ID_TO_HANDLE[handle.uid] = handle
    return handles_dict		

def write_handles(handles_url):
    handles_dict_to_write = {}
    for key, value in USER_ID_TO_HANDLE.iteritems():
        handles_dict_to_write[value.key] = value
    handles_data = jsonpickle.encode(handles_dict_to_write, unpicklable=False)
    print handles_data
    r = requests.put(handles_url, data=handles_data)
    print r.status_code
    print r.content

def main():
    default_url = "https://dobbybackend.firebaseio.com/dobby/release/users/ac0ad728-c80a-4daf-8125-3ae5545e3d7f.json?print=pretty"
    default_leaderboard_url = "https://dobbybackend.firebaseio.com/leaderboard.json?print=pretty"
    default_handles_url = "https://dobbybackend.firebaseio.com/handles.json?print=pretty"
    op = OptionParser()
    op.add_option("-v", "--verbose", action="store_true", help="verbose", default=False)
    op.add_option("-w", "--writetobackend", action="store_true", dest="write_to_backend", help="use -w to write to backend", default=False)
    op.add_option("-a", "--fetchalldata", action="store_true", dest="fetch_all_data", help="Fetch all data for leaderboard (ignores database url, default false)", default=False)
    op.add_option("-r", "--randomusers", action="store_false", dest="random_users_names", help="Gen randome user names", default=True)
    op.add_option("-u", "--dataurl", dest="database_url", help="Database url to crawl", default=default_url)
    op.add_option("-l", "--leaderboardurl", dest="leaderboard_url", help="Leader board url", default=default_leaderboard_url)
    op.add_option("-m", "--maxusers", dest="max_users", help="Max users in leaderboard", default="100")
    (opts, args) = op.parse_args()

    fetch_handle_info(default_handles_url)
	
    if opts.fetch_all_data:
        leaderboard_dict = iterate_over_dobby_inferences()
    else:
    	json_dict = fetch_data(opts.database_url)
    	leaderboard_dict =  parse_json(json_dict)

    #sorted_leaderboard_dict = OrderedDict(sorted(leaderboard_dict.items(), key=lambda t: t[0], reverse=True))
    print "Dumping leaderboard dict"
    sorted_leaderboard_dict = {}
    user_count = 0
    for key in sorted(leaderboard_dict, reverse=True):
        user_count = user_count + 1
        print user_count, key, leaderboard_dict[key]
        sorted_leaderboard_dict[key] = leaderboard_dict[key]
        if (user_count >= int(opts.max_users)):
            break

    if opts.write_to_backend:	
        write_leaderboard(sorted_leaderboard_dict, opts.leaderboard_url)
        write_handles(default_handles_url)

if __name__ == '__main__':
    main()


