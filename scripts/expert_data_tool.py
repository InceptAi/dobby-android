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

def write_json_data_to_endpoint(url, json_dict):
    print "Coming in write"
    r = requests.put(url, data=json.dumps(json_dict))
    print r.status_code
    print r.content


def main():
    default_url = "https://dobbybackend.firebaseio.com/dummy.json"
    op = OptionParser()
    op.add_option("-r", "--release", action="store_true", dest="use_release", help="use release data", default=False)
    op.add_option("-f", "--flavor", dest="app_flavor", help="App flavor (default:wifidoc)", default="wifidoc")
    (opts, args) = op.parse_args()


if __name__ == '__main__':
    main()


