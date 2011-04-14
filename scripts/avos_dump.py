#!/usr/bin/python
import sys
import io
import struct

def readuint(inb,offset):
	inb.seek(offset)
	(i,)=struct.unpack_from('<I',inb.read(4))
	return i

def main():
	if (len(sys.argv) != 2):
		print('usage: ' + sys.argv[0] + ' FILE')
		sys.exit(2)
	inf=sys.argv[1]
	kf='zImage'
	rf='initramfs.cpio.gz'
	print('loading \''+inf+'\' ...')
	ib = open(inf,'r+b')
	# read kernel size
	ksize = readuint(ib,0x94) - 256
	print('kernel size: %i' % ksize)
	# read initramfs size
	rsize = readuint(ib,0x98)
	print('initramfs size: %i' % rsize)
	# write kernel
	print('extracting \''+kf+'\' ...')
	ob = open(kf,'w+b')
	ib.seek(256)
	for i in range(0,ksize):
		ob.write(ib.read(1))
	ob.close()
	# write initramfs
	print('extracting \''+rf+'\' ...')
	ob = open(rf,'w+b')
	for i in range(0,rsize):
		ob.write(ib.read(1))
	ob.close()
	ib.close()

if __name__ == '__main__':
	main()

