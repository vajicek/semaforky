FROM ubuntu:20.04

RUN apt-get update

RUN apt-get install -y \
	make \
	curl \
	unzip \
	bc \
	python3-pip

RUN python3 -m pip install pyserial

RUN curl -fsSL https://deb.nodesource.com/setup_18.x | bash -

RUN apt install -y nodejs

WORKDIR /root

RUN curl https://downloads.arduino.cc/arduino-ide/arduino-ide_2.3.2_Linux_64bit.zip \
		--output arduino-ide_2.3.2_Linux_64bit.zip && \
	unzip arduino-ide_2.3.2_Linux_64bit.zip && \
	ln -s arduino-ide_2.3.2_Linux_64bit Arduino

RUN curl -fsSL https://raw.githubusercontent.com/arduino/arduino-cli/master/install.sh | sh

RUN curl -L \
		https://github.com/igrr/mkspiffs/releases/download/0.2.3/mkspiffs-0.2.3-arduino-esp8266-linux64.tar.gz \
		--output mkspiffs-0.2.3-arduino-esp8266-linux64.tar.gz && \
	tar xvzf mkspiffs-0.2.3-arduino-esp8266-linux64.tar.gz && \
	ln -sf ~/mkspiffs-0.2.3-arduino-esp8266-linux64/mkspiffs ~/bin/

EXPOSE 4200