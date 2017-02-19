#!/bin/sh -x

apt-get update &&
apt-get -f install && 
apt-get install -y wget && 

mkdir -p ~/ibcontroller.paper  &&
cd ~/ibcontroller.paper/ &&
wget https://github.com/ib-controller/ib-controller/releases/download/3.2.0/IBController-3.2.0.zip &&
apt install unzip &&
unzip ./IBController-3.2.0.zip &&


# make two identical copies of IBController folder (one for “real” and one for “paper” account):
cp -r ~/ibcontroller.paper ~/ibcontroller.real

# Install xvfb - Virtual Framebuffer 'fake' X server 
apt install -y xvfb &&

# for now just run virtual frame buffer manually:
/usr/bin/Xvfb :0 -ac -screen 0 1024x768x24 &

# install VNC server on remote Ubuntu (to access TWS GUI remotely)
apt install -y x11vnc &&

# run VNC server (note - it has 2 passwords: one for “view only” other for “full access with keyboard and mouse”):
/usr/bin/x11vnc -ncache 10 -ncache_cr -viewpasswd remote_view_only_pass -passwd some_pass123  -display :0 -forever -shared -logappend /var/log/x11vnc.log -bg -noipv6


mkdir -p ~/tws &&
cd ~/tws &&
wget https://download2.interactivebrokers.com/installers/tws/latest-standalone/tws-latest-standalone-linux-x64.sh &&
chmod a+x tws-latest-standalone-linux-x64.sh &&

apt-get install -y libwebkit2gtk-4.0-37 &&

DISPLAY=:0 yes | ./tws-latest-standalone-linux-x64.sh


# Let us start TWS manually 1st time to configure few things:
cd /root/Jts/963 &&
DISPLAY=:0 ./tws &

cp ~/IBControllerStart-paper.sh ~/ibcontroller.paper/IBControllerStart.sh &&
cd ~/ibcontroller.paper/ &&

apt-get install -y dos2unix &&
dos2unix *ini &&
cp IBController.ini IBController.ini-original &&
cat IBController.ini-original | grep -ve '^#' | grep -ve '^$' > IBController.ini &&

chmod a+x IBControllerStart.sh &&
chmod a+x /root/ibcontroller.paper/Scripts/*.sh &&

DISPLAY=:0 ./IBControllerStart.sh &
