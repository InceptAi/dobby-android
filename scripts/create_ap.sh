sudo nmcli radio wifi off
sudo rfkill unblock wlan

sudo ifconfig wlxec086b132588 10.0.0.1/24 up
sleep 1
#sudo service isc-dhcp-server restart
sudo service hostapd restart
