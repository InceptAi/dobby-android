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
import datetime
from collections import OrderedDict
from optparse import OptionParser
__author__ = """\n""".join(['Vivek Shrivastava (vivek@obiai.tech)'])

ENABLE_RANDOM_USERNAMES = True
GLOBAL_LEADERBOARD = {}
USER_ID_TO_HANDLE = {}
USER_ID_TO_FLAVOR = {}

class BandwidthGrade(object):
    def __init__(self, **kwargs):
        self.bestServerCountry = ""
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

class PhoneInfo(object):
    def __init__(self, **kwargs):
        self.__dict__.update(kwargs)
'''    
    public String manufacturer;
    public String model;
'''
    
class LeaderBoardInfo(object):
    def __init__(self, speed, handle, uid, inference_id, app_version, country, device_model, device_manufacturer, isp):
        if speed:
            self.speed = float("%0.2f" % speed)
        self.handle = handle
        self.uid = uid
        self.inference_id = inference_id
        self.app_version = app_version
        self.country = country
        self.dmake = device_model
        self.dmaker = device_manufacturer
        self.isp = isp
        self.key = format_speed(speed) + "_" + handle

    def __str__(self):
        #return self.key
        return json.dumps(self.__dict__)

    def __repr__(self):
        return self.__str__()

class Handle(object):
    def __init__(self, key=None, uid=None, leaderboard_key=None, speed=None, **kwargs):
        self.key = key
        self.uid = uid
        self.leaderboard_key = leaderboard_key
        if speed:
            self.speed = float("%0.2f" % speed)
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

def parse_inference(inference_json_dict,  inference_key):
    inference_record = InferenceRecord(**inference_json_dict)
    timestamp = inference_record.timestamp
    inference_record.inference_key = inference_key
    return inference_record
'''    
    bandwidth_grade_dict = json.loads(inference_record.bandwidthGradeJson)
    ping_grade_dict = json.loads(inference_record.pingGradeJson)
    wifi_grade_dict = json.loads(inference_record.wifiGradeJson)
    http_grade_dict = json.loads(inference_record.httpGradeJson)
    phone_info_dict = json.loads(inference_record.phoneInfo)
    conditions_dict = json.loads(inference_record.conditionsUsedForInference)
    if handle_to_use is None:
        handle_to_use = generate_handle(inference_record.uid, ENABLE_RANDOM_USERNAMES) 
        USER_ID_TO_HANDLE[inference_record.uid] = handle_to_use 

    if bandwidth_grade.downloadMbps > 0:
        leaderboard_info = LeaderBoardInfo(speed=bandwidth_grade.downloadMbps, 
            handle=handle_to_use.key, 
            uid=inference_record.uid, 
            inference_id=inference_key, 
            app_version=inference_record.appVersion,
            country=bandwidth_grade.bestServerCountry,
            device_manufacturer=phone_info.manufacturer,
            device_model=phone_info.model,
            isp=bandwidth_grade.isp)
    else:
        leaderboard_info = None

    return leaderboard_info
'''
		
def merge_two_dicts(x, y):
    """Given two dicts, merge them into a new dict as a shallow copy."""
    z = x.copy()
    z.update(y)
    return z

def fetch_data(url_to_fetch):
    response = urllib.urlopen(url_to_fetch)
    data = json.loads(response.read())
    if data is None:
        data = {}
    return data


def get_inferences(uid, build_type, flavor):
    inference_list = []
    base_url = "https://dobbybackend.firebaseio.com"
    url_to_fetch = base_url + "/" + flavor + "/" + build_type + "/users/" + "/" + uid + "/inferences.json"
    print ("processing url {0}".format(url_to_fetch))
    inference_data_for_user = fetch_data(url_to_fetch)
    for inference_key, inference_value in inference_data_for_user.iteritems():
        inference = parse_inference(inference_json_dict=inference_value, inference_key=inference_key)
        inference_list.append(inference)
    return inference_list

def print_pretty_timestamp(timestamp):
    timestamp = timestamp / 1000
    print datetime.datetime.utcfromtimestamp(timestamp).strftime('%Y-%m-%d %H:%M:%S')


def pretty_print_inference_list(inference_list, max_inferences):
    sorted_inference_list = sorted(inference_list, key=lambda t: t.timestamp, reverse=True) 
    count = 0
    for inference in sorted_inference_list:

        if count >= max_inferences:
            break

        count = count + 1
        print "======================================================="
        print "INFERENCE {0}\n".format(count)
        for attr, value in inference.__dict__.iteritems():
            print "-----------------------------------------------"
            print "KEY:", attr
            try:
                data_dict = json.loads(value)
                print json.dumps(data_dict, sort_keys=True, indent=4, separators=(',',': '))
            except:
                if isinstance(value, list):
                    for value_in_list in value:
                        print value_in_list
                else:
                    if (attr == "timestamp"):
                        print_pretty_timestamp(value)
                    else:
                        print value
        print "======================================================="

def main():
    default_url = "https://dobbybackend.firebaseio.com/dobby/release/users/ac0ad728-c80a-4daf-8125-3ae5545e3d7f.json?print=pretty"
    default_leaderboard_base_url = "https://dobbybackend.firebaseio.com/leaderboard"
    default_handles_base_url = "https://dobbybackend.firebaseio.com/handles"
    op = OptionParser()
    op.add_option("-v", "--verbose", action="store_true", help="verbose", default=False)
    op.add_option("-p", "--userelease", action="store_true", dest="use_release", help="Use Release app data", default=False)
    op.add_option("-d", "--usedummy", action="store_true", dest="use_dummy", help="Use Dummy data", default=False)
    op.add_option("-m", "--maxinferences", dest="max_inferences", help="Max inferences", default="2")
    op.add_option("-f", "--flavor", dest="app_flavor", help="App flavor (default:wifidoc)", default="wifidoc")
    op.add_option("-u", "--uid", dest="user_id", help="User ID", default=None)
    (opts, args) = op.parse_args()

    if not opts.user_id:
        op.print_help()
        op.error("User id is required")

    if opts.use_release:
        build_type = "release"
    else:
        build_type = "debug"

    if opts.use_dummy:
        flavor = "dummy"
    else:
        flavor = opts.app_flavor
    
    max_inferences = int(opts.max_inferences)
        	
    inference_list = get_inferences(uid=opts.user_id, 
        build_type=build_type,
        flavor=flavor)

    pretty_print_inference_list(inference_list, max_inferences)
    
if __name__ == '__main__':
    main()


