FROM ubuntu:24.04

ARG ARDUINO_CLI_VERSION=1.3.1
ARG DEBIAN_FRONTEND=noninteractive

RUN apt-get update && apt-get install -y --no-install-recommends \
	git \
	make \
	curl \
	bc \
	python3 \
	python3-pip \
	ca-certificates \
	xz-utils \
	&& rm -rf /var/lib/apt/lists/*

RUN python3 -m pip install --break-system-packages --no-cache-dir pyserial==3.5

RUN curl -fsSL https://deb.nodesource.com/setup_22.x | bash -

RUN apt-get update && apt-get install -y --no-install-recommends nodejs \
	&& rm -rf /var/lib/apt/lists/*

RUN curl -fsSL \
		"https://github.com/arduino/arduino-cli/releases/download/v${ARDUINO_CLI_VERSION}/arduino-cli_${ARDUINO_CLI_VERSION}_Linux_64bit.tar.gz" \
		| tar -xz -C /usr/local/bin arduino-cli

RUN curl -L \
		https://github.com/igrr/mkspiffs/releases/download/0.2.3/mkspiffs-0.2.3-arduino-esp8266-linux64.tar.gz \
		--output mkspiffs-0.2.3-arduino-esp8266-linux64.tar.gz && \
	tar xvzf mkspiffs-0.2.3-arduino-esp8266-linux64.tar.gz && \
	install -m 0755 mkspiffs-0.2.3-arduino-esp8266-linux64/mkspiffs /usr/local/bin/mkspiffs && \
	rm -rf mkspiffs-0.2.3-arduino-esp8266-linux64 mkspiffs-0.2.3-arduino-esp8266-linux64.tar.gz

EXPOSE 4200

# Match the mounted repository ownership on the host.
ARG UID=1000
ARG GID=1000

RUN set -eux; \
	group_name="$(getent group "$GID" | cut -d: -f1 || true)"; \
	if [ -n "$group_name" ]; then \
		groupmod --new-name mygroup "$group_name"; \
	else \
		groupadd --gid "$GID" mygroup; \
	fi; \
	user_name="$(getent passwd "$UID" | cut -d: -f1 || true)"; \
	if [ -n "$user_name" ]; then \
		usermod --login myuser --home /home/myuser --move-home "$user_name"; \
	else \
		useradd --uid "$UID" --gid mygroup --create-home --shell /bin/bash myuser; \
	fi; \
	usermod --gid mygroup myuser; \
	mkdir -p /home/myuser/Arduino/libraries /home/myuser/.arduino15; \
	chown -R myuser:mygroup /home/myuser

USER myuser
ENV HOME=/home/myuser
WORKDIR /tmp/home/web_app
