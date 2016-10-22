'''
    Simple socket server using threads
'''

import time
import socket
import sys
import struct
import collections

HOST = '192.168.1.11'   # Symbolic name, meaning all available interfaces
PORT = 8888 # Arbitrary non-privileged port


def SendRegisterStruct(sock):
	format_ = "<i"
	MyStruct = collections.namedtuple("MyStruct", "clienttype")
	tuple_to_send = MyStruct(clienttype=1)
	string_to_send = struct.pack(format_, *tuple_to_send._asdict().values())
	print("send")
	sock.send(string_to_send)
	#print(string_to_send)

def ReceiveControlStruct(sock):
	format_ = "<ii"
	MyStruct = collections.namedtuple("MyStruct", "light status")
	print("recv")
	data_received = sock.recv(struct.calcsize(format_))
	data_received_list = struct.unpack(format_, data_received)
	record = MyStruct(*data_received_list)
	print(record)

def Connect():
	s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
	print 'Socket created'

	#Bind socket to local host and port
	try:
		s.connect((HOST, PORT))
	except socket.error as msg:
		print 'Connect failed. Error Code : ' + str(msg[0]) + ' Message ' + msg[1]
		sys.exit()

	print 'Socket connect complete'

	SendRegisterStruct(s)

	#now keep talking with the client
	while 1:
		time.sleep(1)
		ReceiveControlStruct(s)
	s.close()


Connect()