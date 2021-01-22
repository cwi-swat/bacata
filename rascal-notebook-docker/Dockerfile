FROM maven:3.6.0-jdk-8 as mavenBuild

ARG notebookVersion=6.1.3
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

RUN git clone --depth 1 https://github.com/cwi-swat/bacata.git 

WORKDIR bacata

RUN mvn clean install

WORKDIR bacata-rascal/rascal-kernel
USER root
RUN jupyter kernelspec install rascal

#WORKDIR $HOME/bacata
#RUN cp -a rascal-codemirror/. /notebook/notebook/static/components/codemirror/mode/

# Set a new workspace
USER root
WORKDIR $HOME
RUN chown -R guest *
RUN chown -R guest .*
RUN chmod -R u+rw *
RUN chmod -R u+rw .*

USER guest
RUN mkdir workspace

WORKDIR workspace

EXPOSE 8888
EXPOSE 3434
EXPOSE 9050-10050

CMD ["jupyter", "notebook", "--ip","0.0.0.0"]
