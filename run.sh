#!/bin/bash

intra=""
while getopts ":a:i" opt; do
	case ${opt} in
		a) 
			target=$OPTARG
			;;
    i)
      intra="-intra"
      ;;
		*)
			echo "Usage: run.sh -a x (x is any or a combination of the following options separated by ',')"
                        echo "       hdfs"
                        echo "       mapreduce"
                        echo "       yarn" 
                        echo "       hadoop_common"
                        echo "       hadoop_tools" 
                        echo "       hbase" 
                        echo "       alluxio" 
                        echo "       zookeeper" 
                        echo "       spark"
			exit 1
			;;
	esac
done

rm tmp.txt
export MAVEN_OPTS=-Xmx6g
mvn exec:java -Dexec.mainClass="Main" -Dexec.args="-o tmp.txt -x ./config_files -a ${target} ${intra}" -e
