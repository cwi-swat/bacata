FROM maven:3.6.0-jdk-8 as mavenBuild

ARG notebookVersion=6.1.2
ARG rascalNotebookVersion=0.0.1
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

USER root
RUN mkdir -p /rascal-kernel
COPY ./rascal-notebook/target/rascal-notebook-${rascalNotebookVersion}.jar /rascal-kernel/rascal-notebook.jar
RUN mkdir -p /rascal-kernel/rascal
COPY ./rascal-notebook-docker/kernel/rascal/ /rascal-kernel/rascal/
WORKDIR /rascal-kernel
RUN jupyter kernelspec install rascal

RUN mkdir -p /usr/local/lib/python3.5/dist-packages/notebook/static/components/codemirror/mode/rascal
COPY ./rascal-codemirror/rascal/rascal.js /usr/local/lib/python3.5/dist-packages/notebook/static/components/codemirror/mode/rascal/rascal.js

# Set a new workspace
USER root
WORKDIR $HOME

RUN chown -R guest .*
USER guest
RUN umask 002
RUN mkdir workspace

WORKDIR workspace

# for the Jupyter webserver
EXPOSE 8888
EXPOSE 3434

# a hundred ports for Rascal's web app server
EXPOSE 9050-10050

# for JVM debugging
# EXPOSE 8000

CMD ["jupyter", "notebook", "--ip", "0.0.0.0", "--no-browser", "--port", "8888"]

