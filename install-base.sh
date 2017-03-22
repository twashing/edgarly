#!/bin/sh -x

export DISPLAY=":0.0"

# for now just run virtual frame buffer manually:
/usr/bin/Xvfb :0 -ac -screen 0 1024x768x24 &

# run VNC server (note - it has 2 passwords: one for “view only” other for “full access with keyboard and mouse”):
/usr/bin/x11vnc -ncache 10 -ncache_cr -viewpasswd remote_view_only_pass -passwd some_pass123  -display :0 -forever -shared -logappend /var/log/x11vnc.log -bg -noipv6

# We have to start TWS manually, to configure few things:
cd ~/Jts/964
./tws &

mv ~/IBControllerStart-paper.sh ~/ibcontroller.paper/IBControllerStart.sh
cd ~/ibcontroller.paper/
dos2unix *ini
cp IBController.ini IBController.ini-original
cat IBController.ini-original | grep -ve '^#' | grep -ve '^$' > IBController.ini

# temporary kludge
mkdir -p /root/IBController/
cp IBController.ini /root/IBController/
# end kludge

chmod a+x IBControllerStart.sh
chmod a+x ~/ibcontroller.paper/Scripts/*.sh

./IBControllerStart.sh

touch 1.txt && tail -f 1.txt
