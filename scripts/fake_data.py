#!/usr/bin/python
# vim: tabstop=8 expandtab shiftwidth=4 softtabstop=4

""" 
  Use this to set fake data in the WifiDoc or Dobby apps.
"""

class FakeConfig:
    def __init__(self):
        print("Initializing FakeConfig")
        self.keyDict = {}

    def set_download(self, bandwidth):
        keyDict['download'] = bandwidth

    def set_upload(self, bandwidth):
        keyDict['upload'] = bandwidth

    def get_adb_string(self):
        "adb shell am broadcast -a INTENT_NAME --es key "value" -n com.inceptai.wifiexpert/com.inceptai.dobby.MainActivity"




