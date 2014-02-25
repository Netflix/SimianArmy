#!/bin/bash
# Script for KillProcesses Chaos Monkey

cat << EOF > /tmp/kill_loop.sh
#!/bin/bash
while true;
do
    pkill -KILL -f java
    pkill -KILL -f python
    sleep 1
done
EOF

nohup /bin/bash /tmp/kill_loop.sh &
