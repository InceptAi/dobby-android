"""Helper functions.
"""
import sys
import os
import requests
import json
import urllib
from optparse import OptionParser
__author__ = """\n""".join(['Vivek Shrivastava (vivek@obiai.tech)'])

GLOBAL_LEADERBOARD = {}

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
                self.key = str(round(speed,2)) + "_" + handle

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
    leaderboard_info = LeaderBoardInfo(speed=bandwidth_grade.downloadMbps, handle=inference_record.uid, uid=inference_record.uid, inference_id=inference_key)
    return leaderboard_info
		
def parse_json(json_data_dict):
    leaderboard_dict = {}
    for key in json_data_dict:
        value = json_data_dict[key]
        print("key is {0}".format(key))
        #print("The key and value are ({}) = ({})".format(key, value))	
        if key != "inferences":
            return parse_json(value)
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
    (opts, args) = op.parse_args()
    if not opts.database_url:   # if database_url is not given
        op.error('Database url is not given')
    json_dict = fetch_data(opts.database_url)
    leaderboard_dict =  parse_json(json_dict)
    print leaderboard_dict

if __name__ == '__main__':
    main()


