# Bacatá Notebooks
FROM maven:3.5.3-jdk-8 as cloneBuild

RUN apt-get update

#------------
FROM cloneBuild as pythonInstallation

RUN apt-get install -y python3-pip

####### NodeJS installation
RUN pip3 install --upgrade pip

RUN curl -sL https://deb.nodesource.com/setup_8.x | bash -  && apt-get install -y nodejs

RUN npm install -g bower

RUN pip3 install --upgrade setuptools pip


####### Custom Jupyter installation
FROM pythonInstallation as jupyterInstallation

RUN git clone https://github.com/maveme/notebook

RUN cd notebook \
    && pip3 install -e .

#--------------
FROM jupyterInstallation as bacataImage
# Clone Rascal
RUN cd root \
    && git clone https://github.com/usethesource/rascal.git

RUN cd root/rascal \
    && git checkout jupyter \
    && mvn install -DskipTests

# Clone Bacatá 
RUN cd root \
    && git clone https://github.com/cwi-swat/bacata.git

# Compiles bacatá
RUN cd root/bacata \
	&& mvn clean package
#   && mvn -DmainClass=bacata.rascalNotebook.RascalNotebook clean package

# If salix is selected, these two lines are needed 
RUN cd root \
    && git clone https://github.com/cwi-swat/salix.git

RUN cd root/salix \
    && git checkout scoped-salix

EXPOSE 8888

EXPOSE 3434

CMD ["jupyter", "notebook", "--ip","0.0.0.0", "--allow-root"]