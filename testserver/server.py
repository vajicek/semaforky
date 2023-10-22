#!/usr/bin/env python3

import time
import socket
import sys
import struct
from collections import namedtuple

HOST = '192.168.1.164'   # Symbolic name, meaning all available interfaces
PORT = 8888 # Arbitrary non-privileged port

def SendStruct(conn, control=66, i=33):
	format_ = "II"
	MyStruct = namedtuple("MyStruct", "control value")
	tuple_to_send = MyStruct(control=control, value=i | int("0a000000", 16))
	string_to_send = struct.pack(format_, *tuple_to_send._asdict().values())
	print("Send: " + str(string_to_send))
	bytes_sent = conn.send(string_to_send)
	if len(string_to_send) != bytes_sent:
		return False

	return True

def Number(conn, i=33):
	return SendStruct(conn, 66, i)

def Ping(conn):
	print("Ping")
	SendStruct(conn, 69)
	data = conn.recv(1024)
	print("Pong: " + str(data))

def Listen():
	s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
	s.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
	print('Socket created')

	#Bind socket to local host and port
	try:
		s.bind((HOST, PORT))
	except socket.error as msg:
		print('Bind failed. Error Code : ')
		print(msg)
		sys.exit()

	print('Socket bind complete')

	#Start listening on socket
	s.listen(10)
	print('Socket now listening')

	#now keep talking with the client
	i = 0
	while 1:
		#wait to accept a connection - blocking call
		conn, addr = s.accept()
		conn.settimeout(5)
		print('Connected with ' + addr[0] + ':' + str(addr[1]))
		try:
			while 1:
				time.sleep(1)
				Number(conn, i)
				Ping(conn)
				i += 1
		except ConnectionResetError as e:
			print("Connection reset")
			continue
		except TimeoutError as e:
			print("Connection timeout")
			continue

	s.close()


Listen()