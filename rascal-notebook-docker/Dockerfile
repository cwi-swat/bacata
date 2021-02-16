FROM maven:3.6.0-jdk-8 as mavenBuild

ARG notebookVersion=6.1.2
ARG rascalNotebookVersion=0.0.1-SNAPSHOT
ARG RASCAL_USER=guest
ARG RASCAL_UID=1000
ENV USER ${RASCAL_USER}
ENV RASCAL_UID ${RASCAL_UID}
ENV HOME /home/${RASCAL_USER}

RUN adduser --disabled-password \
    --gecos "Default user" \
    --uid ${RASCAL_UID} \
    ${RASCAL_USER}

WORKDIR $HOME

RUN apt-get update

RUN apt-get -yq install tcpdump

# Python

RUN apt-get install software-properties-common -y
 
RUN apt-get install -y python3-pip

RUN pip3 install six==1.13.0

# Node 

RUN curl -sL https://deb.nodesource.com/setup_12.x | bash -  && apt-get install -y nodejs

RUN npm install -g bower

# Jupyter

RUN python3 -m pip install --upgrade pip

RUN pip3 install --upgrade setuptools pip

RUN pip3 install notebook==${notebookVersion}

# Bacata 

USER guest
WORKDIR $HOME

#RUN git clone --depth 1 https://github.com/cwi-swat/bacata.git 
#WORKDIR bacata
#RUN mvn dependency:go-offline

#WORKDIR ..
#RUN rm -rf bacata
#ADD https://nexus.usethesource.io/content/repositories/snapshots/org/rascalmpl/rascal/0.19.3-SNAPSHOT/maven-metadata.xml lastRascalBuild.xml
#ADD https://api.github.com/repos/cwi-swat/bacata/git/refs/heads/master version.json
#RUN git clone --depth 1 https://github.com/cwi-swat/bacata.git 
#WORKDIR bacata

#RUN mvn clean install

USER root
RUN mkdir -p /rascal-kernel
COPY ./rascal-notebook/target/rascal-notebook-${rascalNotebookVersion}.jar /rascal-kernel/rascal-notebook.jar
RUN mkdir -p /rascal-kernel/rascal
COPY ./rascal-notebook-docker/kernel/rascal/ /rascal-kernel/rascal/
WORKDIR /rascal-kernel
RUN jupyter kernelspec install rascal

#WORKDIR $HOME/bacata
#RUN cp -a rascal-codemirror/. /notebook/notebook/static/components/codemirror/mode/

# Set a new workspace
USER root
WORKDIR $HOME

RUN chown -R guest .*
USER guest
RUN umask 002
RUN mkdir workspace

WORKDIR workspace

COPY ./rascal-notebook-docker/RascalDemo.ipynb .
COPY ./rascal-notebook-docker/DemoStateMachineDSL.ipynb .
USER root
RUN chown guest RascalDemo.ipynb
USER guest

# for the Jupyter webserver
EXPOSE 8888
EXPOSE 3434

# for Rascal's app server
EXPOSE 9050-10050

# for JVM debugging
EXPOSE 8000

CMD ["jupyter", "notebook", "--ip", "0.0.0.0", "--debug", "--no-browser", "--port", "8888"]

