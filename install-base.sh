#!/bin/sh -x


# Install libs

# xvfb - Install xvfb - Virtual Framebuffer 'fake' X server 
# x11vnc - install VNC server on remote Ubuntu (to access TWS GUI remotely)
apt-get update &&
    apt-get install -y \
	    wget \
	    unzip \
	    xvfb \
	    x11vnc \
	    default-jre \
	    libwebkit2gtk-4.0-37 \
	    dos2unix
    
mkdir ~/ibcontroller.paper && \
    cd ~/ibcontroller.paper/ && \
    wget https://github.com/ib-controller/ib-controller/releases/download/3.2.0/IBController-3.2.0.zip && \
    unzip IBController-3.2.0.zip && \
    cp -r ~/ibcontroller.paper ~/ibcontroller.real && \
    cd /tmp/ && \
    wget https://download2.interactivebrokers.com/installers/tws/latest-standalone/tws-latest-standalone-linux-x64.sh && \
    chmod +x tws-latest-standalone-linux-x64.sh && \
    echo "n" | ./tws-latest-standalone-linux-x64.sh && \
    rm -rf /tmp/* 

export DISPLAY=":0.0"

# for now just run virtual frame buffer manually:
/usr/bin/Xvfb :0 -ac -screen 0 1024x768x24 &

# run VNC server (note - it has 2 passwords: one for “view only” other for “full access with keyboard and mouse”):
/usr/bin/x11vnc -ncache 10 -ncache_cr -viewpasswd remote_view_only_pass -passwd some_pass123  -display :0 -forever -shared -logappend /var/log/x11vnc.log -bg -noipv6

# Let us start TWS manually 1st time to configure few things:
# cd /opt/IBJts &&
# ./tws &

mv ~/IBControllerStart-paper.sh ~/ibcontroller.paper/IBControllerStart.sh &&
cd ~/ibcontroller.paper/ &&
dos2unix *ini &&
cp IBController.ini IBController.ini-original &&
cat IBController.ini-original | grep -ve '^#' | grep -ve '^$' > IBController.ini &&

chmod a+x IBControllerStart.sh &&
chmod a+x ~/ibcontroller.paper/Scripts/*.sh &&

./IBControllerStart.sh &
