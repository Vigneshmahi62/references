#Building jar file from from source code using docker multi stage build
#sbt_build local image
FROM sbt_build as builder
WORKDIR /opt
#Downloading scala-2.13 debian package
RUN apt-get update && apt-get install wget && wget https://downloads.lightbend.com/scala/2.13.0/scala-2.13.0.deb
#Copying source code
COPY project /opt/project
COPY src /opt/src
COPY build.sbt ./
#Building jar file
RUN sbt clean assembly

#ubuntu image from dockerhub
FROM ubuntu:focal
#Adding csm user
RUN useradd -ms /bin/bash csm
WORKDIR /home/csm
#Copying jar file and scala package from builder container
COPY --from=builder /opt/target/scala-2.13/coi-builder-assembly-0.1.jar ./
COPY --from=builder /opt/scala-2.13.0.deb ./
#Copying check.sh file for readiness probe
COPY --chown=csm:csm libs/check.sh ./
#Installing java and scala
RUN apt-get -y update && apt-get -y install openjdk-11-jdk netcat && apt-get install /home/csm/scala-2.13.0.deb && mkdir -p /var/log/coi-builder && chown -R csm /var/log/coi-builder && rm -rf /home/csm/scala-2.13.0.deb
USER csm
CMD java -jar coi-builder-assembly-0.1.jar coiRequested coiCreated
 
