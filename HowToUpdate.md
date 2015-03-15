# Introduction #
A new firmware is release, so we need to make new rooting binaries. In this example the version is 2.4.19.

# Details #
  1. Download and flash new firmware from http://www.archos.com/support/support_tech/updates_fwm.html
  1. Flash SDE firmware if not already done and flash the [sysdump kernel](http://archos-gen8-sde-rooting.googlecode.com/files/sysdump.zip)
  1. Reboot into developer mode and wait for the reboot
  1. Reboot into recovery menu `-> Repair system -> Start USB MSC`
  1. Copy `avos.tar` to linux machine
  1. Open terminal on linux machine and filter avos.tar:
```
$ mkdir tmp
$ target=avos_2.4.19
$ mkdir $target
$ tar xvf avos.tar -C tmp
$ dd if=tmp/mnt/system/androidmerged.squashfs.secure \
   of=$target/androidmerged.squashfs bs=256 skip=1
$ cp tmp/mnt/rawfs/* $target/
$ tar cf $target.tar $target
$ rm -rf tmp
$ rm -rf $target
```
  1. Now you've got a file called `avos_2.4.19.tar` including kernels and squashfs image, unpack them:
```
$ tar xf avos_2.4.19.tar
$ archos-gen8-sde-rooting/scripts/avos_dump.py avos_2.4.19/init
```
  1. Create non-rw initramfs
```
$ mkdir avos_2.4.19_tmproot
$ cd avos_2.4.19_tmproot
$ cp ../initramfs.cpio.gz ../zImage .
$ ../archos-gen8-sde-rooting/scripts/extract_cpio.sh
$ cd initramfs
$ patch < ../../archos-gen8-sde-rooting/sources/tmproot_init.patch
$ tar xpf ../../archos-gen8-sde-rooting/sources/initramfs-binaries.tar
$ cd ..
$ ../archos-gen8-sde-rooting/scripts/build_cpio.sh
$ zip ../avos_2.4.19_tmproot.zip initramfs.cpio.gz zImage
$ cd ..
$ rm -rf avos_2.4.19_tmproot
```
  1. Create +rw initramfs
```
$ mkdir avos_2.4.19_sysroot
$ cd avos_2.4.19_sysroot
$ cp ../initramfs.cpio.gz ../zImage .
$ ../archos-gen8-sde-rooting/scripts/extract_cpio.sh
$ cd initramfs
$ patch < ../../archos-gen8-sde-rooting/sources/sysroot_init.patch
$ tar xpf ../../archos-gen8-sde-rooting/sources/initramfs-binaries.tar
$ cd ..
$ ../archos-gen8-sde-rooting/scripts/build_cpio.sh
$ zip ../avos_2.4.19_sysroot.zip initramfs.cpio.gz zImage
$ cd ..
$ rm -rf avos_2.4.19_sysroot
```
  1. Cleanup
```
$ rm -rf initramfs.cpio.gz zImage avos_2.4.19
```
  1. Upload `avos_2.4.19_tmproot.zip` and `avos_2.4.19_sysroot.zip` to project site