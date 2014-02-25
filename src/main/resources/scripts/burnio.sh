#!/bin/bash
# Script for BurnIO Chaos Monkey

cat << EOF > /tmp/loopburnio.sh
#!/bin/bash
while true;
do
    dd if=/dev/urandom of=/burn bs=1M count=1024 iflag=fullblock
done
EOF

nohup /bin/bash /tmp/loopburnio.sh &
