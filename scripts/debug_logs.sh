#!/bin/bash

echo Setting Wifidoc logs to VERBOSE
adb shell setprop log.tag.Dobby VERBOSE

echo Setting Firebase Analytics to debug mode for Wifidoc
adb shell setprop debug.firebase.analytics.app com.inceptai.dobby.wifidoc

