"""Helper functions.
"""
import sys
import os
import requests
import json
import urllib
import xkcdpass.xkcd_password as xp
import random
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
    def __init__(self, speed, handle, uid, inference_id):
		self.speed = speed
		self.handle = handle
		self.uid = uid
		self.inference_id = inference_id
                #self.key = str(round(speed,2)) + "_" + handle
                self.key = format_speed(speed) + "_" + handle

    def __str__(self):
        return self.key
        #return json.dumps(self.__dict__)

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
    mywords = xp.generate_wordlist(wordfile=words, min_length=5, max_length=8)
    raw_password = xp.generate_xkcdpassword(mywords, numwords=1)
    capitalized_password = random_capitalisation(raw_password, 1/10.0)
    number_prefix = generate_random_number(5/10.0)
    if number_prefix > 0:
        capitalized_password = capitalized_password + str(number_prefix)
    return capitalized_password

def generate_handle(user_id, random_handle=False):
    if not random_handle:
        handle = user_id
    else:
        handle = generate_random_username()
    return handle

def format_speed(speed):
    #return str(round(speed,2)).rjust(6, '0')
    return ("%0.2f" % speed).rjust(6, '0')
    
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

def parse_inference(inference_json_dict, inference_key):
    inference_record = InferenceRecord(**inference_json_dict)
    bandwidth_grade_dict = json.loads(inference_record.bandwidthGradeJson)
    bandwidth_grade = BandwidthGrade(**bandwidth_grade_dict)
    if USER_ID_TO_HANDLE.get(inference_record.uid) is None:
        USER_ID_TO_HANDLE[inference_record.uid] = generate_handle(inference_record.uid, ENABLE_RANDOM_USERNAMES) 
    #leaderboard_info = LeaderBoardInfo(speed=bandwidth_grade.downloadMbps, handle=inference_record.uid, uid=inference_record.uid, inference_id=inference_key)
    leaderboard_info = LeaderBoardInfo(speed=bandwidth_grade.downloadMbps, handle=USER_ID_TO_HANDLE.get(inference_record.uid), uid=inference_record.uid, inference_id=inference_key)
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
                leaderboard_dict[linfo.key] = linfo
    return leaderboard_dict	

    

def fetch_data(database_url):
    #url = "https://dobbybackend.firebaseio.com/dobby/release/users/ac0ad728-c80a-4daf-8125-3ae5545e3d7f.json?print=pretty"
    response = urllib.urlopen(database_url)
    data = json.loads(response.read())
    print data
    return data

def main():
    default_url = "https://dobbybackend.firebaseio.com/dobby/release/users/ac0ad728-c80a-4daf-8125-3ae5545e3d7f.json?print=pretty"
    op = OptionParser()
    op.add_option("-v", "--verbose", action="store_true", help="verbose", default=False)
    op.add_option("-u", "--dataurl", dest="database_url", help="Database url to crawl", default=default_url)
    op.add_option("-m", "--maxusers", dest="max_users", help="Max users to crawl", default="10")
    op.add_option("-r", "--randomusers", dest="random_users_names", help="Gen randome user names", default=True)
    (opts, args) = op.parse_args()
    if not opts.database_url:   # if database_url is not given
        op.error('Database url is not given')
    json_dict = fetch_data(opts.database_url)
    leaderboard_dict =  parse_json(json_dict)
    #for key, value in leaderboard_dict.iteritems():
    for key in sorted(leaderboard_dict):
        print key, leaderboard_dict[key]
    #print leaderboard_dict

if __name__ == '__main__':
    main()


