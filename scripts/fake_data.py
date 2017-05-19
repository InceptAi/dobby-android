#!/usr/bin/python
# vim: tabstop=8 expandtab shiftwidth=4 softtabstop=4

import subprocess

""" 
  Use this to set fake data in the WifiDoc or Dobby apps.
"""
class FakeConfig:
    def __init__(self):
        print("Initializing FakeConfig")
        self.keyDict = {}

    def set_download(self, bandwidth):
        self.keyDict['download'] = bandwidth

    def set_upload(self, bandwidth):
        self.keyDict['upload'] = bandwidth

    def get_adb_string(self):
        extras = ""
        for key in self.keyDict.keys():
            extras = extras + " --es " + key + " \"" + str(self.keyDict[key]) + "\""
        command = "adb shell am broadcast -a com.inceptai.dobby.fake.FAKE_DATA " + extras
        return command


def main():
    config = FakeConfig()
    config.set_download(3.4)
    config.set_upload(1.0)
    command = FakeConfig.get_adb_string(config)
    print("Running command:" + command)
    print subprocess.Popen(command, stdout=subprocess.PIPE, shell=True, executable="/bin/bash").stdout.read()

if __name__ == '__main__':
    main()

