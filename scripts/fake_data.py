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

    def set_show_suggestions(self):
        self.keyDict['show_suggestions'] = True

    def get_adb_string(self):
        bundle_string = ""
        for (key, value) in self.keyDict.items():
            bundle_string = bundle_string + " --es " + key + " \"" + str(value) + "\""
        return "adb -s 84B5T15B03009672 shell am broadcast -a com.inceptai.dobby.wifi.fake.FAKE_DATA " + bundle_string

def main():
    fake_config = FakeConfig();
    fake_config.set_download(2.5 * 1.0e6)
    fake_config.set_upload(1.2 * 1.0e6)
    fake_config.set_show_suggestions()
    command = fake_config.get_adb_string()
    print(command)
    print subprocess.Popen(command, shell=True, stdout=subprocess.PIPE).stdout.read()
    pass

if __name__ == "__main__":
    main()
