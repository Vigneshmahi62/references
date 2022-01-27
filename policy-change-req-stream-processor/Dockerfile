#Building jar file from from source code using docker multi stage build
#mvn_build local docker image
FROM 10.0.135.36:31695/openshift/mvn_build as builder
WORKDIR /opt
#Copying source code
COPY src /opt/src
COPY pom.xml ./
#Building policy-changereq--stream-processor jar file
RUN mvn clean compile assembly:single

#Ubuntu officeal image from dockerhub
FROM ubuntu:focal
RUN useradd -ms /bin/bash csm
WORKDIR /home/csm
#Copying jar file from builder image
COPY --from=builder /opt/target/policy-changereq--stream-processor-1.0-SNAPSHOT-jar-with-dependencies.jar ./
#Copying check.sh bash script for readiness probe
COPY lib/check.sh ./
#Install Java and netca

RUN apt-get -y update && apt-get -y install openjdk-11-jdk netcat && mkdir -p /var/log/policy_change_req_stream_processor
#Default environment variable for kafka consumer group
ENV CONSUMERGID='Policy-Change-Request-Stream-Consumer-Group'
USER csm
CMD java -cp policy-changereq--stream-processor-1.0-SNAPSHOT-jar-with-dependencies.jar com.da.app.PolicyChangeReqStreamProcessor $CONSUMERGID 
