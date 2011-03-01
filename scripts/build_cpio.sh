(cd initramfs && find . | cpio -H newc -o | gzip -9 > ../initramfs.cpio.gz)
