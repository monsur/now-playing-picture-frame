#!/bin/bash

export DISPLAY=:0

unclutter -root -idle 0 &

cd /home/monsur/Documents/Projects/now-playing-picture-frame

./build/install/com.monsur.now-playing-picture-frame/bin/com.monsur.now-playing-picture-frame &
response=0

while [ $response -ne 200 ]
do
  sleep 2
  response=$(curl --write-out %{http_code} --silent --output /dev/null http://localhost:5173/health)
done

chromium-browser --kiosk --incognito http://localhost:5173/client.html &
