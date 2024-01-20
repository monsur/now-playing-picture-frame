#!/bin/bash

export DISPLAY=:0

unclutter -root &

cd /home/monsur/Documents/Projects/now-playing-picture-frame

./build/install/com.monsur.now-playing-picture-frame/bin/com.monsur.now-playing-picture-frame &
response=0

while [ $response -ne 200 ]
do
  sleep 2
  response=$(curl --write-out %{http_code} --silent --output /dev/null http://localhost:5173/health)
done

chromium --start-fullscreen http://localhost:5173/client.html &
