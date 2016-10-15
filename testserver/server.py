'''
    Simple socket server using threads
'''

import time
import socket
import sys
import struct
from collections import namedtuple

HOST = '192.168.1.10'   # Symbolic name, meaning all available interfaces
PORT = 8888 # Arbitrary non-privileged port


def SendStruct(sock):
	format_ = "ii"
	MyStruct = namedtuple("MyStruct", "light status")
	tuple_to_send = MyStruct(light=33, status=66)
	string_to_send = struct.pack(format_, *tuple_to_send._asdict().values())
	print("send")
	sock.send(string_to_send)
	print(string_to_send)
	#sock.send(b'0001')

def Listen():
	s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
	print 'Socket created'

	#Bind socket to local host and port
	try:
		s.bind((HOST, PORT))
	except socket.error as msg:
		print 'Bind failed. Error Code : ' + str(msg[0]) + ' Message ' + msg[1]
		sys.exit()

	print 'Socket bind complete'

	#Start listening on socket
	s.listen(10)
	print 'Socket now listening'

	#now keep talking with the client
	while 1:
		#wait to accept a connection - blocking call
		conn, addr = s.accept()
		print 'Connected with ' + addr[0] + ':' + str(addr[1])
		while 1:
			time.sleep(1)
			SendStruct(conn)
	s.close()


Listen()