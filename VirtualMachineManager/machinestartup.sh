#!/bin/bash
#Starting up a new VM
echo "Hello World"
if [ $# -ne 4 ]
then
	echo "Error in Number of Arguments"
	echo "Enter <IP> <USERNAME> <PASSWORD> of a physical machine followed by the <VM Name> to start on it"
	exit
else
	IP=$1
	username=$2
	password=$3
	mc_name=$4	
fi

#sshpass is a utility designed for running ssh using the mode referred to
# as "keyboard-interactive" password authentication, but in non-interactive mode. 
# Use of "<<EOSSH" : Execute the following lines on Remote Machine till next EOSSH
sshpass -p $password ssh -o StrictHostKeyChecking=no  $username@$IP sh -s << EOSSH

#cd to Folder containing Vagrant File
cd vagrant_vm

vagrant up $mc_name 
#vagrant up

#cat temp.txt
#run shell command (after -c) on the remote machine. hostname -I gives the IP
vagrant ssh $mc_name -c "hostname -I | cut -d' ' -f2"
EOSSH


#To solve sudo problem (-t option)
if false
then
ssh -t $HOST bash -c "'
ls

pwd

if true; then
    echo $HELLO
else
    echo "This is false"
fi

echo "Hello world"

sudo ls /root
'"
fi
